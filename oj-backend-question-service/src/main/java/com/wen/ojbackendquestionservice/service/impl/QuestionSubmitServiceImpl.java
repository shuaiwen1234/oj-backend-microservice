package com.wen.ojbackendquestionservice.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wen.ojbackendcommon.common.ErrorCode;
import com.wen.ojbackendcommon.constant.CommonConstant;
import com.wen.ojbackendcommon.constant.MqConstant;
import com.wen.ojbackendcommon.exception.BusinessException;
import com.wen.ojbackendcommon.utils.SqlUtils;
import com.wen.ojbackendmodel.model.dto.questionsubmit.QuestionSubmitAddRequest;
import com.wen.ojbackendmodel.model.dto.questionsubmit.QuestionSubmitQueryRequest;
import com.wen.ojbackendmodel.model.entity.Question;
import com.wen.ojbackendmodel.model.entity.QuestionSubmit;
import com.wen.ojbackendmodel.model.entity.User;
import com.wen.ojbackendmodel.model.enums.QuestionSubmitLanguageEnum;
import com.wen.ojbackendmodel.model.enums.QuestionSubmitStatusEnum;
import com.wen.ojbackendmodel.model.enums.UserRoleEnum;
import com.wen.ojbackendmodel.model.vo.LoginUserVO;
import com.wen.ojbackendmodel.model.vo.QuestionSubmitVO;
import com.wen.ojbackendmodel.model.vo.UserVO;
import com.wen.ojbackendquestionservice.mapper.QuestionMapper;
import com.wen.ojbackendquestionservice.mapper.QuestionSubmitMapper;
import com.wen.ojbackendquestionservice.publisher.MessagePublisher;
import com.wen.ojbackendquestionservice.service.QuestionService;
import com.wen.ojbackendquestionservice.service.QuestionSubmitService;
import com.wen.ojbackendserviceclient.service.JudgeFeignClient;
import com.wen.ojbackendserviceclient.service.UserFeignClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.concurrent.ListenableFutureCallback;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
* @author a1472
* @description 针对表【questionsubmit_submit(题目提交)】的数据库操作Service实现
* @createDate 2026-09-16 22:39:43
*/
@Service("QuestionSubmitService")
@Slf4j
public class QuestionSubmitServiceImpl extends ServiceImpl<QuestionSubmitMapper, QuestionSubmit>
    implements QuestionSubmitService {

    @Autowired
    private UserFeignClient userFeignClient;
    @Autowired
    private QuestionService questionService;
    @Autowired
    @Lazy
    private JudgeFeignClient judgeFeignClient;
    @Autowired
    private QuestionMapper questionMapper;
    @Autowired
    private MessagePublisher messagePublisher;

    /**
     * 题目提交
     *
     * @param questionsubmitSubmitAddRequest 题目提交信息
     * @param loginUser
     * @return 返回插入的记录(题解)的id
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long doQuestionSubmit(QuestionSubmitAddRequest questionsubmitSubmitAddRequest, User loginUser) {
        // 判断实体是否存在，根据类别获取实体
        Question question = questionService.getById(questionsubmitSubmitAddRequest.getQuestionId());
        if (question == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        if(loginUser==null){
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        if(loginUser.getId()==null){
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        //获取用户id
        long userId = loginUser.getId();

        QuestionSubmit questionsubmitSubmit = new QuestionSubmit();
        QuestionSubmitLanguageEnum languageEnum = QuestionSubmitLanguageEnum.getEnumByValue(questionsubmitSubmitAddRequest.getLanguage());
        if(languageEnum == null){
            throw new BusinessException(ErrorCode.OPERATION_ERROR,"暂时不支持该编程语言");
        }

        questionsubmitSubmit.setLanguage(questionsubmitSubmitAddRequest.getLanguage());

        questionsubmitSubmit.setCode(questionsubmitSubmitAddRequest.getCode());
        questionsubmitSubmit.setJudgeInfo("{}");
        //设置初始状态
        questionsubmitSubmit.setStatus(QuestionSubmitStatusEnum.WAITING.getValue());
        questionsubmitSubmit.setQuestionId(questionsubmitSubmitAddRequest.getQuestionId());
        questionsubmitSubmit.setUserId(userId);
        questionsubmitSubmit.setCreateTime(new Date());
        questionsubmitSubmit.setUpdateTime(new Date());

        boolean isSuccess = this.save(questionsubmitSubmit);
        if(!isSuccess){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"插入失败");
        }

        UpdateWrapper<Question> questionUpdateWrapper = new UpdateWrapper<>();
        questionUpdateWrapper.eq("id", question.getId());
        questionUpdateWrapper.setSql("submitNum = submitNum+1");
        questionMapper.update(null, questionUpdateWrapper);


        // 3. 确保事务 COMMIT 成功后再触发异步判题
        Long submitId = questionsubmitSubmit.getId();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                messagePublisher.sendMessage(MqConstant.JUDGE_EXCHANGE,MqConstant.JUDGE_ROUNTING_KEY,submitId.toString());
            }
        });
        /*//异步的去判题
        CompletableFuture.runAsync(() -> judgeFeignClient.doJudge(questionsubmitSubmit.getId()))
                .exceptionally(e -> {
                    log.error("判题失败, submitId={}", questionsubmitSubmit.getId(), e);
                    return null;
                });*/

        return submitId;
    }

    @Override
    public QueryWrapper<QuestionSubmit> getQueryWrapper(QuestionSubmitQueryRequest questionsubmitQueryRequest) {
        QueryWrapper<QuestionSubmit> queryWrapper = new QueryWrapper<>();
        if (questionsubmitQueryRequest == null) {
            return queryWrapper;
        }

        //获取字段
        Long questionId = questionsubmitQueryRequest.getQuestionId();
        Long userId = questionsubmitQueryRequest.getUserId();
        Integer status = questionsubmitQueryRequest.getStatus();
        String language = questionsubmitQueryRequest.getLanguage();
        String sortField = questionsubmitQueryRequest.getSortField();
        String sortOrder = questionsubmitQueryRequest.getSortOrder();


        // 拼接查询条件
        queryWrapper.eq(ObjectUtils.isNotEmpty(questionId), "questionId", questionId);
        queryWrapper.eq(ObjectUtils.isNotEmpty(userId), "userId", userId);
        queryWrapper.eq(StrUtil.isNotBlank(language), "language", language);
        queryWrapper.eq(QuestionSubmitStatusEnum.getEnumByValue(status)!=null, "status", status);
        queryWrapper.orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }

    @Override
    public QuestionSubmitVO getQuestionSubmitVO(QuestionSubmit questionsubmit, HttpServletRequest request) {

        QuestionSubmitVO questionsubmitVO = QuestionSubmitVO.objToVo(questionsubmit);
        //获取当前登录的用户
        LoginUserVO loginUser = userFeignClient.getLoginUser();

        // 1. 关联查询用户信息
        Long userId = questionsubmit.getUserId();
        User user = null;
        if (userId != null && userId > 0) {
            user = userFeignClient.getById(userId);
        }
        UserVO userVO = userFeignClient.getUserVO(user);
        questionsubmitVO.setUserVO(userVO);


        //2. 进行脱敏 仅本人和管理员能看见自己提交的代码
        if(loginUser==null){
            questionsubmitVO.setCode(null);
        }
        else if(!loginUser.getId().equals(userId)&&!UserRoleEnum.ADMIN.equals(UserRoleEnum.getEnumByValue(loginUser.getUserRole()))){
            questionsubmitVO.setCode(null);
        }
        return questionsubmitVO;
    }

    @Override
    public Page<QuestionSubmitVO> getQuestionSubmitVOPage(Page<QuestionSubmit> questionsubmitPage, HttpServletRequest request) {
        List<QuestionSubmit> questionsubmitList = questionsubmitPage.getRecords();
        Page<QuestionSubmitVO> questionsubmitVOPage = new Page<>(questionsubmitPage.getCurrent(), questionsubmitPage.getSize(), questionsubmitPage.getTotal());
        if (CollUtil.isEmpty(questionsubmitList)) {
            return questionsubmitVOPage;
        }
        //获取当前登录的用户
        LoginUserVO loginUser = userFeignClient.getLoginUser();

        // 1. 关联查询用户信息
        Set<Long> userIdSet = questionsubmitList.stream().map(QuestionSubmit::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userFeignClient.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));

        // 填充信息
        List<QuestionSubmitVO> questionsubmitVOList = questionsubmitList.stream().map(questionsubmit -> {
            QuestionSubmitVO questionsubmitVO = QuestionSubmitVO.objToVo(questionsubmit);
            Long userId = questionsubmit.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            questionsubmitVO.setUserVO(userFeignClient.getUserVO(user));

            //2. 进行脱敏 仅本人和管理员能看见自己提交的代码
            if(loginUser==null||loginUser.getId() == null){
                questionsubmitVO.setCode(null);
            }
            else if(!loginUser.getId().equals(userId)&&!UserRoleEnum.ADMIN.equals(UserRoleEnum.getEnumByValue(loginUser.getUserRole()))){
                questionsubmitVO.setCode(null);
            }
            return questionsubmitVO;
        }).collect(Collectors.toList());
        questionsubmitVOPage.setRecords(questionsubmitVOList);
        return questionsubmitVOPage;
    }



    /**
     * 封装了事务的方法
     *
     * @param userId
     * @param questionsubmitId
     * @return
     */
    // TODO 这里以后记得改 自调用会跳过代理使事务失效
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int doQuestionSubmitInner(long userId, long questionsubmitId) {
        QuestionSubmit questionsubmitSubmit = new QuestionSubmit();
        questionsubmitSubmit.setUserId(userId);
        questionsubmitSubmit.setQuestionId(questionsubmitId);
        QueryWrapper<QuestionSubmit> thumbQueryWrapper = new QueryWrapper<>(questionsubmitSubmit);
        QuestionSubmit oldQuestionSubmit = this.getOne(thumbQueryWrapper);
        boolean result;
        // 已提交题目
        if (oldQuestionSubmit != null) {
            result = this.remove(thumbQueryWrapper);
            if (result) {
                // 题目提交数 - 1
                // TODO 自调用
                result = this.update()
                        .eq("id", questionsubmitId)
                        .gt("thumbNum", 0)
                        .setSql("thumbNum = thumbNum - 1")
                        .update();
                return result ? -1 : 0;
            } else {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR);
            }
        } else {
            // 未提交题目
            result = this.save(questionsubmitSubmit);
            if (result) {
                // 题目提交数 + 1
                // TODO 自调用
                result = this.update()
                        .eq("id", questionsubmitId)
                        .setSql("thumbNum = thumbNum + 1")
                        .update();
                return result ? 1 : 0;
            } else {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR);
            }
        }
    }


}




