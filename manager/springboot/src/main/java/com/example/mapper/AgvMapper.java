package com.example.mapper;

import com.example.entity.Agv;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * AGV数据访问层
 */
public interface AgvMapper {

    /**
     * 插入AGV
     */
    void insert(Agv agv);

    /**
     * 根据ID删除AGV
     */
    void deleteById(Integer id);

    /**
     * 根据AGV ID更新状态
     */
    void updateByAgvId(Agv agv);

    /**
     * 根据ID更新AGV
     */
    void updateById(Agv agv);

    /**
     * 根据条件查询所有AGV
     */
    List<Agv> selectAll(Agv agv);

    /**
     * 根据ID查询AGV
     */
    Agv selectById(Integer id);

    /**
     * 根据AGV ID查询
     */
// 在AgvMapper接口中添加
    Agv selectByAgvId(@Param("agvId") String agvId);

}
