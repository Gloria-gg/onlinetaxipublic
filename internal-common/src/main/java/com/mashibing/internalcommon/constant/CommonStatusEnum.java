package com.mashibing.internalcommon.constant;

import lombok.Getter;

/**
 * @Author: Gloria
 * @Description: 所有子项目状态码都定义在这里
 * @Date: Created in 4:32 PM 9/30/22
 */
@Getter
public enum CommonStatusEnum {

    /**
     * 验证码错误提示：1000-1099
     * 比如：1001：验证码有效期过期等
     */
    VERIFICATION_CODE_ERROR(1099, "验证码不正确"),

    /**
     * token类错误提示：1100 - 1199
     */
    TOKEN_ERROR(1199, "token错误"),

    /**
     * 用户信息错误提示：1200-1299
     */
    USER_NOT_EXISTS(1299, "用户信息不存在"),

    /**
     * 计价规则不存在：1300-1399
     */
    PRICE_RULE_EMPTY(1399, "计价规则不存在"),

    /**
     * 请求地图错误：1400-1499
     */
    MAP_DIC_DISTRICT_ERROR(1400, "请求地图错误"),

    /**
     * 司机和车辆：1500-1599
     */
    DRIVER_CAR_BIND_EXISTS(1500, "司机车辆绑定关系已存在，请勿重复绑定！"),

    /**
     * 司机车辆绑定关系不存在
     */
    DRIVER_CAR_BIND_NOT_EXISTS(1501, "司机车辆绑定关系不存在"),

    /**
     * 司机车辆绑定或者解绑过程中出现逻辑错误，导致程序错误！
     */
    DRIVER_CAR_BIND_ERROR(1502, "司机车辆绑定或者解绑过程中出现逻辑错误，导致程序错误！"),

    /**
     * 根据电话号码获取司机信息数据有误，需要重新进行获取
     */
    DRIVER_INFO_ERROR(1503, "获取司机信息数据有误，请重新获取！"),

    /**
     * 司机信息不存在！
     */
    DRIVER_NOT_EXISTS(1504, "司机信息不存在！"),

    /**
     * 成功
     */
    SUCCESS(1, "success"),


    /**
     * 失败
     */
    FAIL(0, "fail");

    private int code;
    private String message;

    CommonStatusEnum(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
