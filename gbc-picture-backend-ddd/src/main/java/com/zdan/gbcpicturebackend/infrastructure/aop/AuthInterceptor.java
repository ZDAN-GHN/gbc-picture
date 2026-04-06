package com.zdan.gbcpicturebackend.infrastructure.aop;

import com.zdan.gbcpicturebackend.infrastructure.annotation.AuthCheck;
import com.zdan.gbcpicturebackend.infrastructure.exception.ErrorCode;
import com.zdan.gbcpicturebackend.infrastructure.exception.ThrowUtils;
import com.zdan.gbcpicturebackend.domain.user.entity.User;
import com.zdan.gbcpicturebackend.domain.user.valueobject.UserRoleEnum;
import com.zdan.gbcpicturebackend.application.service.UserApplicationService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@Aspect
@Component
public class AuthInterceptor {

    @Resource
    private UserApplicationService userApplicationService;

    /**
     * 执行拦截
     *
     * @param joinPoint 连接点（笼统的也叫切点，拦截的具体方法等）
     * @param authCheck 权限校验注解
     * @return
     */
    @Around("@annotation(authCheck)") // 环绕通知，对使用了AuthCheck的注解生效
    public Object doInterceptor(ProceedingJoinPoint joinPoint,
                                AuthCheck authCheck) throws Throwable {
        // 校验是否包含需要的权限
        String mustRole = authCheck.mustRole();
        HttpServletRequest httpServletRequest = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        User loginUser = userApplicationService.getLoginUser(httpServletRequest);
        UserRoleEnum mustRoleEnum = UserRoleEnum.getEnumByValue(mustRole);
        if (mustRoleEnum == null) { // 没有权限限制，放行
            return joinPoint.proceed();
        }
        UserRoleEnum userRoleEnum = UserRoleEnum.getEnumByValue(loginUser.getUserRole());
        ThrowUtils.throwIf(userRoleEnum == null, ErrorCode.NO_AUTH_ERROR);
        // 需要管理员权限，但是用户没有管理员权限（这种判断看起来麻烦，但是拓展性好）
        ThrowUtils.throwIf(UserRoleEnum.ADMIN.equals(mustRoleEnum) && !userRoleEnum.equals(mustRoleEnum),
                ErrorCode.NO_AUTH_ERROR);
        return joinPoint.proceed();
    }
}
