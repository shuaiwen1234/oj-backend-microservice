package com.wen.ojbackendjudgeservice.linster;

import com.wen.ojbackendcommon.constant.MqConstant;
import com.wen.ojbackendjudgeservice.service.JudgeService;
import com.wen.ojbackendmodel.model.entity.QuestionSubmit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class JudgeLinster {

    @Autowired
    private JudgeService judgeService;
    @Autowired
    private RabbitTemplate rabbitTemplate;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = MqConstant.JUDGE_QUEUE),
            exchange = @Exchange(name = MqConstant.JUDGE_EXCHANGE),
            key = MqConstant.JUDGE_ROUNTING_KEY
    ))
    public void doJudge(String questionSubmitIdStr) {
        Long questionSubmitId = Long.parseLong(questionSubmitIdStr);
        try {
            judgeService.doJudge(questionSubmitId);
        } catch (Exception e) {
            log.error("判题服务处理失败，questionSubmitId: {}", questionSubmitId, e);
        }
    }

}
