<template>
  <div class="wrap">
    <el-card class="card">
      <h2>Face-API 模型加载自测</h2>

      <div class="row">
        <el-button type="primary" @click="loadAllModels" :loading="loadingModels">
          1) 加载模型
        </el-button>

        <el-tag :type="modelsReady ? 'success' : 'info'">
          {{ modelsReady ? '模型已就绪' : '未加载' }}
        </el-tag>
      </div>

      <div class="row">
        <el-button @click="startCamera" :disabled="!modelsReady || camOn" :loading="startingCam">
          2) 开启摄像头
        </el-button>
        <el-button @click="detectOnce" type="success" :disabled="!modelsReady || !camOn">
          3) 检测一次（看控制台）
        </el-button>
      </div>

      <div class="tips">
        <div>页面地址应能访问：</div>
        <div class="mono">/models/tiny_face_detector_model-weights_manifest.json</div>
        <div class="mono">/models/face_landmark_68_model-weights_manifest.json</div>
        <div class="mono">/models/face_recognition_model-weights_manifest.json</div>
        <div>并且这些 manifest 里 paths 对应的 shard 文件也必须是 200。</div>
      </div>

      <div class="camera">
        <video ref="video" autoplay playsinline></video>
        <canvas ref="canvas" width="400" height="300" style="display:none;"></canvas>
      </div>

      <div class="result">
        <div><b>最近一次检测结果：</b></div>
        <div class="mono">{{ lastResult }}</div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import * as faceapi from 'face-api.js'
import '@tensorflow/tfjs-backend-webgl'

const video = ref(null)
const canvas = ref(null)

const loadingModels = ref(false)
const startingCam = ref(false)
const modelsReady = ref(false)
const camOn = ref(false)
const lastResult = ref('暂无')

let streamTrack = null

// 1) 加载模型（跟你项目一致：从 /models 目录）
const loadAllModels = async () => {
  loadingModels.value = true
  try {
    const MODEL_URL = '/models'
    await faceapi.nets.tinyFaceDetector.loadFromUri(MODEL_URL)
    await faceapi.nets.faceLandmark68Net.loadFromUri(MODEL_URL)
    await faceapi.nets.faceRecognitionNet.loadFromUri(MODEL_URL)

    modelsReady.value = true
    ElMessage.success('✅ 模型加载成功（tiny + landmark68 + recognition）')
    console.log('[MODEL TEST] models loaded OK')
  } catch (e) {
    modelsReady.value = false
    console.error('[MODEL TEST] load models failed =>', e)
    ElMessage.error('❌ 模型加载失败：请看控制台错误（F12）')
  } finally {
    loadingModels.value = false
  }
}

// 2) 开启摄像头
const startCamera = async () => {
  startingCam.value = true
  try {
    const stream = await navigator.mediaDevices.getUserMedia({ video: true })
    video.value.srcObject = stream
    streamTrack = stream.getTracks()[0]
    camOn.value = true
    ElMessage.success('✅ 摄像头已开启')
  } catch (e) {
    console.error('[MODEL TEST] camera failed =>', e)
    ElMessage.error('❌ 摄像头开启失败：检查权限')
  } finally {
    startingCam.value = false
  }
}

// 3) 检测一次：输出 box + 128维 descriptor
const detectOnce = async () => {
  try {
    // 把当前视频帧画到 canvas
    const ctx = canvas.value.getContext('2d', { willReadFrequently: true })
    ctx.drawImage(video.value, 0, 0, 400, 300)

    const det = await faceapi
        .detectSingleFace(
            canvas.value,
            new faceapi.TinyFaceDetectorOptions({ inputSize: 416, scoreThreshold: 0.5 })
        )
        .withFaceLandmarks()
        .withFaceDescriptor()

    if (!det) {
      lastResult.value = '未检测到人脸'
      ElMessage.warning('未检测到人脸：请正对镜头、光线更亮')
      console.log('[MODEL TEST] no face detected')
      return
    }

    const d = det.descriptor ? Array.from(det.descriptor) : []
    lastResult.value =
        `检测到人脸：box=(${Math.round(det.detection.box.x)},${Math.round(det.detection.box.y)},` +
        `${Math.round(det.detection.box.width)},${Math.round(det.detection.box.height)}), ` +
        `descriptor length=${d.length}`

    console.log('[MODEL TEST] detection =>', det.detection)
    console.log('[MODEL TEST] descriptor length =>', d.length)
    console.log('[MODEL TEST] descriptor =>', d)

    if (d.length === 128) {
      ElMessage.success('✅ 检测成功：descriptor=128维（控制台已打印）')
    } else {
      ElMessage.warning(`⚠️ 检测到了，但 descriptor 维度异常：${d.length}`)
    }
  } catch (e) {
    console.error('[MODEL TEST] detectOnce error =>', e)
    ElMessage.error('❌ 检测过程报错：看控制台')
  }
}

onUnmounted(() => {
  if (streamTrack) streamTrack.stop()
})
</script>

<style scoped>
.wrap {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
}
.card {
  width: 560px;
}
.row {
  display: flex;
  gap: 12px;
  align-items: center;
  margin: 10px 0;
}
.camera {
  width: 400px;
  height: 300px;
  background: #000;
  border-radius: 8px;
  overflow: hidden;
  margin: 14px auto;
}
video {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.tips {
  font-size: 13px;
  color: #666;
  line-height: 1.6;
  margin-top: 10px;
}
.mono {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New", monospace;
  color: #333;
}
.result {
  margin-top: 10px;
}
</style>
