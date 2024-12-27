package top.nomelin.iot.dao;

import org.apache.ibatis.annotations.Mapper;
import top.nomelin.iot.model.Device;
import top.nomelin.iot.model.Template;

import java.util.List;

/**
 * TemplateMapper
 *
 * @author nomelin
 * @since 2024/12/27 16:00
 **/
@Mapper
public interface TemplateMapper {
    int insert(Template template);


    int update(Template template);


    int delete(int id);

    Template selectById(int id);

    List<Template> selectAll(Template template);


    List<Template> selectByUserId(int userId);
}
