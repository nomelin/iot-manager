package top.nomelin.iot.dao;

import org.apache.ibatis.annotations.Mapper;
import top.nomelin.iot.model.Group;

import java.util.List;

@Mapper
public interface GroupMapper {
    int insert(Group group);

    int insertGroupDeviceRelations(int groupId, List<Integer> deviceIds);

    int deleteGroupDeviceRelations(int groupId, List<Integer> deviceIds);

    /**
     * 数据库已经设置级联删除，所以可以直接删除group，无需先删除group_device
     */
    int delete(int id);

    int update(Group group);

    Group selectById(int id);

    List<Group> selectByName(String name);

    List<Group> selectAll();
}
