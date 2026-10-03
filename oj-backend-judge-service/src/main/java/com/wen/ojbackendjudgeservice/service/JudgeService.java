package com.wen.ojbackendjudgeservice.service;


import com.wen.ojbackendmodel.model.entity.QuestionSubmit;

/**
 * 判题服务
 */
public interface JudgeService {

    /**
     * 调用代码沙箱得到执行结果之后 进行判题
     * @param QuestionSubmitId 题解提交记录的id
     * @return
     */
    public QuestionSubmit doJudge(Long QuestionSubmitId);
}
