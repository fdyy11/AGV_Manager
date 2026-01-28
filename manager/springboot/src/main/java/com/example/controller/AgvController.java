package com.example.controller;

import com.example.common.Result;
import com.example.entity.Agv;
import com.example.service.AgvService;
import com.github.pagehelper.PageInfo;
import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;
import java.util.List;

/**
 * AGV管理前端操作接口
 **/
@RestController
@RequestMapping("/agv")
public class AgvController {

    @Resource
    private AgvService agvService;

    /**
     * 新增AGV
     */
    @PostMapping("/add")
    public Result add(@RequestBody Agv agv) {
        agvService.add(agv);
        return Result.success();
    }

    /**
     * 删除AGV
     */
    @DeleteMapping("/delete/{id}")
    public Result deleteById(@PathVariable Integer id) {
        agvService.deleteById(id);
        return Result.success();
    }

    /**
     * 更新AGV状态
     */
    @PutMapping("/updateStatus")
    public Result updateStatus(@RequestBody Agv agv) {
        agvService.updateStatus(agv);
        return Result.success();
    }

    /**
     * 分页查询AGV
     */
    @GetMapping("/selectPage")
    public Result selectPage(Agv agv,
                             @RequestParam(defaultValue = "1") Integer pageNum,
                             @RequestParam(defaultValue = "10") Integer pageSize) {
        PageInfo<Agv> page = agvService.selectPage(agv, pageNum, pageSize);
        return Result.success(page);
    }

    /**
     * 获取所有在线AGV
     */
    @GetMapping("/online")
    public Result getOnlineAgvs() {
        List<Agv> agvs = agvService.getOnlineAgvs();
        return Result.success(agvs);
    }
}
