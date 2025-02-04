package top.nomelin.iot.model.dto;

import top.nomelin.iot.common.enums.CodeMessage;
import top.nomelin.iot.common.exception.SystemException;
import top.nomelin.iot.model.enums.FileTaskStatus;

import java.time.LocalDateTime;

/**
 * 任务状态机
 */
public abstract class Task {
    private String id;// 任务ID
    private FileTaskStatus status;// 状态

    private LocalDateTime startTime;// 开始时间
    private LocalDateTime endTime;// 结束时间
    private String errorMessage;// 错误信息


    /**
     * @return 任务进度百分比
     */
    public abstract double getProgressPercentage();

    public void queue() {
        if (this.status == FileTaskStatus.QUEUED) {
            return;
        }
        //只有还没有处于任何状态的任务才能排队
        if (this.status != null) {
            throw new SystemException(CodeMessage.STATE_MACHINE_ERROR, "任务不能queue，当前状态为" + this.status);
        }
        this.status = FileTaskStatus.QUEUED;
    }

    public void start() {
        if (this.status == FileTaskStatus.PROCESSING) {
            return;
        }
        //只有处于排队状态的任务才能开始
        if (this.status != FileTaskStatus.QUEUED) {
            throw new SystemException(CodeMessage.STATE_MACHINE_ERROR, "任务不能start，当前状态为" + this.status);
        }
        this.status = FileTaskStatus.PROCESSING;
        this.startTime = LocalDateTime.now();
    }

    public void pause() {
        if (this.status == FileTaskStatus.PAUSED) {
            return;
        }
        //只有处于处理状态的任务才能暂停
        if (this.status != FileTaskStatus.PROCESSING) {
            throw new SystemException(CodeMessage.STATE_MACHINE_ERROR, "任务不能pause，当前状态为" + this.status);
        }
        this.status = FileTaskStatus.PAUSED;
    }

    public void resume() {
        if (this.status == FileTaskStatus.PROCESSING) {
            return;
        }
        //只有处于暂停状态的任务才能恢复
        if (this.status != FileTaskStatus.PAUSED) {
            throw new SystemException(CodeMessage.STATE_MACHINE_ERROR, "任务不能resume，当前状态为" + this.status);
        }
        this.status = FileTaskStatus.PROCESSING;
    }

    public void complete() {
        if (this.status == FileTaskStatus.COMPLETED) {
            return;
        }
        //只有处于处理状态的任务才能完成
        if (this.status != FileTaskStatus.PROCESSING) {
            throw new SystemException(CodeMessage.STATE_MACHINE_ERROR, "任务不能complete，当前状态为" + this.status);
        }
        this.status = FileTaskStatus.COMPLETED;
        this.endTime = LocalDateTime.now();
    }

    public void cancel() {
        if (this.status == FileTaskStatus.CANCELLED) {
            return;
        }
        //除了已完成和已失败的任务，其他状态都可以取消
        if (this.status == FileTaskStatus.COMPLETED || this.status == FileTaskStatus.FAILED) {
            throw new SystemException(CodeMessage.STATE_MACHINE_ERROR, "任务不能cancel，当前状态为" + this.status);
        }
        this.status = FileTaskStatus.CANCELLED;
        this.endTime = LocalDateTime.now();
    }

    public void fail(String errorMessage) {
        if (this.status == FileTaskStatus.FAILED) {
            return;
        }
        //除了已完成和已取消的任务，其他状态都可以失败
        if (this.status == FileTaskStatus.COMPLETED || this.status == FileTaskStatus.CANCELLED) {
            throw new SystemException(CodeMessage.STATE_MACHINE_ERROR, "任务不能fail，当前状态为" + this.status);
        }
        this.status = FileTaskStatus.FAILED;
        this.endTime = LocalDateTime.now();
        this.errorMessage = errorMessage;
    }

    // getters and setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public FileTaskStatus getStatus() {
        return status;
    }

    public void setStatus(FileTaskStatus status) {
        this.status = status;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
