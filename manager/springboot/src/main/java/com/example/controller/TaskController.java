package com.example.controller;

import com.example.common.Result;
import com.example.entity.Task;
import com.example.service.TaskService;
import com.github.pagehelper.PageInfo;
import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;
import java.util.List;

/**
 * 任务管理前端操作接口
 **/
@RestController
@RequestMapping("/task")
public class TaskController {

    @Resource
    private TaskService taskService;

    /**
     * 创建任务
     */
    @PostMapping("/create")
    public Result createTask(@RequestBody Task task) {
        taskService.createTask(task);
        return Result.success();
    }

    /**
     * 分配任务
     */
    @PostMapping("/assign")
    public Result assignTask(@RequestBody Task task) {
        taskService.assignTask(task);
        return Result.success();
    }

    /**
     * 更新任务状态
     */
    @PutMapping("/updateStatus")
    public Result updateStatus(@RequestBody Task task) {
        taskService.updateTaskStatus(task);
        return Result.success();
    }

    /**
     * 分页查询任务
     */
    @GetMapping("/selectPage")
    public Result selectPage(Task task,
                             @RequestParam(defaultValue = "1") Integer pageNum,
                             @RequestParam(defaultValue = "10") Integer pageSize) {
        PageInfo<Task> page = taskService.selectPage(task, pageNum, pageSize);
        return Result.success(page);
    }

    /**
     * 获取待处理任务
     */
    @GetMapping("/pendingTasks")
    public Result getPendingTasks() {
        List<Task> tasks = taskService.getPendingTasks();
        return Result.success(tasks);
    }
}
