package com.mashibing.serviceprice.service;

import com.mashibing.internalcommon.constant.CommonStatusEnum;
import com.mashibing.internalcommon.dto.PriceRule;
import com.mashibing.internalcommon.dto.ResponseResult;
import com.mashibing.internalcommon.request.ForecastPriceDTO;
import com.mashibing.internalcommon.response.DirectionResponse;
import com.mashibing.internalcommon.response.ForecastPriceResponse;
import com.mashibing.serviceprice.mapper.PriceRuleMapper;
import com.mashibing.serviceprice.remote.ServiceMapClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author: Gloria
 * @Description:
 * @Date: Created in 9:50 AM 4/24/23
 */
@Service
@Slf4j
public class ForecastPriceService {

    @Autowired
    private ServiceMapClient serviceMapClient;

    @Autowired
    private PriceRuleMapper priceRuleMapper;

    public ResponseResult forecastPrice(String depLongitude, String depLatitude,
                                        String destLongitude, String destLatitude) {

        ForecastPriceDTO forecastPriceDTO = new ForecastPriceDTO();
        forecastPriceDTO.setDepLongitude(depLongitude);
        forecastPriceDTO.setDepLatitude(depLatitude);
        forecastPriceDTO.setDestLongitude(destLongitude);
        forecastPriceDTO.setDestLatitude(destLatitude);

        ResponseResult<DirectionResponse> driving = serviceMapClient.direction(forecastPriceDTO);
        Integer distance = driving.getData().getDistance();
        Integer duration = driving.getData().getDuration();
        log.info("service-price中读取到的距离和时长分别是：" + distance + "   " + duration);


        Map<String, Object> queryMap = new HashMap<>();
        queryMap.put("city_code", "110000");
        queryMap.put("vehicle_type", "1");
        List<PriceRule> priceRules = priceRuleMapper.selectByMap(queryMap);

        //不能没有计价规则
        if (priceRules.size() == 0) {
            return ResponseResult.fail(CommonStatusEnum.PRICE_RULE_EMPTY.getCode(),
                    CommonStatusEnum.PRICE_RULE_EMPTY.getMessage());
        }

        ForecastPriceResponse forecastPriceResponse = new ForecastPriceResponse();
        forecastPriceResponse.setPrice(getPrice(distance, duration, priceRules.get(0)));

        return ResponseResult.success(forecastPriceResponse);
    }

    /**
     * 根据距离、时长、计价规则，进行价格计算
     *
     * @param distance
     * @param duration
     * @param priceRule
     * @return
     */
    private Double getPrice(Integer distance, Integer duration, PriceRule priceRule) {
        BigDecimal finalPrice = new BigDecimal(0);

        //起步价
        finalPrice = finalPrice.add(priceRule.getStartFare());
//        log.info("第一步：起步价的价格是：" + priceRule.getStartFare());
//        log.info("第一步：加了起步价的价格是：" + finalPrice);

        //里程,出去起步价之后的里程，若是负数，那么自动赋值为0
        //得到km数
        BigDecimal kmDistance = new BigDecimal(distance).divide(new BigDecimal(1000), 2, BigDecimal.ROUND_HALF_UP);
//        log.info("第二步：一共路程数：" + distance + " 米");
//        log.info("第二步：换算成公里：" + kmDistance + " 千米");
        //总路程-起步路程
        BigDecimal subtract = kmDistance.subtract(new BigDecimal(priceRule.getStartMile()));
        if (subtract.compareTo(new BigDecimal(0)) > 0) {
            //只有超过起步里程，才能对多余的里程进行计价
            finalPrice = finalPrice.add(subtract.multiply(priceRule.getUnitPricePerMile()).setScale(2, BigDecimal.ROUND_HALF_UP));
        }
//        log.info("第三步：起步里程是：" + priceRule.getStartMile() + " 千米");
//        log.info("第三步：加上多余的路程后总共价格：" + finalPrice + " 元");


        //分钟,每分钟都要进行计价
        //总时间换算成分钟
        BigDecimal minTime = new BigDecimal(duration).divide(new BigDecimal(60), 2, BigDecimal.ROUND_HALF_UP);
        //根据每分钟价格进行时间价格计算
        finalPrice = finalPrice.add(minTime.multiply(priceRule.getUnitPricePerMinute()).setScale(2, BigDecimal.ROUND_HALF_UP));

//        log.info("第四步：总共行驶分钟数：" + duration + " 秒");
//        log.info("第四步：换算成分钟为：" + minTime + " 分钟");
//        log.info("第四步：每分钟需要钱数：" + priceRule.getUnitPricePerMinute() + " 分钟/元");
//        log.info("第四步：最后总价格是：" + finalPrice + " 元");

        return finalPrice.doubleValue();
    }
}