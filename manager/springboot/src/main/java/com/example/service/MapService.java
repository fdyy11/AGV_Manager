// src/main/java/com/example/service/MapService.java
package com.example.service;

import com.example.entity.MapNode;
import java.util.List;

public interface MapService {
    List<MapNode> getAllNodes();
    List<MapNode> getAllEdges();
    List<String> findOptimalPath(String start, String end);
}
