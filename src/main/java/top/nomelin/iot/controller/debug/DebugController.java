package top.nomelin.iot.controller.debug;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.bind.annotation.GetMapping;
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
import java.util.*;
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

    private final ThreadPoolTaskExecutor executor;

    @Value("${file.tempDir}")
    private String tempDir;

    @Value("${iotdb.startPath}")
    private String startIoTDBPath;

    @Value("${iotdb.stopPath}")
    private String stopIoTDBPath;

    public DebugController(CacheOperations cacheOperations, TaskService taskService, AlertService alertService,
                           IoTDBDao iotDBDao, JdbcTemplate jdbcTemplate,
                           @Qualifier("fileProcessingExecutor") ThreadPoolTaskExecutor executor) {
        this.cacheOperations = cacheOperations;
        this.taskService = taskService;
        this.alertService = alertService;
        this.iotDBDao = iotDBDao;
        this.jdbcTemplate = jdbcTemplate;
        this.executor = executor;
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
            log.error("停止 IoTDB 失败");
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

    @GetMapping("/file-thread-pool/states")
    public Result getThreadPoolStates() {
        return Result.success(getThreadPoolStats());
    }

    private Map<String, Object> getThreadPoolStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("threadPoolName", executor.getThreadNamePrefix()); // 线程池名称
        stats.put("activeCount", executor.getActiveCount()); // 当前活动线程数
        stats.put("poolSize", executor.getPoolSize()); // 当前线程池大小
        stats.put("corePoolSize", executor.getCorePoolSize()); // 核心线程数
        stats.put("maxPoolSize", executor.getMaxPoolSize()); // 最大线程数
        stats.put("queueSize", executor.getThreadPoolExecutor().getQueue().size()); // 当前队列大小
        stats.put("queueCapacity", executor.getQueueCapacity()); // 队列容量
        stats.put("completedTaskCount", executor.getThreadPoolExecutor().getCompletedTaskCount()); // 已完成任务数
        stats.put("largestPoolSize", executor.getThreadPoolExecutor().getLargestPoolSize()); // 线程池曾达到的最大线程数
        stats.put("taskCount", executor.getThreadPoolExecutor().getTaskCount()); // 线程池已接收的任务总数
        stats.put("isShutdown", executor.getThreadPoolExecutor().isShutdown()); // 线程池是否已关闭
        stats.put("isTerminated", executor.getThreadPoolExecutor().isTerminated()); // 线程池是否已终止
        return stats;
    }

    @RequestMapping("/file-thread-pool/shutdown")
    public Result shutdownThreadPool() {
        if (executor.getThreadPoolExecutor().isShutdown()) {
            return Result.success("文件处理线程池已经关闭.");
        }
        executor.shutdown();
        log.info("文件处理线程池关闭中...");
        return Result.success("文件处理线程池关闭成功.");
    }

    @RequestMapping("/file-thread-pool/start")
    public Result startThreadPool() {
        if (!executor.getThreadPoolExecutor().isShutdown()) {
            return Result.success("文件处理线程池已经启动.");
        }
        executor.initialize();
        log.info("文件处理线程池启动中...");
        return Result.success("文件处理线程池启动成功.");
    }


}
