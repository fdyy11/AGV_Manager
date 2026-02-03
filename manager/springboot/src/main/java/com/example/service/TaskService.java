package com.example.service;

import com.example.entity.Task;
import com.github.pagehelper.PageInfo;

import java.util.List;

/**
 * 任务服务接口
 */
public interface TaskService {

    /**
     * 创建任务
     */
    void createTask(Task task);

    /**
     * 分配任务给AGV
     */
    void assignTask(Task task);

    /**
     * 更新任务状态
     */
    void updateTaskStatus(Task task);

    /**
     * 分页查询任务
     */
    PageInfo<Task> selectPage(Task task, Integer pageNum, Integer pageSize);

    /**
     * 获取待处理任务
     */
    List<Task> getPendingTasks();

    /**
     * 根据ID查询任务
     */
    Task selectById(Integer id);

    /**
     * 根据条件查询所有任务
     */
    List<Task> selectAll(Task task);

    /**
     * 删除任务
     */
    void deleteById(Integer id);

    /**
     * 更新任务
     */
    void updateById(Task task);
}
