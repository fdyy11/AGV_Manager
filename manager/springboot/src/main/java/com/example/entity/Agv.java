// src/main/java/com/example/entity/Agv.java
package com.example.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.io.Serializable;
import java.util.Date;

public class Agv implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer id;
    private String agvId;            // AGV编号
    private String currentLocation;  // 当前位置
    private String status;           // 状态
    private Integer batteryLevel;    // 电量
    private String carryingMaterial; // 承载物料
    private String assignedTask;     // 当前任务
    private Double speed;            // 当前速度

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date lastUpdateTime;     // 最后更新时间

    private String ipAddress;        // IP 地址
    private Integer port;            // 端口号
    private String macAddress;       // MAC 地址
    private Boolean isOnline;        // 是否在线
        
    // AGV 实时位置信息（从 AGV API 获取）
    private Double currentX;         // 当前 X 坐标（单位：m）
    private Double currentY;         // 当前 Y 坐标（单位：m）
    private Double currentTheta;     // 当前角度（单位：rad）


    // getter and setter methods
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getAgvId() { return agvId; }
    public void setAgvId(String agvId) { this.agvId = agvId; }

    public String getCurrentLocation() { return currentLocation; }
    public void setCurrentLocation(String currentLocation) { this.currentLocation = currentLocation; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getBatteryLevel() { return batteryLevel; }
    public void setBatteryLevel(Integer batteryLevel) { this.batteryLevel = batteryLevel; }

    public String getCarryingMaterial() { return carryingMaterial; }
    public void setCarryingMaterial(String carryingMaterial) { this.carryingMaterial = carryingMaterial; }

    public String getAssignedTask() { return assignedTask; }
    public void setAssignedTask(String assignedTask) { this.assignedTask = assignedTask; }

    public Double getSpeed() { return speed; }
    public void setSpeed(Double speed) { this.speed = speed; }

    public Date getLastUpdateTime() { return lastUpdateTime; }
    public void setLastUpdateTime(Date lastUpdateTime) { this.lastUpdateTime = lastUpdateTime; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getMacAddress() { return macAddress; }
    public void setMacAddress(String macAddress) { this.macAddress = macAddress; }

    public Boolean getIsOnline() { return isOnline; }
    public void setIsOnline(Boolean isOnline) { this.isOnline = isOnline; }

    public Integer getPort() { return port; }
    public void setPort(Integer port) { this.port = port; }
    
    // AGV 实时位置信息的 getter/setter
    public Double getCurrentX() { return currentX; }
    public void setCurrentX(Double currentX) { this.currentX = currentX; }
    
    public Double getCurrentY() { return currentY; }
    public void setCurrentY(Double currentY) { this.currentY = currentY; }
    
    public Double getCurrentTheta() { return currentTheta; }
    public void setCurrentTheta(Double currentTheta) { this.currentTheta = currentTheta; }
    
    @Override
    public String toString() {
        return "Agv{" +
                "id=" + id +
                ", agvId='" + agvId + '\'' +
                ", status='" + status + '\'' +
                ", ipAddress='" + ipAddress + '\'' +
                ", port=" + port +
                '}';
    }
}
