package com.wen.ojbackendquestionservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wen.ojbackendmodel.model.dto.questionsubmit.JudgeInfo;
import com.wen.ojbackendmodel.model.entity.QuestionSubmit;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

/**
* @author a1472
* @description 针对表【question_submit(题目提交)】的数据库操作Mapper
* @createDate 2026-09-16 22:39:43
* @Entity com.wen.oj.model.entity.QuestionSubmit
*/
@Mapper
public interface QuestionSubmitMapper extends BaseMapper<QuestionSubmit> {

    void cleanJudgeMachineMissing(@Param(value = "time") LocalDateTime time, @Param(value = "judgeInfo")String judgeInfo);

    void cleanJudgeingTask(@Param(value = "time")LocalDateTime threeMinutesBefroe, @Param(value = "judgeInfo")String jsonStr);
}




