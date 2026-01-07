<template>
  <div>
    <el-card style="width: 40%; margin-left: 120px; margin-top: 40px">
      <h2 style="padding: 30px">个人信息</h2>

      <el-form :model="form" ref="form" label-width="80px">
        <el-form-item label="用户名">
          <el-input style="width: 80%" v-model="form.username" disabled></el-input>
        </el-form-item>

        <el-form-item label="姓名">
          <el-input style="width: 80%" v-model="form.nickName"></el-input>
        </el-form-item>

        <el-form-item label="权限">
          <span v-if="form.role==1" style="margin:5px">管理员</span>
          <span v-if="form.role==2" style="margin:5px">读者</span>
        </el-form-item>

        <el-form-item label="电话号码">
          <el-input style="width: 80%" v-model="form.phone"></el-input>
        </el-form-item>

        <el-form-item label="性别">
          <div>
            <el-radio v-model="form.sex" label="男">男</el-radio>
            <el-radio v-model="form.sex" label="女">女</el-radio>
          </div>
        </el-form-item>

        <el-form-item label="地址">
          <el-input type="textarea" style="width: 80%" v-model="form.address"></el-input>
        </el-form-item>

        <!-- ✅ 人脸绑定状态：改为检测 faceDescriptor 是否为空 -->
        <el-form-item label="人脸">
          <el-tag v-if="isFaceBound" type="success">已绑定</el-tag>
          <el-tag v-else type="info">未绑定</el-tag>
          <span style="margin-left: 10px; color: #999; font-size: 12px;">
            绑定后可用于刷脸登录/自助借还
          </span>
        </el-form-item>
      </el-form>

      <div style="text-align: center; display:flex; justify-content:center; gap: 12px;">
        <el-button type="primary" @click="update">保存</el-button>

        <!-- ✅ 按绑定状态切换文案：绑定/换绑 -->
        <el-button @click="openFaceDialog">
          {{ isFaceBound ? "换绑人脸" : "绑定人脸" }}
        </el-button>
      </div>
    </el-card>

    <!-- ✅ 绑定/换绑人脸弹窗 -->
    <el-dialog
        v-model="face.dialogVisible"
        :title="isFaceBound ? '换绑人脸' : '绑定人脸'"
        width="520px"
        @closed="onFaceDialogClosed"
    >
      <div style="margin-bottom: 8px; font-size: 13px; color:#666;">
        请正对摄像头，光线充足，点击“采集并保存”。
      </div>

      <div class="video-box">
        <video ref="videoEl" autoplay muted playsinline class="video"></video>
      </div>

      <div style="margin-top: 12px; display:flex; gap: 10px;">
        <el-button type="primary" style="flex: 1" :loading="face.loading" @click="captureAndBind">
          采集并保存
        </el-button>
        <el-button style="flex: 1" @click="toggleCamera">
          {{ face.cameraOn ? "关闭摄像头" : "打开摄像头" }}
        </el-button>
      </div>

      <div v-if="face.status" style="margin-top: 8px; font-size: 12px; color:#999;">
        {{ face.status }}
      </div>

      <template #footer>
        <el-button @click="face.dialogVisible=false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import request from "@/utils/request";
import { ElMessage } from "element-plus";
import * as faceapi from "face-api.js";

export default {
  name: "Person",
  data() {
    return {
      form: {},
      face: {
        dialogVisible: false,
        modelsLoaded: false,
        cameraOn: false,
        stream: null,
        loading: false,
        status: "",
      },
    };
  },

  computed: {
    // ✅ 是否绑定：以 faceDescriptor 是否为空作为判断依据
    isFaceBound() {
      const v = this.form?.faceDescriptor;
      return v !== null && v !== undefined && String(v).trim() !== "";
    },
  },

  created() {
    // 1) 先从 sessionStorage 读，保证页面能立刻显示
    const str = sessionStorage.getItem("user") || "{}";
    const u = JSON.parse(str);
    this.form = u;

    // 2) 再从后端拉最新（关键：否则绑定后重新进入页面仍可能显示未绑定）
    if (u.id) {
      this.refreshUserFromServer(u.id);
    }
  },

  beforeUnmount() {
    this.stopCamera();
  },

  methods: {
    // ✅ 从后端刷新用户信息（需要后端 GET /user/{id}）
    async refreshUserFromServer(id) {
      try {
        const res = await request.get(`/user/${id}`);
        if (res.code === "0" || res.code === 0) {
          this.form = res.data;

          // 合并写回 sessionStorage（避免覆盖 token 等字段）
          const old = JSON.parse(sessionStorage.getItem("user") || "{}");
          sessionStorage.setItem("user", JSON.stringify({ ...old, ...res.data }));
        }
      } catch (e) {
        // 后端没这个接口或网络异常也没关系，至少能显示 sessionStorage 的信息
        console.warn("refreshUserFromServer failed:", e);
      }
    },

    // ✅ 保存个人信息：保存成功后合并写回 sessionStorage（避免把 faceDescriptor/token 覆盖丢）
    update() {
      request.put("/user", this.form).then((res) => {
        if (res.code === "0" || res.code === 0) {
          ElMessage.success("更新成功");

          const old = JSON.parse(sessionStorage.getItem("user") || "{}");
          sessionStorage.setItem("user", JSON.stringify({ ...old, ...this.form }));

          this.$emit("userInfo");
        } else {
          ElMessage.error(res.msg);
        }
      });
    },

    // ✅ 打开绑定/换绑人脸弹窗
    async openFaceDialog() {
      if (!this.form.id) {
        ElMessage.error("用户信息不完整：缺少 id");
        return;
      }
      this.face.dialogVisible = true;
      await this.ensureModelsLoaded();
      await this.startCamera();
    },

    // ✅ 加载模型（public/models）
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

    // ✅ 摄像头控制
    async startCamera() {
      if (this.face.cameraOn) return;
      try {
        const stream = await navigator.mediaDevices.getUserMedia({
          video: { facingMode: "user" },
          audio: false,
        });
        this.face.stream = stream;
        this.face.cameraOn = true;
        this.$refs.videoEl.srcObject = stream;
        this.face.status = "摄像头已开启";
      } catch (e) {
        console.error(e);
        this.face.status = "";
        ElMessage.error("无法打开摄像头：请检查浏览器权限（建议 localhost 或 https）");
      }
    },

    stopCamera() {
      if (this.face.stream) {
        this.face.stream.getTracks().forEach((t) => t.stop());
      }
      this.face.stream = null;
      this.face.cameraOn = false;
      if (this.$refs.videoEl) this.$refs.videoEl.srcObject = null;
    },

    async toggleCamera() {
      if (this.face.cameraOn) this.stopCamera();
      else await this.startCamera();
    },

    // ✅ 采集 descriptor 并调用后端绑定接口（换绑也是覆盖 face_descriptor）
    async captureAndBind() {
      await this.ensureModelsLoaded();
      if (!this.face.cameraOn) await this.startCamera();

      const video = this.$refs.videoEl;
      if (!video) {
        ElMessage.error("视频组件未初始化");
        return;
      }

      try {
        this.face.loading = true;
        this.face.status = "正在识别人脸...";

        const detection = await faceapi
            .detectSingleFace(
                video,
                new faceapi.TinyFaceDetectorOptions({
                  inputSize: 224,
                  scoreThreshold: 0.5,
                })
            )
            .withFaceLandmarks()
            .withFaceDescriptor();

        if (!detection || !detection.descriptor) {
          this.face.status = "";
          ElMessage.error("未检测到人脸：请正对摄像头并保持光线充足");
          return;
        }

        const descriptor = Array.from(detection.descriptor);

        const res = await request.post("/user/face/register", {
          userId: this.form.id,
          descriptor,
        });

        if (res.code === "0" || res.code === 0) {
          ElMessage.success(this.isFaceBound ? "换绑成功" : "绑定成功");

          // ✅ 绑定/换绑成功后：从后端刷新一次用户信息，确保 faceDescriptor 非空 -> 页面立刻显示已绑定
          await this.refreshUserFromServer(this.form.id);

          this.face.dialogVisible = false;
        } else {
          ElMessage.error(res.msg || "人脸绑定失败");
        }
      } catch (e) {
        console.error(e);
        ElMessage.error("绑定异常，请稍后重试");
      } finally {
        this.face.loading = false;
        this.face.status = "";
      }
    },

    onFaceDialogClosed() {
      // 关闭弹窗就关摄像头，避免占用
      this.stopCamera();
      this.face.status = "";
    },
  },
};
</script>

<style scoped>
.video-box {
  width: 100%;
  height: 260px;
  background: #000;
  border-radius: 8px;
  overflow: hidden;
}
.video {
  width: 100%;
  height: 260px;
  object-fit: cover;
}
</style>
