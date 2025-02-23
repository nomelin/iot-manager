package top.nomelin.iot.model.dto;

/**
 * 文件处理任务
 *
 * @author nomelin
 * @since 2025/02/04 16:57
 **/
public class FileTask extends Task {
    private String fileName;// 文件名
    private int totalRows;// 总行数
    private int processedRows;// 已处理的行数

    public void incrementProgress() {
        this.processedRows++;
    }

    public void addProcessedRows(int batchSize) {
        this.processedRows += batchSize;
    }

    @Override
    public double getProgressPercentage() {
        return totalRows > 0 ? (processedRows * 100.0) / totalRows : 0;
    }

    // getters and setters

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    /**
     * 获取文件类型，返回fileName的最后一个"."后面的字符。不包含"."
     */
    public String getFileType() {
        return fileName.substring(fileName.lastIndexOf(".") + 1);
    }

    public int getTotalRows() {
        return totalRows;
    }

    public void setTotalRows(int totalRows) {
        this.totalRows = totalRows;
    }

    public int getProcessedRows() {
        return processedRows;
    }

    public void setProcessedRows(int processedRows) {
        this.processedRows = processedRows;
    }
}
