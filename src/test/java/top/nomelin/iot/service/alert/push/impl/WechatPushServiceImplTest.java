package top.nomelin.iot.service.alert.push.impl;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import top.nomelin.iot.service.alert.push.WechatPushService;

import java.util.List;

@SpringBootTest
class WechatPushServiceImplTest {

    @Autowired
    private WechatPushService wechatPushService;


    @Test
    void send() {
        String title = "测试标题";
        String content = "测试内容<bold>加粗</bold>,测试换行<br>" +
                "新一行.";
        int userId = 1;
        String friendToken = "e9f5ac155327496f93d49dcd5c8a8da9";
        wechatPushService.send(title, content, userId, List.of(friendToken));
    }
}