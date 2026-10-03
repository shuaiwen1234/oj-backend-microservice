package com.wen.ojbackendmodel.model.codesandbox;

import com.wen.ojbackendmodel.model.dto.question.JudgeCase;
import com.wen.ojbackendmodel.model.dto.questionsubmit.JudgeInfo;
import com.wen.ojbackendmodel.model.entity.Question;
import com.wen.ojbackendmodel.model.entity.QuestionSubmit;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
/**
 * 上下文类 用于携带参数
 */
public class JudgeContext {

    /**
     * 题目预定的输入
     */
    private List<String> inPut;

    /**
     * 执行用户代码的输出
     */
    private List<String> outPut;

    /**
     * 对应的问题
     */
    private Question question;

    /**
     * 代码经过代码沙箱执行之后得到的执行信息
     */
    private JudgeInfo judgeInfo;

    /**
     * 这道题目对应的判题用例
     */
    private List<JudgeCase> judgeCaseList;

    /**
     * 题目的提交信息
     */
    private QuestionSubmit questionSubmit;


}
