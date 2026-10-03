package com.wen.ojbackendjudgeservice.codesandbox;


import com.wen.ojbackendmodel.model.codesandbox.ExecuteCodeRequest;
import com.wen.ojbackendmodel.model.codesandbox.ExecuteCodeResponse;

public interface CodeSandBox {

    /**
     * 执行代码
     * @param executeCodeRequest
     * @return
     */
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest);
}
