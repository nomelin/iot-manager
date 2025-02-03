package top.nomelin.iot.service.impl;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import top.nomelin.iot.cache.CurrentUserCache;
import top.nomelin.iot.model.User;
import top.nomelin.iot.service.DataService;

import java.util.ArrayList;
import java.util.List;

@SpringBootTest
class DataServiceTest {
    @Autowired
    DataService dataService;

    @Autowired
    CurrentUserCache currentUserCache;


    @Test
    void insertBatchRecord() {
        User user = new User();
        user.setId(4);
        currentUserCache.setCurrentUser(user);
        int deviceId = 23;// device id
        List<Long> timestamps = new ArrayList<>();
        timestamps.add(1611111111111L);
        timestamps.add(1611111111112L);
        timestamps.add(1611111111113L);
        timestamps.add(1611111111115L);
        timestamps.add(1611111111115L);
        timestamps.add(1611111111116L);
        timestamps.add(1611111111116L);
        List<String> measurements = new ArrayList<>();
        measurements.add("temp");
        measurements.add("humd");
        List<List<Object>> values = new ArrayList<>();
        values.add(List.of(21.5, 51));
        values.add(List.of(22.0, 52));
        values.add(List.of(23.5, 53));
        values.add(List.of(24.5, 54));
        values.add(List.of(25.5, 55));
        values.add(List.of(26.5, 56));
        values.add(List.of(27.5, 57));
        dataService.insertBatchRecord(deviceId, timestamps, measurements, values);

    }

    @Test
    void queryRecord() {


    }
}