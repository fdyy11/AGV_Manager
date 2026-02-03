// src/main/java/com/example/entity/MapEdge.java
package com.example.entity;

import java.io.Serializable;

/**
 * 地图边（路径）实体
 */
public class MapEdge implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer id;
    private String fromNodeId;    // 起始节点ID
    private String toNodeId;      // 目标节点ID
    private Double weight;        // 权重（距离）
    private String edgeType;      // 边类型

    // getter and setter methods
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getFromNodeId() { return fromNodeId; }
    public void setFromNodeId(String fromNodeId) { this.fromNodeId = fromNodeId; }

    public String getToNodeId() { return toNodeId; }
    public void setToNodeId(String toNodeId) { this.toNodeId = toNodeId; }

    public Double getWeight() { return weight; }
    public void setWeight(Double weight) { this.weight = weight; }

    public String getEdgeType() { return edgeType; }
    public void setEdgeType(String edgeType) { this.edgeType = edgeType; }
}
