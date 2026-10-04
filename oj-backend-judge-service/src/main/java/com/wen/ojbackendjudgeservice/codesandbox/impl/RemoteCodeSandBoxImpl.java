package com.wen.ojbackendjudgeservice.codesandbox.impl;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import com.wen.ojbackendcommon.common.ErrorCode;
import com.wen.ojbackendcommon.exception.BusinessException;
import com.wen.ojbackendjudgeservice.codesandbox.CodeSandBox;
import com.wen.ojbackendmodel.model.codesandbox.ExecuteCodeRequest;
import com.wen.ojbackendmodel.model.codesandbox.ExecuteCodeResponse;
import org.springframework.stereotype.Service;


@Service("codeSandBox")
/**
 * 远程调用代码沙箱
 */
public class RemoteCodeSandBoxImpl implements CodeSandBox {

    private final String SECRET_KEY = "mySecretKey";
    private final String SALT = "zhangziwenzuishaui";
    private final String AUTH_HEADER = "apiAuth";
    private final String TIME_STAMP_HEADER = "timeStamp";
    private final String RANDOM_HEADER = "random";


    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        String key = SECRET_KEY+SALT;
        String timeStamp = String.valueOf(System.currentTimeMillis());
        String random = RandomUtil.randomNumbers(6);
        String str = key+timeStamp+random;
        String accessKey = DigestUtil.md5Hex(str);



        String url ="http://localhost:8105/executeCode";
        HttpResponse httpResponse = HttpUtil.createPost(url)
                .body(JSONUtil.toJsonStr(executeCodeRequest))
                .header(AUTH_HEADER,accessKey)
                .header(TIME_STAMP_HEADER,timeStamp)
                .header(RANDOM_HEADER,random)
                .execute();

        if (httpResponse.getStatus() != 200) {
            throw new BusinessException(
                    ErrorCode.API_REQUEST_ERROR,
                    "调用沙箱失败，HTTP 状态码: " + httpResponse.getStatus()
            );
        }

        String response = httpResponse.body();
        if(StrUtil.isBlank(response)){
            throw new BusinessException(ErrorCode.API_REQUEST_ERROR,"executeCode 出现错误 得到的返回值为空字符串 "+ response);
        }
        return JSONUtil.toBean(response,ExecuteCodeResponse.class);
    }
}
