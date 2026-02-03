// src/main/java/com/example/service/PathPlanningService.java
package com.example.service;

import com.example.entity.Task;
import org.springframework.stereotype.Service;

@Service
public interface PathPlanningService {
    /**
     * 为任务找到最佳AGV
     */
    String findBestAgvForTask(Task task);
}
