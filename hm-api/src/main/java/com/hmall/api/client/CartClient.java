package com.hmall.api.client;

import com.hmall.api.client.fallback.CartClientFallback;
import com.hmall.api.config.DefaultFeignConfig;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.List;

@FeignClient(value = "cart-service",
        configuration = DefaultFeignConfig.class,
        fallbackFactory = CartClientFallback.class)
public interface CartClient {


    @DeleteMapping("/carts")
    void deleteCartItemByIds(@RequestParam("ids") Collection<Long> ids);
}
