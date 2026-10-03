package com.wen.ojbackendjudgeservice.strategy;

import com.wen.ojbackendmodel.model.codesandbox.JudgeContext;
import com.wen.ojbackendmodel.model.dto.questionsubmit.JudgeInfo;

public interface JudgeStrategy {
    JudgeInfo doJudge(JudgeContext judgeContext);
}
