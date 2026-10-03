package com.wen.ojbackendquestionservice.publisher;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.concurrent.ListenableFutureCallback;

import javax.annotation.PostConstruct;

@Slf4j
@Component
public class MessagePublisher {
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private RedisTemplate redisTemplate;

    public static final String MQ_FAILURE_KEY = "mqFailure:questionSubmitSet";

    //开启returnCallBack
    @PostConstruct
    public void init(){
        rabbitTemplate.setReturnsCallback(new RabbitTemplate.ReturnsCallback() {
            @Override
            public void returnedMessage(ReturnedMessage returned) {
                log.error("触发return callback,");
                log.debug("exchange: {}", returned.getExchange());
                log.debug("routingKey: {}", returned.getRoutingKey());
                log.debug("message: {}", returned.getMessage());
                log.debug("replyCode: {}", returned.getReplyCode());
                log.debug("replyText: {}", returned.getReplyText());
            }
        });
    }



    public void sendMessage(String exchange,String rountingKey,String message){
        String questionSubmitId = message;

        // 1.创建CorrelationData
        CorrelationData cd = new CorrelationData(questionSubmitId);
        // 2.给Future添加ConfirmCallback
        cd.getFuture().addCallback(new ListenableFutureCallback<CorrelationData.Confirm>() {
            @Override
            public void onFailure(Throwable ex) {
                // 2.1.Future发生异常时的处理逻辑，基本不会触发
                log.error("send message fail", ex);
            }
            @Override
            public void onSuccess(CorrelationData.Confirm result) {
                // 2.2.Future接收到回执的处理逻辑，参数中的result就是回执内容
                if(result.isAck()){ // result.isAck()，boolean类型，true代表ack回执，false 代表 nack回执
                    log.debug("发送消息成功，收到 ack!");
                    redisTemplate.opsForSet().remove(MQ_FAILURE_KEY,questionSubmitId);
                }else{ // result.getReason()，String类型，返回nack时的异常描述
                    log.error("发送消息失败 题目提交id为: {}",questionSubmitId);
                    log.error("发送消息失败，收到 nack, reason : {}", result.getReason());
                    recordFailureToRedis(questionSubmitId);
                }
            }
        });

        try {
            rabbitTemplate.convertAndSend(exchange,rountingKey,message,cd);
        } catch (Exception e) {
            recordFailureToRedis(questionSubmitId);
        }
    }


    public void recordFailureToRedis(String questionSubmitId){
        try{
            redisTemplate.opsForSet().add(MQ_FAILURE_KEY,questionSubmitId);
        }catch (Exception e){
            log.error("消息保存到redis失败: ",e);
        }
    }
}
