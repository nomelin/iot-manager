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
                        int skipRows, int mergeTimestampNum, int batchSize, String tag) throws IOException {
        // 包装为支持 mark/reset 的流
        BufferedInputStream bufferedStream = new BufferedInputStream(inputStream);
        bufferedStream.mark(Integer.MAX_VALUE);

        try {
            log.info("开始处理CSV文件: {}", task.getFileName());
            // 第一阶段：计算总行数
            int totalRows = calculateTotalRows(bufferedStream, skipRows);
            task.setTotalRows(totalRows);
            log.info("文件总数据行数: {}", totalRows);

            // 重置流到起始位置
            bufferedStream.reset();
            log.info("流已重置到起始位置");

            // 第二阶段：数据处理
            processData(bufferedStream, device, task, skipRows, mergeTimestampNum, batchSize, tag);
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

    private int calculateTotalRows(InputStream stream, int skipLines) throws IOException {
        int count = 0;
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));//此处不关闭流，后续还要用到。
        // 跳过指定行数
        for (int i = 0; i < skipLines; i++) {
            String line = reader.readLine();
            if (line == null) {
                break;
            }
        }
        reader.readLine(); // 跳过标题行
        while (reader.readLine() != null) {
            count++;
        }
        return count;
    }

    @LogExecutionTime
    private void processData(InputStream stream, Device device, FileTask task,
                             int skipLines, int mergeTimestampNum, int batchSize, String tag)
            throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        // 跳过指定行数
        for (int i = 0; i < skipLines; i++) {
            String line = reader.readLine();
            if (line == null) {
                throw new IOException("文件行数不足，无法跳过 " + skipLines + " 行");
            }
        }
        log.info("CSV文件{}已跳过 {} 行", task.getFileName(), skipLines);

        //手动读取表头
        String line = reader.readLine();
        if (line == null) {
            throw new IOException("文件行数不足，无法读取表头(时间戳与属性名行)");
        }
        String[] split = line.split(",");
        List<String> headers = new ArrayList<>(Arrays.asList(split));
        log.info("CSV文件{}的表头: {}", task.getFileName(), headers);

        // 配置 CSV 格式
        try (CSVParser parser = new CSVParser(
//                new InputStreamReader(stream),
                reader,
                CSVFormat.DEFAULT.builder()
//                        .setHeader()//自动解析标题行
//                        .setSkipHeaderRecord(false)// 不跳过标题行，标题行是文件表头
//                        .setCommentMarker('#')  // 设置注释标识符，以#开头的行将被忽略
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

            for (CSVRecord record : parser) {
                checkTaskStatus(task); // 状态检查

                processSingleRecord(record, timestamps, valuesBatch, dataTypes); // 传入dataTypes

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
                                     List<List<Object>> valuesBatch, List<IotDataType> dataTypes) {
        try {
            //将任意对象自动转换为毫秒时间戳
            long timestamp = TimestampConverter.convertToMillis(record.get(0));
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
                    String.format("第%d行数据格式错误", record.getRecordNumber()), e);
        }
    }

    private Object parseValue(String rawValue, IotDataType dataType) {
        try {
            return switch (dataType) {
                case INT -> Integer.parseInt(rawValue);
                case LONG -> Long.parseLong(rawValue);
                case FLOAT -> Float.parseFloat(rawValue);
                case DOUBLE -> Double.parseDouble(rawValue);
                default -> throw new SystemException(CodeMessage.DATA_FORMAT_ERROR,
                        "不支持的Iot数据类型: " + dataType);
            };
        } catch (NumberFormatException e) {
            throw new SystemException(CodeMessage.DATA_FORMAT_ERROR,
                    String.format("无法将值 '%s' 解析为类型 %s", rawValue, dataType), e);
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