package com.example.demo.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.commom.Result;
import com.example.demo.entity.LendRecord;
import com.example.demo.mapper.LendRecordMapper;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/LendRecord")
public class LendRecordController {

    @Resource
    private LendRecordMapper lendRecordMapper;

    @DeleteMapping("/{isbn}")
    public Result<?> delete(@PathVariable String isbn) {
        Map<String, Object> map = new HashMap<>();
        map.put("isbn", isbn);
        lendRecordMapper.deleteByMap(map);
        return Result.success();
    }

    // 删除一条记录（用 isbn + borrownum 定位）
    @PostMapping("/deleteRecord")
    public Result<?> deleteRecord(@RequestBody LendRecord lendRecord) {
        Map<String, Object> map = new HashMap<>();
        map.put("isbn", lendRecord.getIsbn());
        map.put("borrownum", lendRecord.getBorrownum());
        lendRecordMapper.deleteByMap(map);
        return Result.success();
    }

    @PostMapping("/deleteRecords")
    public Result<?> deleteRecords(@RequestBody List<LendRecord> lendRecords) {
        for (LendRecord curRecord : lendRecords) {
            Map<String, Object> map = new HashMap<>();
            map.put("isbn", curRecord.getIsbn());
            map.put("borrownum", curRecord.getBorrownum());
            lendRecordMapper.deleteByMap(map);
        }
        return Result.success();
    }

    @PostMapping
    public Result<?> save(@RequestBody LendRecord lendRecord) {
        lendRecordMapper.insert(lendRecord);
        return Result.success();
    }

    @GetMapping
    public Result<?> findPage(@RequestParam(defaultValue = "1") Integer pageNum,
                              @RequestParam(defaultValue = "10") Integer pageSize,
                              @RequestParam(defaultValue = "") String search1,
                              @RequestParam(defaultValue = "") String search2,
                              @RequestParam(defaultValue = "") String search3) {

        LambdaQueryWrapper<LendRecord> wrappers = Wrappers.<LendRecord>lambdaQuery();

        if (StringUtils.isNotBlank(search1)) {
            wrappers.like(LendRecord::getIsbn, search1);
        }
        if (StringUtils.isNotBlank(search2)) {
            wrappers.like(LendRecord::getBookname, search2);
        }
        if (StringUtils.isNotBlank(search3)) {
            wrappers.like(LendRecord::getReaderId, search3);
        }

        Page<LendRecord> page = lendRecordMapper.selectPage(new Page<>(pageNum, pageSize), wrappers);
        return Result.success(page);
    }

    /**
     * ✅ 原接口保留：按 isbn 更新
     * 注意：isbn 在 lend_record 里通常不是唯一键（同一本书多次借会多条记录），
     * 若你希望更准确，建议改为 isbn + borrownum 或者 id 来定位。
     */
    @PutMapping("/{isbn}")
    public Result<?> update(@PathVariable String isbn, @RequestBody LendRecord lendRecord) {
        UpdateWrapper<LendRecord> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("isbn", isbn);

        LendRecord toUpd = new LendRecord();
        toUpd.setLendTime(lendRecord.getLendTime());
        toUpd.setReturnTime(lendRecord.getReturnTime());
        toUpd.setStatus(lendRecord.getStatus());

        lendRecordMapper.update(toUpd, updateWrapper);
        return Result.success();
    }

    /**
     * ✅ 修复冲突：原来的 @PutMapping("/{lendTime}") 与 @PutMapping("/{isbn}") 冲突
     * 改为明确路径：/byLendTime
     *
     * 调用方式示例：
     * PUT /LendRecord/byLendTime?lendTime=2026-01-07 10:00:00
     * body: { "status": "1", "returnTime": "2026-01-07 10:10:00" }
     *
     * 注意：这里为了最稳，不强行用 Date 做 PathVariable 解析，避免格式/时区问题。
     */
    @PutMapping("/byLendTime")
    public Result<?> updateByLendTime(@RequestParam("lendTime") String lendTime,
                                      @RequestBody LendRecord lendRecord) {

        UpdateWrapper<LendRecord> updateWrapper = new UpdateWrapper<>();
        // ✅ 建议用数据库列名 lend_time（比 lendTime 更稳）
        updateWrapper.eq("lend_time", lendTime);

        LendRecord toUpd = new LendRecord();
        toUpd.setReturnTime(lendRecord.getReturnTime());
        toUpd.setStatus(lendRecord.getStatus());

        lendRecordMapper.update(toUpd, updateWrapper);
        return Result.success();
    }
}
