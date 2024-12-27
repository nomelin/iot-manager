package top.nomelin.iot.controller;

import cn.hutool.core.util.ObjectUtil;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.nomelin.iot.common.Result;
import top.nomelin.iot.common.enums.CodeMessage;
import top.nomelin.iot.common.exception.BusinessException;
import top.nomelin.iot.model.Template;
import top.nomelin.iot.service.TemplateService;

/**
 * TemplateController
 *
 * @author nomelin
 * @since 2024/12/27 17:26
 **/
@RestController
@RequestMapping("/template")
public class TemplateController {
    private final TemplateService templateService;


    public TemplateController(TemplateService templateService) {
        this.templateService = templateService;
    }


    @RequestMapping("/add")
    public Result addTemplate(@RequestBody Template template) {
        if (ObjectUtil.isNull(template) || ObjectUtil.isEmpty(template.getName())) {
            throw new BusinessException(CodeMessage.PARAM_LOST_ERROR);
        }
        return Result.success(templateService.createTemplate(template));
    }

    @RequestMapping("/delete/{templateId}")
    public Result deleteTemplate(@PathVariable Integer templateId) {
        if (ObjectUtil.isNull(templateId)) {
            throw new BusinessException(CodeMessage.PARAM_LOST_ERROR);
        }
        templateService.deleteTemplate(templateId);
        return Result.success();
    }

    @RequestMapping("/get/{templateId}")
    public Result getTemplateById(@PathVariable Integer templateId) {
        if (ObjectUtil.isNull(templateId)) {
            throw new BusinessException(CodeMessage.PARAM_LOST_ERROR);
        }
        return Result.success(templateService.getTemplateById(templateId));
    }

    @RequestMapping("/all")
    public Result getAllTemplates() {
        return Result.success(templateService.getAllTemplates());
    }
}
