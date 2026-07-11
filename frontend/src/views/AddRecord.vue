<template>
  <div class="form-page">
    <div class="form-card">
      <h2 class="form-title">新增报错记录</h2>
      <form @submit.prevent="handleSubmit">
        <!-- 报错标题 -->
        <div class="form-group">
          <label class="form-label">报错标题 <span class="required">*</span></label>
          <input v-model="form.errorTitle" class="form-input" placeholder="如：ORA-01555 快照过旧报错" required maxlength="500" />
        </div>

        <!-- 分类（可编辑下拉框） -->
        <div class="form-group">
          <label class="form-label">所属分类 <span class="required">*</span></label>
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
            <ul v-if="showCategoryDropdown && filteredCategories.length > 0" class="combo-dropdown">
              <li
                v-for="(cat, idx) in filteredCategories"
                :key="idx"
                class="combo-option"
                @mousedown.prevent="selectCategory(cat)"
              >{{ cat }}</li>
            </ul>
          </div>
        </div>

        <!-- 报错内容 -->
        <div class="form-group">
          <label class="form-label">报错内容</label>
          <textarea v-model="form.errorContent" class="form-textarea" rows="5" placeholder="详细描述报错信息，包括报错日志、报错场景等..."></textarea>
        </div>

        <!-- 截图上传 -->
        <div class="form-group">
          <label class="form-label">报错截图</label>
          <div class="screenshot-upload">
            <div v-if="!screenshotPreview && !form.errorScreenshot" class="upload-area" @click="$refs.fileInput.click()">
              <span class="upload-icon">+</span>
              <span class="upload-text">点击上传截图</span>
            </div>
            <div v-else class="preview-area">
              <img :src="screenshotPreview || form.errorScreenshot" class="screenshot-preview" alt="截图预览" />
              <button type="button" class="remove-screenshot" @click="removeScreenshot">×</button>
            </div>
            <input ref="fileInput" type="file" accept="image/*" class="file-input-hidden" @change="handleFileChange" />
            <p v-if="uploading" class="upload-tip">上传中...</p>
          </div>
        </div>

        <!-- 处理步骤 -->
        <div class="form-group">
          <label class="form-label">
            处理步骤
            <span class="label-tip">（如果暂时不确定可以留空，状态会标记为"待更新"）</span>
          </label>
          <textarea v-model="form.solutionSteps" class="form-textarea" rows="6" placeholder="请输入处理步骤，建议按步骤顺序描述..."></textarea>
        </div>

        <!-- 关键字 -->
        <div class="form-group">
          <label class="form-label">关键字 <span class="label-tip">（逗号分隔，用于搜索匹配）</span></label>
          <input v-model="form.keywords" class="form-input" placeholder="如：ORA-01555,快照过旧,snapshot,重跑" maxlength="1000" />
        </div>

        <!-- 登记人 -->
        <div class="form-group">
          <label class="form-label">登记人</label>
          <input v-model="form.registrar" class="form-input" placeholder="请输入您的姓名" maxlength="100" />
        </div>

        <!-- 按钮 -->
        <div class="form-buttons">
          <button type="button" class="btn-cancel" @click="$router.push('/')">取消</button>
          <button type="submit" class="btn-submit" :disabled="submitting || uploading">
            {{ submitting ? '提交中...' : '提交' }}
          </button>
        </div>
      </form>
    </div>
  </div>
</template>

<script>
import { createRecord, uploadScreenshot, getCategories } from '../api/index';

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
      screenshotPreview: '',
      // 分类下拉
      allCategories: [],
      filteredCategories: [],
      showCategoryDropdown: false
    };
  },
  created() {
    this.fetchCategories();
  },
  methods: {
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
      // 本地预览
      const reader = new FileReader();
      reader.onload = (ev) => { this.screenshotPreview = ev.target.result; };
      reader.readAsDataURL(file);
      // 上传到服务器
      this.uploadFile(file);
    },
    async uploadFile(file) {
      this.uploading = true;
      try {
        const res = await uploadScreenshot(file);
        if (res.success) {
          this.form.errorScreenshot = res.url;
        } else {
          alert('截图上传失败: ' + res.message);
        }
      } catch (e) {
        alert('截图上传失败: ' + (e.response?.data?.message || e.message));
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
        alert('请填写报错标题和所属分类');
        return;
      }
      this.submitting = true;
      try {
        await createRecord(this.form);
        this.$router.push('/');
      } catch (e) {
        alert('提交失败: ' + (e.response?.data?.message || e.message));
      } finally {
        this.submitting = false;
      }
    }
  }
};
</script>

<style scoped>
.form-page {
  max-width: 700px;
  margin: 0 auto;
}

.form-card {
  background: #fff;
  border-radius: 8px;
  padding: 28px 32px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
}

.form-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid #ebeef5;
}

.form-group {
  margin-bottom: 20px;
}

.form-label {
  display: block;
  font-size: 14px;
  font-weight: 500;
  color: #606266;
  margin-bottom: 6px;
}

.required {
  color: #f56c6c;
}

.label-tip {
  font-weight: 400;
  color: #909399;
  font-size: 12px;
}

.form-input {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  font-size: 14px;
  outline: none;
  transition: border-color 0.2s;
  box-sizing: border-box;
}

.form-input:focus {
  border-color: #1a73e8;
}

.form-textarea {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  font-size: 14px;
  outline: none;
  resize: vertical;
  font-family: inherit;
  line-height: 1.6;
  transition: border-color 0.2s;
  box-sizing: border-box;
}

.form-textarea:focus {
  border-color: #1a73e8;
}

/* 分类下拉框 */
.combo-box-wrapper {
  position: relative;
}

.combo-input {
  width: 100%;
}

.combo-dropdown {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  background: #fff;
  border: 1px solid #dcdfe6;
  border-radius: 0 0 6px 6px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  z-index: 100;
  max-height: 200px;
  overflow-y: auto;
  list-style: none;
  margin: 0;
  padding: 4px 0;
}

.combo-option {
  padding: 8px 12px;
  cursor: pointer;
  font-size: 14px;
  color: #606266;
}

.combo-option:hover {
  background: #f0f2f5;
}

/* 截图上传 */
.screenshot-upload {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
}

.upload-area {
  width: 140px;
  height: 100px;
  border: 2px dashed #dcdfe6;
  border-radius: 6px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: border-color 0.2s;
}

.upload-area:hover {
  border-color: #1a73e8;
}

.upload-icon {
  font-size: 28px;
  color: #c0c4cc;
  line-height: 1;
  margin-bottom: 4px;
}

.upload-text {
  font-size: 12px;
  color: #909399;
}

.preview-area {
  position: relative;
  display: inline-block;
}

.screenshot-preview {
  max-width: 300px;
  max-height: 200px;
  border-radius: 6px;
  border: 1px solid #ebeef5;
  object-fit: contain;
}

.remove-screenshot {
  position: absolute;
  top: -8px;
  right: -8px;
  width: 22px;
  height: 22px;
  background: #f56c6c;
  color: #fff;
  border: none;
  border-radius: 50%;
  cursor: pointer;
  font-size: 14px;
  line-height: 22px;
  text-align: center;
  padding: 0;
}

.file-input-hidden {
  display: none;
}

.upload-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.form-buttons {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 28px;
  padding-top: 20px;
  border-top: 1px solid #ebeef5;
}

.btn-cancel {
  padding: 9px 24px;
  background: #fff;
  color: #606266;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
}

.btn-submit {
  padding: 9px 24px;
  background: #1a73e8;
  color: #fff;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
}

.btn-submit:hover {
  background: #1557b0;
}

.btn-submit:disabled {
  background: #a0c4f1;
  cursor: not-allowed;
}
</style>
