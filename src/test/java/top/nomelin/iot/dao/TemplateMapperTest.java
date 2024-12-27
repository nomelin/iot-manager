package top.nomelin.iot.dao;


import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import top.nomelin.iot.common.enums.IotDataType;
import top.nomelin.iot.model.Config;
import top.nomelin.iot.model.Template;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SpringBootTest
class TemplateMapperTest {

    @Autowired
    private TemplateMapper templateMapper;


    @Test
    public void testTemplateMapper() {


        // 1. 插入测试数据
        Template insertTemplate = new Template();
        insertTemplate.setName("测试模板");
        insertTemplate.setUserId(4);
        Config config = new Config();
        Map<String, IotDataType> dataTypes = new HashMap<>();
        dataTypes.put("温度", IotDataType.DOUBLE);
        dataTypes.put("相对湿度", IotDataType.INT);
        config.setDataTypes(dataTypes);
        config.setDataTypes(dataTypes);
        insertTemplate.setConfig(config);
        int insertResult = templateMapper.insert(insertTemplate);
        System.out.println("Insert Result: " + insertResult);
        System.out.println("Inserted Template ID: " + insertTemplate.getId());

        // 2. 更新测试数据
        Template updateTemplate = new Template();
        updateTemplate.setId(insertTemplate.getId());
        updateTemplate.setName("Updated Template");
        updateTemplate.setUserId(4);
        dataTypes = new HashMap<>();
        dataTypes.put("温度", IotDataType.FLOAT);
        dataTypes.put("pm2.5", IotDataType.INT);
        config.setDataTypes(dataTypes);
        updateTemplate.setConfig(config);
        int updateResult = templateMapper.update(updateTemplate);
        System.out.println("Update Result: " + updateResult);

        // 3. 根据 ID 查询数据
        Template selectedById = templateMapper.selectById(insertTemplate.getId());
        System.out.println("Selected by ID: " + selectedById);

        // 4. 条件查询所有数据
        Template condition = new Template();
        condition.setName("Updated Template");
        List<Template> allTemplates = templateMapper.selectAll(condition);
        System.out.println("Condition Query Result: " + allTemplates);

        // 5. 根据用户 ID 查询数据
        List<Template> templatesByUserId = templateMapper.selectByUserId(4);
        System.out.println("Templates by User ID: " + templatesByUserId);

//        // 6. 删除数据
//        int deleteResult = templateMapper.delete(insertTemplate.getId());
//        System.out.println("Delete Result: " + deleteResult);


    }
}
