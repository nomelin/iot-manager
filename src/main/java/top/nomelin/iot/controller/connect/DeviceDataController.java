package top.nomelin.iot.controller.connect;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.nomelin.iot.common.Result;
import top.nomelin.iot.common.enums.CodeMessage;
import top.nomelin.iot.common.exception.BusinessException;
import top.nomelin.iot.service.buffer.BufferManager;

import java.util.Map;

@RestController
@RequestMapping("/connect")
public class DeviceDataController {

    private final BufferManager bufferManager;

    public DeviceDataController(BufferManager bufferManager) {
        this.bufferManager = bufferManager;
    }

    @RequestMapping("/uploadData")
    public Result uploadData(@RequestBody Map<String, Object> payload) {
        try {
            String deviceIdStr = payload.get("id").toString();
            int deviceId = Integer.parseInt(deviceIdStr);
            Map<String, Object> data = (Map<String, Object>) payload.get("data");

            bufferManager.addData(deviceId, data);
            return Result.success();
        } catch (Exception e) {
            throw new BusinessException(CodeMessage.UPLOAD_DATA_FAILED, "连接上传数据失败", e);
        }
    }
}