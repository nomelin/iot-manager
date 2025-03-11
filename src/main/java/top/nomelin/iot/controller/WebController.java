package top.nomelin.iot.controller;


import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import top.nomelin.iot.cache.CurrentUserCache;
import top.nomelin.iot.common.Result;
import top.nomelin.iot.common.enums.CodeMessage;
import top.nomelin.iot.common.exception.BusinessException;
import top.nomelin.iot.model.User;
import top.nomelin.iot.service.UserService;

import java.util.List;


/**
 * 公共前端接口和非标准前端接口
 *
 * @author nomelin
 */
@RestController
@RequestMapping("/openapi")
public class WebController {
    private static final Logger log = LoggerFactory.getLogger(WebController.class);
    private final UserService userService;

    public WebController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/")
    public Result hello() {
        return Result.success("hello, iot manager");
    }

    /**
     * 登录
     */
    @PostMapping("/login")
    public Result login(@RequestBody User user) {
        if (ObjectUtil.isEmpty(user.getName()) || ObjectUtil.isEmpty(user.getPassword())) {
            throw new BusinessException(CodeMessage.PARAM_LOST_ERROR);
        }
        user = userService.login(user);
        log.info("用户登录成功：{}", user);
        return Result.success(user);
    }

    /**
     * 注册
     */
    @PostMapping("/register")
    public Result register(@RequestBody User user) {
        if (StrUtil.isBlank(user.getName()) || StrUtil.isBlank(user.getPassword())) {
            throw new BusinessException(CodeMessage.PARAM_LOST_ERROR);
        }
        user = userService.register(user);
        log.info("注册成功：{}", user);
        return Result.success(user);
    }

    /**
     * 验证用户名是否可用
     */
    @GetMapping("/valid/{username}")
    public Result validUserName(@PathVariable String username) {
        User user = new User();
        user.setName(username);
        List<User> users = userService.selectAll(user);//模糊查找，所以要遍历再验证
        boolean res = true;
        for (User u : users) {
            if (u.getName().equals(username)) {
                res = false;
                break;
            }
        }
        return Result.success(res);
    }

}

