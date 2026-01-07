<template>
  <div class="page">
    <div class="panel">
      <!-- 顶部标题 -->
      <div class="header">
        <div class="title">扫脸快速还书</div>
        <div class="sub">无需登录系统：刷脸确认身份后，扫码或手动输入完成归还。</div>
      </div>

      <!-- 顶部步骤条 -->
      <div class="steps-wrap">
        <el-steps :active="step" finish-status="success" simple>
          <el-step title="刷脸确认" />
          <el-step title="扫码/输入归还" />
          <el-step title="完成" />
        </el-steps>
      </div>

      <!-- ① 刷脸确认 -->
      <el-card class="card" shadow="never">
        <template #header>
          <div class="card-header">
            <div class="card-title">
              <span class="num">①</span>
              刷脸确认身份
            </div>
            <el-tag v-if="userMatched" type="success" effect="plain" class="tag">
              已识别：{{ matchedUser?.nickName || matchedUser?.username || ("ID=" + matchedUser?.id) }}
            </el-tag>
            <el-tag v-else type="info" effect="plain" class="tag">未识别</el-tag>
          </div>
        </template>

        <div class="grid">
          <!-- 左：视频 -->
          <div class="video-wrap">
            <video ref="faceVideo" autoplay muted playsinline class="video"></video>
            <div class="video-mask">
              <div class="mask-text">请正对镜头</div>
            </div>
          </div>

          <!-- 右：操作 -->
          <div class="ops">
            <div class="tips">
              建议：正对镜头、光线充足、保持 30~60cm 距离。
            </div>

            <el-button class="btn primary" type="primary" :loading="face.loading" @click="doFaceMatch">
              开始识别
            </el-button>

            <el-button class="btn" @click="toggleFaceCamera">
              {{ face.cameraOn ? "关闭摄像头" : "打开摄像头" }}
            </el-button>

            <div v-if="face.status" class="status">{{ face.status }}</div>

            <el-alert
                v-if="userMatched"
                title="身份确认成功：请在下方扫码或手动输入归还"
                type="success"
                :closable="false"
                show-icon
                class="alert"
            />
          </div>
        </div>
      </el-card>

      <!-- ② 扫码/输入归还 -->
      <el-card class="card" shadow="never" style="margin-top: 14px;">
        <template #header>
          <div class="card-header">
            <div class="card-title">
              <span class="num">②</span>
              扫码 / 手动输入归还
            </div>

            <div class="right-tools">
              <el-button
                  size="small"
                  type="primary"
                  plain
                  :disabled="!userMatched"
                  :loading="scan.loading"
                  @click="startScanOnce"
              >
                开始扫码
              </el-button>
              <el-button size="small" :disabled="!userMatched" @click="stopScan">
                停止扫码
              </el-button>
            </div>
          </div>
        </template>

        <div class="grid">
          <!-- 左：扫码视频 -->
          <div>
            <div class="video-wrap scan">
              <video ref="scanVideo" autoplay muted playsinline class="video"></video>
              <div class="scan-frame"></div>
            </div>
            <div v-if="scan.status" class="status" style="margin-top: 8px;">
              {{ scan.status }}
            </div>
          </div>

          <!-- 右：手动输入 -->
          <div class="manual">
            <div class="tips" style="margin-bottom: 10px;">
              手动输入 ISBN / 条码（输入后点击“归还”提交）。
            </div>

            <div class="manual-label">手动输入 ISBN / 条码</div>

            <el-input
                v-model="manual.code"
                placeholder="请输入 ISBN 或条码内容"
                clearable
                @keyup.enter="returnByCode(manual.code, 'manual')"
            >
            </el-input>

            <el-button
                class="manual-btn"
                type="primary"
                :loading="manual.loading"
                :disabled="!userMatched"
                @click="returnByCode(manual.code, 'manual')"
            >
              归还
            </el-button>
            <div v-if="manual.status" class="status" style="margin-top: 8px;">
              {{ manual.status }}
            </div>

            <el-alert
                v-if="!userMatched"
                title="请先在上方刷脸确认身份"
                type="warning"
                :closable="false"
                show-icon
                class="alert"
                style="margin-top: 12px;"
            />
          </div>
        </div>

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
    // ===== 人脸 =====
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
        const stream = await navigator.mediaDevices.getUserMedia({
          video: { facingMode: "user" },
          audio: false,
        });
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
            .detectSingleFace(
                video,
                new faceapi.TinyFaceDetectorOptions({ inputSize: 224, scoreThreshold: 0.5 })
            )
            .withFaceLandmarks()
            .withFaceDescriptor();

        const timeoutPromise = new Promise((_, reject) =>
            setTimeout(() => reject(new Error("FACE_TIMEOUT")), 6000)
        );

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

    // ===== 扫码 =====
    async startScanOnce() {
      this.returnMode = "scan";

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
                  await this.returnByCode(code, "scan");
                } finally {
                  if (controls) controls.stop(); // 识别一次就停，避免一直扫
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

    // ===== 归还（扫码&手动共用） =====
    async returnByCode(code, mode = "scan") {
      this.returnMode = mode;

      const c = (code || "").trim();
      if (!c) {
        ElMessage.warning("请输入/识别到 ISBN 或条码内容");
        return;
      }
      if (!this.userMatched || !this.matchedUser?.id) {
        ElMessage.warning("请先刷脸确认身份");
        return;
      }

      // 手动模式 loading
      if (mode === "manual") {
        this.manual.loading = true;
        this.manual.status = "";
      }

      try {
        // 你后端若改成 {userId, code}，这里建议传 code
        // 目前你用的是 isbn 字段，我保持兼容
        const res = await request.post("/borrow/return", {
          userId: this.matchedUser.id,
          isbn: c,
        });

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
        if (mode === "manual") this.manual.loading = false;
      }
    },

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
/* 背景与容器 */
.page {
  position: fixed;
  inset: 0;
  background: url("../img/bg2.svg");
  background-size: cover;
  background-position: center;
  overflow: auto;
  padding: 26px 0;
}
.panel {
  width: 980px;
  margin: 0 auto;
  padding: 18px 18px 16px;
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(234, 234, 234, 0.9);
  border-radius: 14px;
  box-shadow: 0 18px 45px rgba(0, 0, 0, 0.14);
  backdrop-filter: blur(8px);
}

/* 头部 */
.header {
  margin-bottom: 12px;
}
.title {
  font-size: 22px;
  font-weight: 800;
  color: #222;
}
.sub {
  margin-top: 6px;
  font-size: 13px;
  color: #666;
}

/* steps 容器 */
.steps-wrap {
  margin-bottom: 12px;
}

/* 卡片 */
.card {
  border-radius: 14px;
  border: 1px solid #f0f0f0;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.card-title {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  font-weight: 750;
  color: #222;
}
.num {
  width: 26px;
  height: 26px;
  display: inline-flex;
  justify-content: center;
  align-items: center;
  border-radius: 9px;
  background: linear-gradient(135deg, rgba(64, 158, 255, 0.12), rgba(103, 194, 58, 0.10));
  color: #2f5fb3;
  font-weight: 800;
  border: 1px solid rgba(64, 158, 255, 0.18);
  font-size: 12px;
}
.tag {
  border-radius: 10px;
}

/* 网格布局 */
.grid {
  display: grid;
  grid-template-columns: 1.2fr 1fr;
  gap: 14px;
  align-items: stretch;
}

/* 视频区 */
.video-wrap {
  position: relative;
  width: 100%;
  height: 260px;
  border-radius: 14px;
  overflow: hidden;
  background: #0b0b0b;
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.08);
}
.video {
  width: 100%;
  height: 260px;
  object-fit: cover;
  filter: contrast(1.05) saturate(1.05);
}

/* 人脸视频轻提示遮罩 */
.video-mask {
  position: absolute;
  inset: 0;
  pointer-events: none;
  display: flex;
  align-items: flex-start;
  justify-content: flex-start;
}
.mask-text {
  margin: 10px;
  padding: 6px 10px;
  font-size: 12px;
  color: rgba(255,255,255,.85);
  background: rgba(0,0,0,.35);
  border: 1px solid rgba(255,255,255,.18);
  border-radius: 10px;
}

/* 扫码框虚线 */
.video-wrap.scan .scan-frame {
  position: absolute;
  inset: 14px;
  border-radius: 12px;
  border: 2px dashed rgba(255, 255, 255, 0.55);
  box-shadow: 0 0 0 9999px rgba(0,0,0,.16) inset;
  pointer-events: none;
}

/* 右侧操作区 */
.ops,
.manual {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 6px 2px;
}

/* 提示块 */
.tips {
  font-size: 12.5px;
  color: #666;
  line-height: 1.45;
  background: #fafafa;
  border: 1px solid #f0f0f0;
  padding: 10px 10px;
  border-radius: 12px;
}

/* 按钮 */
.btn {
  width: 100%;
  height: 40px;
  border-radius: 12px;
  font-weight: 650;
}
.btn.primary {
  box-shadow: 0 10px 18px rgba(64, 158, 255, 0.18);
}

/* 右上角工具按钮 */
.right-tools {
  display: flex;
  gap: 10px;
}
.right-tools :deep(.el-button) {
  border-radius: 12px;
}

/* 输入区美化 */
.manual-label {
  font-size: 12px;
  color: #666;
  margin-bottom: 2px;
}
.manual :deep(.el-input__wrapper) {
  border-radius: 12px;
  height: 42px;
}
.manual :deep(.el-input-group__append) {
  padding: 0;
  border-radius: 0 12px 12px 0;
  overflow: hidden;
}

/* 状态 */
.status {
  font-size: 12px;
  color: #8a8a8a;
}

/* alert */
.alert {
  border-radius: 12px;
}

/* 底部 */
.footer-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 12px;
  padding-top: 10px;
  border-top: 1px dashed #eee;
}

/* steps 更清爽 */
:deep(.el-steps--simple) {
  background: transparent;
}
:deep(.el-step__title) {
  font-weight: 650;
}
</style>
