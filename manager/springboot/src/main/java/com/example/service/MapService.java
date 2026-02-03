// src/main/java/com/example/service/MapService.java
package com.example.service;

import com.example.entity.MapEdge;
import com.example.entity.MapNode;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface MapService {
    List<MapNode> getAllNodes();
    List<MapEdge> getAllEdges();
    List<String> findOptimalPath(String start, String end);
    List<MapNode> getNodesByType(String nodeType);
    /**
     * 导入地图文件
     */
    void importMapFromDbhFile(MultipartFile file) throws IOException;



}
