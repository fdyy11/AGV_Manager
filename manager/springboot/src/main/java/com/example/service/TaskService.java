package com.example.service;

import cn.hutool.core.date.DateUtil;
import com.example.entity.Task;
import com.example.mapper.TaskMapper;
import com.example.service.AgvService;
import com.example.service.PathPlanningService;
import com.example.utils.TokenUtils;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import javax.annotation.Resource;
import org.springframework.stereotype.Service;
import java.util.List;

/**
 * 任务业务处理
 **/
@Service
public class TaskService {

    @Resource
    private TaskMapper taskMapper;

    @Resource
    private AgvService agvService;

    @Resource
    private PathPlanningService pathPlanningService;

    /**
     * 创建任务
     */
    public void createTask(Task task) {
        task.setTaskId(generateTaskId());
        task.setStatus("pending"); // 待分配
        task.setCreateTime(DateUtil.date());
        taskMapper.insert(task);
    }

    /**
     * 分配任务给AGV
     */
    public void assignTask(Task task) {
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
        }
    }

    /**
     * 更新任务状态
     */
    public void updateTaskStatus(Task task) {
        taskMapper.updateById(task);
    }

    /**
     * 分页查询任务
     */
    public PageInfo<Task> selectPage(Task task, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<Task> list = taskMapper.selectAll(task);
        return PageInfo.of(list);
    }

    /**
     * 获取待处理任务
     */
    public List<Task> getPendingTasks() {
        Task condition = new Task();
        condition.setStatus("pending");
        return taskMapper.selectAll(condition);
    }

    /**
     * 生成任务ID
     */
    private String generateTaskId() {
        return "TASK_" + System.currentTimeMillis();
    }
}
