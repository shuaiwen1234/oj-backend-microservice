package com.wen.ojbackendjudgeservice.controller;

import com.wen.ojbackendjudgeservice.service.JudgeService;
import com.wen.ojbackendmodel.model.entity.QuestionSubmit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class JudgeController {

    @Autowired
    private JudgeService judgeService;
    /**
     * 调用代码沙箱得到执行结果之后 进行判题
     * @param questionSubmitId 题解提交记录的id
     * @return
     */
    @PostMapping("/inner/do/{questionSubmitId}")
    QuestionSubmit doJudge(@PathVariable(name = "questionSubmitId") Long questionSubmitId){
        return judgeService.doJudge(questionSubmitId);
    }
}
