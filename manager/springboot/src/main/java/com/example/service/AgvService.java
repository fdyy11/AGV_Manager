package com.example.service;

import cn.hutool.core.date.DateUtil;
import com.example.entity.Agv;
import com.example.mapper.AgvMapper;
import com.example.service.TcpConnectionService;
import com.example.utils.TokenUtils;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import javax.annotation.Resource;
import org.springframework.stereotype.Service;
import java.util.List;

/**
 * AGV业务处理
 **/
@Service
public class AgvService {

    @Resource
    private AgvMapper agvMapper;

    @Resource
    private TcpConnectionService tcpConnectionService;

    /**
     * 新增AGV
     */
    public void add(Agv agv) {
        agv.setIsOnline(false);
        agv.setLastUpdateTime(DateUtil.date());
        agvMapper.insert(agv);
    }

    /**
     * 删除AGV
     */
    public void deleteById(Integer id) {
        agvMapper.deleteById(id);
    }

    /**
     * 更新AGV状态
     */
    public void updateStatus(Agv agv) {
        agv.setLastUpdateTime(DateUtil.date());
        agvMapper.updateById(agv);
    }

    /**
     * 分页查询AGV
     */
    public PageInfo<Agv> selectPage(Agv agv, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<Agv> list = agvMapper.selectAll(agv);
        return PageInfo.of(list);
    }

    /**
     * 获取在线AGV
     */
    public List<Agv> getOnlineAgvs() {
        Agv condition = new Agv();
        condition.setIsOnline(true);
        return agvMapper.selectAll(condition);
    }

    /**
     * 为AGV分配任务
     */
    /**
     * 为AGV分配任务
     */
    public void assignTaskToAgv(String agvId, String taskId) {
        // 更新AGV的当前任务
        Agv agv = agvMapper.selectByAgvId(agvId);
        if (agv != null) {
            agv.setAssignedTask(taskId);
            agvMapper.updateByAgvId(agv);
        }
    }


    /**
     * 更新AGV位置
     */
    public void updateAgvLocation(String agvId, String location) {
        Agv agv = new Agv();
        agv.setAgvId(agvId);
        agv.setCurrentLocation(location);
        agv.setLastUpdateTime(DateUtil.date());
        agvMapper.updateByAgvId(agv);
    }
}
