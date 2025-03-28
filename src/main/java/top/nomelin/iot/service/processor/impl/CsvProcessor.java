package top.nomelin.iot.service.processor.impl;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import top.nomelin.iot.common.annotation.LogExecutionTime;
import top.nomelin.iot.common.enums.CodeMessage;
import top.nomelin.iot.common.exception.BusinessException;
import top.nomelin.iot.common.exception.SystemException;
import top.nomelin.iot.model.Device;
import top.nomelin.iot.model.dto.FileTask;
import top.nomelin.iot.model.enums.FileTaskStatus;
import top.nomelin.iot.model.enums.IotDataType;
import top.nomelin.iot.service.DataService;
import top.nomelin.iot.service.processor.FileProcessor;
import top.nomelin.iot.util.TimestampConverter;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;

@Component
public class CsvProcessor implements FileProcessor {
    private static final Logger log = LoggerFactory.getLogger(CsvProcessor.class);
    private final DataService dataService;

    public CsvProcessor(DataService dataService) {
        this.dataService = dataService;
    }

    @LogExecutionTime
    @Override
    public void process(InputStream inputStream, Device device, FileTask task,
                        int skipRows, int mergeTimestampNum, int batchSize, String tag,
                        int autoHandleErrorTimeStamp) throws IOException {
        // 包装为支持 mark/reset 的流
        BufferedInputStream bufferedStream = new BufferedInputStream(inputStream);
        bufferedStream.mark(Integer.MAX_VALUE);

        try {
            log.info("开始处理CSV文件: {}", task.getFileName());
            // 第一阶段：前置校验并计算总行数
            List<String> headers = new ArrayList<>();
            int totalRows = preCheckAndCalculateTotalRows(bufferedStream, device, skipRows, headers, autoHandleErrorTimeStamp);
            task.setTotalRows(totalRows);
            log.info("文件总数据行数: {}", totalRows);

            // 重置流到起始位置
            bufferedStream.reset();
            log.info("流已重置到起始位置");

            processData(bufferedStream, device, task, skipRows, mergeTimestampNum, batchSize, tag, autoHandleErrorTimeStamp);
            log.info("CSV文件处理完成: {}", task.getFileName());
        } catch (Exception e) {
            task.fail("CSV处理失败: " + e);
            throw new BusinessException(CodeMessage.FILE_HANDLER_ERROR, "CSV处理失败，文件名：" + task.getFileName(), e);
        } finally {
            // 确保流被关闭
            try {
                bufferedStream.close();
            } catch (IOException e) {
                log.warn("关闭BufferedInputStream失败: {}", e.getMessage());
            }
        }
    }

    @Override
    public String getSupportedType() {
        return "csv";
    }

    private int preCheckAndCalculateTotalRows(InputStream stream, Device device,
                                              int skipLines, List<String> headers,
                                              int autoHandleErrorTimeStamp) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        int lineNumber = 0;

        // 跳过指定的行
        for (int i = 0; i < skipLines; i++) {
            String line = reader.readLine();
            lineNumber++;
            if (line == null) {
                throw new IOException("文件行数不足，无法跳过" + skipLines + "行");
            }
        }

        // 读取表头行
        String headerLine = reader.readLine();
        lineNumber++;
        if (headerLine == null) {
            throw new IOException("无法读取表头行");
        }

        // 解析表头
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setIgnoreEmptyLines(true)
                .setTrim(true)
                .build();
        CSVParser headerParser = CSVParser.parse(headerLine, format);
        CSVRecord headerRecord;
        try {
            headerRecord = headerParser.iterator().next();
        } catch (NoSuchElementException e) {
            throw new IOException("表头行解析失败", e);
        } finally {
            headerParser.close();
        }

        headers.clear();
        for (String header : headerRecord) {
            headers.add(header.trim());
        }

        // 检查measurements的数据类型配置
        List<String> measurements = headers.subList(1, headers.size());
        for (String measurement : measurements) {
            if (!device.getConfig().getDataTypes().containsKey(measurement)) {
                throw new SystemException(CodeMessage.PARAM_LOST_ERROR,
                        "未找到测量项 '" + measurement + "' 的数据类型配置");
            }
        }

        // 创建CSVParser来读取剩余数据行
        CSVParser parser = new CSVParser(reader, format);
        int totalRows = 0;
        try {
            for (CSVRecord record : parser) {
                totalRows++;
                lineNumber++;

                // 检查列数
                if (record.size() != headers.size()) {
                    throw new SystemException(CodeMessage.DATA_FORMAT_ERROR,
                            "第" + lineNumber + "行列数与表头不符，预期" + headers.size() +
                                    "列，实际" + record.size() + "列");
                }

                // 仅在autoHandleErrorTimeStamp为0时检查时间戳
                if (autoHandleErrorTimeStamp == 0) {
                    try {
                        TimestampConverter.convertToMillis(record.get(0));
                    } catch (Exception e) {
                        throw new SystemException(CodeMessage.DATA_FORMAT_ERROR,
                                "第" + lineNumber + "行时间戳格式错误", e);
                    }
                }
            }
        } finally {
            //不关闭流
        }

        return totalRows;
    }

    @LogExecutionTime
    private void processData(InputStream stream, Device device, FileTask task,
                             int skipLines, int mergeTimestampNum, int batchSize, String tag,
                             int autoHandleErrorTimeStamp) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        int lineNumber = 0;

        // 跳过指定行数
        for (int i = 0; i < skipLines; i++) {
            String line = reader.readLine();
            lineNumber++;
            if (line == null) {
                throw new IOException("文件行数不足，无法跳过 " + skipLines + " 行");
            }
        }
        log.info("CSV文件{}已跳过 {} 行", task.getFileName(), skipLines);

        // 读取表头行
        String headerLine = reader.readLine();
        lineNumber++;
        if (headerLine == null) {
            throw new IOException("文件行数不足，无法读取表头(时间戳与属性名行)");
        }
        List<String> headers = Arrays.asList(headerLine.split(","));
        log.info("CSV文件{}的表头: {}", task.getFileName(), headers);

        try (CSVParser parser = new CSVParser(
                reader,
                CSVFormat.DEFAULT.builder()
                        .setIgnoreEmptyLines(true)
                        .setTrim(true)
                        .build()
        )) {
            List<String> measurements = headers.subList(1, headers.size()); // 跳过timestamp列
            log.info("CSV文件{}的属性列: {}", task.getFileName(), measurements);
            // 获取每个测量项的数据类型配置
            List<IotDataType> dataTypes = new ArrayList<>();
            for (String measurement : measurements) {
                IotDataType dataType = device.getConfig().getDataTypes().get(measurement);
                if (dataType == null) {
                    throw new SystemException(CodeMessage.PARAM_LOST_ERROR,
                            "未找到测量项 '" + measurement + "' 的数据类型配置");
                }
                dataTypes.add(dataType);
            }

            //时间戳和数据值的批次缓存列表
            List<Long> timestamps = new ArrayList<>(batchSize);
            List<List<Object>> valuesBatch = new ArrayList<>(batchSize);
            Long lastValidTimestamp = null;

            for (CSVRecord record : parser) {
                lineNumber++;
                checkTaskStatus(task); // 状态检查

                long currentTimestamp;
//                boolean timestampValid = true;
                try {
                    currentTimestamp = TimestampConverter.convertToMillis(record.get(0));
                    lastValidTimestamp = currentTimestamp;
                } catch (Exception e) {
//                    timestampValid = false;
                    //按照用户选择，处理错误时间戳数据行
                    if (autoHandleErrorTimeStamp == 0) {
                        throw new SystemException(CodeMessage.DATA_FORMAT_ERROR,
                                "第" + lineNumber + "行时间戳错误", e);
                    } else if (autoHandleErrorTimeStamp == 1 || autoHandleErrorTimeStamp == 2) {
                        if (lastValidTimestamp == null) {
                            if (autoHandleErrorTimeStamp == 1) {
                                throw new SystemException(CodeMessage.DATA_FORMAT_ERROR,
                                        "第" + lineNumber + "行时间戳错误，且没有有效的上一行时间戳", e);
                            } else {
                                log.warn("删除第{}行，时间戳错误且没有有效的上一行时间戳", lineNumber);
                                continue;
                            }
                        } else {
                            currentTimestamp = lastValidTimestamp;
                            log.warn("第{}行时间戳错误，已替换为上一行的有效时间戳{}", lineNumber, currentTimestamp);
                        }
                    } else if (autoHandleErrorTimeStamp == 3) {
                        log.warn("删除第{}行，时间戳错误", lineNumber);
                        continue;
                    } else {
                        throw new SystemException(CodeMessage.PARAM_ERROR,
                                "不支持的autoHandleErrorTimeStamp值: " + autoHandleErrorTimeStamp);
                    }
                }
                processSingleRecord(record, timestamps, valuesBatch, dataTypes, currentTimestamp, lineNumber);

                if (shouldFlushBatch(timestamps, batchSize)) {
                    flushBatch(device, measurements, timestamps, valuesBatch, task, mergeTimestampNum, tag);
                    updateProgress(task);
                }
            }

            // 处理剩余批次
            if (!timestamps.isEmpty()) {
                flushBatch(device, measurements, timestamps, valuesBatch, task, mergeTimestampNum, tag);
            }
        }
    }

    private void checkTaskStatus(FileTask task) {
        synchronized (task.lock) {
            while (task.getStatus() == FileTaskStatus.PAUSED) {
                try {
                    log.info("任务已暂停，等待继续：taskId={}", task.getId());
                    task.lock.wait(); // 释放锁并等待
                    log.info("任务继续执行：taskId={}", task.getId());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new SystemException(
                            CodeMessage.STATE_MACHINE_ERROR,
                            "处理线程被中断",
                            e
                    );
                }
            }
        }

        if (task.getStatus() == FileTaskStatus.CANCELLED) {
            log.info("任务已被取消，不再处理，taskId={}", task.getId());
            throw new SystemException(
                    CodeMessage.TASK_CANCELLED,
                    "任务已被取消"
            );
        }

    }

    // 解析单行数据，并将其添加到批次中
    private void processSingleRecord(CSVRecord record, List<Long> timestamps,
                                     List<List<Object>> valuesBatch, List<IotDataType> dataTypes,
                                     long timestamp, int lineNumber) {
        try {
            //将任意对象自动转换为毫秒时间戳
//            long timestamp = TimestampConverter.convertToMillis(record.get(0));
//            log.info("timestamp: {}->{}", record.get(0), timestamp);
            List<Object> values = new ArrayList<>();

            for (int i = 1; i < record.size(); i++) {
                if (i - 1 >= dataTypes.size()) {
                    throw new SystemException(CodeMessage.DATA_FORMAT_ERROR,
                            String.format("第%d行数据列数超过配置的测量项数量", record.getRecordNumber()));
                }
                IotDataType dataType = dataTypes.get(i - 1);
                String rawValue = record.get(i);
                Object value = parseValue(rawValue, dataType);
                values.add(value);
            }

            timestamps.add(timestamp);
            valuesBatch.add(values);
        } catch (Exception e) {
            throw new SystemException(CodeMessage.DATA_FORMAT_ERROR,
                    String.format("第%d行数据格式错误", lineNumber), e);
        }
    }

    private Object parseValue(String rawValue, IotDataType dataType) {
        try {
            return switch (dataType) {
                case INT -> Integer.parseInt(rawValue);
                case LONG -> Long.parseLong(rawValue);
                case FLOAT -> Float.parseFloat(rawValue);
                case DOUBLE -> Double.parseDouble(rawValue);
                case STRING -> rawValue;
                default -> throw new SystemException(CodeMessage.DATA_FORMAT_ERROR,
                        "不支持的Iot数据类型: " + dataType);
            };
        } catch (NumberFormatException e) {
//            throw new SystemException(CodeMessage.DATA_FORMAT_ERROR,
//                    String.format("无法将值 '%s' 解析为类型 %s", rawValue, dataType), e);
            log.warn("无法将值 '{}' 解析为类型 {}", rawValue, dataType);
            return null;
        }
    }

    private boolean shouldFlushBatch(List<Long> timestamps, int batchSize) {
        return timestamps.size() >= batchSize;
    }

    // 刷新批次数据到数据库
    @LogExecutionTime
    private void flushBatch(Device device, List<String> measurements,
                            List<Long> timestamps, List<List<Object>> valuesBatch,
                            FileTask task, int mergeTimestampNum, String tag) {
        try {
            dataService.insertBatchRecord(
                    device,
//                    new ArrayList<>(timestamps),
                    timestamps,
                    tag,
                    measurements,
//                    new ArrayList<>(valuesBatch)
                    valuesBatch,
                    mergeTimestampNum
            );
            task.addProcessedRows(timestamps.size());// 已处理行数增加
            log.info("-----成功插入批次数据，数量: {}", timestamps.size());

            timestamps.clear();
            valuesBatch.clear();
        } catch (Exception e) {
            log.error("批次数据插入失败: {}，error: {}", e.getMessage(), e);
            throw new SystemException(CodeMessage.INSERT_DATA_ERROR,
                    "批次数据插入失败", e);
        }
    }

    private void updateProgress(FileTask task) {
        log.info("任务进度: {}/{} ({}%)",
                task.getProcessedRows(),
                task.getTotalRows(),
                String.format("%.1f", task.getProgressPercentage()));
    }
}