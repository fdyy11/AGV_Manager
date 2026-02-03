package com.example.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.io.Serializable;
import java.util.Date;

/**
 * 任务实体
 */
public class Task implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer id;
    private String taskId;           // 任务编号
    private String taskType;         // 任务类型
    private String materialType;     // 物料类型
    private Integer quantity;        // 数量
    private String startPoint;       // 起点位置
    private String endPoint;         // 终点位置
    private String agvId;            // 分配的AGV
    private String status;           // 任务状态
    private String priority;         // 优先级

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;         // 创建时间

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date startTime;          // 开始时间

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date endTime;            // 结束时间

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date needTime;           // 需求节拍时间

    // getter and setter methods
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }

    public String getTaskType() { return taskType; }
    public void setTaskType(String taskType) { this.taskType = taskType; }

    public String getMaterialType() { return materialType; }
    public void setMaterialType(String materialType) { this.materialType = materialType; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public String getStartPoint() { return startPoint; }
    public void setStartPoint(String startPoint) { this.startPoint = startPoint; }

    public String getEndPoint() { return endPoint; }
    public void setEndPoint(String endPoint) { this.endPoint = endPoint; }

    public String getAgvId() { return agvId; }
    public void setAgvId(String agvId) { this.agvId = agvId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }

    public Date getStartTime() { return startTime; }
    public void setStartTime(Date startTime) { this.startTime = startTime; }

    public Date getEndTime() { return endTime; }
    public void setEndTime(Date endTime) { this.endTime = endTime; }

    public Date getNeedTime() { return needTime; }
    public void setNeedTime(Date needTime) { this.needTime = needTime; }
}
