<template>
  <div class="form-page">
    <!-- Breadcrumb -->
    <nav class="breadcrumb">
      <router-link to="/" class="breadcrumb-link">首页</router-link>
      <span class="breadcrumb-sep">/</span>
      <span class="breadcrumb-current">新增记录</span>
    </nav>

    <div class="form-card">
      <h1 class="form-title">新增报错记录</h1>
      <p class="form-desc">记录新的报错信息，便于团队快速检索和解决</p>

      <form @submit.prevent="handleSubmit">
        <!-- 报错标题 -->
        <div class="form-group">
          <label class="form-label">
            报错标题
            <span class="form-required">*</span>
          </label>
          <input
            v-model="form.errorTitle"
            class="form-input"
            placeholder="例如：ORA-01555 快照过旧报错"
            required
            maxlength="500"
          />
          <span class="form-counter">{{ form.errorTitle.length }}/500</span>
        </div>

        <!-- 分类 -->
        <div class="form-group">
          <label class="form-label">
            所属分类
            <span class="form-required">*</span>
          </label>
          <div class="combo-box-wrapper">
            <input
              v-model="form.category"
              class="form-input combo-input"
              placeholder="选择或输入新的分类名称..."
              required
              maxlength="200"
              @focus="showCategoryDropdown = true"
              @blur="handleCategoryBlur"
              @input="filterCategoryOptions"
            />
            <ul
              v-if="showCategoryDropdown && filteredCategories.length > 0"
              class="combo-dropdown"
            >
              <li
                v-for="(cat, idx) in filteredCategories"
                :key="idx"
                class="combo-option"
                @mousedown.prevent="selectCategory(cat)"
              >
                {{ cat }}
              </li>
            </ul>
          </div>
        </div>

        <!-- 报错内容 -->
        <div class="form-group">
          <label class="form-label">报错内容</label>
          <textarea
            v-model="form.errorContent"
            class="form-textarea"
            rows="5"
            placeholder="详细描述报错信息，包括报错日志、报错场景、环境信息等..."
          ></textarea>
        </div>

        <!-- 截图上传 -->
        <div class="form-group">
          <label class="form-label">报错截图</label>
          <div class="screenshot-area">
            <template v-if="!screenshotPreview && !form.errorScreenshot">
              <label class="upload-zone" @dragover.prevent @drop.prevent="handleDrop">
                <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                  <rect x="3" y="3" width="18" height="18" rx="2" ry="2"/>
                  <circle cx="8.5" cy="8.5" r="1.5"/>
                  <polyline points="21 15 16 10 5 21"/>
                </svg>
                <span class="upload-zone-text">点击或拖拽上传截图</span>
                <span class="upload-zone-hint">支持 PNG、JPG、GIF</span>
                <input
                  ref="fileInput"
                  type="file"
                  accept="image/*"
                  class="file-input-hidden"
                  @change="handleFileChange"
                />
              </label>
            </template>
            <template v-else>
              <div class="preview-card">
                <img
                  :src="screenshotPreview || form.errorScreenshot"
                  class="preview-image"
                  alt="截图预览"
                />
                <div class="preview-actions">
                  <span class="preview-label">截图已{{ uploading ? '在上传中...' : '上传' }}</span>
                  <button type="button" class="btn-remove" @click="removeScreenshot">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                      <line x1="18" y1="6" x2="6" y2="18"/>
                      <line x1="6" y1="6" x2="18" y2="18"/>
                    </svg>
                    移除
                  </button>
                </div>
                <input
                  ref="fileInput"
                  type="file"
                  accept="image/*"
                  class="file-input-hidden"
                  @change="handleFileChange"
                />
              </div>
            </template>
          </div>
        </div>

        <!-- 处理步骤 -->
        <div class="form-group">
          <label class="form-label">
            处理步骤
            <span class="form-hint">（暂不确定可留空，系统将标记为"待更新"）</span>
          </label>
          <textarea
            v-model="form.solutionSteps"
            class="form-textarea"
            rows="6"
            placeholder="按步骤顺序描述处理方案..."
          ></textarea>
        </div>

        <!-- 关键字 -->
        <div class="form-group">
          <div class="form-label form-label--row">
            <span>
              关键字
              <span class="form-hint">（逗号分隔，用于搜索匹配）</span>
            </span>
            <button
              type="button"
              class="btn-extract"
              :disabled="!form.errorContent || extracting"
              title="从报错内容中自动识别错误码、异常类名等特征"
              @click="handleExtractKeywords"
            >
              {{ extracting ? '提取中...' : '从报错内容提取' }}
            </button>
          </div>
          <input
            v-model="form.keywords"
            class="form-input"
            placeholder="例如：ORA-01555, 快照过旧, snapshot, 重跑"
            maxlength="1000"
          />
        </div>

        <!-- 登记人 -->
        <div class="form-group">
          <label class="form-label">登记人</label>
          <input
            v-model="form.registrar"
            class="form-input"
            placeholder="请输入您的姓名"
            maxlength="100"
          />
        </div>

        <!-- 操作按钮 -->
        <div class="form-actions">
          <button type="button" class="btn btn-secondary" @click="$router.push('/')">
            取消
          </button>
          <button
            type="submit"
            class="btn btn-primary"
            :disabled="submitting || uploading"
          >
            <template v-if="submitting">
              <span class="btn-spinner"></span>
              提交中...
            </template>
            <template v-else>
              提交记录
            </template>
          </button>
        </div>
      </form>
    </div>
  </div>
</template>

<script>
import { createRecord, uploadScreenshot, getCategories, extractKeywords } from '../api/index';

export default {
  name: 'AddRecord',
  data() {
    return {
      form: {
        errorTitle: '',
        errorContent: '',
        errorScreenshot: '',
        solutionSteps: '',
        category: '',
        keywords: '',
        registrar: ''
      },
      submitting: false,
      uploading: false,
      extracting: false,
      screenshotPreview: '',
      allCategories: [],
      filteredCategories: [],
      showCategoryDropdown: false
    };
  },
  created() {
    this.fetchCategories();
    this.applyPrefill();
  },
  methods: {
    applyPrefill() {
      // 首页智能匹配未命中时，「登记为待处理报错」带过来的预填内容
      const raw = sessionStorage.getItem('kb_prefill');
      if (!raw) return;
      sessionStorage.removeItem('kb_prefill');
      try {
        const prefill = JSON.parse(raw);
        if (prefill.errorTitle) this.form.errorTitle = prefill.errorTitle.slice(0, 500);
        if (prefill.errorContent) this.form.errorContent = prefill.errorContent;
        if (prefill.keywords) this.form.keywords = prefill.keywords.slice(0, 1000);
        this.$root.$emit('toast', {
          message: '已带入报错日志和识别出的关键字，选择分类后即可登记',
          type: 'info'
        });
      } catch (e) {
        console.error('读取预填内容失败:', e);
      }
    },
    async handleExtractKeywords() {
      if (!this.form.errorContent || this.extracting) return;
      this.extracting = true;
      try {
        const res = await extractKeywords(this.form.errorContent);
        const suggested = (res.keywords || []).slice(0, 10);
        if (!suggested.length) {
          this.$root.$emit('toast', { message: '未能从报错内容中识别出特征关键字，可手动填写', type: 'warning' });
          return;
        }
        const existing = this.form.keywords
          ? this.form.keywords.split(/[,，;；]/).map(s => s.trim()).filter(Boolean)
          : [];
        const merged = existing.slice();
        suggested.forEach(k => {
          if (!merged.some(m => m.toLowerCase() === k.toLowerCase())) merged.push(k);
        });
        this.form.keywords = merged.join(',').slice(0, 1000);
        this.$root.$emit('toast', { message: '已提取 ' + suggested.length + ' 个特征关键字', type: 'success' });
      } catch (e) {
        this.$root.$emit('toast', {
          message: '提取失败: ' + (e.response?.data?.message || e.message),
          type: 'error'
        });
      } finally {
        this.extracting = false;
      }
    },
    async fetchCategories() {
      try {
        this.allCategories = await getCategories();
        this.filteredCategories = this.allCategories.slice();
      } catch (e) {
        console.error('获取分类失败:', e);
      }
    },
    filterCategoryOptions() {
      const val = this.form.category.toLowerCase();
      this.filteredCategories = this.allCategories.filter(
        cat => cat.toLowerCase().indexOf(val) !== -1
      );
      this.showCategoryDropdown = this.filteredCategories.length > 0;
    },
    selectCategory(cat) {
      this.form.category = cat;
      this.showCategoryDropdown = false;
    },
    handleCategoryBlur() {
      setTimeout(() => { this.showCategoryDropdown = false; }, 200);
    },
    handleFileChange(e) {
      const file = e.target.files[0];
      if (!file) return;
      const reader = new FileReader();
      reader.onload = (ev) => { this.screenshotPreview = ev.target.result; };
      reader.readAsDataURL(file);
      this.uploadFile(file);
    },
    handleDrop(e) {
      const file = e.dataTransfer.files[0];
      if (!file || !file.type.startsWith('image/')) return;
      const reader = new FileReader();
      reader.onload = (ev) => { this.screenshotPreview = ev.target.result; };
      reader.readAsDataURL(file);
      this.uploadFile(file);
    },
    async uploadFile(file) {
      this.uploading = true;
      try {
        const res = await uploadScreenshot(file);
        if (res.success) {
          this.form.errorScreenshot = res.url;
        } else {
          this.$root.$emit('toast', { message: '截图上传失败: ' + res.message, type: 'error' });
        }
      } catch (e) {
        this.$root.$emit('toast', {
          message: '截图上传失败: ' + (e.response?.data?.message || e.message),
          type: 'error'
        });
        this.screenshotPreview = '';
      } finally {
        this.uploading = false;
      }
    },
    removeScreenshot() {
      this.form.errorScreenshot = '';
      this.screenshotPreview = '';
      if (this.$refs.fileInput) {
        this.$refs.fileInput.value = '';
      }
    },
    async handleSubmit() {
      if (!this.form.errorTitle || !this.form.category) {
        this.$root.$emit('toast', { message: '请填写报错标题和所属分类', type: 'warning' });
        return;
      }
      this.submitting = true;
      try {
        await createRecord(this.form);
        this.$root.$emit('toast', { message: '记录创建成功', type: 'success' });
        this.$router.push('/');
      } catch (e) {
        this.$root.$emit('toast', {
          message: '提交失败: ' + (e.response?.data?.message || e.message),
          type: 'error'
        });
      } finally {
        this.submitting = false;
      }
    }
  }
};
</script>

<style scoped>
.form-page {
  max-width: 720px;
  margin: 0 auto;
}

/* === Breadcrumb === */
.breadcrumb {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  margin-bottom: var(--space-5);
  font-size: var(--text-sm);
}

.breadcrumb-link {
  color: var(--text-tertiary);
  text-decoration: none;
  transition: color var(--transition-fast);
}

.breadcrumb-link:hover {
  color: var(--color-link);
}

.breadcrumb-sep {
  color: var(--color-neutral-300);
}

.breadcrumb-current {
  color: var(--text-primary);
  font-weight: var(--font-medium);
}

/* === Form Card === */
.form-card {
  background: var(--surface-card);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-xl);
  padding: var(--space-8);
  box-shadow: var(--shadow-xs);
}

.form-title {
  font-size: var(--text-3xl);
  font-weight: var(--font-bold);
  color: var(--text-primary);
  letter-spacing: -0.01em;
  margin-bottom: var(--space-2);
}

.form-desc {
  font-size: var(--text-sm);
  color: var(--text-tertiary);
  margin-bottom: var(--space-8);
  padding-bottom: var(--space-6);
  border-bottom: 1px solid var(--border-light);
}

/* === Form Group === */
.form-group {
  margin-bottom: var(--space-6);
  position: relative;
}

.form-label {
  display: block;
  font-size: var(--text-sm);
  font-weight: var(--font-medium);
  color: var(--text-primary);
  margin-bottom: var(--space-2);
}

.form-required {
  color: var(--form-required);
  margin-left: 2px;
}

.form-hint {
  font-weight: var(--font-normal);
  color: var(--form-hint);
  font-size: var(--text-xs);
}

.form-counter {
  position: absolute;
  right: 0;
  top: 0;
  font-size: var(--text-2xs);
  color: var(--text-tertiary);
}

.form-label--row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
}

.btn-extract {
  flex-shrink: 0;
  padding: 2px 10px;
  font-size: var(--text-xs);
  font-family: var(--font-sans);
  font-weight: var(--font-medium);
  color: var(--color-primary-600);
  background: var(--color-primary-50);
  border: 1px solid var(--color-primary-200);
  border-radius: var(--radius-full);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.btn-extract:hover:not(:disabled) {
  background: var(--color-primary-100);
  color: var(--color-primary-700);
}

.btn-extract:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* === Input & Textarea === */
.form-input {
  width: 100%;
  padding: var(--space-3) var(--space-4);
  font-size: var(--text-sm);
  font-family: var(--font-sans);
  color: var(--input-text);
  background: var(--input-bg);
  border: 1px solid var(--input-border);
  border-radius: var(--input-radius);
  outline: none;
  transition: all var(--transition-fast);
  box-sizing: border-box;
}

.form-input:focus {
  border-color: var(--input-focus-border);
  box-shadow: 0 0 0 3px var(--input-focus-ring);
}

.form-input::placeholder {
  color: var(--input-placeholder);
}

.form-textarea {
  width: 100%;
  padding: var(--space-3) var(--space-4);
  font-size: var(--text-sm);
  font-family: var(--font-mono);
  color: var(--input-text);
  background: var(--input-bg);
  border: 1px solid var(--input-border);
  border-radius: var(--input-radius);
  outline: none;
  resize: vertical;
  line-height: var(--leading-relaxed);
  transition: all var(--transition-fast);
  box-sizing: border-box;
}

.form-textarea:focus {
  border-color: var(--input-focus-border);
  box-shadow: 0 0 0 3px var(--input-focus-ring);
}

.form-textarea::placeholder {
  font-family: var(--font-sans);
  color: var(--input-placeholder);
}

/* === Combo Box === */
.combo-box-wrapper {
  position: relative;
}

.combo-input {
  width: 100%;
}

.combo-dropdown {
  position: absolute;
  top: calc(100% + 4px);
  left: 0;
  right: 0;
  background: var(--surface-card);
  border: 1px solid var(--border-default);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-lg);
  z-index: 100;
  max-height: 200px;
  overflow-y: auto;
  list-style: none;
  margin: 0;
  padding: var(--space-1) 0;
}

.combo-option {
  padding: var(--space-2) var(--space-4);
  cursor: pointer;
  font-size: var(--text-sm);
  color: var(--text-primary);
  transition: background var(--transition-fast);
}

.combo-option:hover {
  background: var(--color-primary-50);
  color: var(--color-primary-700);
}

/* === Screenshot Upload === */
.screenshot-area {
  display: flex;
}

.upload-zone {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--space-2);
  width: 100%;
  padding: var(--space-8) var(--space-4);
  border: 2px dashed var(--border-default);
  border-radius: var(--radius-lg);
  cursor: pointer;
  transition: all var(--transition-fast);
  color: var(--text-tertiary);
}

.upload-zone:hover {
  border-color: var(--color-primary-300);
  background: var(--color-primary-50);
  color: var(--color-primary-600);
}

.upload-zone-text {
  font-size: var(--text-sm);
  font-weight: var(--font-medium);
}

.upload-zone-hint {
  font-size: var(--text-xs);
  opacity: 0.7;
}

.file-input-hidden {
  display: none;
}

.preview-card {
  width: 100%;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  overflow: hidden;
}

.preview-image {
  width: 100%;
  max-height: 320px;
  object-fit: contain;
  background: var(--color-neutral-100);
  display: block;
}

.preview-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-3) var(--space-4);
  background: var(--color-neutral-50);
  border-top: 1px solid var(--border-light);
}

.preview-label {
  font-size: var(--text-xs);
  color: var(--text-secondary);
}

.btn-remove {
  display: inline-flex;
  align-items: center;
  gap: var(--space-1);
  padding: var(--space-1) var(--space-3);
  font-size: var(--text-xs);
  font-family: var(--font-sans);
  font-weight: var(--font-medium);
  color: var(--color-danger);
  background: var(--color-danger-bg);
  border: 1px solid var(--color-danger-border);
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.btn-remove:hover {
  background: var(--color-danger);
  color: #fff;
}

/* === Form Actions === */
.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: var(--space-3);
  margin-top: var(--space-8);
  padding-top: var(--space-6);
  border-top: 1px solid var(--border-light);
}

/* === Buttons === */
.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-2);
  padding: var(--space-3) var(--space-6);
  font-size: var(--text-sm);
  font-weight: var(--font-medium);
  font-family: var(--font-sans);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all var(--transition-fast);
  border: none;
  text-decoration: none;
}

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-primary {
  background: var(--btn-primary-bg);
  color: var(--btn-primary-text);
}

.btn-primary:hover:not(:disabled) {
  background: var(--btn-primary-hover);
}

.btn-primary:active:not(:disabled) {
  background: var(--btn-primary-active);
  transform: scale(0.98);
}

.btn-secondary {
  background: var(--btn-secondary-bg);
  color: var(--btn-secondary-text);
  border: 1px solid var(--btn-secondary-border);
}

.btn-secondary:hover:not(:disabled) {
  background: var(--btn-secondary-hover);
}

.btn-spinner {
  width: 14px;
  height: 14px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* === Responsive === */
@media (max-width: 640px) {
  .form-card {
    padding: var(--space-5);
    border-radius: var(--radius-lg);
  }

  .form-title {
    font-size: var(--text-2xl);
  }

  .form-actions {
    flex-direction: column-reverse;
  }

  .btn {
    width: 100%;
  }
}
</style>
