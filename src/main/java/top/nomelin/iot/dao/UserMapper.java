package top.nomelin.iot.dao;



import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import top.nomelin.iot.model.User;

import java.util.List;

/**
 *
 * @author nomelin
 */
@Mapper
public interface UserMapper {
    /**
     * 新增
     */
    int insert(User user);

    /**
     * 删除
     */
    int deleteById(Integer id);

    /**
     * 修改
     */
    int updateById(User user);

    /**
     * 根据ID查询
     */
    User selectById(Integer id);

    /**
     * 查询所有
     */
    List<User> selectAll(User user);

    @Select("select * from user where name = #{name}")
    User selectByName(String name);
}
