<template>
  <div class="documents-page">
    <div class="documents-header">
      <div>
        <h1 class="page-title">文档管理</h1>
        <p class="page-desc">上传任意格式文档，点击「预览」在线查看；支持图片、PDF、Word/Excel/PPT 新版格式、文本、音视频等，旧版 doc/xls/ppt 可下载查看</p>
      </div>
      <button type="button" class="btn-primary" @click="triggerUpload">+ 上传文档</button>
      <input ref="fileInput" type="file" multiple class="file-input-hidden" @change="handleFileChange" />
    </div>

    <!-- Upload Zone -->
    <div class="upload-zone" @dragover.prevent @drop.prevent="handleDrop" @click="triggerUpload">
      <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
        <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
        <polyline points="17 8 12 3 7 8"/>
        <line x1="12" y1="3" x2="12" y2="15"/>
      </svg>
      <div class="upload-zone-text">点击或拖拽上传文档</div>
      <div class="upload-zone-hint">格式不限，单文件最大 50MB，支持一次选择多个文件</div>
    </div>

    <!-- Upload Progress -->
    <div v-if="uploading" class="upload-progress">
      <span class="upload-progress-text">正在上传 {{ uploadingIndex }}/{{ uploadingTotal }}：{{ uploadingName }}</span>
      <div class="progress-bar">
        <div class="progress-bar-inner" :style="{ width: uploadingPercent + '%' }"></div>
      </div>
    </div>

    <!-- List -->
    <div v-if="loading" class="doc-loading">加载中...</div>

    <template v-else>
      <div v-if="documents.length" class="doc-list">
        <div v-for="doc in documents" :key="doc.id" class="doc-item">
          <div class="doc-icon" :class="'doc-icon--' + fileKind(doc)">
            <span>{{ iconLabel(fileKind(doc)) }}</span>
          </div>
          <div class="doc-main">
            <div class="doc-name" :title="doc.originalName" @click="openPreview(doc)">{{ doc.originalName }}</div>
            <div class="doc-meta">
              <span class="doc-type-badge">{{ typeLabel(doc) }}</span>
              <span>{{ formatSize(doc.size) }}</span>
              <span>{{ formatTime(doc.uploadTime) }}</span>
            </div>
          </div>
          <div class="doc-actions">
            <button type="button" class="btn-mini" @click="openPreview(doc)">预览</button>
            <a class="btn-mini btn-mini--link" :href="doc.url" :download="doc.originalName">下载</a>
            <button type="button" class="btn-mini btn-mini--danger" @click="remove(doc)">删除</button>
          </div>
        </div>
      </div>
      <EmptyState v-else title="还没有上传文档" description="点击上方按钮或拖拽文件到虚线区域，即可上传任意格式文档" />
    </template>

    <!-- Preview Modal -->
    <div v-if="previewDoc" class="modal-mask preview-mask" @click.self="closePreview">
      <div class="modal-body preview-body">
        <div class="preview-header">
          <div class="preview-title" :title="previewDoc.originalName">{{ previewDoc.originalName }}</div>
          <div class="preview-header-actions">
            <a class="preview-download" :href="previewDoc.url" :download="previewDoc.originalName">下载</a>
            <button type="button" class="preview-close" @click="closePreview">×</button>
          </div>
        </div>
        <div class="preview-content">
          <img v-if="previewKind === 'image'" :src="previewDoc.url" class="preview-image" alt="文档预览" />

          <iframe v-else-if="previewKind === 'pdf'" :src="previewDoc.url" class="preview-iframe"></iframe>

          <div v-else-if="previewHtml" class="preview-html-wrap">
            <iframe class="preview-iframe" :srcdoc="previewHtml"></iframe>
          </div>

          <div v-else-if="previewSheets.length" class="preview-excel">
            <div class="sheet-tabs">
              <button
                v-for="(sheet, idx) in previewSheets"
                :key="idx"
                type="button"
                :class="['sheet-tab', { 'sheet-tab--active': idx === activeSheet }]"
                @click="activeSheet = idx"
              >{{ sheet.name }}</button>
            </div>
            <iframe class="preview-iframe" :srcdoc="previewSheets[activeSheet].html"></iframe>
          </div>

          <div v-else-if="previewKind === 'docx'" ref="docxContainer" class="preview-docx"></div>

          <div v-else-if="previewKind === 'pptx'" ref="pptxContainer" class="preview-pptx"></div>

          <pre v-else-if="previewKind === 'text' || previewKind === 'ppt'" class="preview-text">{{ textContent }}</pre>

          <video v-else-if="previewKind === 'video'" :src="previewDoc.url" class="preview-video" controls></video>

          <audio v-else-if="previewKind === 'audio'" :src="previewDoc.url" class="preview-audio" controls></audio>

          <div v-else class="preview-unsupported">
            <p>该格式暂不支持在线预览，请下载后使用本机软件查看</p>
            <a class="btn-primary" :href="previewDoc.url" :download="previewDoc.originalName">下载文件</a>
          </div>

          <div v-if="previewLoading" class="preview-loading">正在加载预览...</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { getDocuments, uploadDocument, deleteDocument, getDocumentPreview } from '../api/index';
import { renderAsync } from 'docx-preview';
import { init as initPptxPreview } from 'pptx-preview';
import EmptyState from '../components/EmptyState.vue';

const IMAGE_EXTS = ['png', 'jpg', 'jpeg', 'gif', 'webp', 'bmp', 'svg', 'ico', 'avif'];
const VIDEO_EXTS = ['mp4', 'webm', 'ogg', 'mov', 'm4v', 'mkv', 'avi'];
const AUDIO_EXTS = ['mp3', 'wav', 'm4a', 'aac', 'flac', 'wma'];
const TEXT_EXTS = [
  'txt', 'md', 'log', 'json', 'xml', 'yml', 'yaml', 'csv', 'properties', 'ini', 'conf',
  'java', 'py', 'js', 'ts', 'vue', 'html', 'css', 'sql', 'sh', 'bat', 'gitignore', 'env'
];

const KIND_LABELS = {
  image: '图片',
  pdf: 'PDF',
  docx: 'Word',
  word: 'Word(旧版)',
  excel: 'Excel',
  pptx: 'PPT',
  ppt: 'PPT(旧版)',
  text: '文本',
  video: '视频',
  audio: '音频',
  other: '文件'
};

const KIND_ICONS = {
  image: 'IMG',
  pdf: 'PDF',
  docx: 'DOC',
  word: 'DOC',
  excel: 'XLS',
  pptx: 'PPT',
  ppt: 'PPT',
  text: 'TXT',
  video: 'VID',
  audio: 'AUD',
  other: 'FILE'
};

export default {
  name: 'Documents',
  components: { EmptyState },
  data() {
    return {
      loading: true,
      uploading: false,
      uploadingIndex: 0,
      uploadingTotal: 0,
      uploadingName: '',
      uploadingPercent: 0,
      documents: [],
      previewDoc: null,
      previewKind: '',
      previewLoading: false,
      previewHtml: '',
      previewSheets: [],
      activeSheet: 0,
      textContent: '',
      pptxPreviewer: null
    };
  },
  created() {
    this.fetchList();
  },
  methods: {
    async fetchList() {
      this.loading = true;
      try {
        this.documents = (await getDocuments()) || [];
      } catch (e) {
        this.$root.$emit('toast', { message: '加载文档失败: ' + (e.response?.data?.message || e.message), type: 'error' });
      } finally {
        this.loading = false;
      }
    },

    triggerUpload() {
      this.$refs.fileInput.click();
    },

    handleFileChange(e) {
      const files = Array.from(e.target.files || []);
      e.target.value = '';
      this.uploadFiles(files);
    },

    handleDrop(e) {
      this.uploadFiles(Array.from(e.dataTransfer.files || []));
    },

    async uploadFiles(files) {
      if (!files.length) return;
      this.uploading = true;
      this.uploadingTotal = files.length;
      let ok = 0;
      for (let i = 0; i < files.length; i++) {
        const file = files[i];
        this.uploadingIndex = i + 1;
        this.uploadingName = file.name;
        this.uploadingPercent = Math.round(((i + 1) / files.length) * 100);
        try {
          const res = await uploadDocument(file);
          if (res.success) {
            ok++;
          } else {
            this.$root.$emit('toast', { message: file.name + ' 上传失败: ' + (res.message || '未知错误'), type: 'error' });
          }
        } catch (e) {
          this.$root.$emit('toast', { message: file.name + ' 上传失败: ' + (e.response?.data?.message || e.message), type: 'error' });
        }
      }
      this.uploading = false;
      this.$root.$emit('toast', { message: '上传完成：成功 ' + ok + ' / ' + files.length + ' 个文件', type: ok === files.length ? 'success' : 'warning' });
      this.fetchList();
    },

    async remove(doc) {
      if (!window.confirm('确定删除文档「' + doc.originalName + '」吗？删除后不可恢复。')) return;
      try {
        const res = await deleteDocument(doc.id);
        if (res.success) {
          this.$root.$emit('toast', { message: '文档已删除', type: 'success' });
          this.fetchList();
        } else {
          this.$root.$emit('toast', { message: res.message || '删除失败', type: 'error' });
        }
      } catch (e) {
        this.$root.$emit('toast', { message: '删除失败: ' + (e.response?.data?.message || e.message), type: 'error' });
      }
    },

    openPreview(doc) {
      this.previewDoc = doc;
      this.previewKind = this.fileKind(doc);
      this.previewHtml = '';
      this.previewSheets = [];
      this.activeSheet = 0;
      this.textContent = '';
      this.previewLoading = true;
      this.$nextTick(() => this.loadPreviewContent());
    },

    async loadPreviewContent() {
      const kind = this.previewKind;
      if (['image', 'pdf', 'video', 'audio'].includes(kind)) {
        this.previewLoading = false;
        return;
      }
      try {
        // Office 等文档先请求服务端转换（可识别真实格式、保留 Excel 样式并支持多 sheet）
        const res = await getDocumentPreview(this.previewDoc.id);
        if (res.kind === 'doc') {
          this.previewHtml = res.html || '';
          this.previewLoading = false;
          return;
        }
        if (res.kind === 'excel') {
          this.previewSheets = res.sheets || [];
          this.activeSheet = 0;
          this.previewLoading = false;
          return;
        }
        if (res.kind === 'ppt') {
          this.textContent = res.text || '';
          this.previewLoading = false;
          return;
        }
        if (res.kind === 'error' || !res.success) {
          throw new Error(res.message || '预览失败');
        }

        // 其余格式由前端渲染
        const buf = await this.fetchArrayBuffer();
        if (kind === 'docx') {
          if (this.$refs.docxContainer) {
            await renderAsync(buf, this.$refs.docxContainer);
          }
        } else if (kind === 'pptx') {
          if (this.$refs.pptxContainer) {
            this.destroyPptxPreviewer();
            this.pptxPreviewer = initPptxPreview(this.$refs.pptxContainer, { width: 960, height: 540 });
            await this.pptxPreviewer.preview(buf);
          }
        } else if (kind === 'text') {
          this.textContent = this.decodeText(buf);
        }
        this.previewLoading = false;
      } catch (e) {
        console.error('预览加载失败:', e);
        this.previewLoading = false;
        this.$root.$emit('toast', { message: '预览加载失败，请下载后查看', type: 'error' });
      }
    },

    async fetchArrayBuffer() {
      const response = await fetch(this.previewDoc.url);
      if (!response.ok) throw new Error('HTTP ' + response.status);
      return response.arrayBuffer();
    },

    decodeText(buf) {
      let text = new TextDecoder('utf-8').decode(new Uint8Array(buf));
      const replacementCount = (text.match(/\uFFFD/g) || []).length;
      if (replacementCount > Math.max(2, text.length * 0.01)) {
        try {
          text = new TextDecoder('gbk').decode(new Uint8Array(buf));
        } catch (e) {
          // 保持 UTF-8 解码结果
        }
      }
      return text;
    },

    closePreview() {
      this.destroyPptxPreviewer();
      this.previewDoc = null;
      this.previewKind = '';
      this.previewHtml = '';
      this.previewSheets = [];
      this.activeSheet = 0;
      this.textContent = '';
    },

    destroyPptxPreviewer() {
      if (this.pptxPreviewer && typeof this.pptxPreviewer.destroy === 'function') {
        try {
          this.pptxPreviewer.destroy();
        } catch (e) {
          // ignore
        }
      }
      this.pptxPreviewer = null;
      if (this.$refs.pptxContainer) {
        this.$refs.pptxContainer.innerHTML = '';
      }
    },

    getExtension(name) {
      const idx = (name || '').lastIndexOf('.');
      return idx >= 0 ? name.substring(idx + 1).toLowerCase() : '';
    },

    fileKind(doc) {
      const ext = (doc.ext || this.getExtension(doc.originalName || '')).toLowerCase();
      if (IMAGE_EXTS.includes(ext)) return 'image';
      if (ext === 'pdf') return 'pdf';
      if (ext === 'docx') return 'docx';
      if (ext === 'doc') return 'word';
      if (ext === 'xlsx' || ext === 'xls') return 'excel';
      if (ext === 'pptx') return 'pptx';
      if (ext === 'ppt') return 'ppt';
      if (VIDEO_EXTS.includes(ext)) return 'video';
      if (AUDIO_EXTS.includes(ext)) return 'audio';
      if (TEXT_EXTS.includes(ext)) return 'text';
      return 'other';
    },

    iconLabel(kind) {
      return KIND_ICONS[kind] || 'FILE';
    },

    typeLabel(doc) {
      const kind = this.fileKind(doc);
      if (kind === 'other' && doc.contentType) {
        return doc.contentType.split(';')[0].split('/').pop().toUpperCase() || '文件';
      }
      return KIND_LABELS[kind] || '文件';
    },

    formatSize(size) {
      if (size == null) return '—';
      if (size < 1024) return size + ' B';
      if (size < 1024 * 1024) return (size / 1024).toFixed(1) + ' KB';
      if (size < 1024 * 1024 * 1024) return (size / 1024 / 1024).toFixed(1) + ' MB';
      return (size / 1024 / 1024 / 1024).toFixed(2) + ' GB';
    },

    formatTime(t) {
      if (!t) return '—';
      return String(t).replace('T', ' ').substring(0, 19);
    }
  }
};
</script>

<style scoped>
.documents-page {
  max-width: 980px;
  margin: 0 auto;
  padding: 24px 16px 48px;
}

.documents-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 20px;
}

.page-title {
  margin: 0 0 4px;
  font-size: 22px;
  font-weight: 700;
}

.page-desc {
  margin: 0;
  color: #6b7280;
  font-size: 13px;
  line-height: 1.5;
}

.btn-primary {
  padding: 9px 18px;
  border: none;
  border-radius: 8px;
  background: #4f46e5;
  color: #fff;
  font-size: 14px;
  cursor: pointer;
  white-space: nowrap;
}

.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.file-input-hidden {
  display: none;
}

/* === Upload Zone === */
.upload-zone {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 34px 20px;
  border: 2px dashed #d1d5db;
  border-radius: 12px;
  background: #fff;
  color: #9ca3af;
  cursor: pointer;
  transition: border-color 0.2s, background 0.2s;
  margin-bottom: 20px;
}

.upload-zone:hover {
  border-color: #818cf8;
  background: #f5f7ff;
  color: #6366f1;
}

.upload-zone-text {
  margin-top: 10px;
  font-size: 15px;
  font-weight: 600;
  color: #374151;
}

.upload-zone:hover .upload-zone-text {
  color: #4f46e5;
}

.upload-zone-hint {
  margin-top: 4px;
  font-size: 12px;
}

/* === Upload Progress === */
.upload-progress {
  margin-bottom: 18px;
  padding: 14px 16px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
}

.upload-progress-text {
  display: block;
  font-size: 13px;
  color: #374151;
  margin-bottom: 8px;
}

.progress-bar {
  height: 6px;
  border-radius: 999px;
  background: #eef2ff;
  overflow: hidden;
}

.progress-bar-inner {
  height: 100%;
  border-radius: 999px;
  background: #4f46e5;
  transition: width 0.3s;
}

/* === List === */
.doc-loading {
  padding: 48px 0;
  text-align: center;
  color: #9ca3af;
}

.doc-list {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  overflow: hidden;
}

.doc-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 16px;
  border-bottom: 1px solid #f3f4f6;
}

.doc-item:last-child {
  border-bottom: none;
}

.doc-icon {
  width: 44px;
  height: 44px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 10px;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.02em;
  color: #fff;
}

.doc-icon--image { background: #10b981; }
.doc-icon--pdf { background: #ef4444; }
.doc-icon--docx, .doc-icon--word { background: #3b82f6; }
.doc-icon--excel { background: #16a34a; }
.doc-icon--pptx, .doc-icon--ppt { background: #f97316; }
.doc-icon--text { background: #6b7280; }
.doc-icon--video { background: #8b5cf6; }
.doc-icon--audio { background: #ec4899; }
.doc-icon--other { background: #9ca3af; }

.doc-main {
  flex: 1;
  min-width: 0;
}

.doc-name {
  font-size: 14px;
  font-weight: 600;
  color: #111827;
  cursor: pointer;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.doc-name:hover {
  color: #4f46e5;
}

.doc-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 4px;
  font-size: 12px;
  color: #9ca3af;
}

.doc-type-badge {
  display: inline-block;
  padding: 1px 8px;
  border-radius: 999px;
  background: #f3f4f6;
  color: #6b7280;
  font-size: 11px;
}

.doc-actions {
  flex-shrink: 0;
}

.btn-mini {
  padding: 4px 12px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  background: #fff;
  font-size: 13px;
  color: #374151;
  cursor: pointer;
  margin-left: 6px;
  display: inline-block;
  text-decoration: none;
}

.btn-mini:hover {
  background: #f9fafb;
}

.btn-mini--link {
  color: #4f46e5;
}

.btn-mini--danger {
  color: #dc2626;
  border-color: #fecaca;
}

/* === Preview Modal === */
.modal-mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-body {
  background: #fff;
  border-radius: 14px;
  overflow: hidden;
}

.preview-body {
  width: min(1100px, 94vw);
  height: min(88vh, 900px);
  display: flex;
  flex-direction: column;
}

.preview-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 18px;
  border-bottom: 1px solid #e5e7eb;
  background: #fafafa;
}

.preview-title {
  font-size: 14px;
  font-weight: 600;
  color: #111827;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 70%;
}

.preview-header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}

.preview-download {
  font-size: 13px;
  color: #4f46e5;
  text-decoration: none;
}

.preview-close {
  width: 28px;
  height: 28px;
  border: none;
  border-radius: 6px;
  background: transparent;
  color: #6b7280;
  font-size: 20px;
  line-height: 1;
  cursor: pointer;
}

.preview-close:hover {
  background: #f3f4f6;
  color: #111827;
}

.preview-content {
  flex: 1;
  overflow: auto;
  position: relative;
  background: #f9fafb;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding: 16px;
}

.preview-image {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
  border-radius: 6px;
}

.preview-iframe {
  width: 100%;
  height: 100%;
  border: none;
  border-radius: 6px;
  background: #fff;
}

.preview-docx {
  width: 100%;
  max-height: 100%;
  overflow: auto;
  background: #fff;
  border-radius: 6px;
  padding: 20px;
}

.preview-docx >>> .docx-wrapper {
  background: #fff;
}

.preview-html-wrap {
  width: 100%;
  height: 100%;
  background: #fff;
  border-radius: 6px;
}

.preview-excel {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-radius: 6px;
  overflow: hidden;
}

.sheet-tabs {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 12px;
  border-bottom: 1px solid #e5e7eb;
  background: #fafafa;
  flex-shrink: 0;
  overflow-x: auto;
}

.sheet-tab {
  padding: 4px 12px;
  border: 1px solid transparent;
  border-radius: 6px;
  background: transparent;
  font-size: 13px;
  color: #6b7280;
  cursor: pointer;
  white-space: nowrap;
}

.sheet-tab:hover {
  background: #f3f4f6;
}

.sheet-tab--active {
  background: #eef2ff;
  border: 1px solid #e5e7eb;
  color: #4f46e5;
  font-weight: 600;
}

.preview-excel .preview-iframe {
  flex: 1;
  border-radius: 0;
}

.preview-pptx {
  width: 100%;
  max-height: 100%;
  overflow: auto;
  display: flex;
  justify-content: center;
}

.preview-text {
  width: 100%;
  margin: 0;
  padding: 18px;
  background: #fff;
  border-radius: 6px;
  font-family: var(--font-mono);
  font-size: 13px;
  line-height: 1.6;
  color: #1f2937;
  white-space: pre-wrap;
  word-break: break-all;
}

.preview-video {
  max-width: 100%;
  max-height: 100%;
}

.preview-audio {
  width: min(560px, 100%);
  margin-top: 80px;
}

.preview-unsupported {
  align-self: center;
  text-align: center;
  color: #6b7280;
  padding: 48px 20px;
}

.preview-unsupported p {
  margin-bottom: 16px;
  font-size: 14px;
}

.preview-loading {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.85);
  font-size: 14px;
  color: #4f46e5;
  z-index: 10;
}

@media (max-width: 640px) {
  .documents-header {
    flex-direction: column;
    gap: 12px;
  }
  .doc-item {
    flex-wrap: wrap;
  }
  .doc-actions {
    width: 100%;
    display: flex;
    justify-content: flex-end;
  }
  .btn-mini {
    margin-left: 0;
    margin-right: 6px;
  }
}
</style>
