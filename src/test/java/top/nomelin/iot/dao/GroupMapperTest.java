package top.nomelin.iot.dao;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import top.nomelin.iot.model.Group;

import java.util.ArrayList;
import java.util.List;

@SpringBootTest
public class GroupMapperTest {

    @Autowired
    private GroupMapper groupMapper;

    @Test
    public void testAllGroupMapperMethods() {
        // 1. 测试插入群组方法（insert）
        Group groupToInsert = new Group();
        groupToInsert.setName("测试群组1");
        groupToInsert.setDescription("这是一个测试群组的描述");
        groupToInsert.setUserId(2);  // 这里假设用户id为1，根据实际情况修改
        int insertResult = groupMapper.insert(groupToInsert);
        System.out.println("插入群组结果，受影响行数：" + insertResult);
        System.out.println("插入后群组的id：" + groupToInsert.getId());

        // 2. 测试插入群组与设备关联关系方法（insertGroupDeviceRelations）
        List<Integer> deviceIdsToInsert = new ArrayList<>();
        deviceIdsToInsert.add(3);  // 假设设备id为，根据实际情况修改
        deviceIdsToInsert.add(5);
        int insertRelationsResult = groupMapper.insertGroupDeviceRelations(groupToInsert.getId(), deviceIdsToInsert);
        System.out.println("插入群组与设备关联关系结果，受影响行数：" + insertRelationsResult);

        // 3. 测试根据id查询群组方法（selectById）
        Group selectedGroupById = groupMapper.selectById(groupToInsert.getId());
        System.out.println("根据id查询群组结果：");
        System.out.println(selectedGroupById);

        // 4. 测试根据名称查询群组方法（selectByName）
        List<Group> groupsByName = groupMapper.selectByName("测试群组1");
        System.out.println("根据名称查询群组结果，查询到的群组数量：" + groupsByName.size());
        for (Group group : groupsByName) {
            System.out.println(group);
        }

        // 5. 测试查询所有群组方法（selectAll）
        Group group = new Group();
        group.setUserId(2);
        List<Group> allGroups = groupMapper.selectAll(group);
        System.out.println("查询所有群组结果，查询到的群组数量：" + allGroups.size());
        for (Group g : allGroups) {
            System.out.println(g);
        }

        // 6. 测试更新群组方法（update）
        Group groupToUpdate = groupMapper.selectById(groupToInsert.getId());
        groupToUpdate.setName("更新后的测试群组1");
        groupToUpdate.setDescription("这是更新后的描述");
        int updateResult = groupMapper.update(groupToUpdate);
        System.out.println("更新群组结果，受影响行数：" + updateResult);

//        // 7. 测试删除群组与设备关联关系方法（deleteGroupDeviceRelations）
//        int deleteRelationsResult = groupMapper.deleteGroupDeviceRelations(groupToInsert.getId(), deviceIdsToInsert);
//        System.out.println("删除群组与设备关联关系结果，受影响行数：" + deleteRelationsResult);

//        // 8. 测试删除群组方法（delete）
//        int deleteResult = groupMapper.delete(groupToInsert.getId());
//        System.out.println("删除群组结果，受影响行数：" + deleteResult);

    }
}