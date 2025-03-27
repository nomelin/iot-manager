package top.nomelin.iot.interceptor;


import cn.hutool.core.util.ObjectUtil;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import top.nomelin.iot.cache.CurrentUserCache;
import top.nomelin.iot.common.Constants;
import top.nomelin.iot.common.enums.CodeMessage;
import top.nomelin.iot.common.exception.BusinessException;
import top.nomelin.iot.model.User;
import top.nomelin.iot.service.UserService;

/**
 * 验证token的拦截器
 *
 * @author nomelin
 */
@Component
public class TokenInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(TokenInterceptor.class);

    private final UserService userService;
    private final CurrentUserCache currentUserCache;

    @Value("${debug.print-extra-info:false}")
    private boolean printExtraInfo;

    @Autowired
    public TokenInterceptor(UserService userService, CurrentUserCache currentUserCache) {
        this.userService = userService;
        this.currentUserCache = currentUserCache;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        HttpSession session = request.getSession();
        if (printExtraInfo) {
            log.info("session id:" + session.getId() + ",请求路径：" + request.getRequestURI());
        }
        if (!ObjectUtil.isNull(currentUserCache.getCurrentUser())) {
            if (printExtraInfo) {
                log.info("从session缓存bean中取到用户信息，跳过token解析。");
            }
            return true;
        }
        //从http请求的header中获取token
        String token = request.getHeader(Constants.TOKEN);
        if (ObjectUtil.isEmpty(token)) {
            // 如果没拿到，从参数里再拿一次
            token = request.getParameter(Constants.TOKEN);
        }
        if (ObjectUtil.isEmpty(token)) {
            throw new BusinessException(CodeMessage.TOKEN_INVALID_ERROR);// token为空
        }
        // 2. 验证token
        User user;
        String userId;
        String name;
        try {
            // 解析token获取存储的数据
            String audience = JWT.decode(token).getAudience().get(0);// 获取用户
            userId = audience.split("-")[0];
            name = audience.split("-")[1];
        } catch (Exception e) {
            throw new BusinessException(CodeMessage.TOKEN_PARSING_ERROR);
        }
        // 根据userId查询数据库
        user = userService.selectById(Integer.valueOf(userId));
        if (ObjectUtil.isNull(user)) {
            throw new BusinessException(CodeMessage.USER_NOT_EXIST_ERROR);
        }
        try {
            // 用户密码加签验证 token
            JWTVerifier jwtVerifier = JWT.require(Algorithm.HMAC256(user.getPassword())).build();// 构建JWT验证器
            jwtVerifier.verify(token); // 验证token
        } catch (TokenExpiredException e) {
            // Token过期异常处理
            throw new BusinessException(CodeMessage.TOKEN_EXPIRED_ERROR);
        } catch (JWTVerificationException e) {
            throw new BusinessException(CodeMessage.TOKEN_CHECK_ERROR);
        }
//        log.info("token验证通过，用户:" + user);
        currentUserCache.setCurrentUser(user);// 将用户信息存储到缓存中,以便使用
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        HandlerInterceptor.super.postHandle(request, response, handler, modelAndView);
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        HandlerInterceptor.super.afterCompletion(request, response, handler, ex);
    }
}