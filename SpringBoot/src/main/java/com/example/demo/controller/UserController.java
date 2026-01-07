package com.example.demo.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.LoginUser;
import com.example.demo.commom.Result;
import com.example.demo.entity.User;
import com.example.demo.mapper.UserMapper;
import com.example.demo.utils.FaceMatchUtils;
import com.example.demo.utils.TokenUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/user")
@CrossOrigin
public class UserController {

    @Resource
    UserMapper userMapper;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // ===================== 账号注册/登录（原有） =====================

    @PostMapping("/register")
    public Result<?> register(@RequestBody User user) {
        User res = userMapper.selectOne(Wrappers.<User>lambdaQuery().eq(User::getUsername, user.getUsername()));
        if (res != null) {
            return Result.error("-1", "用户名已重复");
        }
        userMapper.insert(user);
        return Result.success();
    }

    @PostMapping("/login")
    public Result<?> login(@RequestBody User user){
        User res = userMapper.selectOne(Wrappers.<User>lambdaQuery().eq(User::getUsername,user.getUsername()).eq(User::getPassword,user.getPassword()));
        if(res == null)
        {
            return Result.error("-1","用户名或密码错误");
        }
        String token = TokenUtils.genToken(res);
        res.setToken(token);
        LoginUser loginuser = new LoginUser();
        loginuser.addVisitCount();
        return Result.success(res);
    }

    // ===================== 人脸注册/人脸登录（新增） =====================

    /**
     * 绑定人脸（已注册用户绑定 descriptor）
     * 入参：{ "userId": 1, "descriptor": [ ...128个数字... ] }
     */
    @PostMapping("/face/register")
    public Result<?> faceRegister(@RequestBody FaceRegisterRequest req) {
        if (req == null || req.userId == null || req.descriptor == null || req.descriptor.isEmpty()) {
            return Result.error("-1", "参数缺失：userId/descriptor");
        }

        double[] input = FaceMatchUtils.toDoubleArray(req.descriptor);
        if (!FaceMatchUtils.isValidDescriptor(input)) {
            return Result.error("-1", "descriptor 维度不合法，应为 128 维");
        }

        // user 是否存在
        User user = userMapper.selectById(req.userId);
        if (user == null) {
            return Result.error("-1", "用户不存在");
        }

        // descriptor 存库为 JSON 字符串
        String json;
        try {
            json = MAPPER.writeValueAsString(req.descriptor);
        } catch (Exception e) {
            return Result.error("-1", "descriptor 序列化失败");
        }

        // 用 UpdateWrapper 直接更新列名，避免因为实体字段未同步导致更新失败
        UpdateWrapper<User> uw = new UpdateWrapper<>();
        uw.eq("id", req.userId)
                .set("face_descriptor", json);
        userMapper.update(null, uw);

        return Result.success("人脸绑定成功");
    }

    /**
     * 刷脸登录（免借书证）
     * 入参：{ "descriptor": [ ...128个数字... ] }
     * 行为：遍历 face_registered=1 的用户，找最小欧氏距离 < 阈值 即视为同一人
     */
    @PostMapping("/face/login")
    public Result<?> faceLogin(@RequestBody FaceLoginRequest req) {
        if (req == null || req.descriptor == null || req.descriptor.isEmpty()) {
            return Result.error("-1", "参数缺失：descriptor");
        }

        double[] input = FaceMatchUtils.toDoubleArray(req.descriptor);
        if (!FaceMatchUtils.isValidDescriptor(input)) {
            return Result.error("-1", "descriptor 维度不合法，应为 128 维");
        }

        // 取出所有已绑定人脸的用户
        List<User> candidates = userMapper.selectList(
                Wrappers.<User>query()
                        .isNotNull("face_descriptor")
                        .ne("face_descriptor", "")
        );
        // 也可以改成更精确：只查 face_registered=1（如果你 User 实体已加 faceRegistered 字段）
        // List<User> candidates = userMapper.selectList(
        //        Wrappers.<User>query().eq("face_registered", 1).isNotNull("face_descriptor")
        // );

        User bestUser = null;
        double bestDist = Double.POSITIVE_INFINITY;

        for (User u : candidates) {
            // 这里读取数据库字段 face_descriptor：
            // 1) 如果你 User 实体已经加了 faceDescriptor 字段：直接 u.getFaceDescriptor()
            // 2) 若暂时没加字段，则建议尽快加；否则这里拿不到 descriptor
            String storedJson;
            try {
                // 如果你的 User 已增加 faceDescriptor 字段，就用这一行：
                storedJson = (String) User.class.getMethod("getFaceDescriptor").invoke(u);
            } catch (Exception reflectionFail) {
                // 反射失败说明你还没加字段（或名字不同）
                // 你可以把字段名改对，或者直接在 User 中新增 getFaceDescriptor()
                continue;
            }

            if (storedJson == null || storedJson.trim().isEmpty()) {
                continue;
            }

            double dist = FaceMatchUtils.distanceToStoredJson(input, storedJson);
            if (dist < bestDist) {
                bestDist = dist;
                bestUser = u;
            }
        }

        if (bestUser == null || bestDist >= FaceMatchUtils.DEFAULT_THRESHOLD) {
            return Result.error("-1", "未识别到已注册用户（匹配失败）");
        }

        // 生成 token（复用原逻辑）
        String token = TokenUtils.genToken(bestUser);
        bestUser.setToken(token);

        LoginUser loginuser = new LoginUser();
        loginuser.addVisitCount();

        return Result.success(bestUser);
    }

    // ===================== 用户管理（原有） =====================

    @PostMapping
    public Result<?> save(@RequestBody User user) {
        if (user.getPassword() == null) {
            user.setPassword("abc123456");
        }
        userMapper.insert(user);
        return Result.success();
    }

    @PutMapping("/password")
    public Result<?> update(@RequestParam Integer id,
                            @RequestParam String password2) {
        UpdateWrapper<User> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", id);
        User user = new User();
        user.setPassword(password2);
        userMapper.update(user, updateWrapper);
        return Result.success();
    }

    @PutMapping
    public Result<?> password(@RequestBody User user) {
        userMapper.updateById(user);
        return Result.success();
    }

    @PostMapping("/deleteBatch")
    public Result<?> deleteBatch(@RequestBody List<Integer> ids) {
        userMapper.deleteBatchIds(ids);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        userMapper.deleteById(id);
        return Result.success();
    }
    @GetMapping("/{id}")
    public Result<?> getById(@PathVariable Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            return Result.error("-1", "用户不存在");
        }
        return Result.success(user);
    }
    @GetMapping
    public Result<?> findPage(@RequestParam(defaultValue = "1") Integer pageNum,
                              @RequestParam(defaultValue = "10") Integer pageSize,
                              @RequestParam(defaultValue = "") String search) {
        LambdaQueryWrapper<User> wrappers = Wrappers.<User>lambdaQuery();
        if (StringUtils.isNotBlank(search)) {
            wrappers.like(User::getNickName, search);
        }
        wrappers.like(User::getRole, 2);
        Page<User> userPage = userMapper.selectPage(new Page<>(pageNum, pageSize), wrappers);
        return Result.success(userPage);
    }

    @GetMapping("/usersearch")
    public Result<?> findPage2(@RequestParam(defaultValue = "1") Integer pageNum,
                               @RequestParam(defaultValue = "10") Integer pageSize,
                               @RequestParam(defaultValue = "") String search1,
                               @RequestParam(defaultValue = "") String search2,
                               @RequestParam(defaultValue = "") String search3,
                               @RequestParam(defaultValue = "") String search4) {
        LambdaQueryWrapper<User> wrappers = Wrappers.<User>lambdaQuery();
        if (StringUtils.isNotBlank(search1)) {
            wrappers.like(User::getId, search1);
        }
        if (StringUtils.isNotBlank(search2)) {
            wrappers.like(User::getNickName, search2);
        }
        if (StringUtils.isNotBlank(search3)) {
            wrappers.like(User::getPhone, search3);
        }
        if (StringUtils.isNotBlank(search4)) {
            wrappers.like(User::getAddress, search4);
        }
        wrappers.like(User::getRole, 2);
        Page<User> userPage = userMapper.selectPage(new Page<>(pageNum, pageSize), wrappers);
        return Result.success(userPage);
    }

    // ===================== DTO（放在 Controller 内最省事） =====================

    public static class FaceRegisterRequest {
        public Integer userId;
        public List<Double> descriptor;
    }

    public static class FaceLoginRequest {
        public List<Double> descriptor;
    }
}
