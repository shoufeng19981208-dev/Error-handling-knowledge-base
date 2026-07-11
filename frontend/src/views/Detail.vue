<template>
  <div class="detail-page">
    <div class="detail-card" v-if="record">
      <!-- 状态和操作 -->
      <div class="detail-actions">
        <button class="btn-back" @click="$router.push('/')">← 返回列表</button>
        <div class="action-right">
          <button class="btn-edit" @click="$router.push('/edit/' + record.id)">编辑</button>
          <button class="btn-delete" @click="handleDelete">删除</button>
        </div>
      </div>

      <!-- 标题区域 -->
      <div class="detail-header">
        <h2 class="detail-title">
          <span v-if="record.status === 'PENDING'" class="badge badge-pending">待更新</span>
          {{ record.errorTitle }}
        </h2>
        <span class="detail-category">{{ record.category }}</span>
      </div>

      <!-- 报错内容 -->
      <div class="detail-section">
        <h3 class="section-title">报错内容</h3>
        <div class="section-content">
          <pre class="error-content-text">{{ record.errorContent || '暂无详细描述' }}</pre>
        </div>
      </div>

      <!-- 截图 -->
      <div class="detail-section" v-if="record.errorScreenshot">
        <h3 class="section-title">报错截图</h3>
        <div class="section-content">
          <img :src="record.errorScreenshot" class="screenshot-img" alt="报错截图" @click="previewImage(record.errorScreenshot)" />
          <div class="screenshot-hint">点击图片可放大查看</div>
        </div>
      </div>

      <!-- 处理步骤 -->
      <div class="detail-section">
        <h3 class="section-title">处理步骤</h3>
        <div class="section-content">
          <div v-if="record.solutionSteps" class="solution-steps">{{ record.solutionSteps }}</div>
          <div v-else class="no-solution">
            暂无处理步骤，
            <router-link :to="'/edit/' + record.id">点击补充</router-link>
          </div>
        </div>
      </div>

      <!-- 关键字 -->
      <div class="detail-section" v-if="record.keywords">
        <h3 class="section-title">关键字</h3>
        <div class="section-content">
          <span v-for="(kw, idx) in parseKeywords(record.keywords)" :key="idx" class="keyword-tag">{{ kw }}</span>
        </div>
      </div>

      <!-- 元信息 -->
      <div class="detail-meta">
        <div class="meta-row">
          <span class="meta-label">登记人：</span><span>{{ record.registrar }}</span>
          <span class="meta-label">登记时间：</span><span>{{ record.registerTime | formatDateTime }}</span>
        </div>
        <div class="meta-row">
          <span class="meta-label">更新人：</span><span>{{ record.updater }}</span>
          <span class="meta-label">更新时间：</span><span>{{ record.updateTime | formatDateTime }}</span>
        </div>
      </div>
    </div>

    <div v-else class="loading-text">加载中...</div>

    <!-- 图片预览弹窗 -->
    <div v-if="previewImageUrl" class="image-preview-overlay" @click="closePreview">
      <div class="image-preview-box">
        <img :src="previewImageUrl" class="image-preview-content" alt="截图预览" />
        <button class="image-preview-close" @click="closePreview">✕</button>
      </div>
    </div>
  </div>
</template>

<script>
import { getRecordById, deleteRecord } from '../api/index';

export default {
  name: 'Detail',
  data() {
    return {
      record: null,
      previewImageUrl: ''
    };
  },
  created() {
    this.fetchDetail();
  },
  methods: {
    async fetchDetail() {
      try {
        this.record = await getRecordById(this.$route.params.id);
      } catch (e) {
        console.error('获取详情失败:', e);
        this.$router.push('/');
      }
    },
    async handleDelete() {
      if (confirm('确定要删除这条记录吗？此操作不可恢复。')) {
        try {
          await deleteRecord(this.$route.params.id);
          this.$router.push('/');
        } catch (e) {
          alert('删除失败: ' + (e.response?.data?.message || e.message));
        }
      }
    },
    parseKeywords(keywords) {
      if (!keywords) return [];
      return keywords.split(',').filter(k => k.trim());
    },
    previewImage(url) {
      this.previewImageUrl = url;
    },
    closePreview() {
      this.previewImageUrl = '';
    }
  },
  filters: {
    formatDateTime(val) {
      if (!val) return '';
      return val.replace('T', ' ').substring(0, 19);
    }
  }
};
</script>

<style scoped>
.detail-page {
  max-width: 800px;
  margin: 0 auto;
}

.detail-card {
  background: #fff;
  border-radius: 8px;
  padding: 24px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
}

.detail-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #ebeef5;
}

.btn-back {
  background: none;
  border: none;
  color: #1a73e8;
  cursor: pointer;
  font-size: 14px;
}

.action-right {
  display: flex;
  gap: 8px;
}

.btn-edit {
  padding: 6px 16px;
  background: #1a73e8;
  color: #fff;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 13px;
}

.btn-delete {
  padding: 6px 16px;
  background: #fff;
  color: #f56c6c;
  border: 1px solid #f56c6c;
  border-radius: 4px;
  cursor: pointer;
  font-size: 13px;
}

.btn-delete:hover {
  background: #fef0f0;
}

.detail-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.detail-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  display: flex;
  align-items: center;
  gap: 10px;
}

.badge {
  font-size: 12px;
  padding: 3px 10px;
  border-radius: 12px;
  font-weight: 500;
}

.badge-pending {
  background: #fdf6ec;
  color: #e6a23c;
  border: 1px solid #faecd8;
}

.detail-category {
  font-size: 12px;
  color: #fff;
  background: #1a73e8;
  padding: 4px 12px;
  border-radius: 12px;
}

.detail-section {
  margin-bottom: 20px;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 10px;
  padding-left: 10px;
  border-left: 3px solid #1a73e8;
}

.section-content {
  background: #fafbfc;
  border-radius: 6px;
  padding: 16px;
}

.error-content-text {
  font-size: 14px;
  color: #606266;
  line-height: 1.8;
  white-space: pre-wrap;
  word-break: break-all;
  font-family: inherit;
}

.screenshot-img {
  max-width: 100%;
  border-radius: 4px;
  border: 1px solid #ebeef5;
  cursor: pointer;
}

.screenshot-hint {
  font-size: 12px;
  color: #909399;
  margin-top: 8px;
}

.solution-steps {
  font-size: 14px;
  color: #606266;
  line-height: 1.8;
  white-space: pre-wrap;
}

.no-solution {
  font-size: 14px;
  color: #e6a23c;
}

.no-solution a {
  color: #1a73e8;
}

.keyword-tag {
  display: inline-block;
  font-size: 12px;
  background: #f0f2f5;
  color: #606266;
  padding: 3px 10px;
  border-radius: 3px;
  margin-right: 6px;
  margin-bottom: 4px;
}

.detail-meta {
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid #ebeef5;
  font-size: 13px;
  color: #909399;
}

.meta-row {
  margin-bottom: 6px;
}

.meta-label {
  margin-left: 16px;
}

.meta-label:first-child {
  margin-left: 0;
}

.loading-text {
  text-align: center;
  padding: 60px;
  color: #909399;
}

.image-preview-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.75);
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
}

.image-preview-box {
  position: relative;
  max-width: 90vw;
  max-height: 90vh;
}

.image-preview-content {
  max-width: 90vw;
  max-height: 90vh;
  border-radius: 4px;
  object-fit: contain;
}

.image-preview-close {
  position: absolute;
  top: -36px;
  right: 0;
  background: rgba(255, 255, 255, 0.2);
  color: #fff;
  border: none;
  font-size: 20px;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  cursor: pointer;
  line-height: 32px;
  text-align: center;
}

.image-preview-close:hover {
  background: rgba(255, 255, 255, 0.4);
}
</style>
