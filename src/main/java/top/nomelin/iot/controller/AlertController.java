package top.nomelin.iot.controller;

import cn.hutool.core.util.ObjectUtil;
import org.springframework.web.bind.annotation.*;
import top.nomelin.iot.common.Result;
import top.nomelin.iot.common.enums.CodeMessage;
import top.nomelin.iot.common.exception.BusinessException;
import top.nomelin.iot.model.alert.Alert;
import top.nomelin.iot.service.alert.AlertService;

@RestController
@RequestMapping("/alert")
public class AlertController {
    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @RequestMapping("/add")
    public Result addAlert(@RequestBody Alert alert) {
        if (ObjectUtil.isNull(alert)) {
            throw new BusinessException(CodeMessage.PARAM_LOST_ERROR);
        }
        if (!alert.isValid()) {
            throw new BusinessException(CodeMessage.INVALID_ALERT_RULE_ERROR, "alert rule is invalid: " + alert);
        }
        return Result.success(alertService.createAlert(alert));
    }

    @RequestMapping("/update")
    public Result updateAlert(@RequestBody Alert alert) {
        if (ObjectUtil.isNull(alert) || ObjectUtil.isNull(alert.getId())) {
            throw new BusinessException(CodeMessage.PARAM_LOST_ERROR);
        }
        return Result.success(alertService.updateAlert(alert));
    }

    @RequestMapping("/get/{id}")
    public Result getAlert(@PathVariable Integer id) {
        if (ObjectUtil.isNull(id)) {
            throw new BusinessException(CodeMessage.PARAM_LOST_ERROR);
        }
        return Result.success(alertService.getAlert(id));
    }

    @RequestMapping("/query")
    public Result listAlerts(@RequestBody Alert query) {
        return Result.success(alertService.listAlerts(query));
    }

    @RequestMapping("/all")
    public Result getAllAlerts() {
        return Result.success(alertService.getAllAlerts());
    }

    @RequestMapping("/delete/{id}")
    public Result deleteAlert(@PathVariable Integer id) {
        if (ObjectUtil.isNull(id)) {
            throw new BusinessException(CodeMessage.PARAM_LOST_ERROR);
        }
        alertService.deleteAlert(id);
        return Result.success();
    }

    @RequestMapping("/enable/{id}")
    public Result enableAlert(@PathVariable Integer id) {
        return toggleAlertStatus(id, true);
    }

    @RequestMapping("/disable/{id}")
    public Result disableAlert(@PathVariable Integer id) {
        return toggleAlertStatus(id, false);
    }

    private Result toggleAlertStatus(Integer id, boolean enable) {
        if (ObjectUtil.isNull(id)) {
            throw new BusinessException(CodeMessage.PARAM_LOST_ERROR);
        }
        Alert alert = alertService.getAlert(id);
        alert.setEnable(enable);
        return Result.success(alertService.updateAlert(alert));
    }
}