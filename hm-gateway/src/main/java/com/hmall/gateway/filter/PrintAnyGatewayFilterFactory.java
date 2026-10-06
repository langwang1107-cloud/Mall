package com.hmall.gateway.filter;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * GatewayFilter
 */
//@Component
@Slf4j
public class PrintAnyGatewayFilterFactory extends AbstractGatewayFilterFactory<PrintAnyGatewayFilterFactory.MyConfig> implements Ordered {
    @Override
    public GatewayFilter apply(MyConfig config) {
        return new GatewayFilter() {
            @Override
            public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
                ServerHttpRequest request = exchange.getRequest();
                exchange.getResponse();
                log.info("GatewayFilter收到请求：{}",request.getLocalAddress());
                log.info("a:{},b:{},c:{}",config.getA(),config.getB(),config.getC());
                return chain.filter(exchange);//放行
            }
        };
    }

    @Override
    public int getOrder() {
        return 10;//数字越小，优先级越高
    }

    @Data
    public static class MyConfig{
        private String a;
        private String b;
        private String c;
    }

    @Override
    public List<String> shortcutFieldOrder() {
        return List.of("a","b","c");
    }

    @Override
    public Class<MyConfig> getConfigClass() {
        return MyConfig.class;
    }
}
