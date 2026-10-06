package com.hmall.gateway.filter;

import cn.hutool.core.util.StrUtil;
import com.hmall.common.utils.CollUtils;
import com.hmall.gateway.config.AuthProperties;
import com.hmall.gateway.utils.JwtTool;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(AuthProperties.class)
public class UserLoginGlobalFilter implements GlobalFilter {
    private final AuthProperties authProperties;
    private static final AntPathMatcher ANT_PATH_MATCHER = new AntPathMatcher();
    private final JwtTool jwtTool;


    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpResponse response = exchange.getResponse();
        ServerHttpRequest request = exchange.getRequest();
        //获取请求的路径
        String path = request.getPath().toString();
        //判断是否需要校验
        if(isExclude(path)){
            //说明排掉了，放行
            return chain.filter(exchange);
        }
        //先获取请求头
        List<String> list = request.getHeaders().get("authorization");
        if (CollUtils.isEmpty(list)) {
            //说明没有token，返回401
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return response.setComplete();
        }
        String token = list.get(0);
        if (StrUtil.isBlank(token)) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return response.setComplete();
        }
        //拿到token
        //解析
        Long userId = null;
        try {
            userId = jwtTool.parseToken(token);
        } catch (Exception e) {
            //如果出现异常，说明这个token有问题
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return response.setComplete();
        }
        if(userId==null){
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return response.setComplete();
        }
        //把用户id转发给下游服务
        String userIdStr = userId.toString();
        exchange.mutate().request(r->r.header("user-info",userIdStr));
        //5.放行
        return chain.filter(exchange);
    }

    private boolean isExclude(String path){
        List<String> excludePaths = authProperties.getExcludePaths();
        for (String pattern : excludePaths) {
            boolean match = ANT_PATH_MATCHER.match(pattern, path);
            if(match){
                //说明匹配成功
                return true;
            }
        }
        //代码走到这，说明每个都没有匹配上
        return false;
    }
}
