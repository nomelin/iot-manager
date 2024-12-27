package top.nomelin.iot.service;

import top.nomelin.iot.model.Template;

import java.util.List;

/**
 * TempleteService
 *
 * @author nomelin
 * @since 2024/12/27 15:28
 **/
public interface TemplateService {
    Template checkPermission(int templateId);

    Template createTemplate(Template template);

    void updateTemplateName(int templateId, String name);

    void deleteTemplate(int templateId);

    Template getTemplateById(int templateId);

    List<Template> getAllTemplates();


}
