package com.example.demo.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.demo.commom.Result;
import com.example.demo.entity.Book;
import com.example.demo.entity.BookWithUser;
import com.example.demo.entity.LendRecord;
import com.example.demo.entity.User;
import com.example.demo.mapper.BookMapper;
import com.example.demo.mapper.BookWithUserMapper;
import com.example.demo.mapper.LendRecordMapper;
import com.example.demo.mapper.UserMapper;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.*;

@RestController
@RequestMapping("/borrow")
@CrossOrigin
public class BorrowController {

    @Resource private BookMapper bookMapper;
    @Resource private BookWithUserMapper bookWithUserMapper;
    @Resource private LendRecordMapper lendRecordMapper;
    @Resource private UserMapper userMapper;

    // 扫码借阅
    // 入参：{ userId, code }
    @PostMapping("/scan")
    @Transactional(rollbackFor = Exception.class)
    public Result<?> scanBorrow(@RequestBody Map<String, Object> req) {
        Integer userId = parseInt(req.get("userId"));
        String code = req.get("code") == null ? null : String.valueOf(req.get("code")).trim();

        if (userId == null) return Result.error("400", "缺少 userId");
        if (code == null || code.isEmpty()) return Result.error("400", "缺少 code");

        User user = userMapper.selectById(userId);
        if (user == null) return Result.error("404", "用户不存在");

        // 1) 查书：优先 barcode，兜底 isbn
        LambdaQueryWrapper<Book> bq = Wrappers.<Book>lambdaQuery()
                .eq(Book::getBarcode, code)
                .or()
                .eq(Book::getIsbn, code);
        Book book = bookMapper.selectOne(bq);
        if (book == null) return Result.error("404", "未找到该条码对应图书");

        // 2) 库存校验
        int stock = book.getStock() == null ? 0 : book.getStock();
        if (stock <= 0) return Result.error("400", "库存不足，无法借阅");

        // 3) 不允许重复借同一本（避免 bookwithuser 主键冲突）
        Long already = bookWithUserMapper.selectCount(
                Wrappers.<BookWithUser>lambdaQuery()
                        .eq(BookWithUser::getId, userId)
                        .eq(BookWithUser::getIsbn, book.getIsbn())
        );
        if (already != null && already > 0) {
            return Result.error("400", "你已借阅过该书，请先归还后再借");
        }

        // 4) 限制最多借 5 本（前端也做了，后端兜底）
        Long cnt = bookWithUserMapper.selectCount(
                Wrappers.<BookWithUser>lambdaQuery().eq(BookWithUser::getId, userId)
        );
        if (cnt != null && cnt >= 5) {
            return Result.error("400", "最多只能借 5 本书");
        }

        // 5) 有逾期不允许借（后端兜底）
        Date now = new Date();
        List<BookWithUser> borrowed = bookWithUserMapper.selectList(
                Wrappers.<BookWithUser>lambdaQuery().eq(BookWithUser::getId, userId)
        );
        for (BookWithUser it : borrowed) {
            if (it.getDeadtime() != null && it.getDeadtime().before(now)) {
                return Result.error("400", "存在逾期书籍，请先归还");
            }
        }

        // 6) 扣库存、借阅次数+1、更新状态
        int borrowNum = book.getBorrownum() == null ? 0 : book.getBorrownum();
        borrowNum++;
        int newStock = stock - 1;

        UpdateWrapper<Book> bw = new UpdateWrapper<>();
        bw.eq("id", book.getId())
                .set("stock", newStock)
                .set("borrownum", borrowNum)
                .set("status", newStock > 0 ? "1" : "0");
        bookMapper.update(null, bw);

        // 7) 写 lend_record（历史记录）
        LendRecord record = new LendRecord();
        record.setReaderId(userId);
        record.setIsbn(book.getIsbn());
        record.setBookname(book.getName());
        record.setLendTime(now);
        record.setReturnTime(null);
        record.setStatus("0");
        record.setBorrownum(borrowNum);
        lendRecordMapper.insert(record);

        // 8) 写 bookwithuser（当前借阅中）
        BookWithUser bwu = new BookWithUser();
        bwu.setId(userId);
        bwu.setIsbn(book.getIsbn());
        bwu.setBookName(book.getName());
        bwu.setNickName(user.getNickName() != null && !user.getNickName().isEmpty()
                ? user.getNickName()
                : user.getUsername());
        bwu.setLendtime(now);

        Calendar cal = Calendar.getInstance();
        cal.setTime(now);
        cal.add(Calendar.DATE, 30); // 30天到期
        bwu.setDeadtime(cal.getTime());
        bwu.setProlong(1);
        bookWithUserMapper.insert(bwu);

        Map<String, Object> data = new HashMap<>();
        data.put("isbn", book.getIsbn());
        data.put("name", book.getName());
        data.put("barcode", book.getBarcode());
        data.put("stock", newStock);
        data.put("borrownum", borrowNum);
        data.put("deadtime", bwu.getDeadtime());

        return Result.success(data);
    }

    // 还书
    // 入参：{ userId, isbn }
    // ✅ 注意：这里的 isbn 可能实际传的是 barcode / 二维码内容(URL)
    @PostMapping("/return")
    @Transactional(rollbackFor = Exception.class)
    public Result<?> returnBook(@RequestBody Map<String, Object> req) {
        Integer userId = parseInt(req.get("userId"));
        String input = req.get("isbn") == null ? null : String.valueOf(req.get("isbn")).trim();

        if (userId == null) return Result.error("400", "缺少 userId");
        if (input == null || input.isEmpty()) return Result.error("400", "缺少 isbn");

        Book book = bookMapper.selectOne(Wrappers.<Book>lambdaQuery().eq(Book::getBarcode, input));
        if (book == null) {
            book = bookMapper.selectOne(Wrappers.<Book>lambdaQuery().eq(Book::getIsbn, input));
        }
        if (book == null) return Result.error("404", "未找到该码对应图书");

        String realIsbn = book.getIsbn();
        Date now = new Date();
        Long borrowedCnt = bookWithUserMapper.selectCount(
                Wrappers.<BookWithUser>lambdaQuery()
                        .eq(BookWithUser::getId, userId)
                        .eq(BookWithUser::getIsbn, realIsbn)
        );
        if (borrowedCnt == null || borrowedCnt <= 0) {
            return Result.error("400", "你未借阅该书，无法归还");
        }

        // 4) 库存 +1，状态=可借
        int stock = book.getStock() == null ? 0 : book.getStock();
        int newStock = stock + 1;

        bookMapper.update(null,
                new UpdateWrapper<Book>()
                        .eq("id", book.getId())
                        .set("stock", newStock)
                        .set("status", "1")
        );

        // 5) lend_record：只更新“最近一条未归还记录”（避免多条一起改）
        LendRecord lastUnreturned = lendRecordMapper.selectOne(
                Wrappers.<LendRecord>lambdaQuery()
                        .eq(LendRecord::getReaderId, userId)
                        .eq(LendRecord::getIsbn, realIsbn)
                        .eq(LendRecord::getStatus, "0")
                        .orderByDesc(LendRecord::getLendTime)
                        .last("limit 1")
        );
        if (lastUnreturned != null) {
            lendRecordMapper.update(null,
                    new UpdateWrapper<LendRecord>()
                            .eq("reader_id", userId)
                            .eq("isbn", realIsbn)
                            .eq("lend_time", lastUnreturned.getLendTime())
                            .eq("status", "0")
                            .set("status", "1")
                            .set("return_time", now)
            );
        }

        // 6) bookwithuser：删除当前借阅中记录
        Map<String, Object> map = new HashMap<>();
        map.put("id", userId);
        map.put("isbn", realIsbn);
        bookWithUserMapper.deleteByMap(map);

        // ✅ 可选：返回一点数据给前端提示
        Map<String, Object> data = new HashMap<>();
        data.put("isbn", realIsbn);
        data.put("barcode", book.getBarcode());
        data.put("name", book.getName());
        data.put("stock", newStock);

        return Result.success(data);
    }

    private Integer parseInt(Object o) {
        if (o == null) return null;
        if (o instanceof Number) return ((Number) o).intValue();
        try {
            return Integer.parseInt(String.valueOf(o));
        } catch (Exception e) {
            return null;
        }
    }
}
