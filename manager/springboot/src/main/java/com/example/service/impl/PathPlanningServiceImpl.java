// src/main/java/com/example/service/impl/PathPlanningServiceImpl.java
package com.example.service.impl;

import com.example.entity.Task;
import com.example.service.AgvService;
import com.example.service.PathPlanningService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PathPlanningServiceImpl implements PathPlanningService {

    private static final Logger log = LoggerFactory.getLogger(PathPlanningServiceImpl.class);

    @Autowired
    private AgvService agvService;

    @Override
    public String findBestAgvForTask(Task task) {
        try {
            log.info("为任务查找最佳AGV，任务ID: {}", task.getTaskId());

            // 获取所有在线的AGV
            List<com.example.entity.Agv> onlineAgvs = agvService.getOnlineAgvs();

            if (onlineAgvs != null && !onlineAgvs.isEmpty()) {
                // 返回第一个在线AGV的ID
                String bestAgvId = onlineAgvs.get(0).getAgvId();
                log.info("找到最佳AGV: {}", bestAgvId);
                return bestAgvId;
            } else {
                log.warn("没有找到在线的AGV");
                return null;
            }
        } catch (Exception e) {
            log.error("查找最佳AGV时发生错误", e);
            return null;
        }
    }
}
