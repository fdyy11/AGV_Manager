// src/main/java/com/example/controller/TaskController.java
package com.example.controller;

import com.example.common.Result;
import com.example.entity.Task;
import com.example.service.TaskService;
import com.github.pagehelper.PageInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 任务管理控制器
 */
@RestController
@RequestMapping("/task")
public class TaskController {

    private static final Logger log = LoggerFactory.getLogger(TaskController.class);

    @Resource
    private TaskService taskService;

    /**
     * 创建任务
     */
    @PostMapping("/create")
    public Result createTask(@RequestBody Task task) {
        try {
            log.info("接收到创建任务请求: {}", task);
            taskService.createTask(task);
            log.info("任务创建成功，任务ID: {}", task.getTaskId());
            return Result.success(task);
        } catch (Exception e) {
            log.error("创建任务失败", e);
            return Result.error("201", "创建任务失败: " + e.getMessage());
        }
    }

    /**
     * 分配任务
     */
    @PostMapping("/assign")
    public Result assignTask(@RequestBody Task task) {
        try {
            log.info("接收到分配任务请求，任务ID: {}", task.getId());
            taskService.assignTask(task);
            log.info("任务分配成功，任务ID: {}", task.getId());
            return Result.success();
        } catch (Exception e) {
            log.error("分配任务失败", e);
            return Result.error("201", "分配任务失败: " + e.getMessage());
        }
    }

    /**
     * 更新任务状态
     */
    @PutMapping("/updateStatus")
    public Result updateTaskStatus(@RequestBody Task task) {
        try {
            log.info("接收到更新任务状态请求，任务ID: {}", task.getId());
            taskService.updateTaskStatus(task);
            return Result.success();
        } catch (Exception e) {
            log.error("更新任务状态失败", e);
            return Result.error("201", "更新任务状态失败: " + e.getMessage());
        }
    }

    /**
     * 分页查询任务
     */
    @GetMapping("/selectPage")
    public Result selectPage(Task task,
                             @RequestParam(defaultValue = "1") Integer pageNum,
                             @RequestParam(defaultValue = "10") Integer pageSize) {
        try {
            log.info("接收到分页查询任务请求，页码: {}, 页大小: {}", pageNum, pageSize);
            PageInfo<Task> page = taskService.selectPage(task, pageNum, pageSize);
            return Result.success(page);
        } catch (Exception e) {
            log.error("分页查询任务失败", e);
            return Result.error("201", "查询任务失败: " + e.getMessage());
        }
    }

    /**
     * 获取待处理任务
     */
    @GetMapping("/pending")
    public Result getPendingTasks() {
        try {
            log.info("接收到获取待处理任务请求");
            List<Task> tasks = taskService.getPendingTasks();
            return Result.success(tasks);
        } catch (Exception e) {
            log.error("获取待处理任务失败", e);
            return Result.error("201", "获取待处理任务失败: " + e.getMessage());
        }
    }
}
