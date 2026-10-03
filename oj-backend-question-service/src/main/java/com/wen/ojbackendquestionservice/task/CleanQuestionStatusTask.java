package com.wen.ojbackendquestionservice.task;

import cn.hutool.json.JSONUtil;
import com.wen.ojbackendcommon.constant.MqConstant;
import com.wen.ojbackendmodel.model.dto.questionsubmit.JudgeInfo;
import com.wen.ojbackendmodel.model.entity.QuestionSubmit;
import com.wen.ojbackendmodel.model.enums.JudgeInfoMessageEnum;
import com.wen.ojbackendquestionservice.mapper.QuestionSubmitMapper;
import com.wen.ojbackendquestionservice.publisher.MessagePublisher;
import com.wen.ojbackendquestionservice.service.QuestionSubmitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class CleanQuestionStatusTask {

    @Autowired
    private QuestionSubmitMapper questionSubmitMapper;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private MessagePublisher messagePublisher;

    public static final String MQ_FAILURE_KEY = "mqFailure:questionSubmitSet";

    /**
     * 每三分钟去扫描数据库 发现有数据的更新时间在当前时间的三分钟之前并且状态为待判题 则更新他的状态
     */
    @Scheduled(cron = "0 0/3 * * * ?") // 每 3 分钟执行一次
    public void cleanJudgeMachineMissingTask(){
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime threeMinutesBefroe = now.minusMinutes(3);
        JudgeInfo judgeInfo = new JudgeInfo();
        judgeInfo.setMessage("判题机未响应");
        judgeInfo.setTime(-1L);
        judgeInfo.setMemory(-1L);
        judgeInfo.setStackSize(-1L);

        questionSubmitMapper.cleanJudgeMachineMissing(threeMinutesBefroe, JSONUtil.toJsonStr(judgeInfo));
    }


    /**
     * 每三分钟去扫描数据库 发现有数据的更新时间在当前时间的三分钟之前并且状态为判题中 则更新他的状态
     */
    @Scheduled(cron = "0 0/3 * * * ?") // 每 3 分钟执行一次
    public void cleanJudgeingTask(){
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime threeMinutesBefroe = now.minusMinutes(3);
        JudgeInfo judgeInfo = new JudgeInfo();
        judgeInfo.setMessage(JudgeInfoMessageEnum.TIME_LIMIT_EXCEEDED.getValue());
        judgeInfo.setTime(-1L);
        judgeInfo.setMemory(-1L);
        judgeInfo.setStackSize(-1L);

        questionSubmitMapper.cleanJudgeingTask(threeMinutesBefroe,JSONUtil.toJsonStr(judgeInfo));
    }

    /**
     * 每天凌晨三点去扫描数据库 将RabbitMQ发送失败的消息重新发送
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void reSendFailMessage(){
        Set<String> questionSubmitIds = redisTemplate.opsForSet().members(MQ_FAILURE_KEY);
        if(questionSubmitIds == null ||questionSubmitIds.isEmpty()){
            return;
        }
        List<Long> questionSubmitIdsList = questionSubmitIds.stream().map(Long::valueOf).collect(Collectors.toList());
        List<QuestionSubmit> questionSubmits = questionSubmitMapper.selectBatchIds(questionSubmitIdsList);
        for(QuestionSubmit questionSubmit : questionSubmits){
            //只有状态为待判题或者判题机未响应的消息需要重新发送
            if(questionSubmit.getStatus()==0 || questionSubmit.getStatus()==4){
                messagePublisher.sendMessage(MqConstant.JUDGE_EXCHANGE,MqConstant.JUDGE_ROUNTING_KEY,questionSubmit.getId().toString());
            }
        }

    }
}
