package top.nomelin.iot.service;

import top.nomelin.iot.model.User;

import java.util.List;

public interface UserService {
    User register(User user);

    User login(User user);

    void updatePassword(User user, String newPassword);

    User updateById(User user);

    void deleteById(Integer id);

    List<User> selectAll(User user);

    User selectById(Integer id);
}
