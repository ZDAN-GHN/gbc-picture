package com.zdan.gbcpicturebackend.shared.auth.strategy.authloader.spaceuser;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.hutool.http.ContentType;
import cn.hutool.json.JSONUtil;
import com.zdan.gbcpicturebackend.domain.picture.repository.PictureRepository;
import com.zdan.gbcpicturebackend.domain.space.repository.SpaceRepository;
import com.zdan.gbcpicturebackend.domain.space.repository.SpaceUserRepository;
import com.zdan.gbcpicturebackend.infrastructure.exception.BusinessException;
import com.zdan.gbcpicturebackend.infrastructure.exception.ErrorCode;
import com.zdan.gbcpicturebackend.shared.auth.StpKit;
import com.zdan.gbcpicturebackend.shared.auth.model.SpaceUserPermissionConstant;
import com.zdan.gbcpicturebackend.shared.auth.strategy.annotaion.AuthLoader;
import com.zdan.gbcpicturebackend.shared.auth.strategy.interfaces.AuthLoaderInterface;
import com.zdan.gbcpicturebackend.domain.picture.entity.Picture;
import com.zdan.gbcpicturebackend.domain.space.entity.Space;
import com.zdan.gbcpicturebackend.domain.space.entity.SpaceUser;
import com.zdan.gbcpicturebackend.domain.user.entity.User;
import com.zdan.gbcpicturebackend.domain.space.valueobjectt.SpaceRoleEnum;
import com.zdan.gbcpicturebackend.domain.space.valueobjectt.SpaceTypeEnum;
import com.zdan.gbcpicturebackend.application.service.UserApplicationService;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.lang.annotation.*;
import java.util.*;

import static com.zdan.gbcpicturebackend.domain.user.constant.UserConstant.USER_LOGIN_STATE;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.CollectionUtils;

/**
 * SpaceUser 体系的 sa-token 权限加载器
 */
@AuthLoader(loginType = StpKit.SPACE_TYPE)
public class SpaceUserAuthLoader implements AuthLoaderInterface, ApplicationContextAware {

    // 策略存储 map
    private final Map<String, WrapAuthContext> authContextWrapStrategyMap = new HashMap<>();

    // Spring 应用上下文
    private ApplicationContext applicationContext;

    // region --- 依赖注入
    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Resource
    private UserApplicationService userApplicationService;

    @Resource
    private SpaceRepository spaceRepository;

    @Resource
    private SpaceUserRepository spaceUserRepository;

    @Resource
    private PictureRepository pictureRepository;

    @Resource
    private SpaceUserAuthManager spaceUserAuthManager;

    // endregion 依赖注入

    // region --- loginType 为 SpaceUser 下鉴权的类和接口定义

    /**
     * SpaceUserAuthContext
     * 表示用户在特定空间内的授权上下文，包括关联的图片、空间和用户信息。
     */
    @Data
    protected class SpaceUserAuthContext {

        /**
         * 临时参数，不同请求对应的 id 可能不同
         */
        private Long id;

        /**
         * 图片 ID
         */
        private Long pictureId;

        /**
         * 空间 ID
         */
        private Long spaceId;

        /**
         * 空间用户 ID
         */
        private Long spaceUserId;

        /**
         * 图片信息
         */
        private Picture picture;

        /**
         * 空间信息
         */
        private Space space;

        /**
         * 空间用户信息
         */
        private SpaceUser spaceUser;
    }

    /**
     * 修改 authContext 策略注解
     */
    @Target({ElementType.TYPE}) // 仅作用于类
    @Retention(RetentionPolicy.RUNTIME) // 运行时保留，可通过反射获取
    @Documented
    @Component // 间接让 Spring 扫描为 Bean（无需额外加 @Service/@Component）
    protected @interface AuthContextWrapper {

        /**
         * 策略标识（作为 Map 的 key）
         * 默认为空时，使用类名首字母小写作为 key
         */
        String moduleName() default "";
    }

    /**
     * 包装上下文器策略接口
     */
    protected interface WrapAuthContext {
        void wrapSpaceUserAuthContext(SpaceUserAuthContext authContext);
    }

    // endregion

    // region --- 接口实现
    @Override
    public List<String> getPermissionList(Object loginId) {
        // 管理员权限，表示权限校验通过
        List<String> ADMIN_PERMISSIONS = spaceUserAuthManager.getPermissionsByRole(SpaceRoleEnum.ADMIN.getValue());
        // 获取 userId
        User loginUser = (User) StpKit.SPACE.getSessionByLoginId(loginId).get(USER_LOGIN_STATE);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "用户未登录");
        }
        // 系统管理员具有所有的权限
        if (loginUser.isAdmin()) {
            return ADMIN_PERMISSIONS;
        }
        Long userId = loginUser.getId();
        // 获取上下文对象
        SpaceUserAuthContext authContext = getAuthContext();
        // 如果所有字段都为空，表示查询公共图库，需要根据图片的持有人判断权限
        if (isAllFieldsNull(authContext)) {
            return spaceUserAuthManager.getPermissionList(null, loginUser);
        }
        // 优先从上下文中获取 SpaceUser 对象
        SpaceUser spaceUser = authContext.getSpaceUser();
        if (spaceUser != null) {
            return spaceUserAuthManager.getPermissionsByRole(spaceUser.getSpaceRole());
        }
        // 如果有 spaceUserId，必然是团队空间，通过数据库查询 SpaceUser 对象
        Long spaceUserId = authContext.getSpaceUserId();
        if (spaceUserId != null) {
            spaceUser = spaceUserRepository.getById(spaceUserId);
            if (spaceUser == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "未找到空间用户信息");
            }
            // 取出当前登录用户对应的 spaceUser
            SpaceUser loginSpaceUser = spaceUserRepository.lambdaQuery()
                    .eq(SpaceUser::getSpaceId, spaceUser.getSpaceId())
                    .eq(SpaceUser::getUserId, userId)
                    .one();
            if (loginSpaceUser == null) {
                return new ArrayList<>();
            }
            // 这里会导致管理员在私有空间没有权限，可以再查一次库处理
            return spaceUserAuthManager.getPermissionsByRole(loginSpaceUser.getSpaceRole());
        }
        // 如果没有 spaceUserId，尝试通过 spaceId 或 pictureId 获取 Space 对象并处理
        Long spaceId = authContext.getSpaceId();
        if (spaceId == null) {
            // 如果没有 spaceId，通过 pictureId 获取 Picture 对象和 Space 对象
            Long pictureId = authContext.getPictureId();
            // 图片 id 也没有，则默认通过权限校验
            if (pictureId == null) {
                return ADMIN_PERMISSIONS;
            }
            Picture picture = pictureRepository.lambdaQuery()
                    .eq(Picture::getId, pictureId)
                    .select(Picture::getId, Picture::getSpaceId, Picture::getUserId)
                    .one();
            if (picture == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "未找到图片信息");
            }
            spaceId = picture.getSpaceId();
            // 公共图库，仅本人或管理员可操作
            if (spaceId == null) {
                if (picture.getUserId().equals(userId) || loginUser.isAdmin()) {
                    return ADMIN_PERMISSIONS;
                } else {
                    // 不是自己的图片，仅可查看
                    return Collections.singletonList(SpaceUserPermissionConstant.PICTURE_VIEW);
                }
            }
        }
        // 获取 Space 对象
        Space space = spaceRepository.getById(spaceId);
        if (space == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "未找到空间信息");
        }
        // 根据 Space 类型判断权限
        if (space.getSpaceType() == SpaceTypeEnum.PRIVATE.getValue()) {
            // 私有空间，仅本人或管理员有权限
            if (space.getUserId().equals(userId) || loginUser.isAdmin()) {
                return ADMIN_PERMISSIONS;
            } else {
                return new ArrayList<>();
            }
        } else {
            // 团队空间，查询 SpaceUser 并获取角色和权限
            spaceUser = spaceUserRepository.lambdaQuery()
                    .eq(SpaceUser::getSpaceId, spaceId)
                    .eq(SpaceUser::getUserId, userId)
                    .one();
            if (spaceUser == null) {
                return new ArrayList<>();
            }
            return spaceUserAuthManager.getPermissionsByRole(spaceUser.getSpaceRole());
        }
    }

    /**
     * 容器被 springboot 加载的时候触发 @AuthContextWrapper 策略的采集
     *
     * @param applicationContext
     * @throws BeansException
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
        // 1. 扫描容器中所有带 @Strategy 注解的 Bean（key=Bean名称，value=Bean实例）
        Map<String, Object> annotatedBeans = applicationContext.getBeansWithAnnotation(AuthContextWrapper.class);

        if (CollectionUtils.isEmpty(annotatedBeans)) {
            return;
        }

        // 2. 遍历 Bean，解析注解的 key，存入 strategyMap
        for (Map.Entry<String, Object> entry : annotatedBeans.entrySet()) {
            Object bean = entry.getValue();
            Class<?> beanClass = bean.getClass();
            AuthContextWrapper strategyAnnotation = beanClass.getAnnotation(AuthContextWrapper.class);

            // 3. 确定 Map 的 key：注解指定的 key 优先，否则用 Bean 名称（默认类名首字母小写）
            String strategyKey = strategyAnnotation.moduleName();
            if (strategyKey.isEmpty()) {
                strategyKey = entry.getKey(); // Bean 名称（如 userStrategy、vipStrategy）
            }
            // 4. 校验 Bean 是否实现了 WrapAuthContext 接口（可选，强制规范）
            if (bean instanceof WrapAuthContext) {
                authContextWrapStrategyMap.put(strategyKey, (WrapAuthContext) bean);
                System.out.println("策略注册成功：moduleName = " + strategyKey + "，bean = " + beanClass.getName());
            } else {
                throw new IllegalArgumentException("类 " + beanClass.getName() + " 标注了 @AuthContextMutateStrategy，但未实现 AuthContextMutateInterface 接口");
            }
        }
    }

    /**
     * 根据请求获取请求上下文
     */
    @Override
    public SpaceUserAuthContext getAuthContext() {
        HttpServletRequest request = ((ServletRequestAttributes) Objects.requireNonNull(RequestContextHolder.getRequestAttributes())).getRequest();
        // HTTP 请求中的 Content-Type（内容类型） 请求头的值。
        // 用于告诉服务器客户端发送的数据是什么格式的。
        SpaceUserAuthContext authContext = new SpaceUserAuthContext();
        String contentType = request.getContentType();
        if (ContentType.JSON.getValue().equals(contentType)) { // dto 在请求体中
            String jsonBody = ServletUtil.getBody(request);
            authContext = JSONUtil.toBean(jsonBody, SpaceUserAuthContext.class);
        } else { // dto 不在请求体，在请求参数中
            Map<String, String> paramMap = ServletUtil.getParamMap(request);
            authContext = BeanUtil.toBean(paramMap, SpaceUserAuthContext.class);
        }
        // 如果id不为空，说明需要确定 dto 携带的 id 含义
        if (ObjectUtil.isNotNull(authContext.getId())) {
            String requestURI = request.getRequestURI();
            String partURI = requestURI.replace(contextPath + "/", "");
            String moduleName = StrUtil.subBefore(partURI, "/", false);
            this.authContextWrapStrategyMap.get(moduleName).wrapSpaceUserAuthContext(authContext);
        }
        return authContext;
    }

    // endregion 接口实现

    // region --- 自定义私有方法

    /**
     * 判断对象的所有字段是否为空
     *
     * @param object
     * @return
     */
    private boolean isAllFieldsNull(Object object) {
        if (object == null) {
            return true; // 对象本身为空
        }
        // 获取所有字段并判断是否所有字段都为空
        return Arrays.stream(ReflectUtil.getFields(object.getClass()))
                // 获取字段值
                .map(field -> ReflectUtil.getFieldValue(object, field))
                // 检查是否所有字段都为空
                .allMatch(ObjectUtil::isEmpty);
    }
    // endregion 自定义私有方法
}
