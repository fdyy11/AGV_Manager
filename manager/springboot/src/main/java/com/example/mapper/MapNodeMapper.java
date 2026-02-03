package com.example.mapper;

import com.example.entity.MapEdge;
import com.example.entity.MapNode;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MapNodeMapper {

    /**
     * 根据节点ID查询节点信息
     */
    MapNode selectByNodeId(@Param("nodeId") String nodeId);

    /**
     * 获取相邻节点列表
     */
    List<String> getNeighbors(@Param("nodeId") String nodeId);

    /**
     * 获取所有节点
     */
    List<MapNode> selectAllNodes();

    /**
     * 插入地图节点
     */
    void insertNode(MapNode node);

    /**
     * 清空所有节点
     */
    void deleteAllNodes();

    /**
     * 获取所有边
     */
    List<MapEdge> selectAllEdges();

    /**
     * 插入地图边
     */
    void insertEdge(MapEdge edge);

    /**
     * 清空所有边
     */
    void deleteAllEdges();
}
