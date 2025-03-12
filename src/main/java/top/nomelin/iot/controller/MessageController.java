package top.nomelin.iot.controller;

import cn.hutool.core.util.ObjectUtil;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.nomelin.iot.common.Result;
import top.nomelin.iot.common.enums.CodeMessage;
import top.nomelin.iot.common.exception.BusinessException;
import top.nomelin.iot.model.enums.MessageStatus;
import top.nomelin.iot.model.enums.MessageType;
import top.nomelin.iot.service.MessageService;

/**
 * MessageController
 * 消息管理控制器
 *
 * @author nomelin
 * @since 2025/03/12
 **/
@RestController
@RequestMapping("/message")
public class MessageController {
    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    /**
     * 获取消息详情
     */
    @RequestMapping("/get/{id}")
    public Result getMessage(@PathVariable Integer id) {
        if (ObjectUtil.isNull(id)) {
            throw new BusinessException(CodeMessage.PARAM_LOST_ERROR, "查询消息失败，id为空");
        }
        return Result.success(messageService.getMessage(id));
    }

    /**
     * 标记消息状态（已读/删除/标记）
     */
    @RequestMapping("/mark/{id}")
    public Result markMessageStatus(@PathVariable Integer id, @RequestParam MessageStatus status) {
        if (ObjectUtil.isNull(id) || ObjectUtil.isNull(status)) {
            throw new BusinessException(CodeMessage.PARAM_LOST_ERROR, "标记消息状态失败，参数为空");
        }
        messageService.markMessageStatus(id, status);
        return Result.success();
    }

    /**
     * 获取简略消息列表（不包含内容）
     *
     * @param type   消息类型，null表示全部。
     * @param status 消息状态，null表示全部，但是不包括已删除的消息。
     */
    @RequestMapping("/list/simple")
    public Result getAllSimpleMessages(@RequestParam(required = false) MessageType type,
                                       @RequestParam(required = false) MessageStatus status) {
        if (status == MessageStatus.DELETED) {
            throw new BusinessException(CodeMessage.PARAM_LOST_ERROR, "查询消息失败，不能查询已删除的消息");
        }
        return Result.success(messageService.getAllSimpleMessages(type, status));
    }

    /**
     * 获取简略消息列表（不包含内容）
     *
     * @param type    消息类型，null表示全部。
     * @param status  消息状态，null表示全部，但是不包括已删除的消息。
     * @param keyword 搜索关键字，模糊搜索标题和内容。
     */
    @RequestMapping("/list/simple/search")
    public Result searchSimpleMessages(@RequestParam(required = false) MessageType type,
                                       @RequestParam(required = false) MessageStatus status,
                                       @RequestParam() String keyword) {
        if (status == MessageStatus.DELETED) {
            throw new BusinessException(CodeMessage.PARAM_LOST_ERROR, "查询消息失败，不能查询已删除的消息");
        }
        return Result.success(messageService.getAllSimpleMessages(type, status, keyword));
    }


    /**
     * 获取未读消息数量
     */
    @RequestMapping("/unread-count")
    public Result getUnreadCount() {
        return Result.success(messageService.getUnreadCount());
    }
}
