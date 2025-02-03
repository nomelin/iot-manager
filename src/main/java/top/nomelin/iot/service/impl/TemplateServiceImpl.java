package top.nomelin.iot.service.impl;

import cn.hutool.core.util.ObjectUtil;
import org.apache.iotdb.session.template.MeasurementNode;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;
import top.nomelin.iot.cache.CurrentUserCache;
import top.nomelin.iot.common.Constants;
import top.nomelin.iot.common.enums.CodeMessage;
import top.nomelin.iot.common.exception.BusinessException;
import top.nomelin.iot.dao.IoTDBDao;
import top.nomelin.iot.dao.TemplateMapper;
import top.nomelin.iot.model.Template;
import top.nomelin.iot.service.TemplateService;
import top.nomelin.iot.service.storage.StorageStrategy;
import top.nomelin.iot.service.storage.StorageStrategyManager;

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

    // 新增策略管理器
    private final StorageStrategyManager strategyManager;


    private final CurrentUserCache currentUserCache;

    public TemplateServiceImpl(TemplateMapper templateMapper, IoTDBDao iotDBDao, StorageStrategyManager strategyManager, CurrentUserCache currentUserCache) {
        this.templateMapper = templateMapper;
        this.iotDBDao = iotDBDao;
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
        //插入mysql
        template.setUserId(currentUserCache.getCurrentUser().getId());
        templateMapper.insert(template);
        log.info("mysql中插入模板成功，template: {}", template);
        //template.config.storageMode只是创建设备时的默认值，每个设备的实际存储模式由设备自身的配置决定。

        // 预先对所有策略创建对应的iotdb模板
        List<StorageStrategy> strategies = strategyManager.getAllStrategies();
        // 预处理模板节点类型。所以iotdb中的模板类型和mysql中记录的模板类型不一定一致。
        List<MeasurementNode> originalNodes = template.getConfig().convertToMeasurementNodes();
        for (StorageStrategy strategy : strategies) {
            List<MeasurementNode> processedNodes = strategy.preprocessTemplateNodes(originalNodes);
            // 创建IoTDB模板
            iotDBDao.createSchema(
                    Constants.TEMPLATE_PREFIX + template.getId() + strategy.getTemplateSuffix(),
                    processedNodes // 使用处理后的节点
            );
            log.info("创建iotdb模板成功，templateName: {}, measurementNodes: {}, strategy: {}",
                    Constants.TEMPLATE_PREFIX + template.getId() + strategy.getTemplateSuffix(),
                    processedNodes, strategy.getClass().getSimpleName());
        }
        log.info("全部策略创建iotdb模板成功，template: {}", template);
        return template;
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
        checkPermission(templateId);
        templateMapper.delete(templateId);
        log.info("删除模板成功，templateId: {}", templateId);
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
}
