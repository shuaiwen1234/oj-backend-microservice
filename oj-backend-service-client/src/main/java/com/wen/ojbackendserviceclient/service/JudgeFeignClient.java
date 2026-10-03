package com.wen.ojbackendserviceclient.service;


import com.wen.ojbackendmodel.model.entity.QuestionSubmit;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * 判题服务
 */
@FeignClient(name = "oj-backend-judge-service",path = "/api/judge/inner")
public interface JudgeFeignClient {

    /**
     * 调用代码沙箱得到执行结果之后 进行判题
     * @param questionSubmitId 题解提交记录的id
     * @return
     */
    @PostMapping("/do/{questionSubmitId}")
    QuestionSubmit doJudge(@PathVariable(name = "questionSubmitId") Long questionSubmitId);
}
