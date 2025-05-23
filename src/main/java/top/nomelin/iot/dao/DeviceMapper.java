package top.nomelin.iot.dao;

import org.apache.ibatis.annotations.Mapper;
import top.nomelin.iot.model.Device;

import java.util.List;

@Mapper
public interface DeviceMapper {

    int insert(Device device);

    /**
     * 不使用自增主键，使用手动插入主键id。仅用于事务恢复等特殊场景。
     * @param device 包含id的device
     */
    void insertWithId(Device device);


    int update(Device device);


    int delete(int id);

    Device selectById(int id);

    Device selectByIdForUpdate(int id);

    List<Device> selectAll(Device device);


    List<Device> selectByUserId(int userId);

    List<Device> selectByTemplateId(int templateId);
}

