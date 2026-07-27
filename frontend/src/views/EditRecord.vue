<template>
  <div class="form-page">
    <!-- Breadcrumb -->
    <nav class="breadcrumb">
      <router-link to="/" class="breadcrumb-link">首页</router-link>
      <span class="breadcrumb-sep">/</span>
      <router-link :to="'/detail/' + $route.params.id" class="breadcrumb-link">记录详情</router-link>
      <span class="breadcrumb-sep">/</span>
      <span class="breadcrumb-current">编辑</span>
    </nav>

    <div class="form-card">
      <h1 class="form-title">编辑报错记录</h1>
      <p class="form-desc">更新处理步骤或修正报错描述信息</p>

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
            required
            maxlength="500"
          />
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
            placeholder="详细描述报错信息..."
          ></textarea>
        </div>

        <!-- 截图 -->
        <div class="form-group">
          <label class="form-label">报错截图</label>
          <div class="screenshot-area">
            <template v-if="form.errorScreenshot">
              <div class="preview-card">
                <img :src="form.errorScreenshot" class="preview-image" alt="当前截图" />
                <div class="preview-actions">
                  <span class="preview-label">当前截图</span>
                  <div class="preview-actions-right">
                    <label class="btn-replace">
                      <input
                        type="file"
                        accept="image/*"
                        class="file-input-hidden"
                        @change="handleScreenshotUpload"
                      />
                      <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                        <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
                        <polyline points="17 8 12 3 7 8"/>
                        <line x1="12" y1="3" x2="12" y2="15"/>
                      </svg>
                      更换
                    </label>
                    <button type="button" class="btn-remove" @click="form.errorScreenshot = ''">
                      <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                        <line x1="18" y1="6" x2="6" y2="18"/>
                        <line x1="6" y1="6" x2="18" y2="18"/>
                      </svg>
                      移除
                    </button>
                  </div>
                </div>
              </div>
            </template>
            <template v-else>
              <label class="upload-zone">
                <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                  <rect x="3" y="3" width="18" height="18" rx="2" ry="2"/>
                  <circle cx="8.5" cy="8.5" r="1.5"/>
                  <polyline points="21 15 16 10 5 21"/>
                </svg>
                <span class="upload-zone-text">点击上传截图</span>
                <span class="upload-zone-hint">支持 PNG、JPG、GIF</span>
                <input
                  type="file"
                  accept="image/*"
                  class="file-input-hidden"
                  @change="handleScreenshotUpload"
                />
              </label>
            </template>
          </div>
        </div>

        <!-- 处理步骤 -->
        <div class="form-group">
          <label class="form-label">
            处理步骤
            <span class="form-hint">（填写后状态将自动更新为"已记录"）</span>
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
          <label class="form-label">
            关键字
            <span class="form-hint">（逗号分隔）</span>
          </label>
          <input
            v-model="form.keywords"
            class="form-input"
            placeholder="逗号分隔多个关键字"
            maxlength="1000"
          />
        </div>

        <!-- 更新人 -->
        <div class="form-group">
          <label class="form-label">更新人</label>
          <input
            v-model="form.updater"
            class="form-input"
            placeholder="请输入您的姓名"
            maxlength="100"
          />
        </div>

        <!-- 操作按钮 -->
        <div class="form-actions">
          <button type="button" class="btn btn-secondary" @click="$router.back()">
            取消
          </button>
          <button
            type="submit"
            class="btn btn-primary"
            :disabled="submitting"
          >
            <template v-if="submitting">
              <span class="btn-spinner"></span>
              保存中...
            </template>
            <template v-else>
              保存更新
            </template>
          </button>
        </div>
      </form>
    </div>
  </div>
</template>

<script>
import { getRecordById, updateRecord, uploadScreenshot, getCategories } from '../api/index';

export default {
  name: 'EditRecord',
  data() {
    return {
      form: {
        errorTitle: '',
        errorContent: '',
        errorScreenshot: '',
        solutionSteps: '',
        category: '',
        keywords: '',
        updater: ''
      },
      allCategories: [],
      filteredCategories: [],
      showCategoryDropdown: false,
      submitting: false
    };
  },
  async created() {
    try {
      const record = await getRecordById(this.$route.params.id);
      this.form.errorTitle = record.errorTitle || '';
      this.form.errorContent = record.errorContent || '';
      this.form.errorScreenshot = record.errorScreenshot || '';
      this.form.solutionSteps = record.solutionSteps || '';
      this.form.category = record.category || '';
      this.form.keywords = record.keywords || '';
      this.form.updater = '';
      await this.fetchCategories();
    } catch (e) {
      console.error('加载记录失败:', e);
      this.$router.push('/');
    }
  },
  methods: {
    async fetchCategories() {
      if (this.allCategories.length > 0) return;
      try {
        this.allCategories = await getCategories();
        this.filteredCategories = this.allCategories.slice();
      } catch (e) {
        console.error('加载分类失败:', e);
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
    async handleScreenshotUpload(e) {
      const file = e.target.files[0];
      if (!file) return;
      try {
        const res = await uploadScreenshot(file);
        if (res.success) {
          this.form.errorScreenshot = res.url;
          this.$root.$emit('toast', { message: '截图上传成功', type: 'success' });
        } else {
          this.$root.$emit('toast', { message: '截图上传失败: ' + res.message, type: 'error' });
        }
      } catch (err) {
        this.$root.$emit('toast', {
          message: '截图上传失败: ' + (err.response?.data?.message || err.message),
          type: 'error'
        });
      }
    },
    async handleSubmit() {
      if (!this.form.errorTitle || !this.form.category) {
        this.$root.$emit('toast', { message: '请填写报错标题和所属分类', type: 'warning' });
        return;
      }
      this.submitting = true;
      try {
        await updateRecord(this.$route.params.id, this.form);
        this.$root.$emit('toast', { message: '更新成功', type: 'success' });
        this.$router.push('/detail/' + this.$route.params.id);
      } catch (e) {
        this.$root.$emit('toast', {
          message: '更新失败: ' + (e.response?.data?.message || e.message),
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

.preview-actions-right {
  display: flex;
  gap: var(--space-2);
}

.btn-replace {
  display: inline-flex;
  align-items: center;
  gap: var(--space-1);
  padding: var(--space-1) var(--space-3);
  font-size: var(--text-xs);
  font-family: var(--font-sans);
  font-weight: var(--font-medium);
  color: var(--color-primary-600);
  background: var(--color-primary-50);
  border: 1px solid var(--color-primary-200);
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.btn-replace:hover {
  background: var(--color-primary-100);
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
