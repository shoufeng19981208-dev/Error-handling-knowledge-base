<template>
  <div class="form-page">
    <div class="form-card">
      <h2 class="form-title">编辑报错记录</h2>
      <div class="edit-hint">
        此页面用于更新/补充处理步骤，以及修改报错描述信息。
      </div>
      <form @submit.prevent="handleSubmit">
        <!-- 报错标题 -->
        <div class="form-group">
          <label class="form-label">报错标题 <span class="required">*</span></label>
          <input v-model="form.errorTitle" class="form-input" required maxlength="500" />
        </div>

        <!-- 分类 -->
        <div class="form-group">
          <label class="form-label">所属分类 <span class="required">*</span></label>
          <input v-model="form.category" class="form-input" required maxlength="200" list="edit-category-list" @focus="loadCategories" />
          <datalist id="edit-category-list">
            <option v-for="cat in categories" :key="cat" :value="cat" />
          </datalist>
        </div>

        <!-- 报错内容 -->
        <div class="form-group">
          <label class="form-label">报错内容</label>
          <textarea v-model="form.errorContent" class="form-textarea" rows="5"></textarea>
        </div>

        <!-- 截图 -->
        <div class="form-group">
          <label class="form-label">报错截图</label>
          <div class="screenshot-upload">
            <div v-if="form.errorScreenshot" class="screenshot-preview">
              <img :src="form.errorScreenshot" class="screenshot-preview-img" />
              <button type="button" class="btn-remove-screenshot" @click="form.errorScreenshot = ''">移除</button>
            </div>
            <label class="upload-area">
              <input type="file" accept="image/*" @change="handleScreenshotUpload" class="upload-input" />
              <span class="upload-text">{{ form.errorScreenshot ? '更换截图' : '点击上传截图' }}</span>
            </label>
          </div>
        </div>

        <!-- 处理步骤 -->
        <div class="form-group">
          <label class="form-label">
            处理步骤
            <span class="label-tip">（填写后状态将自动更新为"已记录"）</span>
          </label>
          <textarea v-model="form.solutionSteps" class="form-textarea" rows="6" placeholder="请输入处理步骤..."></textarea>
        </div>

        <!-- 关键字 -->
        <div class="form-group">
          <label class="form-label">关键字</label>
          <input v-model="form.keywords" class="form-input" placeholder="逗号分隔" maxlength="1000" />
        </div>

        <!-- 更新人 -->
        <div class="form-group">
          <label class="form-label">更新人</label>
          <input v-model="form.updater" class="form-input" placeholder="请输入您的姓名" maxlength="100" />
        </div>

        <!-- 按钮 -->
        <div class="form-buttons">
          <button type="button" class="btn-cancel" @click="$router.back()">取消</button>
          <button type="submit" class="btn-submit" :disabled="submitting">
            {{ submitting ? '保存中...' : '保存更新' }}
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
      categories: [],
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
      this.loadCategories();
    } catch (e) {
      console.error('加载记录失败:', e);
      this.$router.push('/');
    }
  },
  methods: {
    async loadCategories() {
      if (this.categories.length > 0) return;
      try {
        this.categories = await getCategories();
      } catch (e) {
        console.error('加载分类失败:', e);
      }
    },
    async handleScreenshotUpload(e) {
      const file = e.target.files[0];
      if (!file) return;
      try {
        const res = await uploadScreenshot(file);
        if (res.success) {
          this.form.errorScreenshot = res.url;
        } else {
          alert('截图上传失败: ' + res.message);
        }
      } catch (err) {
        alert('截图上传失败: ' + (err.response?.data?.message || err.message));
      }
    },
    async handleSubmit() {
      if (!this.form.errorTitle || !this.form.category) {
        alert('请填写报错标题和所属分类');
        return;
      }
      this.submitting = true;
      try {
        await updateRecord(this.$route.params.id, this.form);
        this.$router.push('/detail/' + this.$route.params.id);
      } catch (e) {
        alert('更新失败: ' + (e.response?.data?.message || e.message));
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
  margin-bottom: 8px;
}

.edit-hint {
  font-size: 13px;
  color: #909399;
  margin-bottom: 20px;
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

.screenshot-upload {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.screenshot-preview {
  position: relative;
  display: inline-block;
}

.screenshot-preview-img {
  max-width: 150px;
  max-height: 100px;
  border-radius: 4px;
  border: 1px solid #dcdfe6;
  object-fit: cover;
}

.btn-remove-screenshot {
  position: absolute;
  top: -8px;
  right: -8px;
  background: #f56c6c;
  color: #fff;
  border: none;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  cursor: pointer;
  font-size: 12px;
  line-height: 20px;
  text-align: center;
  padding: 0;
}

.upload-area {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 1px dashed #dcdfe6;
  border-radius: 6px;
  padding: 12px 20px;
  cursor: pointer;
  transition: border-color 0.2s;
}

.upload-area:hover {
  border-color: #1a73e8;
}

.upload-input {
  display: none;
}

.upload-text {
  font-size: 13px;
  color: #909399;
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
