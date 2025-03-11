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
 * UserService
 *
 * @author nomelin
 * @since 2025/03/11 20:37
 **/


@RestController
@RequestMapping("/user")
public class UserController {


    public static final Logger log = LoggerFactory.getLogger(UserController.class);
    private final UserService userService;
    private final CurrentUserCache currentUserCache;


    public UserController(UserService userService, CurrentUserCache currentUserCache) {
        this.userService = userService;
        this.currentUserCache = currentUserCache;
    }

    /**
     * 修改
     */
    @PutMapping("")
    public Result updateById(@RequestBody User user) {
        userService.updateById(user);
        return Result.success();
    }


    /**
     * 修改密码
     */
    @PutMapping("/updatePassword")
    public Result updatePassword(@RequestBody JSONObject requestBody) {
        String name = requestBody.getStr("name");
        String password = requestBody.getStr("oldPassword");
        String newPassword = requestBody.getStr("newPassword");
        if (StrUtil.isBlank(name) || StrUtil.isBlank(password)
                || ObjectUtil.isNull(newPassword) || StrUtil.isBlank(newPassword)) {
            throw new BusinessException(CodeMessage.PARAM_LOST_ERROR);// 参数缺失
        }
        log.info("更改密码请求：name:{},password:{},newPassword:{}", name, password, newPassword);
        if (!currentUserCache.getCurrentUser().getName().equals(name)) {
            throw new BusinessException(CodeMessage.INVALID_USER_NAME_ERROR);// 非当前用户修改密码
        }
        if (currentUserCache.getCurrentUser().getPassword().equals(newPassword)) {
            throw new BusinessException(CodeMessage.EQUAL_PASSWORD_ERROR);// 新密码不能与旧密码相同
        }
        User user = new User();
        user.setName(name);
        user.setPassword(password);
        userService.updatePassword(user, newPassword);// 修改密码
        return Result.success();
    }

    @DeleteMapping("/logout")
    public Result logout() {
        currentUserCache.clear();
        return Result.success();
    }

    /**
     * 根据ID查询
     */
    @GetMapping("/{id}")
    public Result selectById(@PathVariable Integer id) {
        User user = userService.selectById(id);
        return Result.success(user);
    }

    /**
     * 查询所有
     */
    @GetMapping("")
    public Result selectAll(User user) {
        List<User> list = userService.selectAll(user);
        return Result.success(list);
    }
}
