package com.example.service.impl;

import com.example.entity.MapEdge;
import com.example.entity.MapNode;
import com.example.service.MapService;
import com.example.mapper.MapNodeMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MapServiceImpl implements MapService {

    @Autowired
    private MapNodeMapper mapNodeMapper;

    @Override
    public List<MapNode> getAllNodes() {
        return mapNodeMapper.selectAllNodes();
    }

    @Override
    public List<MapEdge> getAllEdges() {
        return mapNodeMapper.selectAllEdges();
    }

    @Override
    public List<MapNode> getNodesByType(String nodeType) {
        // 根据节点类型查询
        List<MapNode> allNodes = getAllNodes();
        return allNodes.stream()
                .filter(node -> node.getNodeType() != null &&
                        node.getNodeType().equalsIgnoreCase(nodeType))
                .collect(Collectors.toList());
    }

    @Override
    public List<String> findOptimalPath(String start, String end) {
        // 实现路径查找逻辑
        return new ArrayList<>();
    }

    /**
     * 导入.dbh地图文件
     */
    public void importMapFromDbhFile(MultipartFile file) throws IOException {
        java.io.File tempFile = java.nio.file.Files.createTempFile("temp_map", ".dbh").toFile();
        file.transferTo(tempFile);

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + tempFile.getAbsolutePath());
             Statement stmt = conn.createStatement()) {

            // 清空现有数据
            mapNodeMapper.deleteAllNodes();
            mapNodeMapper.deleteAllEdges();

            // 查询.dbh文件中的地图节点数据
            ResultSet rs = stmt.executeQuery("SELECT * FROM nodes");

            while (rs.next()) {
                MapNode node = new MapNode();
                node.setNodeId(rs.getString("node_id"));
                node.setX(rs.getDouble("x"));
                node.setY(rs.getDouble("y"));
                node.setNodeType(rs.getString("node_type"));
                node.setDescription(rs.getString("description"));

                // 插入数据库
                mapNodeMapper.insertNode(node);
            }

            // 查询.dbh文件中的地图边数据 - 使用实际字段名
            try {
                ResultSet edgeRs = stmt.executeQuery("SELECT * FROM edges");

                while (edgeRs.next()) {
                    MapEdge edge = new MapEdge();
                    // 映射实际字段名到实体类属性
                    edge.setFromNodeId(edgeRs.getString("start_node_id")); // 实际字段名
                    edge.setToNodeId(edgeRs.getString("end_node_id"));     // 实际字段名
                    edge.setWeight(edgeRs.getDouble("distance"));         // 实际字段名
                    edge.setEdgeType(edgeRs.getString("direction"));      // 实际字段名

                    // 插入数据库
                    mapNodeMapper.insertEdge(edge);
                }

                System.out.println("成功导入路径数据");

            } catch (SQLException e) {
                // 如果edges表不存在，尝试其他可能的表名
                System.out.println("edges表不存在，尝试其他表名: " + e.getMessage());

                // 尝试可能的表名
                String[] possibleTableNames = {"edges", "connections", "paths", "links"};
                boolean imported = false;

                for (String tableName : possibleTableNames) {
                    try {
                        ResultSet edgeRs = stmt.executeQuery("SELECT * FROM " + tableName);

                        while (edgeRs.next()) {
                            MapEdge edge = new MapEdge();

                            // 尝试不同的字段名映射
                            try {
                                edge.setFromNodeId(edgeRs.getString("start_node_id"));
                            } catch (SQLException ex) {
                                try {
                                    edge.setFromNodeId(edgeRs.getString("from_node_id"));
                                } catch (SQLException ex2) {
                                    edge.setFromNodeId(edgeRs.getString("node1_id"));
                                }
                            }

                            try {
                                edge.setToNodeId(edgeRs.getString("end_node_id"));
                            } catch (SQLException ex) {
                                try {
                                    edge.setToNodeId(edgeRs.getString("to_node_id"));
                                } catch (SQLException ex2) {
                                    edge.setToNodeId(edgeRs.getString("node2_id"));
                                }
                            }

                            try {
                                edge.setWeight(edgeRs.getDouble("distance"));
                            } catch (SQLException ex) {
                                try {
                                    edge.setWeight(edgeRs.getDouble("weight"));
                                } catch (SQLException ex2) {
                                    edge.setWeight(1.0); // 默认权重
                                }
                            }

                            try {
                                edge.setEdgeType(edgeRs.getString("direction"));
                            } catch (SQLException ex) {
                                try {
                                    edge.setEdgeType(edgeRs.getString("edge_type"));
                                } catch (SQLException ex2) {
                                    edge.setEdgeType("normal"); // 默认类型
                                }
                            }

                            // 插入数据库
                            mapNodeMapper.insertEdge(edge);
                        }

                        imported = true;
                        System.out.println("成功从表 " + tableName + " 导入路径数据");
                        break;

                    } catch (SQLException tableEx) {
                        System.out.println("表 " + tableName + " 不存在: " + tableEx.getMessage());
                        continue;
                    }
                }

                if (!imported) {
                    System.out.println("未找到有效的路径表，将基于节点位置自动生成路径");
                    // 自动生成路径的逻辑
                    generatePathsFromNodes();
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("导入地图文件失败: " + e.getMessage(), e);
        } finally {
            // 删除临时文件
            tempFile.delete();
        }
    }

    /**
     * 根据节点位置自动生成路径
     */
    private void generatePathsFromNodes() {
        List<MapNode> nodes = mapNodeMapper.selectAllNodes();

        // 根据节点位置生成邻近节点间的连接
        for (int i = 0; i < nodes.size(); i++) {
            MapNode node1 = nodes.get(i);
            for (int j = i + 1; j < nodes.size(); j++) {
                MapNode node2 = nodes.get(j);

                // 计算两点间距离
                double distance = Math.sqrt(Math.pow(node1.getX() - node2.getX(), 2) +
                        Math.pow(node1.getY() - node2.getY(), 2));

                // 如果距离小于阈值，创建连接
                if (distance < 100) { // 可调整距离阈值
                    MapEdge edge = new MapEdge();
                    edge.setFromNodeId(node1.getNodeId());
                    edge.setToNodeId(node2.getNodeId());
                    edge.setWeight(distance);
                    edge.setEdgeType("auto-generated");

                    mapNodeMapper.insertEdge(edge);

                    // 为了支持双向路径，也添加反向连接
                    MapEdge reverseEdge = new MapEdge();
                    reverseEdge.setFromNodeId(node2.getNodeId());
                    reverseEdge.setToNodeId(node1.getNodeId());
                    reverseEdge.setWeight(distance);
                    reverseEdge.setEdgeType("auto-generated");

                    mapNodeMapper.insertEdge(reverseEdge);
                }
            }
        }
    }
}
