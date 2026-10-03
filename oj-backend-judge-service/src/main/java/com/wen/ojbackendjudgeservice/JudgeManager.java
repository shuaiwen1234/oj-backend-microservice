package com.wen.ojbackendjudgeservice;

import com.wen.ojbackendjudgeservice.strategy.DefaultJudgeStrategy;
import com.wen.ojbackendjudgeservice.strategy.JavaJudgeStrategy;
import com.wen.ojbackendjudgeservice.strategy.JudgeStrategy;
import com.wen.ojbackendmodel.model.codesandbox.JudgeContext;
import com.wen.ojbackendmodel.model.dto.questionsubmit.JudgeInfo;
import com.wen.ojbackendmodel.model.enums.QuestionSubmitLanguageEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 这个类用来根据传入的judgeContext里面的编程语言来选择一个判题策略(默认或者java)来执行doJudge
 * 使用了策略模式
 *
 * ⚠️ 策略对象必须由 Spring 注入，不能用 new：
 * 策略实现类内部用 @Autowired 注入 CodeSandBoxProxy、QuestionMapper，
 * new 出来的对象这些字段全是 null，判题走到写库那一步就会 NullPointerException。
 */
@Service
@RequiredArgsConstructor
public class JudgeManager {

    private final JavaJudgeStrategy javaJudgeStrategy;
    private final DefaultJudgeStrategy defaultJudgeStrategy;


    public JudgeInfo doJudge(JudgeContext judgeContext) {
        String language = judgeContext.getQuestionSubmit().getLanguage();
        JudgeStrategy judgeStrategy = QuestionSubmitLanguageEnum.JAVA.getValue().equals(language)
                ? javaJudgeStrategy
                : defaultJudgeStrategy;

        return  judgeStrategy.doJudge(judgeContext);
    }
}
