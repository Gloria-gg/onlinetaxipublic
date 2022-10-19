package com.mashibing.apipassenger.service;

import com.mashibing.apipassenger.remote.ServicePassengerUserClient;
import com.mashibing.apipassenger.remote.ServiceVerificationCodeClient;
import com.mashibing.internalcommon.constant.CommonStatusEnum;
import com.mashibing.internalcommon.dto.ResponseResult;
import com.mashibing.internalcommon.request.VerificationCodeDTO;
import com.mashibing.internalcommon.response.NumberCodeResponse;
import com.mashibing.internalcommon.response.TokenResponse;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * @Author: Gloria
 * @Description:
 * @Date: Created in 9:43 AM 9/30/22
 */
@Service
public class VerificationCodeService {

    /**
     * 存入redis的验证码的key的前缀
     */
    private String verificationCodePrefix = "passenger_verification_code_";

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private ServiceVerificationCodeClient serviceVerificationCodeClient;

    @Autowired
    private ServicePassengerUserClient servicePassengerUserClient;

    /**
     * 根据乘客电话号码进行redis的key生成
     * 因为有拷贝行为，所以对有拷贝行为都生成一个方法
     * 万一之后改变规则，也好进行统一更改
     * （算是一个开发中的小技巧）
     *
     * @param passengerPhone
     * @return
     */
    private String generateRedisKeyByPassengerPhone(String passengerPhone) {
        return verificationCodePrefix + passengerPhone;
    }

    /**
     * 该方法获取乘客手机号，生成一个六位验证码
     *
     * @param passengerPhone
     * @return
     */
    public ResponseResult generateCode(String passengerPhone) {
        //调用验证码服务，获取验证码
        ResponseResult<NumberCodeResponse> numberCodeResponse = serviceVerificationCodeClient.getNumberCode(6);
        int numberCode = numberCodeResponse.getData().getNumberCode();

        //存入redis
        //key：前缀+手机号   value：验证码   ttl：过期时间，在redis设置中统一设置
        String key = generateRedisKeyByPassengerPhone(passengerPhone);
        //存入redis中
        stringRedisTemplate.opsForValue().set(key, numberCode + "", 2, TimeUnit.MINUTES);

        //通过短信服务商，将对应验证码发送到手机上。阿里短信服务、腾讯短信通、华信、容联等。

        //返回成功或者失败码
        return ResponseResult.success();
    }

    /**
     * 接收乘客手机号以及输入的验证码
     * 对输入的验证码进行验证码校验以及是否超过验证时间校验
     * 返回结果，以及，若成功返回token
     *
     * @param passengerPhone
     * @param verificationCode
     * @return
     */
    public ResponseResult checkVerificationCode(String passengerPhone, String verificationCode) {
        //根据手机号去redis中获取对应验证码。第一步：生成key
        String key = generateRedisKeyByPassengerPhone(passengerPhone);

        //第二步：根据key获取value
        String redisCode = stringRedisTemplate.opsForValue().get(key);

        System.out.println("从redis中获取到的验证码是：" + redisCode);

        //redis验证码与输入验证码进行校验
        System.out.println("redis验证码与输入验证码进行校验");
        if (StringUtils.isBlank(redisCode)) {
            //验证码为空，提示L验证码不正确
            return ResponseResult.fail(CommonStatusEnum.VERIFICATION_CODE_ERROR.getCode(), CommonStatusEnum.VERIFICATION_CODE_ERROR.getMessage());
        }

        if (!verificationCode.trim().equals(redisCode.trim())) {
            //若用户输入验证码和redis中验证码不一致，也进行报错提示
            return ResponseResult.fail(CommonStatusEnum.VERIFICATION_CODE_ERROR.getCode(), CommonStatusEnum.VERIFICATION_CODE_ERROR.getMessage());
        }


        //若原来有用户，那么返回登录token；若没有用户，那么直接插入一条新数据,这里需要调用另一个服务
        VerificationCodeDTO verificationCodeDTO = new VerificationCodeDTO();
        verificationCodeDTO.setPassengerPhone(passengerPhone);
        servicePassengerUserClient.logOrRegister(verificationCodeDTO);

        //颁发token令牌
        System.out.println("颁发token令牌");

        //设置返回值
        TokenResponse tokenResponse = new TokenResponse();
        tokenResponse.setToken("token value");

        return ResponseResult.success(tokenResponse);
    }
}
