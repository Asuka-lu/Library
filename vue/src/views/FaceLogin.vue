<template>
  <div class="login-container">
    <div class="login-page">
      <h2 class="title" style="margin-bottom: 12px">人脸登录</h2>
      <div style="margin-bottom: 10px; font-size: 13px; color: #666;">
        请正对摄像头，保持光线充足，点击“开始识别”。
      </div>
      <div class="video-box">
        <video ref="videoEl" autoplay muted playsinline class="video"></video>
      </div>
      <div style="margin-top: 12px; display: flex; gap: 10px;">
        <el-button type="primary" style="flex: 1" :loading="loading" @click="doFaceLogin">
          开始识别
        </el-button>
        <el-button style="flex: 1" @click="toggleCamera">
          {{ cameraOn ? "关闭摄像头" : "打开摄像头" }}
        </el-button>
      </div>
      <div style="margin-top: 10px; display:flex; justify-content: space-between;">
        <el-button type="text" @click="$router.push('/login')">返回账号登录</el-button>
        <el-button type="text" @click="$router.push('/register')">前往注册 >></el-button>
      </div>
      <div v-if="status" style="margin-top: 6px; font-size: 12px; color: #999;">
        {{ status }}
      </div>
    </div>
  </div>
</template>

<script>
import request from "../utils/request";
import { ElMessage } from "element-plus";
import * as faceapi from "face-api.js";

export default {
  name: "FaceLogin",
  data() {
    return {
      modelsLoaded: false,
      cameraOn: false,
      stream: null,
      loading: false,
      status: "",
    };
  },
  async mounted() {
    await this.ensureModelsLoaded();
  },
  beforeUnmount() {
    this.stopCamera();
  },
  methods: {
    // 等待 video 真正有画面
    waitVideoReady(video, timeoutMs = 3000) {
      return new Promise((resolve, reject) => {
        const start = Date.now();
        const timer = setInterval(() => {
          if (video && video.readyState >= 2 && video.videoWidth > 0 && video.videoHeight > 0) {
            clearInterval(timer);
            resolve();
          } else if (Date.now() - start > timeoutMs) {
            clearInterval(timer);
            reject(new Error("视频未就绪（可能未开始播放或 videoWidth=0）"));
          }
        }, 100);
      });
    },

    async ensureModelsLoaded() {
      if (this.modelsLoaded) return;
      try {
        this.status = "正在加载人脸模型...";
        await faceapi.nets.tinyFaceDetector.loadFromUri("/models");
        await faceapi.nets.faceLandmark68Net.loadFromUri("/models");
        await faceapi.nets.faceRecognitionNet.loadFromUri("/models");
        this.modelsLoaded = true;
        this.status = "模型加载完成";
      } catch (e) {
        console.error(e);
        this.status = "";
        ElMessage.error("人脸模型加载失败：请检查 public/models 是否放置正确");
      }
    },

    async startCamera() {
      if (this.cameraOn) return;
      try {
        const stream = await navigator.mediaDevices.getUserMedia({
          video: { facingMode: "user" },
          audio: false,
        });

        this.stream = stream;
        this.cameraOn = true;

        const video = this.$refs.videoEl;
        video.srcObject = stream;

        // 等待 metadata 并强制播放
        await new Promise((resolve) => {
          video.onloadedmetadata = () => resolve();
        });
        await video.play();

        this.status = "摄像头已开启";
      } catch (e) {
        console.error(e);
        this.status = "";
        this.cameraOn = false;
        ElMessage.error("无法打开摄像头：请检查浏览器权限（建议 localhost 或 https）");
      }
    },

    stopCamera() {
      if (this.stream) {
        this.stream.getTracks().forEach((t) => t.stop());
      }
      this.stream = null;
      this.cameraOn = false;
      const video = this.$refs.videoEl;
      if (video) video.srcObject = null;
    },

    async toggleCamera() {
      if (this.cameraOn) {
        this.stopCamera();
      } else {
        await this.ensureModelsLoaded();
        await this.startCamera();
      }
    },

    async doFaceLogin() {
      await this.ensureModelsLoaded();
      if (!this.cameraOn) await this.startCamera();

      const video = this.$refs.videoEl;
      if (!video) {
        ElMessage.error("视频组件未初始化");
        return;
      }

      try {
        this.loading = true;
        this.status = "正在识别人脸...";

        // 等 video 真正有帧数据
        await this.waitVideoReady(video, 3000);

        const options = new faceapi.TinyFaceDetectorOptions({
          inputSize: 160,
          scoreThreshold: 0.5,
        });

        const detection = await faceapi
            .detectSingleFace(video, options)
            .withFaceLandmarks()
            .withFaceDescriptor();

        if (!detection || !detection.descriptor) {
          this.status = "";
          ElMessage.error("未检测到人脸：请正对摄像头并保持光线充足");
          return;
        }

        const descriptor = Array.from(detection.descriptor);

        // 调后端刷脸登录
        const res = await request.post("/user/face/login", { descriptor });

        // 兼容 code 为 0 或 "0"
        const ok = res && (res.code === 0 || res.code === "0");
        if (ok && res.data) {
          ElMessage.success("刷脸登录成功");
          sessionStorage.setItem("user", JSON.stringify(res.data));
          this.stopCamera();

          // 跳转首页（和你现有登录一致）
          this.$router.push("/dashboard");
        } else {
          ElMessage.error((res && res.msg) ? res.msg : "刷脸登录失败");
        }
      } catch (e) {
        console.error(e);
        ElMessage.error(e?.message || "刷脸登录异常，请稍后重试");
      } finally {
        this.loading = false;
        this.status = "";
      }
    },
  },
};
</script>

<style scoped>
.login-container {
  position: fixed;
  width: 100%;
  height: 100vh;
  background: url("../img/bg2.svg");
  background-size: contain;
  overflow: hidden;
}

.login-page {
  border-radius: 5px;
  margin: 180px auto;
  width: 380px;
  padding: 28px 30px 18px;
  background: #fff;
  border: 1px solid #eaeaea;
  box-shadow: 0 0 25px #cac6c6;
}

.video-box {
  width: 100%;
  height: 240px;
  background: #000;
  border-radius: 8px;
  overflow: hidden;
}

.video {
  width: 100%;
  height: 240px;
  object-fit: cover;
}
</style>
