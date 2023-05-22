package com.mashibing.serviceorder.remote;

import com.mashibing.internalcommon.dto.ResponseResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * @Author: Gloria
 * @Description:
 * @Date: Created in 9:03 AM 5/22/23
 */
@FeignClient("service-price")
public interface ServicePriceClient {

    @RequestMapping(method = RequestMethod.GET, value = "/price-rule/is-latest-version")
    public ResponseResult<Boolean> isLatestFareVersion(@RequestParam String fareType, @RequestParam Integer fareVersion);

}