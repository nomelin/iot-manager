package top.nomelin.iot.dao;

import org.apache.ibatis.annotations.Mapper;
import top.nomelin.iot.model.View;

import java.util.List;

/**
 * ViewMapper
 *
 * @author nomelin
 * @since 2025/01/26 23:14
 **/
@Mapper
public interface ViewMapper {
    int insert(View view);

    int update(View view);


    int delete(int id);

    View selectById(int id);

    List<View> selectByGroupId(int groupId);
}
