package top.nomelin.iot.service.impl;

import cn.hutool.core.util.ObjectUtil;
import org.apache.iotdb.session.template.MeasurementNode;
import org.apache.tsfile.enums.TSDataType;
import org.apache.tsfile.file.metadata.enums.TSEncoding;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;
import top.nomelin.iot.cache.CurrentUserCache;
import top.nomelin.iot.common.Constants;
import top.nomelin.iot.common.enums.CodeMessage;
import top.nomelin.iot.common.exception.BusinessException;
import top.nomelin.iot.dao.DeviceMapper;
import top.nomelin.iot.dao.IoTDBDao;
import top.nomelin.iot.dao.TemplateMapper;
import top.nomelin.iot.model.Device;
import top.nomelin.iot.model.Template;
import top.nomelin.iot.service.DeviceService;
import top.nomelin.iot.service.MessageService;
import top.nomelin.iot.service.TemplateService;
import top.nomelin.iot.service.storage.StorageStrategy;
import top.nomelin.iot.service.storage.StorageStrategyManager;
import top.nomelin.iot.util.SagaExecutor;

import java.util.List;
import java.util.Objects;

/**
 * TemplateServiceImpl
 *
 * @author nomelin
 * @since 2024/12/27 16:43
 **/
@Service
public class TemplateServiceImpl implements TemplateService {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(TemplateServiceImpl.class);

    private final TemplateMapper templateMapper;
    private final IoTDBDao iotDBDao;

    private final MessageService messageService;

    private final DeviceMapper deviceMapper;


    // 新增策略管理器
    private final StorageStrategyManager strategyManager;


    private final CurrentUserCache currentUserCache;

    public TemplateServiceImpl(TemplateMapper templateMapper, IoTDBDao iotDBDao, MessageService messageService, DeviceMapper deviceMapper, StorageStrategyManager strategyManager, CurrentUserCache currentUserCache) {
        this.templateMapper = templateMapper;
        this.iotDBDao = iotDBDao;
        this.messageService = messageService;
        this.deviceMapper = deviceMapper;
        this.strategyManager = strategyManager;
        this.currentUserCache = currentUserCache;
    }

    /**
     * 检查用户是否有权限访问模板
     */
    @Override
    public Template checkPermission(int templateId) {
        Template template = templateMapper.selectById(templateId);
        if (ObjectUtil.isNull(template)) {
            log.warn("template不存在，templateId: {}", templateId);
            throw new BusinessException(CodeMessage.NOT_FOUND_ERROR);
        }
        if (!Objects.equals(template.getUserId(), currentUserCache.getCurrentUser().getId())) {
            log.warn("当前用户没有权限访问该模板，templateId: {}, userId: {}", templateId, currentUserCache.getCurrentUser().getId());
            throw new BusinessException(CodeMessage.NO_PERMISSION_ERROR);
        }
        return template;
    }

    @Override
    public Template createTemplate(Template template) {
        checkTemplate(template);
        //插入mysql。因为后面要使用id，所以必须先插入mysql。
        template.setUserId(currentUserCache.getCurrentUser().getId());
        Integer userId = template.getUserId();

        SagaExecutor saga = new SagaExecutor(messageService, userId, "创建模板");

        saga.addStep(
                ctx -> "mysql中插入模板，name：" + template.getName(),
                ctx -> {
                    templateMapper.insert(template);
                    ctx.put("templateId", template.getId());//将templateId放入上下文，供后续步骤使用。
                    log.info("[添加模板|SAGA正向]mysql中插入模板成功，template: {}", template);
                },
                ctx -> {
                    templateMapper.delete(template.getId());
                    log.info("[添加模板|SAGA补偿]mysql中删除模板成功，templateId: {}", ctx.get("templateId"));
                }
        );

        //template.config.storageMode只是创建设备时的默认值，每个设备的实际存储模式由设备自身的配置决定。

        // 预先对所有策略创建对应的iotdb模板
        List<StorageStrategy> strategies = strategyManager.getAllStrategies();
        // 预处理模板节点类型。所以iotdb中的模板类型和mysql中记录的模板类型不一定一致。
        List<MeasurementNode> originalNodes = template.getConfig().convertToMeasurementNodes();
        for (StorageStrategy strategy : strategies) {
            List<MeasurementNode> processedNodes = strategy.preprocessTemplateNodes(originalNodes);
            addTagNode(processedNodes);//增加tag属性，一定要在策略处理之后再添加。
            // 创建IoTDB模板
            saga.addStep(
                    ctx -> {
                        Integer tid = (Integer) ctx.get("templateId");
                        return "创建IoTDB模板：" + Constants.TEMPLATE_PREFIX + tid + strategy.getTemplateSuffix();
                    },
                    ctx -> {
                        Integer tid = (Integer) ctx.get("templateId");
                        String templateName = Constants.TEMPLATE_PREFIX + tid + strategy.getTemplateSuffix();
                        iotDBDao.createSchema(templateName, processedNodes);
                        log.info("[添加模板|SAGA正向]创建iotdb模板成功，templateName: {}, measurementNodes: {}, strategy: {}",
                                templateName,
                                processedNodes.stream().map(MeasurementNode::getName).toList(), strategy.getClass().getSimpleName());
                    },
                    ctx -> {
                        Integer tid = (Integer) ctx.get("templateId");
                        String templateName = Constants.TEMPLATE_PREFIX + tid + strategy.getTemplateSuffix();
                        iotDBDao.deleteSchema(templateName);
                        log.info("[添加模板|SAGA补偿]删除iotdb模板成功，templateName: {}", templateName);
                    }
            );
        }
        saga.execute();
        log.info("全部策略创建iotdb模板成功，template: {}", template);
        return template;
    }

    private void addTagNode(List<MeasurementNode> nodes) {
        // 默认会增加一个tag属性。
        nodes.removeIf(node -> node.getName().equals(Constants.TAG));//删除策略中可能转换过的tag属性
        MeasurementNode tagNode = new MeasurementNode(
                Constants.TAG,
                TSDataType.TEXT,
                TSEncoding.PLAIN,
                Constants.COMPRESSION_TYPE
        );
        nodes.add(tagNode);
    }


    @Override
    public void updateTemplateName(int templateId, String name) {
        checkPermission(templateId);
        Template template = new Template();
        template.setId(templateId);
        template.setName(name);
        templateMapper.update(template);
        log.info("更新模板名称成功，templateId: {}, name: {}", templateId, name);
    }

    @Override
    public void deleteTemplate(int templateId) {
        Template template = checkPermission(templateId);
        Integer userId = currentUserCache.getCurrentUser().getId();

        SagaExecutor saga = new SagaExecutor(messageService, userId, "删除模板");

        saga.put("template", template);
        List<StorageStrategy> strategies = strategyManager.getAllStrategies();

        // 获取关联设备
        List<Device> relatedDevices = deviceMapper.selectByTemplateId(templateId);
        saga.put("relatedDevices", relatedDevices);

        // 删除每个设备，并添加补偿
        // 只能直接操作mysql设备表，不能使用service封装的方法，因为里面有其它逻辑。
        for (Device device : relatedDevices) {
            int deviceId = device.getId();
            saga.addStep(
                    ctx -> "删除设备：" + device.getName() + ", id: " + deviceId,
                    ctx -> {
                        deviceMapper.delete(deviceId);
                        log.info("[删除模板|SAGA正向] 删除设备行成功，deviceId: {}", deviceId);
                    },
                    ctx -> {
                        deviceMapper.insertWithId(device);//不能使用insert，需要指定主键以恢复原记录
                        log.info("[删除模板|SAGA补偿] 恢复设备成功，deviceId: {}", device.getId());
                    }
            );
        }


        // 删除 MySQL 中的模板记录
        // 不能改变顺序，否则由于外键约束，会导致删除失败。
        saga.addStep(
                ctx -> "删除MySQL模板行，id: " + templateId,
                ctx -> {
                    templateMapper.delete(templateId);
                    log.info("[删除模板|SAGA正向]删除MySQL模板成功，templateId: {}", templateId);
                },
                ctx -> {
                    Template origin = (Template) ctx.get("template");
                    templateMapper.insertWithId(origin);//不能使用insert，需要指定主键以恢复原记录
                    log.info("[删除模板|SAGA补偿]恢复MySQL模板成功，templateId: {}", origin.getId());
                }
        );

        // 删除 IoTDB 模板（多个策略）
        for (StorageStrategy strategy : strategies) {
            String templateName = Constants.TEMPLATE_PREFIX + templateId + strategy.getTemplateSuffix();
            saga.addStep(
                    ctx -> "删除IoTDB模板：" + templateName,
                    ctx -> {
                        iotDBDao.deleteSchema(templateName);
                        log.info("[删除模板|SAGA正向]删除IoTDB模板成功，templateName: {}", templateName);
                    },
                    ctx -> {
                        // 补偿逻辑：重建 IoTDB 模板
                        Template origin = (Template) ctx.get("template");
                        List<MeasurementNode> originNodes = origin.getConfig().convertToMeasurementNodes();
                        List<MeasurementNode> processedNodes = strategy.preprocessTemplateNodes(originNodes);
                        addTagNode(processedNodes); // 保证 tag 节点存在
                        iotDBDao.createSchema(templateName, processedNodes);
                        log.info("[删除模板|SAGA补偿]重建IoTDB模板成功，templateName: {}", templateName);
                    }
            );
        }

        //正向操作时先删除设备表，再删除模板表，符合外键约束。
        //恢复时是反向的，先插入模板表，再插入设备表，符合外键约束。
        saga.execute();
        log.info("Saga事务删除模板成功，templateId: {}", templateId);
    }


    @Override
    public Template getTemplateById(int templateId) {
        return checkPermission(templateId);
    }

    @Override
    public List<Template> getAllTemplates() {
        int userId = currentUserCache.getCurrentUser().getId();
        return templateMapper.selectByUserId(userId);
    }

    private void checkTemplate(Template template) {
        if (ObjectUtil.isNull(template)) {
            log.warn("模板不存在");
            throw new BusinessException(CodeMessage.PARAM_LOST_ERROR);
        }
        if (ObjectUtil.isNull(template.getName())) {
            log.warn("模板名称不能为空");
            throw new BusinessException(CodeMessage.PARAM_LOST_ERROR);
        }
        if (ObjectUtil.isNull(template.getConfig())) {
            log.warn("模板配置不能为空");
            throw new BusinessException(CodeMessage.PARAM_LOST_ERROR);
        }
        if (ObjectUtil.isNull(template.getConfig().getStorageMode())) {
            log.warn("模板存储模式不能为空");
            throw new BusinessException(CodeMessage.PARAM_LOST_ERROR);
        }
        if (ObjectUtil.isNull(template.getConfig().getDataTypes())) {
            log.warn("模板节点不能为空");
            throw new BusinessException(CodeMessage.PARAM_LOST_ERROR);
        }
        if (ObjectUtil.isNull(template.getConfig().getAggregationTime())) {
            log.warn("模板聚合时间不能为空");
            throw new BusinessException(CodeMessage.PARAM_LOST_ERROR);
        }
    }
}
