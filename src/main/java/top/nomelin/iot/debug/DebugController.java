package top.nomelin.iot.debug;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.nomelin.iot.cache.CacheOperations;
import top.nomelin.iot.common.Result;
import top.nomelin.iot.service.TaskService;

/**
 * DebugController
 *
 * @author nomelin
 * @since 2025/03/13 20:03
 **/
@RestController
@RequestMapping("/debug")
public class DebugController {
    private final CacheOperations cacheOperations;

    private final TaskService taskService;

    public DebugController(CacheOperations cacheOperations, TaskService taskService) {
        this.cacheOperations = cacheOperations;
        this.taskService = taskService;
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

}
