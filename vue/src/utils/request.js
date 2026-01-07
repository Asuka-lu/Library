import axios from "axios";
import router from "../router";

const request = axios.create({
    baseURL: "/api", // ① 所有请求前面统一加 /api
    timeout: 5000,
});

// ====================== request 拦截器 ======================
// 在请求发送前：
// 1) 统一 Content-Type
// 2) 自动携带 token
// 3) 未登录时，只放行登录/注册/刷脸登录接口，其余跳转 /login
request.interceptors.request.use(
    (config) => {
        config.headers["Content-Type"] = "application/json;charset=utf-8";

        const url = config.url || "";

        // ✅ 白名单：未登录也允许调用的接口（非常关键：刷脸登录要放行）
        const whiteList = ["/user/login", "/user/register", "/user/face/login"];
        const isWhite = whiteList.some((p) => url.includes(p));

        // 取出 sessionStorage 里面缓存的用户信息
        const userStr = sessionStorage.getItem("user");

        if (userStr) {
            // 已登录：带 token
            try {
                const user = JSON.parse(userStr);
                if (user && user.token) {
                    config.headers["token"] = user.token;
                }
            } catch (e) {
                // userStr 被写坏了就清理掉，避免一直报错
                sessionStorage.removeItem("user");
            }
        } else {
            // 未登录：非白名单接口 -> 跳转到 /login
            if (!isWhite) {
                // 避免重复 push 报错
                const curPath = router?.currentRoute?.value?.path;
                if (curPath !== "/login") {
                    router.push("/login");
                }
            }
        }

        return config;
    },
    (error) => Promise.reject(error)
);

// ====================== response 拦截器 ======================
// 统一处理返回值：直接把后端的 res.data 取出来
request.interceptors.response.use(
    (response) => {
        let res = response.data;

        // 如果是返回文件
        if (response.config.responseType === "blob") {
            return res;
        }

        // 兼容后端可能返回 string 的情况
        if (typeof res === "string") {
            res = res ? JSON.parse(res) : res;
        }

        return res;
    },
    (error) => {
        console.log("err" + error);
        return Promise.reject(error);
    }
);

export default request;
