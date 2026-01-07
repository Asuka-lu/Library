<template>
  <div class="login-container">
    <el-form ref="form" :model="form" :rules="rules" class="login-card">
      <div class="header">
        <h2 class="title">系统登录</h2>
        <div class="subtitle">欢迎使用自助式图书管理系统</div>
      </div>

      <el-form-item prop="username">
        <el-input v-model="form.username" clearable placeholder="请输入用户名">
          <template #prefix>
            <el-icon class="el-input__icon"><User /></el-icon>
          </template>
        </el-input>
      </el-form-item>

      <el-form-item prop="password">
        <el-input v-model="form.password" clearable show-password placeholder="请输入密码">
          <template #prefix>
            <el-icon class="el-input__icon"><Lock /></el-icon>
          </template>
        </el-input>
      </el-form-item>

      <!-- 验证码 -->
      <el-form-item>
        <div class="valid-row">
          <el-input
              v-model="form.validCode"
              placeholder="验证码"
              clearable
              class="valid-input"
          />
          <div class="valid-img">
            <ValidCode @input="createValidCode" />
          </div>
        </div>
      </el-form-item>

      <el-form-item>
        <el-button type="primary" class="btn" @click="login">
          登 录
        </el-button>
      </el-form-item>

      <el-form-item>
        <el-button class="btn" @click="$router.push('/face-login')">
          人脸登录
        </el-button>
      </el-form-item>

      <el-form-item>
        <el-button type="success" plain class="btn" @click="$router.push('/face-return')">
          扫脸还书
        </el-button>
      </el-form-item>

      <div class="footer">
        <el-button type="text" @click="$router.push('/register')">
          前往注册 >>
        </el-button>
      </div>
    </el-form>
  </div>
</template>

<script>
import request from "../utils/request";
import { ElMessage } from "element-plus";
import ValidCode from "../components/Validate";

export default {
  name: "Login",
  components: { ValidCode },
  data() {
    return {
      validCode: "",
      form: {},
      rules: {
        username: [{ required: true, message: "请输入用户名", trigger: "blur" }],
        password: [{ required: true, message: "请输入密码", trigger: "blur" }],
      },
    };
  },
  methods: {
    createValidCode(data) {
      this.validCode = data;
    },
    login() {
      this.$refs["form"].validate((valid) => {
        if (valid) {
          if (!this.form.validCode) {
            ElMessage.error("请填写验证码");
            return;
          }
          if (this.form.validCode.toLowerCase() !== this.validCode.toLowerCase()) {
            ElMessage.error("验证码错误");
            return;
          }
          const loginData = {
            username: this.form.username,
            password: this.form.password,
          };
          request.post("user/login", loginData).then((res) => {
            if (res.code == 0) {
              ElMessage.success("登录成功");
              sessionStorage.setItem("user", JSON.stringify(res.data));
              this.$router.push("/dashboard");
            } else {
              ElMessage.error(res.msg);
            }
          });
        }
      });
    },
  },
};
</script>

<style scoped>
/* 背景 */
.login-container {
  position: fixed;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: url("../img/bg2.svg");
  background-size: cover;
  background-position: center;
  overflow: hidden;
}
.login-card {
  width: 380px;
  padding: 28px 28px 18px;
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(234, 234, 234, 0.9);
  border-radius: 12px;
  box-shadow: 0 12px 34px rgba(0, 0, 0, 0.18);
  backdrop-filter: blur(6px);
}
.header {
  margin-bottom: 18px;
  text-align: center;
}
.title {
  margin: 0;
  font-size: 22px;
  font-weight: 800;
  color: #222;
  letter-spacing: 0.5px;
}
.subtitle {
  margin-top: 6px;
  font-size: 12px;
  color: #777;
}
.valid-row {
  display: flex;
  gap: 10px;
  align-items: center;
}
.valid-input {
  flex: 1;
}
.valid-img {
  width: 130px;
  height: 34px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid #eee;
  background: #fff;
}
.btn {
  width: 100%;
  height: 40px;
  border-radius: 10px;
  font-weight: 600;
}
.footer {
  margin-top: 4px;
  display: flex;
  justify-content: flex-end;
}
</style>
