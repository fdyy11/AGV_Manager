// src/main/java/com/example/service/impl/TaskServiceImpl.java
package com.example.service.impl;

import cn.hutool.core.date.DateUtil;
import com.example.entity.Task;
import com.example.mapper.TaskMapper;
import com.example.service.TaskService;
import com.example.service.AgvService;
import com.example.service.PathPlanningService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

/**
 * 任务业务处理实现类
 **/
@Service
@Transactional
public class TaskServiceImpl implements TaskService {

    private static final Logger log = LoggerFactory.getLogger(TaskServiceImpl.class);

    @Resource
    private TaskMapper taskMapper;

    @Resource
    private AgvService agvService;

    @Resource
    private PathPlanningService pathPlanningService;

    /**
     * 创建任务
     */
    @Override
    @Transactional
    public void createTask(Task task) {
        try {
            log.info("开始创建任务，任务信息: {}", task);

            if (task == null) {
                throw new IllegalArgumentException("任务信息不能为空");
            }

            task.setTaskId(generateTaskId());
            task.setStatus("pending"); // 待分配
            task.setCreateTime(DateUtil.date());

            log.info("准备插入任务到数据库，任务ID: {}", task.getTaskId());
            taskMapper.insert(task);
            log.info("任务创建成功，任务ID: {}", task.getTaskId());
        } catch (Exception e) {
            log.error("创建任务失败，任务信息: {}", task, e);
            throw new RuntimeException("创建任务失败: " + e.getMessage(), e);
        }
    }

    /**
     * 分配任务给AGV
     */
    @Override
    @Transactional
    public void assignTask(Task task) {
        try {
            log.info("开始分配任务，任务ID: {}", task.getId());

            if (task.getId() == null) {
                throw new IllegalArgumentException("任务ID不能为空");
            }

            // 使用调度算法选择最适合的AGV
            String selectedAgvId = pathPlanningService.findBestAgvForTask(task);
            if (selectedAgvId != null) {
                task.setAgvId(selectedAgvId);
                task.setStatus("assigned"); // 已分配
                task.setStartTime(DateUtil.date());

                // 更新任务
                taskMapper.updateById(task);

                // 通知AGV执行任务
                agvService.assignTaskToAgv(selectedAgvId, task.getTaskId());

                log.info("任务分配成功，任务ID: {}，AGV ID: {}", task.getTaskId(), selectedAgvId);
            } else {
                log.warn("没有找到合适的AGV来执行任务 {}", task.getTaskId());
            }
        } catch (Exception e) {
            log.error("分配任务失败，任务ID: {}", task.getId(), e);
            throw new RuntimeException("分配任务失败: " + e.getMessage(), e);
        }
    }

    /**
     * 更新任务状态
     */
    @Override
    @Transactional
    public void updateTaskStatus(Task task) {
        try {
            log.info("更新任务状态，任务ID: {}", task.getId());
            taskMapper.updateById(task);
        } catch (Exception e) {
            log.error("更新任务状态失败，任务ID: {}", task.getId(), e);
            throw new RuntimeException("更新任务状态失败: " + e.getMessage(), e);
        }
    }

    /**
     * 分页查询任务
     */
    @Override
    public PageInfo<Task> selectPage(Task task, Integer pageNum, Integer pageSize) {
        try {
            log.info("分页查询任务，页码: {}, 页大小: {}", pageNum, pageSize);
            PageHelper.startPage(pageNum, pageSize);
            List<Task> list = taskMapper.selectAll(task);
            return PageInfo.of(list);
        } catch (Exception e) {
            log.error("分页查询任务失败", e);
            throw new RuntimeException("分页查询任务失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取待处理任务
     */
    @Override
    public List<Task> getPendingTasks() {
        try {
            log.info("获取待处理任务");
            Task condition = new Task();
            condition.setStatus("pending");
            return taskMapper.selectAll(condition);
        } catch (Exception e) {
            log.error("获取待处理任务失败", e);
            throw new RuntimeException("获取待处理任务失败: " + e.getMessage(), e);
        }
    }

    /**
     * 根据ID查询任务
     */
    @Override
    public Task selectById(Integer id) {
        try {
            log.info("根据ID查询任务，ID: {}", id);
            return taskMapper.selectById(id);
        } catch (Exception e) {
            log.error("根据ID查询任务失败，ID: {}", id, e);
            throw new RuntimeException("查询任务失败: " + e.getMessage(), e);
        }
    }

    /**
     * 根据条件查询所有任务
     */
    @Override
    public List<Task> selectAll(Task task) {
        try {
            log.info("根据条件查询所有任务");
            return taskMapper.selectAll(task);
        } catch (Exception e) {
            log.error("根据条件查询所有任务失败", e);
            throw new RuntimeException("查询任务失败: " + e.getMessage(), e);
        }
    }

    /**
     * 删除任务
     */
    @Override
    @Transactional
    public void deleteById(Integer id) {
        try {
            log.info("删除任务，ID: {}", id);
            taskMapper.deleteById(id);
        } catch (Exception e) {
            log.error("删除任务失败，ID: {}", id, e);
            throw new RuntimeException("删除任务失败: " + e.getMessage(), e);
        }
    }

    /**
     * 更新任务
     */
    @Override
    @Transactional
    public void updateById(Task task) {
        try {
            log.info("更新任务，ID: {}", task.getId());
            taskMapper.updateById(task);
        } catch (Exception e) {
            log.error("更新任务失败，ID: {}", task.getId(), e);
            throw new RuntimeException("更新任务失败: " + e.getMessage(), e);
        }
    }

    /**
     * 生成任务ID
     */
    private String generateTaskId() {
        return "TASK_" + System.currentTimeMillis();
    }
}
