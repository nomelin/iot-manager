package top.nomelin.iot.controller.debug;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.nomelin.iot.cache.CacheOperations;
import top.nomelin.iot.common.Result;
import top.nomelin.iot.dao.IoTDBDao;
import top.nomelin.iot.service.TaskService;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
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

    private final IoTDBDao iotDBDao;
    private final JdbcTemplate jdbcTemplate;

    @Value("${file.tempDir}")
    private String tempDir;

    public DebugController(CacheOperations cacheOperations, TaskService taskService, IoTDBDao iotDBDao, JdbcTemplate jdbcTemplate) {
        this.cacheOperations = cacheOperations;
        this.taskService = taskService;
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

    @RequestMapping("/iotdb/checkConnection")
    public Result checkIoTDBConnection() {
        return Result.success(iotDBDao.checkConnection());
    }

    @RequestMapping("/mysql/checkConnection")
    public Result checkMySQLConnection() {
        try {
            jdbcTemplate.execute("SELECT 1");
            return Result.success(true);
        } catch (Exception e) {
            return Result.success(false);
        }
    }

}
