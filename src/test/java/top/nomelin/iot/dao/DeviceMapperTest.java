package top.nomelin.iot.dao;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import top.nomelin.iot.model.Device;
import top.nomelin.iot.model.User;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class DeviceMapperTest {

    @Autowired
    private DeviceMapper deviceMapper;

    @Autowired
    private UserMapper userMapper;

    private Device testDevice;
    private final int deviceId=4;

    @BeforeEach
    void setUp() {
        testDevice = new Device();
        testDevice.setName("NewDevice");
        testDevice.setUserId(1);
        testDevice.setTags(List.of("tagA", "tagB"));
    }

    @Test
    void test(){
        User user = new User();
        user.setName("test");
        user.setPassword("password");
        int userId = userMapper.insert(user);
        System.out.println("userId: " + userId);
    }

    @Test
    void testInsertDevice() {
        deviceMapper.insert(testDevice);
    }
    @Test
    void testUpdateDevice() {
        Device device = deviceMapper.selectById(3);
        assertNotNull(device);
        device.setName("UpdatedDevice");
        int result = deviceMapper.update(device);
        assertEquals(1, result);

        Device updatedDevice = deviceMapper.selectById(3);
        assertEquals("UpdatedDevice", updatedDevice.getName());
    }

    @Test
    void testDeleteDevice() {
        int result = deviceMapper.delete(4);
    }

    @Test
    void testSelectById() {
        Device device = deviceMapper.selectById(2);
        System.out.println("device: " + device);
    }

    @Test
    void testSelectAllWithCondition() {
        Device condition = new Device();
        condition.setName("Device");
        System.out.println("condition: " + condition);
        List<Device> devices = deviceMapper.selectAll(condition);
        System.out.println("devices: " + devices);
    }

    @Test
    void testSelectByUserId() {
        List<Device> devices = deviceMapper.selectByUserId(1);
        System.out.println("devices: " + devices);
    }
}
