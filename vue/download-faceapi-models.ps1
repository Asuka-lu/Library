# download-faceapi-models.ps1
# 作用：下载 face-api.js 必需的 3 组模型到 public/models

# 1) 强制 TLS 1.2（解决“基础连接已经关闭”等常见问题）
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12

# 2) 目标目录：你的前端项目 public/models
$TargetDir = Join-Path (Get-Location) "public\models"
New-Item -ItemType Directory -Force -Path $TargetDir | Out-Null

# 3) 模型来源（GitHub Raw）
$Base = "https://raw.githubusercontent.com/justadudewhohacks/face-api.js/master/weights"

# 4) 要下载的文件清单（只包含你当前代码用到的三套）
$Files = @(
  "tiny_face_detector_model-weights_manifest.json",
  "tiny_face_detector_model-shard1",

  "face_landmark_68_model-weights_manifest.json",
  "face_landmark_68_model-shard1",

  "face_recognition_model-weights_manifest.json",
  "face_recognition_model-shard1",
  "face_recognition_model-shard2"
)

function Download-FileWithRetry($url, $outFile, $retries = 3) {
  for ($i = 1; $i -le $retries; $i++) {
    try {
      Write-Host "↓ ($i/$retries) $url"
      Invoke-WebRequest -Uri $url -OutFile $outFile -UseBasicParsing -TimeoutSec 60
      # 简单校验：文件必须存在且大小 > 0
      $len = (Get-Item $outFile).Length
      if ($len -le 0) { throw "Downloaded file is empty." }
      return
    } catch {
      Write-Warning "下载失败：$url"
      Write-Warning $_.Exception.Message
      if ($i -eq $retries) { throw }
      Start-Sleep -Seconds 2
    }
  }
}

Write-Host "目标目录：$TargetDir"
foreach ($f in $Files) {
  $url = "$Base/$f"
  $out = Join-Path $TargetDir $f
  Download-FileWithRetry $url $out 3
}

Write-Host "`n✅ 下载完成！请检查 public/models 下是否有这些文件："
Get-ChildItem $TargetDir | Select-Object Name, Length | Format-Table -AutoSize

Write-Host "`n下一步：打开浏览器访问（本地 dev server 启动后）"
Write-Host "http://localhost:5173/models/face_recognition_model-weights_manifest.json"
Write-Host "能看到 JSON 就说明路径没问题。"
