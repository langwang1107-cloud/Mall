package com.hmall.gateway.route;

import cn.hutool.json.JSONUtil;
import com.alibaba.cloud.nacos.NacosConfigManager;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import com.hmall.common.utils.CollUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionWriter;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;

@Component
@RequiredArgsConstructor
public class DynamicRouteLoader {
    private final NacosConfigManager nacosConfigManager;
    private static final String DATA_ID = "gateway-routes.json";
    private static final String GROUP_ID = "DEFAULT_GROUP";
    private static final Long TIMEOUT = 5000L;
    private final RouteDefinitionWriter writer;
    private final Set<String> routeSet = new HashSet<>();

    @PostConstruct//项目启动的时候执行
    public void init() throws NacosException {
        //读取配置，并且设置监听器
        //这个方法干了两件事，
        //1.读取当前的配置
        //2.对这个配置设置监听器
        String config = nacosConfigManager.getConfigService().getConfigAndSignListener(DATA_ID, GROUP_ID, TIMEOUT, new Listener() {
            @Override
            public Executor getExecutor() {
                //线程池
                return null;
            }
            @Override
            public void receiveConfigInfo(String configInfo) {
                //这个方法什么时候触发？---》第一次修改配置的时候就会触发
                updateConfig(configInfo);
            }
        });
        updateConfig(config);
    }

    private void updateConfig(String configInfo) {
        //把老的路由表给清空，由于清空路由表是需要一个id的，我们现在没有老id，所以需要自己维护一个集合
        for (String routeId : routeSet) {
            writer.delete(Mono.just(routeId)).subscribe();
        }
        routeSet.clear();
        //把新的路由表给写入
        List<RouteDefinition> list = JSONUtil.toList(configInfo, RouteDefinition.class);
        if(CollUtils.isEmpty(list)){
            //说明他把所有的路由信息都删了
            return;
        }
        //代码走到这，有一些新的路由信息
        for (RouteDefinition routeDefinition : list) {
            writer.save(Mono.just(routeDefinition)).subscribe();
            routeSet.add(routeDefinition.getId());
        }
    }
}
