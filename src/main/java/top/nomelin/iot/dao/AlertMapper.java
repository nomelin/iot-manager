package top.nomelin.iot.dao;

import org.apache.ibatis.annotations.Mapper;
import top.nomelin.iot.model.alert.Alert;

import java.util.List;

@Mapper
public interface AlertMapper {

    int insert(Alert alert);

    int update(Alert alert);

    int delete(int id);

    Alert selectById(int id);

    List<Alert> selectAll(Alert alert);
}
