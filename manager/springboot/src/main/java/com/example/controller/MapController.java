package com.example.controller;

import com.example.common.Result;
import com.example.entity.MapEdge;
import com.example.entity.MapNode;
import com.example.service.MapService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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

    // 在MapController.java中添加按类型获取节点的API
    /**
     * 根据节点类型获取地图节点
     */
    @GetMapping("/nodes/type/{nodeType}")
    public Result getNodesByType(@PathVariable String nodeType) {
        List<MapNode> nodes = mapService.getNodesByType(nodeType);
        return Result.success(nodes);
    }


    /**
     * 获取地图边（路径）
     */
    @GetMapping("/edges")
    public Result getAllEdges() {
        List<MapEdge> edges = mapService.getAllEdges();
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

    /**
     * 导入地图文件
     */
    @PostMapping("/import")
    public Result importMap(@RequestParam("file") MultipartFile file) {
        try {
            mapService.importMapFromDbhFile(file);
            return Result.success("地图导入成功");
        } catch (Exception e) {
            return Result.error("201", "地图导入失败: " + e.getMessage());
        }
    }

}
