package top.nomelin.iot.controller.debug;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.nomelin.iot.cache.CacheOperations;
import top.nomelin.iot.common.Result;
import top.nomelin.iot.common.enums.CodeMessage;
import top.nomelin.iot.common.exception.SystemException;
import top.nomelin.iot.dao.IoTDBDao;
import top.nomelin.iot.service.TaskService;
import top.nomelin.iot.service.alert.AlertService;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * DebugController
 *
 * @author nomelin
 * @since 2025/03/13 20:03
 **/
@RestController
@RequestMapping("/debug")
public class DebugController {
    public static final Logger log = org.slf4j.LoggerFactory.getLogger(DebugController.class);
    private final CacheOperations cacheOperations;

    private final TaskService taskService;

    private final AlertService alertService;

    private final IoTDBDao iotDBDao;
    private final JdbcTemplate jdbcTemplate;

    @Value("${file.tempDir}")
    private String tempDir;

    @Value("${iotdb.startPath}")
    private String startIoTDBPath;

    @Value("${iotdb.stopPath}")
    private String stopIoTDBPath;

    public DebugController(CacheOperations cacheOperations, TaskService taskService, AlertService alertService, IoTDBDao iotDBDao, JdbcTemplate jdbcTemplate) {
        this.cacheOperations = cacheOperations;
        this.taskService = taskService;
        this.alertService = alertService;
        this.iotDBDao = iotDBDao;
        this.jdbcTemplate = jdbcTemplate;
    }

    @RequestMapping("/hello")
    public Result hello() {
        return Result.success("hello,debugger");
    }

    @RequestMapping("/cache/listAllKeys")
    public Result listAllKeys() {
        return Result.success(cacheOperations.getAllKeys());
    }

    @RequestMapping("/cache/clearAll")
    public Result clearAll() {
        cacheOperations.clear();
        return Result.success();
    }

    @RequestMapping("/cache/getStats")
    public Result getStats() {
        return Result.success(cacheOperations.getStats());
    }

    @RequestMapping("/task/getAllTaskIds")
    public Result getAllTaskIds() {
        return Result.success(taskService.getAllTaskIds());
    }

    @RequestMapping("/task/getTask/{taskId}")
    public Result getTask(@PathVariable String taskId) {
        return Result.success(taskService.getTask(taskId));
    }

    @RequestMapping("/file/allTempFiles")
    public Result getTempDir() {
        File folder = new File(tempDir);
        if (!folder.exists() || !folder.isDirectory()) {
            return Result.success(Collections.emptyList());
        }
        List<String> fileNames = Arrays.stream(Objects.requireNonNull(folder.listFiles()))
                .filter(File::isFile)
                .map(File::getName)
                .collect(Collectors.toList());
        return Result.success(fileNames);
    }

    @RequestMapping("/file/deleteAllTempFiles")
    public Result deleteAllTempFiles() {
        File folder = new File(tempDir);
        if (!folder.exists() || !folder.isDirectory()) {
            return Result.success("临时文件夹不存在或不是文件夹.");
        }
        for (File file : Objects.requireNonNull(folder.listFiles())) {
            if (file.isFile()) {
                boolean deleted = file.delete();
                if (!deleted) {
                    log.error("删除临时文件失败: " + file.getName());
                }
            }
        }
        return Result.success("成功删除所有临时文件.");
    }

    @RequestMapping("/alert/getAlertStates")
    public Result getAlertStates() {
        return Result.success(alertService.getAlertStates());
    }

    @RequestMapping("/alert/clearAllAlerts")
    public Result clearAllAlerts() {
        alertService.clearAllAlertStates();
        return Result.success();
    }

    @RequestMapping("/iotdb/checkConnection")
    public Result checkIoTDBConnection() {
        try {
            iotDBDao.checkConnection();//aop 获取session失败返回，会直接抛出异常
            return Result.success(true);
        } catch (Exception e) {
            log.error("IoTDB 连接失败.", e);
            return Result.success(false);
        }
    }

    @RequestMapping("/mysql/checkConnection")
    public Result checkMySQLConnection() {
        try {
            jdbcTemplate.execute("SELECT 1");
            log.info("MySQL 连接成功.");
            return Result.success(true);
        } catch (Exception e) {
            log.error("MySQL 连接失败.", e);
            return Result.success(false);
        }
    }

    /**
     * 启动 IoTDB
     */
    @RequestMapping("/iotdb/start")
    public Result startIoTDB() {
        return executeCommand(startIoTDBPath);
    }

    /**
     * 停止 IoTDB
     */
    @RequestMapping("/iotdb/stop")
    public Result stopIoTDB() {
        return executeCommand(stopIoTDBPath);
    }

    /**
     * 重启 IoTDB
     */
    @RequestMapping("/iotdb/restart")
    public Result restartIoTDB() {
        Result stopResult = executeCommand(stopIoTDBPath);
        if (!"200".equals(stopResult.getCode())) {
            log.error("停止 IoTDB 失败：");
            throw new SystemException(CodeMessage.SYSTEM_ERROR, "停止 IoTDB 失败：");
        }
        try {
            Thread.sleep(3000); // 停止后等待, 再启动，避免端口未释放
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return executeCommand(startIoTDBPath);
    }


    /**
     * 执行外部命令
     */
    private Result executeCommand(String commandPath) {
        try {
            File scriptFile = new File(commandPath);
            if (!scriptFile.exists()) {
                log.error("命令文件不存在：" + commandPath);
                throw new SystemException(CodeMessage.NOT_FOUND_ERROR, "命令文件不存在：" + commandPath);
            }

            String os = System.getProperty("os.name").toLowerCase();
            ProcessBuilder builder;
            if (os.contains("win")) {
                // Windows 使用 cmd 启动 bat 文件
                builder = new ProcessBuilder("cmd.exe", "/c", "start", "/min", scriptFile.getAbsolutePath());
            } else {
                // Linux/Unix 使用 sh 执行脚本
                builder = new ProcessBuilder("sh", scriptFile.getAbsolutePath());
            }

            builder.redirectErrorStream(true);
            Process process = builder.start();
            // 异步处理输出流避免阻塞
            Executors.newSingleThreadExecutor().submit(() -> {
                try {
                    process.getInputStream().transferTo(System.out);
                } catch (IOException e) {
                    log.error("处理命令输出流失败", e);
                }
            });
            return Result.success("命令执行成功：" + commandPath);
        } catch (IOException e) {
            log.error("命令执行失败：" + commandPath, e);
            throw new SystemException(CodeMessage.SYSTEM_ERROR, "命令执行失败：" + commandPath);
        }
    }

}
