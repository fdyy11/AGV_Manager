package com.example.entity;

import java.io.Serializable;

/**
 * 地图节点实体
 */
public class MapNode implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer id;
    private String nodeId;           // 节点ID
    private Double x;                // X坐标
    private Double y;                // Y坐标
    private String nodeType;         // 节点类型
    private String description;      // 描述
    private Integer capacity;        // 容量
    private Boolean isAvailable;     // 是否可用

    // getter and setter methods
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getNodeId() { return nodeId; }
    public void setNodeId(String nodeId) { this.nodeId = nodeId; }

    public Double getX() { return x; }
    public void setX(Double x) { this.x = x; }

    public Double getY() { return y; }
    public void setY(Double y) { this.y = y; }

    public String getNodeType() { return nodeType; }
    public void setNodeType(String nodeType) { this.nodeType = nodeType; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }

    public Boolean getIsAvailable() { return isAvailable; }
    public void setIsAvailable(Boolean isAvailable) { this.isAvailable = isAvailable; }
}
