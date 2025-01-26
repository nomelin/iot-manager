package top.nomelin.iot.service.impl;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import top.nomelin.iot.cache.CurrentUserCache;
import top.nomelin.iot.model.User;
import top.nomelin.iot.service.GroupService;

import java.util.List;

@SpringBootTest
class GroupServiceImplTest {
    @Autowired
    GroupService groupService;

    @Autowired
    CurrentUserCache currentUserCache;



    @Test
    void getAllMeasurement() {
        User user = new User();
        user.setId(4);
        currentUserCache.setCurrentUser(user);
        List<String> measurementList = groupService.getAllMeasurement(7);
        System.out.println(measurementList);

    }
}