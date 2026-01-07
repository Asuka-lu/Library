<template>
  <div class="page">
    <div class="panel">
      <div class="header">
        <div class="title">扫脸快速还书</div>
        <div class="sub">无需登录系统：刷脸确认身份后，扫码或手动输入完成归还。</div>
      </div>

      <!-- Stepper -->
      <el-steps :active="step" finish-status="success" simple style="margin-bottom: 12px;">
        <el-step title="刷脸确认" />
        <el-step title="扫码/输入归还" />
        <el-step title="完成" />
      </el-steps>

      <!-- ① 刷脸确认 -->
      <el-card class="card" shadow="never">
        <template #header>
          <div class="card-header">
            <div class="card-title">
              <span class="badge">①</span>
              刷脸确认身份
            </div>
            <div>
              <el-tag v-if="userMatched" type="success" effect="plain">
                已识别：{{ matchedUser?.nickName || matchedUser?.username || ("ID=" + matchedUser?.id) }}
              </el-tag>
              <el-tag v-else type="info" effect="plain">未识别</el-tag>
            </div>
          </div>
        </template>

        <div class="content-grid">
          <div class="video-wrap">
            <video ref="faceVideo" autoplay muted playsinline class="video"></video>
          </div>
          <div class="actions">
            <div class="hint">建议：正对镜头、光线充足、保持 30~60cm 距离。</div>
            <el-button type="primary" :loading="face.loading" @click="doFaceMatch">开始识别</el-button>
            <el-button @click="toggleFaceCamera">{{ face.cameraOn ? "关闭摄像头" : "打开摄像头" }}</el-button>
            <div v-if="face.status" class="status">{{ face.status }}</div>
            <el-alert v-if="userMatched" title="身份确认成功：请在下方选择扫码或手动输入归还" type="success" :closable="false" show-icon style="margin-top: 10px;" />
          </div>
        </div>
      </el-card>

      <!-- ② 归还：扫码 & 手动并排 -->
      <el-card class="card" shadow="never" style="margin-top: 12px;">
        <template #header>
          <div class="card-header">
            <div class="card-title">
              <span class="badge">②</span>
              扫码 / 手动输入归还
            </div>
            <div class="right-tools">
              <el-button size="small" type="primary" plain :disabled="!userMatched" :loading="scan.loading" @click="startScanOnce">开始扫码</el-button>
              <el-button size="small" :disabled="!userMatched" @click="stopScan">停止扫码</el-button>
            </div>
          </div>
        </template>

        <!-- 左右并排 -->
        <div class="content-grid">
          <!-- 左侧扫码 -->
          <div class="video-wrap">
            <video ref="scanVideo" autoplay muted playsinline class="video"></video>
            <div v-if="scan.status" class="status">{{ scan.status }}</div>
          </div>

          <!-- 右侧手动输入 -->
          <div class="manual-side">
            <div class="hint" style="margin-bottom: 8px;">手动输入 ISBN / 条码</div>
            <el-input v-model="manual.code" placeholder="请输入 ISBN 或条码内容" clearable @keyup.enter="returnByCode(manual.code)">
              <template #append>
                <el-button type="primary" :loading="manual.loading" :disabled="!userMatched" @click="returnByCode(manual.code)">归还</el-button>
              </template>
            </el-input>
            <div v-if="manual.status" class="status">{{ manual.status }}</div>
          </div>
        </div>

        <!-- 底部工具栏 -->
        <div class="footer-row">
          <el-button type="text" @click="$router.push('/login')">返回登录</el-button>
          <el-button type="text" @click="resetAll">重置流程</el-button>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script>
import request from "@/utils/request";
import { ElMessage } from "element-plus";
import * as faceapi from "face-api.js";
import { BrowserMultiFormatReader } from "@zxing/browser";

export default {
  name: "FaceReturn",
  data() {
    return {
      step: 1,
      matchedUser: null,
      userMatched: false,
      returnMode: "scan",
      face: {
        modelsLoaded: false,
        cameraOn: false,
        stream: null,
        loading: false,
        status: "",
      },
      scan: {
        reader: null,
        controls: null,
        loading: false,
        status: "",
        handling: false,
      },
      manual: {
        code: "",
        loading: false,
        status: "",
      },
    };
  },
  async mounted() {
    await this.ensureModelsLoaded();
  },
  beforeUnmount() {
    this.stopFaceCamera();
    this.stopScan();
  },
  methods: {
    /* 人脸相关 */
    async ensureModelsLoaded() {
      if (this.face.modelsLoaded) return;
      try {
        this.face.status = "正在加载人脸模型...";
        await faceapi.nets.tinyFaceDetector.loadFromUri("/models");
        await faceapi.nets.faceLandmark68Net.loadFromUri("/models");
        await faceapi.nets.faceRecognitionNet.loadFromUri("/models");
        this.face.modelsLoaded = true;
        this.face.status = "模型加载完成";
      } catch (e) {
        console.error(e);
        this.face.status = "";
        ElMessage.error("人脸模型加载失败：请检查 public/models 是否放置正确");
      }
    },
    async startFaceCamera() {
      if (this.face.cameraOn) return;
      try {
        const stream = await navigator.mediaDevices.getUserMedia({ video: { facingMode: "user" }, audio: false });
        this.face.stream = stream;
        this.face.cameraOn = true;
        this.$refs.faceVideo.srcObject = stream;
        this.face.status = "摄像头已开启";
      } catch (e) {
        console.error(e);
        ElMessage.error("无法打开摄像头：请检查浏览器权限（建议 localhost 或 https）");
      }
    },
    stopFaceCamera() {
      if (this.face.stream) this.face.stream.getTracks().forEach((t) => t.stop());
      this.face.stream = null;
      this.face.cameraOn = false;
      if (this.$refs.faceVideo) this.$refs.faceVideo.srcObject = null;
    },
    async toggleFaceCamera() {
      if (this.face.cameraOn) this.stopFaceCamera();
      else {
        await this.ensureModelsLoaded();
        await this.startFaceCamera();
      }
    },
    async doFaceMatch() {
      await this.ensureModelsLoaded();
      if (!this.face.cameraOn) await this.startFaceCamera();
      const video = this.$refs.faceVideo;
      if (!video) return;
      try {
        this.face.loading = true;
        this.face.status = "正在识别人脸...";
        const detectPromise = faceapi
            .detectSingleFace(video, new faceapi.TinyFaceDetectorOptions({ inputSize: 224, scoreThreshold: 0.5 }))
            .withFaceLandmarks()
            .withFaceDescriptor();
        const timeoutPromise = new Promise((_, reject) => setTimeout(() => reject(new Error("FACE_TIMEOUT")), 6000));
        const detection = await Promise.race([detectPromise, timeoutPromise]);
        if (!detection || !detection.descriptor) {
          ElMessage.error("未检测到人脸：请正对摄像头并保持光线充足");
          return;
        }
        const descriptor = Array.from(detection.descriptor);
        const res = await request.post("/user/face/login", { descriptor });
        if (res.code === "0" || res.code === 0) {
          this.matchedUser = res.data;
          this.userMatched = true;
          this.step = 2;
          this.face.status = "识别成功";
          ElMessage.success("身份确认成功，请归还图书");
        } else {
          this.userMatched = false;
          this.matchedUser = null;
          ElMessage.error(res.msg || "识别失败：可能未绑定人脸");
        }
      } catch (e) {
        console.error(e);
        if (String(e.message) === "FACE_TIMEOUT") {
          ElMessage.error("识别超时：请调整光线/距离后重试");
        } else {
          ElMessage.error("识别异常，请稍后重试");
        }
      } finally {
        this.face.loading = false;
        this.face.status = "";
      }
    },

    /* 扫码相关 */
    async startScanOnce() {
      if (!this.userMatched || !this.matchedUser?.id) {
        ElMessage.warning("请先刷脸确认身份");
        return;
      }
      if (this.scan.loading) return;
      const video = this.$refs.scanVideo;
      if (!video) return;
      try {
        this.scan.loading = true;
        this.scan.status = "正在打开摄像头...";
        if (!this.scan.reader) this.scan.reader = new BrowserMultiFormatReader();
        this.scan.controls = await this.scan.reader.decodeFromVideoDevice(
            undefined,
            video,
            async (result, err, controls) => {
              if (result && !this.scan.handling) {
                this.scan.handling = true;
                const code = result.getText();
                this.scan.status = `识别到：${code}，正在归还...`;
                try {
                  await this.returnByCode(code);
                } finally {
                  if (controls) controls.stop();
                  this.scan.handling = false;
                }
              }
              if (err && err.name && err.name !== "NotFoundException") {
                console.error("扫码异常：", err);
              }
            }
        );
        this.scan.status = "摄像头已开启，请对准条形码/二维码...";
      } catch (e) {
        console.error(e);
        ElMessage.error("无法开启扫码：请检查摄像头权限（建议 localhost 或 https）");
      } finally {
        this.scan.loading = false;
      }
    },
    stopScan() {
      try {
        if (this.scan.controls) this.scan.controls.stop();
        if (this.scan.reader) this.scan.reader.reset();
      } catch (e) {}
      this.scan.controls = null;
      this.scan.status = "";
      this.scan.loading = false;
      this.scan.handling = false;
    },

    /* 归还逻辑（扫码 & 手动共用） */
    async returnByCode(code) {
      const c = (code || "").trim();
      if (!c) {
        ElMessage.warning("请输入/识别到 ISBN 或条码内容");
        return;
      }
      if (!this.userMatched || !this.matchedUser?.id) {
        ElMessage.warning("请先刷脸确认身份");
        return;
      }
      if (this.returnMode === "manual") {
        this.manual.loading = true;
        this.manual.status = "";
      }
      try {
        const res = await request.post("/borrow/return", { userId: this.matchedUser.id, isbn: c });
        if (res.code === "0" || res.code === 0) {
          this.step = 3;
          ElMessage.success("还书成功");
          this.manual.code = "";
          this.manual.status = "还书成功，可继续归还下一本";
          this.scan.status = "还书成功，可继续扫码归还下一本";
        } else {
          ElMessage.error(res.msg || "还书失败");
        }
      } catch (e) {
        console.error(e);
        ElMessage.error("还书异常，请稍后重试");
      } finally {
        if (this.returnMode === "manual") this.manual.loading = false;
      }
    },

    /* 重置 */
    resetAll() {
      this.stopScan();
      this.manual.code = "";
      this.manual.status = "";
      this.scan.status = "";
      this.userMatched = false;
      this.matchedUser = null;
      this.step = 1;
      ElMessage.success("已重置");
    },
  },
};
</script>

<style scoped>
.page {
  position: fixed;
  inset: 0;
  background: url("../img/bg2.svg");
  background-size: contain;
  overflow: auto;
  padding: 22px 0;
}
.panel {
  width: 860px;
  margin: 0 auto;
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid #eee;
  border-radius: 10px;
  box-shadow: 0 0 25px rgba(0, 0, 0, 0.12);
  padding: 18px 18px 16px;
}
.header {
  margin-bottom: 10px;
}
.title {
  font-size: 20px;
  font-weight: 700;
  color: #333;
}
.sub {
  margin-top: 6px;
  font-size: 13px;
  color: #666;
}
.card {
  border-radius: 10px;
}
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.card-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 650;
}
.badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border-radius: 6px;
  background: #f3f4f6;
  color: #333;
  font-size: 12px;
}
.right-tools {
  display: flex;
  gap: 8px;
}
.content-grid {
  display: grid;
  grid-template-columns: 1.2fr 1fr;
  gap: 12px;
  align-items: start;
}
.video-wrap {
  width: 100%;
  height: 240px;
  background: #000;
  border-radius: 10px;
  overflow: hidden;
}
.video {
  width: 100%;
  height: 240px;
  object-fit: cover;
}
.actions {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.hint {
  font-size: 12px;
  color: #777;
  line-height: 1.4;
}
.status {
  font-size: 12px;
  color: #999;
}
.manual-side {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.footer-row {
  display: flex;
  justify-content: space-between;
  margin-top: 10px;
}
</style>