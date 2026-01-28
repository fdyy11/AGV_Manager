package com.example.service;

import com.example.entity.Agv;
import com.example.entity.MapNode;
import com.example.entity.Task;
import com.example.mapper.AgvMapper;
import com.example.mapper.MapNodeMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

/**
 * 路径规划服务
 **/
@Service
public class PathPlanningService {

    @Resource
    private AgvMapper agvMapper;

    @Resource
    private MapNodeMapper mapNodeMapper;

    /**
     * 查找最优路径（A*算法）
     */
    public List<String> findOptimalPath(String startNode, String endNode) {
        // A*算法实现
        Map<String, Double> gScore = new HashMap<>();
        Map<String, Double> fScore = new HashMap<>();
        Set<String> openSet = new HashSet<>();
        Set<String> closedSet = new HashSet<>();
        Map<String, String> cameFrom = new HashMap<>();

        gScore.put(startNode, 0.0);
        fScore.put(startNode, heuristicCostEstimate(startNode, endNode));
        openSet.add(startNode);

        while (!openSet.isEmpty()) {
            String current = getLowestFScoreNode(openSet, fScore);

            if (current.equals(endNode)) {
                return reconstructPath(cameFrom, current);
            }

            openSet.remove(current);
            closedSet.add(current);

            for (String neighbor : getNeighbors(current)) {
                if (closedSet.contains(neighbor)) continue;

                double tentativeGScore = gScore.get(current) + distanceBetween(current, neighbor);

                if (!openSet.contains(neighbor)) {
                    openSet.add(neighbor);
                } else if (tentativeGScore >= gScore.getOrDefault(neighbor, Double.MAX_VALUE)) {
                    continue;
                }

                cameFrom.put(neighbor, current);
                gScore.put(neighbor, tentativeGScore);
                fScore.put(neighbor, gScore.get(neighbor) + heuristicCostEstimate(neighbor, endNode));
            }
        }

        return new ArrayList<>(); // 无路径
    }

    /**
     * 查找最适合任务的AGV
     */
    public String findBestAgvForTask(Task task) {
        // 获取所有空闲的AGV
        Agv condition = new Agv();
        condition.setStatus("idle");
        List<Agv> idleAgvs = agvMapper.selectAll(condition);

        if (idleAgvs.isEmpty()) {
            return null; // 没有空闲AGV
        }

        String bestAgvId = null;
        double minDistance = Double.MAX_VALUE;

        // 选择距离起点最近的AGV
        for (Agv agv : idleAgvs) {
            if (agv.getCurrentLocation() != null) {
                List<String> path = findOptimalPath(agv.getCurrentLocation(), task.getStartPoint());
                if (!path.isEmpty()) {
                    double distance = calculatePathDistance(path);
                    if (distance < minDistance) {
                        minDistance = distance;
                        bestAgvId = agv.getAgvId();
                    }
                }
            }
        }

        return bestAgvId;
    }

    /**
     * 检测并解决路径冲突
     */
    public boolean detectAndResolveConflicts(String agvId1, String agvId2, List<String> path1, List<String> path2) {
        // 检测路径冲突
        Set<String> path1Set = new HashSet<>(path1);
        for (String node : path2) {
            if (path1Set.contains(node)) {
                // 发现冲突点，需要解决
                return resolveConflict(agvId1, agvId2, node, path1, path2);
            }
        }
        return true; // 无冲突
    }

    private boolean resolveConflict(String agvId1, String agvId2, String conflictNode, List<String> path1, List<String> path2) {
        // 简单的冲突解决策略：让一个AGV等待
        // 更复杂的策略可以包括重新规划路径
        System.out.println("检测到AGV " + agvId1 + " 和 AGV " + agvId2 + " 在节点 " + conflictNode + " 发生冲突");
        return true; // 假设冲突已解决
    }

    // 辅助方法
    private String getLowestFScoreNode(Set<String> openSet, Map<String, Double> fScore) {
        String lowest = null;
        double lowestScore = Double.MAX_VALUE;
        for (String node : openSet) {
            double score = fScore.getOrDefault(node, Double.MAX_VALUE);
            if (score < lowestScore) {
                lowest = node;
                lowestScore = score;
            }
        }
        return lowest;
    }

    private List<String> reconstructPath(Map<String, String> cameFrom, String current) {
        List<String> totalPath = new ArrayList<>();
        totalPath.add(current);
        while (cameFrom.containsKey(current)) {
            current = cameFrom.get(current);
            totalPath.add(0, current); // 在前面插入
        }
        return totalPath;
    }

    private double heuristicCostEstimate(String start, String goal) {
        // 简化的启发式估计（欧几里得距离）
        MapNode startNode = mapNodeMapper.selectByNodeId(start);
        MapNode goalNode = mapNodeMapper.selectByNodeId(goal);

        if (startNode != null && goalNode != null) {
            double dx = startNode.getX() - goalNode.getX();
            double dy = startNode.getY() - goalNode.getY();
            return Math.sqrt(dx * dx + dy * dy);
        }
        return 0.0;
    }

    private List<String> getNeighbors(String node) {
        // 获取相邻节点列表
        return mapNodeMapper.getNeighbors(node);
    }

    private double distanceBetween(String node1, String node2) {
        // 计算两个节点之间的距离
        MapNode n1 = mapNodeMapper.selectByNodeId(node1);
        MapNode n2 = mapNodeMapper.selectByNodeId(node2);

        if (n1 != null && n2 != null) {
            double dx = n1.getX() - n2.getX();
            double dy = n1.getY() - n2.getY();
            return Math.sqrt(dx * dx + dy * dy);
        }
        return Double.MAX_VALUE;
    }

    private double calculatePathDistance(List<String> path) {
        double totalDistance = 0.0;
        for (int i = 0; i < path.size() - 1; i++) {
            totalDistance += distanceBetween(path.get(i), path.get(i + 1));
        }
        return totalDistance;
    }
}
