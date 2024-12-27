package top.nomelin.iot.service.impl;

import cn.hutool.core.util.ObjectUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import top.nomelin.iot.common.Constants;
import top.nomelin.iot.common.enums.CodeMessage;
import top.nomelin.iot.common.exception.BusinessException;
import top.nomelin.iot.dao.IoTDBDao;
import top.nomelin.iot.dao.UserMapper;
import top.nomelin.iot.model.User;
import top.nomelin.iot.service.UserService;
import top.nomelin.iot.util.TokenUtil;

import java.util.List;

/**
 * UserServiceImpl
 *
 * @author nomelin
 * @since 2024/12/18 16:27
 **/
@Service
public class UserServiceImpl implements UserService {
    private final static Logger log = LoggerFactory.getLogger(UserServiceImpl.class);
    private final UserMapper userMapper;
    private final IoTDBDao iotDBDao;

    public UserServiceImpl(UserMapper userMapper, IoTDBDao iotDBDao) {
        this.userMapper = userMapper;
        this.iotDBDao = iotDBDao;
    }

    @Override
    public User register(User user) {
        if (ObjectUtil.isNull(user.getName())) {
            throw new BusinessException(CodeMessage.USER_NAME_NULL_ERROR);//用户名不能为空
        }
        User dbUser = userMapper.selectByName(user.getName());
        if (ObjectUtil.isNotNull(dbUser)) {
            throw new BusinessException(CodeMessage.USER_NAME_EXIST_ERROR);//用户名已存在
        }
        if (ObjectUtil.isEmpty(user.getPassword())) {
            user.setPassword(Constants.DEFAULT_PASSWORD);//默认密码
        }
        userMapper.insert(user);//插入数据库,id会自动生成
//        log.info("插入数据库成功：{}", user);
//        iotDBDao.createDatabase(Constants.DATABASE_PREFIX + user.getId());//创建iotDB数据库
//        log.info("创建iotDB数据库成功：{}", Constants.DATABASE_PREFIX + user.getId());
        log.info("注册用户成功：{}", user);
        return user;
    }

    @Override
    public User login(User user) {
        User dbUser = userMapper.selectByName(user.getName());
        if (ObjectUtil.isNull(dbUser)) {
            throw new BusinessException(CodeMessage.USER_NOT_EXIST_ERROR);//用户不存在
        }
        if (!user.getPassword().equals(dbUser.getPassword())) {
            throw new BusinessException(CodeMessage.USER_ACCOUNT_ERROR);//用户名或密码错误
        }
        // 生成token
        String tokenData = dbUser.getId() + "-" + dbUser.getName();
        String token = TokenUtil.createToken(tokenData, dbUser.getPassword());
        dbUser.setToken(token);
        return dbUser;
    }

    @Override
    public void updatePassword(User user, String newPassword) {
        User dbUser = userMapper.selectByName(user.getName());
        if (ObjectUtil.isNull(dbUser)) {
            throw new BusinessException(CodeMessage.USER_NOT_EXIST_ERROR);//用户不存在
        }
        if (!user.getPassword().equals(dbUser.getPassword())) {
            throw new BusinessException(CodeMessage.PARAM_PASSWORD_ERROR);//原密码输入错误
        }
        dbUser.setPassword(newPassword);
        userMapper.updateById(dbUser);
        log.info("更新密码成功：{}", user);
    }

    @Override
    public User updateById(User user) {
        userMapper.updateById(user);
        log.info("更新用户成功：{}", user);
        return user;
    }

    @Override
    public void deleteById(Integer id) {
        userMapper.deleteById(id);
        log.info("删除用户成功：{}", id);
    }

    @Override
    public List<User> selectAll(User user) {
        return userMapper.selectAll(user);
    }

    @Override
    public User selectById(Integer id) {
        return userMapper.selectById(id);
    }
}
