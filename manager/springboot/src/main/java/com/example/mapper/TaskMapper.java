package com.example.mapper;

import com.example.entity.Task;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 任务数据访问层
 */
public interface TaskMapper {

    /**
     * 插入任务
     */
    void insert(Task task);

    /**
     * 根据ID删除任务
     */
    void deleteById(Integer id);

    /**
     * 根据ID更新任务
     */
    void updateById(Task task);

    /**
     * 根据条件查询所有任务
     */
    List<Task> selectAll(Task task);

    /**
     * 根据ID查询任务
     */
    Task selectById(Integer id);
}
