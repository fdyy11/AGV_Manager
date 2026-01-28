package com.example.controller;

import com.example.common.Result;
import com.example.entity.MapNode;
import com.example.service.MapService;
import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;
import java.util.List;

/**
 * 工厂地图管理接口
 **/
@RestController
@RequestMapping("/map")
public class MapController {

    @Resource
    private MapService mapService;

    /**
     * 获取所有地图节点
     */
    @GetMapping("/nodes")
    public Result getAllNodes() {
        List<MapNode> nodes = mapService.getAllNodes();
        return Result.success(nodes);
    }

    /**
     * 获取地图边（路径）
     */
    @GetMapping("/edges")
    public Result getAllEdges() {
        List<MapNode> edges = mapService.getAllEdges();
        return Result.success(edges);
    }

    /**
     * 计算最优路径
     */
    @GetMapping("/path")
    public Result getPath(@RequestParam String start, @RequestParam String end) {
        List<String> path = mapService.findOptimalPath(start, end);
        return Result.success(path);
    }
}
