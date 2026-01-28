package com.example.mapper;

import com.example.entity.MapNode;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface MapNodeMapper {

    /**
     * 根据节点ID查询节点信息
     */
    @Select("SELECT * FROM map_node WHERE node_id = #{nodeId}")
    MapNode selectByNodeId(@Param("nodeId") String nodeId);

    /**
     * 获取相邻节点列表
     */
    @Select("SELECT to_node_id FROM node_connection WHERE from_node_id = #{nodeId} AND status = 'active'")
    List<String> getNeighbors(@Param("nodeId") String nodeId);

    /**
     * 获取所有节点
     */
    @Select("SELECT * FROM map_node")
    List<MapNode> selectAllNodes();
}
