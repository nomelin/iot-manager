package top.nomelin.iot.controller.request;

import top.nomelin.iot.model.Device;

/**
 * DeviceAddRequest
 *
 * @author nomelin
 * @since 2025/02/15 15:08
 **/
public class DeviceAddRequest {
    private Device device;
    private Integer templateId;

    public Device getDevice() {
        return device;
    }

    public void setDevice(Device device) {
        this.device = device;
    }

    public Integer getTemplateId() {
        return templateId;
    }

    public void setTemplateId(Integer templateId) {
        this.templateId = templateId;
    }
}
