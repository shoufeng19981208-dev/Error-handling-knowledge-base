<template>
  <div
    class="screenshot-uploader"
    :class="{ 'screenshot-uploader--dragging': dragging }"
    tabindex="0"
    @dragenter.prevent="dragging = true"
    @dragover.prevent="dragging = true"
    @dragleave.prevent="dragging = false"
    @drop.prevent="handleDrop"
    @paste="handlePaste"
  >
    <input
      ref="fileInput"
      type="file"
      accept="image/*"
      class="file-input-hidden"
      @change="handleFileChange"
    />

    <button v-if="!displayUrl" type="button" class="upload-zone" @click="pickFile">
      <span class="upload-icon">
        <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
          <rect x="3" y="3" width="18" height="18" rx="2"/>
          <circle cx="8.5" cy="8.5" r="1.5"/>
          <polyline points="21 15 16 10 5 21"/>
        </svg>
      </span>
      <strong>{{ dragging ? '松开即可上传' : '点击、拖拽或粘贴截图' }}</strong>
      <span>支持 PNG、JPG、GIF、WebP · 可直接 Ctrl/Command + V</span>
    </button>

    <div v-else class="preview-card">
      <div class="preview-image-wrap">
        <img :src="displayUrl" class="preview-image" alt="截图预览" @click="openPreview" />
        <span v-if="uploading" class="uploading-mask"><span class="spinner"></span>上传中…</span>
      </div>
      <div class="preview-actions">
        <span>{{ uploading ? '正在上传' : '截图已就绪' }}</span>
        <div>
          <button type="button" class="text-btn" :disabled="uploading" @click="pickFile">更换</button>
          <button type="button" class="text-btn text-btn--danger" :disabled="uploading" @click="remove">移除</button>
        </div>
      </div>
    </div>

    <div v-if="previewOpen" class="lightbox" @click="previewOpen = false">
      <img :src="displayUrl" alt="截图大图预览" @click.stop />
      <button type="button" aria-label="关闭预览" @click="previewOpen = false">×</button>
    </div>
  </div>
</template>

<script>
import { uploadScreenshot } from '../api/index';

export default {
  name: 'ScreenshotUploader',
  props: {
    value: { type: String, default: '' }
  },
  data() {
    return {
      localPreview: '',
      uploading: false,
      dragging: false,
      previewOpen: false
    };
  },
  computed: {
    displayUrl() {
      return this.localPreview || this.value;
    }
  },
  mounted() {
    window.addEventListener('paste', this.handleWindowPaste);
    window.addEventListener('keydown', this.handleKeydown);
  },
  beforeDestroy() {
    window.removeEventListener('paste', this.handleWindowPaste);
    window.removeEventListener('keydown', this.handleKeydown);
  },
  methods: {
    pickFile() {
      this.$refs.fileInput.click();
    },
    handleFileChange(event) {
      const file = event.target.files && event.target.files[0];
      event.target.value = '';
      if (file) this.processFile(file);
    },
    handleDrop(event) {
      this.dragging = false;
      const file = event.dataTransfer.files && event.dataTransfer.files[0];
      if (file) this.processFile(file);
    },
    handlePaste(event) {
      const file = this.imageFromClipboard(event.clipboardData);
      if (file) {
        event.preventDefault();
        this.processFile(file);
      }
    },
    handleWindowPaste(event) {
      if (event.defaultPrevented) return;
      const file = this.imageFromClipboard(event.clipboardData);
      if (file) this.processFile(file);
    },
    imageFromClipboard(clipboardData) {
      const items = Array.from((clipboardData && clipboardData.items) || []);
      const image = items.find(item => item.type && item.type.startsWith('image/'));
      return image ? image.getAsFile() : null;
    },
    async processFile(file) {
      if (!file.type || !file.type.startsWith('image/')) {
        this.$toast('请选择图片文件', 'warning');
        return;
      }
      this.localPreview = URL.createObjectURL(file);
      this.uploading = true;
      this.$emit('uploading', true);
      try {
        const result = await uploadScreenshot(file);
        if (!result.success) throw new Error(result.message || '上传失败');
        this.$emit('input', result.url);
        this.$toast('截图上传成功', 'success');
      } catch (error) {
        this.localPreview = '';
        this.$toast('截图上传失败：' + (error.response?.data?.message || error.message), 'error');
      } finally {
        this.uploading = false;
        this.$emit('uploading', false);
      }
    },
    remove() {
      this.localPreview = '';
      this.$emit('input', '');
    },
    openPreview() {
      if (!this.uploading) this.previewOpen = true;
    },
    handleKeydown(event) {
      if (event.key === 'Escape') this.previewOpen = false;
    }
  }
};
</script>

<style scoped>
.screenshot-uploader { outline: none; }
.file-input-hidden { display: none; }
.upload-zone { width: 100%; min-height: 150px; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 8px; border: 1.5px dashed var(--border-default); border-radius: var(--radius-lg); background: var(--color-neutral-50); color: var(--text-secondary); font-family: var(--font-sans); cursor: pointer; transition: all var(--transition-fast); }
.upload-zone:hover, .screenshot-uploader--dragging .upload-zone, .screenshot-uploader:focus-visible .upload-zone { border-color: var(--color-primary-400); background: var(--color-primary-50); color: var(--color-primary-700); }
.upload-zone span:last-child { font-size: var(--text-xs); color: var(--text-tertiary); }
.upload-icon { display: flex; color: var(--color-primary-500); }
.preview-card { border: 1px solid var(--border-light); border-radius: var(--radius-lg); overflow: hidden; background: var(--surface-card); }
.preview-image-wrap { position: relative; background: var(--color-neutral-100); }
.preview-image { display: block; width: 100%; max-height: 360px; object-fit: contain; cursor: zoom-in; }
.uploading-mask { position: absolute; inset: 0; display: flex; align-items: center; justify-content: center; gap: 8px; color: white; background: rgba(28,25,23,.58); }
.spinner { width: 16px; height: 16px; border: 2px solid rgba(255,255,255,.4); border-top-color: white; border-radius: 50%; animation: spin .7s linear infinite; }
.preview-actions { display: flex; align-items: center; justify-content: space-between; padding: 10px 14px; color: var(--text-tertiary); font-size: var(--text-xs); }
.text-btn { border: 0; background: transparent; color: var(--color-primary-600); padding: 4px 8px; cursor: pointer; font: inherit; }
.text-btn--danger { color: var(--color-danger); }
.text-btn:disabled { opacity: .45; cursor: not-allowed; }
.lightbox { position: fixed; inset: 0; z-index: 1000; display: flex; align-items: center; justify-content: center; padding: 5vw; background: rgba(0,0,0,.82); }
.lightbox img { max-width: 90vw; max-height: 90vh; object-fit: contain; }
.lightbox button { position: fixed; top: 20px; right: 24px; width: 40px; height: 40px; border: 0; border-radius: 50%; background: rgba(255,255,255,.16); color: white; font-size: 28px; cursor: pointer; }
@keyframes spin { to { transform: rotate(360deg); } }
</style>
