package com.wen.ojbackendquestionservice.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wen.ojbackendcommon.common.ErrorCode;
import com.wen.ojbackendcommon.constant.CommonConstant;
import com.wen.ojbackendcommon.exception.BusinessException;
import com.wen.ojbackendcommon.exception.ThrowUtils;
import com.wen.ojbackendcommon.utils.SqlUtils;
import com.wen.ojbackendmodel.model.dto.question.QuestionQueryRequest;
import com.wen.ojbackendmodel.model.entity.Question;
import com.wen.ojbackendmodel.model.entity.User;
import com.wen.ojbackendmodel.model.vo.QuestionVO;
import com.wen.ojbackendmodel.model.vo.UserVO;
import com.wen.ojbackendquestionservice.mapper.QuestionMapper;
import com.wen.ojbackendquestionservice.mapper.QuestionSubmitMapper;
import com.wen.ojbackendquestionservice.service.QuestionService;
import com.wen.ojbackendserviceclient.service.UserFeignClient;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
* @author a1472
* @description 针对表【question(题目)】的数据库操作Service实现
* @createDate 2026-09-16 22:33:46
*/
@Service("QuestionService")
public class QuestionServiceImpl extends ServiceImpl<QuestionMapper, Question> implements QuestionService {
    @Autowired
    private UserFeignClient userFeignClient;

    @Autowired
    private QuestionMapper questionMapper;

    @Autowired
    private QuestionSubmitMapper questionSubmitMapper;

    @Override
    public void validQuestion(Question question, boolean add) {
        if (question == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        String title = question.getTitle();
        String content = question.getContent();
        String tags = question.getTags();
        // 创建时，参数不能为空
        if (add) {
            ThrowUtils.throwIf(StringUtils.isAnyBlank(title, content, tags), ErrorCode.PARAMS_ERROR);
        }
        // 有参数则校验
        if (StringUtils.isNotBlank(title) && title.length() > 80) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "标题过长");
        }
        if (StringUtils.isNotBlank(content) && content.length() > 8192) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "内容过长");
        }
        if (StringUtils.isNotBlank(question.getAnswer()) && question.getAnswer().length() > 8192) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "答案长度过长");
        }
        if (StringUtils.isBlank(question.getTags())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请至少输入一个标签");
        }
        if (question.getTags().length() > 500) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "标签过多");
        }
        if (StringUtils.isNotBlank(question.getJudgeCase())&&question.getJudgeCase().length()>500) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "测试用例输入过多");
        }
        if (question.getJudgeConfig()!=null&&question.getJudgeConfig().toString().length()>500) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "判题配置过长");
        }

    }

    /**
     * 获取查询包装类
     * 判断用户可能根据哪些字段进行查询(用户根据哪些字段进行查询，前端传进来的对象就包含哪些字段，再根据这个对象得到mp使用的查询wrapper)
     * @param questionQueryRequest
     * @return
     */
    @Override
    public QueryWrapper<Question> getQueryWrapper(QuestionQueryRequest questionQueryRequest) {
        QueryWrapper<Question> queryWrapper = new QueryWrapper<>();
        if (questionQueryRequest == null) {
            return queryWrapper;
        }

        String sortField = questionQueryRequest.getSortField();
        String sortOrder = questionQueryRequest.getSortOrder();
        Long id = questionQueryRequest.getId();
        String title = questionQueryRequest.getTitle();
        String content = questionQueryRequest.getContent();
        List<String> tagList = questionQueryRequest.getTags();
        String answer = questionQueryRequest.getAnswer();
        Long userId = questionQueryRequest.getUserId();
        // 拼接查询条件

        queryWrapper.like(StringUtils.isNotBlank(title), "title", title);
        queryWrapper.like(StringUtils.isNotBlank(content), "content", content);
        queryWrapper.like(StringUtils.isNotBlank(answer), "answer", answer);
        if (CollUtil.isNotEmpty(tagList)) {
            for (String tag : tagList) {
                queryWrapper.like("tags", "\"" + tag + "\"");
            }
        }
        queryWrapper.eq(ObjectUtils.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjectUtils.isNotEmpty(userId), "userId", userId);
        queryWrapper.orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }

    @Override
    public QuestionVO getQuestionVO(Question question, HttpServletRequest request) {
        QuestionVO questionVO = QuestionVO.objToVo(question);
        long questionId = question.getId();
        // 1. 关联查询用户信息
        Long userId = question.getUserId();
        User user = null;
        if (userId != null && userId > 0) {
            user = userFeignClient.getById(userId);
        }
        UserVO userVO = userFeignClient.getUserVO(user);
        questionVO.setUserVO(userVO);

        return questionVO;
    }

    @Override
    public Page<QuestionVO> getQuestionVOPage(Page<Question> questionPage, HttpServletRequest request) {
        List<Question> questionList = questionPage.getRecords();
        Page<QuestionVO> questionVOPage = new Page<>(questionPage.getCurrent(), questionPage.getSize(), questionPage.getTotal());
        if (CollUtil.isEmpty(questionList)) {
            return questionVOPage;
        }
        // 1. 关联查询用户信息
        Set<Long> userIdSet = questionList.stream().map(Question::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userFeignClient.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));

        // 填充信息
        List<QuestionVO> questionVOList = questionList.stream().map(question -> {
            QuestionVO questionVO = QuestionVO.objToVo(question);
            Long userId = question.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            questionVO.setUserVO(userFeignClient.getUserVO(user));
            return questionVO;
        }).collect(Collectors.toList());
        questionVOPage.setRecords(questionVOList);
        return questionVOPage;
    }

    @Override
    public Boolean increaseQuestionPassedNum(Long questionId) {
        LambdaUpdateWrapper<Question> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Question::getId, questionId);
        wrapper.setSql("acceptedNum = acceptedNum+1");

        int update = questionMapper.update(null, wrapper);

        return update == 1;

    }

}




