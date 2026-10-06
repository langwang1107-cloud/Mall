package com.hmall.common.interceptor;

import cn.hutool.core.util.StrUtil;
import com.hmall.common.utils.UserContext;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 我只需要把用户id取出来，放到ThreadLocal里
 * 如果id拿不出来，那跟我没关系
 */
public class UserInfoInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        //把userId拿到并且放ThreadLocal
        String userIdStr = request.getHeader("user-info");
        if (StrUtil.isNotBlank(userIdStr)) {
            UserContext.setUser(Long.valueOf(userIdStr));
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        UserContext.removeUser();
    }
}
