package com.mashibing.serviceorder.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mashibing.internalcommon.constant.CommonStatusEnum;
import com.mashibing.internalcommon.constant.OrderConstants;
import com.mashibing.internalcommon.dto.OrderInfo;
import com.mashibing.internalcommon.dto.ResponseResult;
import com.mashibing.internalcommon.request.OrderRequest;
import com.mashibing.internalcommon.util.RedisPrefixUtils;
import com.mashibing.serviceorder.mapper.OrderInfoMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mashibing.serviceorder.remote.ServicePriceClient;
import lombok.extern.slf4j.Slf4j;
import org.omg.CORBA.COMM_FAILURE;
import org.omg.CORBA.ORB;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author Gloria
 * @since 2023-05-18
 */
@Service
@Slf4j
public class OrderInfoService {
    @Autowired
    private OrderInfoMapper orderInfoMapper;

    @Autowired
    private ServicePriceClient servicePriceClient;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 插入用户新增订单
     *
     * @param orderRequest
     * @return
     */
    public ResponseResult addOrder(OrderRequest orderRequest) {
        //判断计价规则是否是当前最新计价规则
        if (!(servicePriceClient.isLatestFareVersion(orderRequest.getFareType(),
                orderRequest.getFareVersion()).getData())) {
            return ResponseResult.fail(CommonStatusEnum.PRICE_RULE_CHANGED.getCode(),
                    CommonStatusEnum.PRICE_RULE_CHANGED.getMessage());
        }

        //若系统中已经存在还未完成的订单，那么就不能进行新订单的生成
        if (isOrderGoingOn(orderRequest.getPassengerId()) > 0) {
            return ResponseResult.fail(CommonStatusEnum.ORDER_GOING_ON.getCode(),
                    CommonStatusEnum.ORDER_GOING_ON.getMessage());
        }
        //需要判断下单的用户是否是黑名单用户,是，则进行错误值返回
        if (isBlackDevice(orderRequest.getDeviceCode())) {
            return ResponseResult.fail(CommonStatusEnum.DEVICE_IS_BLACK.getCode(),
                    CommonStatusEnum.DEVICE_IS_BLACK.getMessage());
        }

        OrderInfo orderInfo = new OrderInfo();
        BeanUtils.copyProperties(orderRequest, orderInfo);
        orderInfo.setOrderStatus(OrderConstants.ORDER_START);

        LocalDateTime now = LocalDateTime.now();
        orderInfo.setGmtCreate(now);
        orderInfo.setGmtModified(now);

        orderInfoMapper.insert(orderInfo);

        return ResponseResult.success("");
    }

    /**
     * 判断是否有正在进行中的订单
     *
     * @param passengerId
     * @return
     */
    public int isOrderGoingOn(Long passengerId) {
        //乘客还有未完成的订单则不允许下单
        QueryWrapper<OrderInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("passenger_id", passengerId);
        queryWrapper.and(
                wrapper -> wrapper.eq("order_status", OrderConstants.ORDER_START)
                        .or().eq("order_status", OrderConstants.DRIVER_RECEIVE_ORDER)
                        .or().eq("order_status", OrderConstants.DRIVER_TO_PICK_UP_PASSENGER)
                        .or().eq("order_status", OrderConstants.DRIVER_ARRIVED_DEPARTURE)
                        .or().eq("order_status", OrderConstants.PICK_UP_PASSENGER)
                        .or().eq("order_status", OrderConstants.PASSENGER_GET_OFF)
                        .or().eq("order_status", OrderConstants.TO_START_PAY)
        );

        Integer validOrderNum = orderInfoMapper.selectCount(queryWrapper);

        return validOrderNum;
    }

    /**
     * 判断是否是黑名单用户，若不是，那么进行赋值，否则直接返回true
     *
     * @param deviceCode
     * @return
     */
    public Boolean isBlackDevice(String deviceCode) {
        String key = RedisPrefixUtils.blackDeviceCodePrefix + deviceCode;
        //存在key值，并且出现次数超过2，那么说明就是黑名单车辆
        if (stringRedisTemplate.hasKey(key)) {
            if (Integer.parseInt(stringRedisTemplate.opsForValue().get(key)) > 1) {
                return true;
            } else {
                stringRedisTemplate.opsForValue().increment(key);
            }
        } else {
            //不存在key值，那么进行key值初始化设置
            stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 1L, TimeUnit.HOURS);
        }
        return false;
    }

}
