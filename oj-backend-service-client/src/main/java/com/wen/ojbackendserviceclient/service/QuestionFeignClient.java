package com.wen.ojbackendserviceclient.service;


import com.wen.ojbackendmodel.model.entity.Question;
import com.wen.ojbackendmodel.model.entity.QuestionSubmit;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

/**
* @author a1472
* @description 针对表【question(题目)】的数据库操作Service
* @createDate 2026-09-16 22:33:46
*/
@FeignClient(name = "oj-backend-question-service",path = "/api/question/inner")
public interface QuestionFeignClient{

   /**
    * 根据questionId获取question对象
    * @param questionId
    * @return
    */
   @GetMapping("/get/{questionId}")
   Question getQuestionById(@PathVariable(name = "questionId") Long questionId);

   /**
    * 根据questionSubmitId获取questionSubmit对象
    * @param questionSubmitId
    * @return
    */
   @GetMapping("/questionSubmit/get/{questionSubmitId}")
   QuestionSubmit getQuestionSubmitById(@PathVariable(name = "questionSubmitId") Long questionSubmitId);

   /**
    * 更新questionSubmit
    * @param questionSubmitUpdate
    * @return
    */
   @PostMapping("/questionSubmit/update")
   Boolean updateQuestionSubmitById(@RequestBody QuestionSubmit questionSubmitUpdate);

   /**
    * 将对应题目的通过人数加一
    * @param questionId
    * @return
    */
   @PutMapping("/incr/{questionId}")
   Boolean increaseQuestionPassedNum(@PathVariable(name = "questionId") Long questionId);


}
