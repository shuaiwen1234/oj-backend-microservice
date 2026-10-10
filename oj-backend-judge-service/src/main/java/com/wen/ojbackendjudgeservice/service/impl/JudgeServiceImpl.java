package com.wen.ojbackendjudgeservice.service.impl;

import cn.hutool.json.JSONUtil;
import com.wen.ojbackendcommon.common.ErrorCode;
import com.wen.ojbackendcommon.exception.BusinessException;
import com.wen.ojbackendjudgeservice.JudgeManager;
import com.wen.ojbackendjudgeservice.codesandbox.impl.CodeSandBoxProxy;
import com.wen.ojbackendjudgeservice.service.JudgeService;
import com.wen.ojbackendmodel.model.codesandbox.ExecuteCodeRequest;
import com.wen.ojbackendmodel.model.codesandbox.ExecuteCodeResponse;
import com.wen.ojbackendmodel.model.codesandbox.JudgeContext;
import com.wen.ojbackendmodel.model.dto.question.JudgeCase;
import com.wen.ojbackendmodel.model.dto.questionsubmit.JudgeInfo;
import com.wen.ojbackendmodel.model.entity.Question;
import com.wen.ojbackendmodel.model.entity.QuestionSubmit;
import com.wen.ojbackendmodel.model.enums.QuestionSubmitStatusEnum;
import com.wen.ojbackendserviceclient.service.QuestionFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class JudgeServiceImpl implements JudgeService {

    @Autowired
    private QuestionFeignClient questionFeignClient;
    @Autowired
    private CodeSandBoxProxy codeSandBoxProxy;
    @Autowired
    private JudgeManager judgeManager;

    @Override
    public QuestionSubmit doJudge(Long questionSubmitId) {
        //1. 根据传入的题目提交记录id 获取对应的题目 提交信息
        QuestionSubmit questionSubmit = questionFeignClient.getQuestionSubmitById(questionSubmitId);
        if(questionSubmit == null){
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR,"题目提交信息不存在");
        }
        Long questionId = questionSubmit.getQuestionId();
        Question question = questionFeignClient.getQuestionById(questionId);
        if(question == null){
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR,"题目信息不存在");
        }

        //2. 判断这个题目提交信息的状态 如果不是待判题就不用判题了
        if(QuestionSubmitStatusEnum.WAITING.getValue()!=questionSubmit.getStatus()&&QuestionSubmitStatusEnum.JUDGE_MACHINETIME_OUT.getValue()!=questionSubmit.getStatus()){
            throw new BusinessException(ErrorCode.OPERATION_ERROR,"题目正在判题或已判题结束，请勿重复操作");
        }

        //2.1 修改题目提交信息为判题中
        QuestionSubmit questionSubmitUpdate = new QuestionSubmit();
        questionSubmitUpdate.setId(questionSubmit.getId());
        questionSubmitUpdate.setStatus(QuestionSubmitStatusEnum.RUNNING.getValue());

        //2.2 更新判题状态
        boolean b = questionFeignClient.updateQuestionSubmitById(questionSubmitUpdate);
        if(!b){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"题目提交状态更新失败 请稍后再试");
        }


        //3. 获取输入用例
        String judgeCaseStr = question.getJudgeCase();
        List<JudgeCase> judgeCaseList = JSONUtil.toList(judgeCaseStr, JudgeCase.class);
        List<String> inPut = judgeCaseList.stream().map(JudgeCase::getInPut).collect(Collectors.toList());

        ExecuteCodeRequest executeCodeRequest = ExecuteCodeRequest.builder()
                .code(questionSubmit.getCode())
                .input(inPut)
                .language(questionSubmit.getLanguage())
                .build();

        //3.1 调用沙箱 获取到执行结果
        ExecuteCodeResponse executeCodeResponse = codeSandBoxProxy.executeCode(executeCodeRequest);
        //3.2 对结果做出判断
        JudgeContext judgeContext = JudgeContext.builder()
                .inPut(inPut)
                .outPut(executeCodeResponse.getOutput())
                .question(question)
                .questionSubmit(questionSubmit)
                .judgeInfo(executeCodeResponse.getJudgeInfo())
                .judgeCaseList(judgeCaseList).build();

        //4 进行判题
        JudgeInfo judgeInfo = judgeManager.doJudge(judgeContext);

        //4.1 修改题目提交信息为判题结束
        questionSubmitUpdate.setStatus(QuestionSubmitStatusEnum.SUCCEED.getValue());
        questionSubmitUpdate.setJudgeInfo(JSONUtil.toJsonStr(judgeInfo));

        //4.2 更新判题状态
         b = questionFeignClient.updateQuestionSubmitById(questionSubmitUpdate);
        if(!b){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"题目提交状态更新失败 请稍后再试");
        }

        QuestionSubmit questionSubmitResult = questionFeignClient.getQuestionSubmitById(questionSubmitId);
        return questionSubmitResult;

    }
}
