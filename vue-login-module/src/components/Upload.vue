<template>
  <div class="upload-page">
    <div class="card">
      <div class="header">文件上传</div>
      <div class="dropzone" @dragover.prevent @drop.prevent="onDrop" @click="pickFile">
        <input ref="fileInput" type="file" class="hidden" @change="onPick" />
        <div class="hint">拖拽文件到此处或点击选择</div>
        <div v-if="file" class="file-info">
          <div class="name">{{ file.name }}</div>
          <div class="meta">{{ formatSize(file.size) }}</div>
        </div>
      </div>
      <div class="controls" v-if="file">
        <label class="label">分片大小</label>
        <input type="range" min="5" max="32" v-model.number="chunkSizeMb" />
        <div class="range-val">{{ chunkSizeMb }} MB</div>
        <div class="actions">
          <button class="btn primary" :disabled="running" @click="start">开始上传</button>
          <button class="btn" :disabled="!running || paused" @click="pause">暂停</button>
          <button class="btn" :disabled="!running || !paused" @click="resume">继续</button>
          <button class="btn" :disabled="!running" @click="abort">取消</button>
        </div>
      </div>
      <div v-if="progress.total > 0" class="progress">
        <div class="bar">
          <div class="fill" :style="{ width: percent + '%' }"></div>
        </div>
        <div class="stat">{{ progress.done }}/{{ progress.total }} 分片 · {{ percent.toFixed(1) }}% · 状态：{{ state }}</div>
      </div>
      <div v-if="message" class="message">{{ message }}</div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import axios from 'axios'
import { sha256 } from 'js-sha256'

const api = axios.create({ baseURL: '/upload/' })

const file = ref(null)
const fileInput = ref(null)
const chunkSizeMb = ref(8)
const running = ref(false)
const paused = ref(false)
const canceled = ref(false)
const state = ref('INIT')
const message = ref('')
const fileId = ref('')
const progress = ref({ done: 0, total: 0 })

let controllers = []
let resumeResolver = null

const percent = computed(() => {
  if (!progress.value.total) return 0
  return (progress.value.done * 100) / progress.value.total
})

const MAX_CHUNK_RETRY = 3
const MAX_MERGE_RETRY = 3

function pickFile() {
  fileInput.value && fileInput.value.click()
}

function onPick(e) {
  const f = e.target.files?.[0]
  if (f) file.value = f
}

function onDrop(e) {
  const f = e.dataTransfer.files?.[0]
  if (f) file.value = f
}

function formatSize(sz) {
  if (sz < 1024) return `${sz} B`
  if (sz < 1024 * 1024) return `${(sz / 1024).toFixed(1)} KB`
  if (sz < 1024 * 1024 * 1024) return `${(sz / 1024 / 1024).toFixed(1)} MB`
  return `${(sz / 1024 / 1024 / 1024).toFixed(1)} GB`
}

async function start() {
  if (!file.value) return
  running.value = true
  paused.value = false
  canceled.value = false
  controllers = []
  message.value = ''
  try {
    const sha256 = await calcSha256(file.value).catch(() => '')
    const reqChunkSize = chunkSizeMb.value * 1024 * 1024
    const initRes = await api.post('/init', {
      filename: file.value.name,
      size: file.value.size,
      chunkSize: reqChunkSize,
      sha256
    })
    fileId.value = initRes.data.data.fileId
    const serverChunkSize = initRes.data.data.chunkSize || reqChunkSize
    chunkSizeMb.value = Math.round(serverChunkSize / 1024 / 1024)
    const total = initRes.data.data.totalChunks || Math.ceil(file.value.size / serverChunkSize)
    const uploaded = initRes.data.data.uploadedIndices || []
    const missing = initRes.data.data.missingIndices || []
    const uploadedSet = new Set(uploaded)
    progress.value = { done: uploaded.length, total }
    state.value = 'UPLOADING'
    if (missing.length > 0) {
      await uploadSpecificChunks(file.value, fileId.value, serverChunkSize, missing)
    } else {
      await uploadChunks(file.value, fileId.value, serverChunkSize, uploadedSet)
    }
    if (!canceled.value) {
      const merged = await tryMergeWithMissing(fileId.value, serverChunkSize)
      if (!merged) {
        message.value = '合并失败：缺失分片重试已达上限'
        running.value = false
        return
      }
      await pollUntilFinished()
    }
  } catch (err) {
    message.value = '上传失败'
  } finally {
    running.value = false
  }
}

async function uploadChunks(f, fid, size, skipSet) {
  const total = Math.ceil(f.size / size)
  const concurrency = 3
  let idx = 0
  async function next() {
    if (canceled.value) return
    if (idx >= total) return
    const current = idx++
    if (skipSet && skipSet.has(current)) return next()
    if (canceled.value) return
    await waitIfPaused()
    if (canceled.value) return
    const start = current * size
    const end = Math.min(f.size, start + size)
    const blob = f.slice(start, end)
    const form = new FormData()
    form.append('file', blob)
    const controller = new AbortController()
    controllers.push(controller)
    let success = false
    for (let attempt = 0; attempt < MAX_CHUNK_RETRY && !success; attempt++) {
      try {
        await api.put(`/${fid}/chunk/${current}`, form, {
          headers: { 'Content-Type': 'multipart/form-data' },
          signal: controller.signal
        })
        success = true
        if (!canceled.value) {
          progress.value.done++
        }
      } catch (e) {
        if (attempt === MAX_CHUNK_RETRY - 1) {
          if (!canceled.value) message.value = `分片上传失败（索引 ${current}）`
          throw e
        }
        await new Promise(r => setTimeout(r, 500))
      }
    }
    try {
    } finally {
      controllers = controllers.filter(c => c !== controller)
    }
    if (!canceled.value) return next()
  }
  const tasks = Array.from({ length: Math.min(concurrency, total) }, () => next())
  await Promise.all(tasks)
}

async function uploadSpecificChunks(f, fid, size, indices) {
  const concurrency = 3
  let pos = 0
  async function next() {
    if (canceled.value) return
    if (pos >= indices.length) return
    const current = indices[pos++]
    await waitIfPaused()
    if (canceled.value) return
    const start = current * size
    const end = Math.min(f.size, start + size)
    const blob = f.slice(start, end)
    const form = new FormData()
    form.append('file', blob)
    const controller = new AbortController()
    controllers.push(controller)
    let success = false
    for (let attempt = 0; attempt < MAX_CHUNK_RETRY && !success; attempt++) {
      try {
        await api.put(`/${fid}/chunk/${current}`, form, {
          headers: { 'Content-Type': 'multipart/form-data' },
          signal: controller.signal
        })
        success = true
        if (!canceled.value) {
          progress.value.done++
        }
      } catch (e) {
        if (attempt === MAX_CHUNK_RETRY - 1) {
          if (!canceled.value) message.value = `分片上传失败（索引 ${current}）`
          throw e
        }
        await new Promise(r => setTimeout(r, 500))
      }
    }
    try {
    } finally {
      controllers = controllers.filter(c => c !== controller)
    }
    if (!canceled.value) return next()
  }
  const tasks = Array.from({ length: Math.min(concurrency, indices.length) }, () => next())
  await Promise.all(tasks)
}

async function tryMergeWithMissing(fid, size) {
  for (let attempt = 0; attempt < MAX_MERGE_RETRY; attempt++) {
    try {
      await api.post(`/${fid}/merge`)
      return true
    } catch (e) {
      if (!(e && e.response && e.response.status === 400)) throw e
      const st = await api.get(`/${fid}/status`)
      const miss = st.data?.data?.missingIndices || []
      if (!Array.isArray(miss) || miss.length === 0) return false
      await uploadSpecificChunks(file.value, fid, size, miss)
    }
  }
  return false
}

async function pollStatus() {
  const res = await api.get(`/${fileId.value}/status`)
  const data = res.data.data
  state.value = data.state
  progress.value.done = data.doneChunks
  progress.value.total = data.totalChunks
  if (data.state === 'FAILED' && data.errorMessage) message.value = data.errorMessage
}

async function pollUntilFinished() {
  let tries = 0
  while (true) {
    await pollStatus()
    if (state.value !== 'MERGING') break
    tries++
    if (tries > 60) break
    await new Promise(r => setTimeout(r, 1000))
  }
}

async function abort() {
  if (!fileId.value) return
  canceled.value = true
  await api.post(`/${fileId.value}/abort`)
  controllers.forEach(c => { try { c.abort() } catch {} })
  controllers = []
  state.value = 'CANCELLED'
  running.value = false
}

async function calcSha256(f) {
  const hash = sha256.create()
  if (f && typeof f.stream === 'function') {
    const reader = f.stream().getReader()
    while (true) {
      const { value, done } = await reader.read()
      if (done) break
      if (value && value.length) hash.update(value)
    }
  } else {
    const chunkSize = 200 * 1024 * 1024
    let offset = 0
    while (offset < f.size) {
      const end = Math.min(offset + chunkSize, f.size)
      const blob = f.slice(offset, end)
      const buf = await blob.arrayBuffer()
      hash.update(new Uint8Array(buf))
      offset = end
    }
  }
  return hash.hex()
}

function pause() {
  if (!running.value || paused.value) return
  paused.value = true
}

function resume() {
  if (!running.value || !paused.value) return
  paused.value = false
  if (resumeResolver) { const r = resumeResolver; resumeResolver = null; r() }
}

function waitIfPaused() {
  if (!paused.value) return Promise.resolve()
  return new Promise(res => { resumeResolver = res })
}
</script>

<style scoped>
.upload-page { display: flex; align-items: center; justify-content: center; min-height: 100vh; background: #0f172a; }
.card { width: 720px; background: #111827; border: 1px solid #1f2937; border-radius: 16px; padding: 24px; color: #e5e7eb; box-shadow: 0 10px 30px rgba(0,0,0,0.3); }
.header { font-size: 20px; font-weight: 600; margin-bottom: 12px; }
.dropzone { border: 2px dashed #374151; border-radius: 12px; padding: 24px; text-align: center; cursor: pointer; background: #0b1220; }
.dropzone:hover { border-color: #60a5fa; }
.hint { color: #9ca3af; }
.file-info { margin-top: 12px; display: flex; justify-content: center; gap: 12px; }
.name { font-weight: 500; }
.meta { color: #94a3b8; }
.controls { display: flex; align-items: center; gap: 12px; margin-top: 16px; }
.label { color: #9ca3af; }
.range-val { width: 80px; text-align: right; color: #93c5fd; }
.actions { margin-left: auto; display: flex; gap: 8px; }
.btn { padding: 10px 16px; border-radius: 8px; border: 1px solid #374151; background: #1f2937; color: #e5e7eb; }
.btn.primary { background: #2563eb; border-color: #2563eb; }
.btn:disabled { opacity: 0.6; cursor: not-allowed; }
.progress { margin-top: 16px; }
.bar { height: 10px; background: #111827; border: 1px solid #1f2937; border-radius: 6px; overflow: hidden; }
.fill { height: 100%; background: linear-gradient(90deg, #3b82f6, #06b6d4); }
.stat { margin-top: 8px; color: #9ca3af; }
.message { margin-top: 12px; color: #fca5a5; }
.hidden { display: none; }
</style>