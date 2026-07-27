<template>
  <div class="detail-page">
    <!-- Loading -->
    <LoadingSkeleton v-if="loading" :count="1" />

    <template v-else-if="record">
      <!-- Breadcrumb -->
      <nav class="breadcrumb">
        <router-link to="/" class="breadcrumb-link">首页</router-link>
        <span class="breadcrumb-sep">/</span>
        <span class="breadcrumb-current">记录详情</span>
      </nav>

      <article class="detail-card">
        <!-- Action Bar -->
        <div class="action-bar">
          <button class="btn-back" @click="$router.push('/')">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <line x1="19" y1="12" x2="5" y2="12"/>
              <polyline points="12 19 5 12 12 5"/>
            </svg>
            返回列表
          </button>
          <div class="action-group">
            <router-link :to="'/edit/' + record.id" class="btn btn-secondary">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/>
                <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/>
              </svg>
              编辑
            </router-link>
            <button class="btn btn-danger" @click="handleDelete">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <polyline points="3 6 5 6 21 6"/>
                <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/>
              </svg>
              删除
            </button>
          </div>
        </div>

        <!-- Title Section -->
        <header class="detail-header">
          <div class="detail-title-row">
            <span v-if="record.status === 'PENDING'" class="badge badge-pending">待更新</span>
            <span v-else class="badge badge-resolved">已记录</span>
            <h1 class="detail-title">{{ record.errorTitle }}</h1>
          </div>
          <span class="detail-category">{{ record.category }}</span>
        </header>

        <!-- Error Content -->
        <section class="detail-section">
          <h2 class="section-title">报错内容</h2>
          <div class="section-body">
            <pre v-if="record.errorContent" class="content-text">{{ record.errorContent }}</pre>
            <p v-else class="content-empty">暂无详细描述</p>
          </div>
        </section>

        <!-- Screenshot -->
        <section v-if="record.errorScreenshot" class="detail-section">
          <h2 class="section-title">报错截图</h2>
          <div class="section-body">
            <img
              :src="record.errorScreenshot"
              class="screenshot-image"
              alt="报错截图"
              @click="previewImage(record.errorScreenshot)"
            />
            <p class="screenshot-hint">点击图片可放大查看</p>
          </div>
        </section>

        <!-- Solution Steps -->
        <section class="detail-section">
          <h2 class="section-title">处理步骤</h2>
          <div class="section-body">
            <div v-if="record.solutionSteps" class="solution-text">{{ record.solutionSteps }}</div>
            <div v-else class="solution-empty">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <circle cx="12" cy="12" r="10"/>
                <line x1="12" y1="8" x2="12" y2="12"/>
                <line x1="12" y1="16" x2="12.01" y2="16"/>
              </svg>
              暂无处理步骤，
              <router-link :to="'/edit/' + record.id" class="inline-link">点击补充</router-link>
            </div>
          </div>
        </section>

        <!-- Keywords -->
        <section v-if="record.keywords" class="detail-section">
          <h2 class="section-title">关键字</h2>
          <div class="section-body">
            <div class="keywords-list">
              <span v-for="(kw, idx) in parseKeywords(record.keywords)" :key="idx" class="keyword-tag">{{ kw }}</span>
            </div>
          </div>
        </section>

        <!-- Meta Info -->
        <footer class="detail-meta">
          <div class="meta-grid">
            <div class="meta-item">
              <span class="meta-label">登记人</span>
              <span class="meta-value">{{ record.registrar || '-' }}</span>
            </div>
            <div class="meta-item">
              <span class="meta-label">登记时间</span>
              <span class="meta-value">{{ record.registerTime | formatDate }}</span>
            </div>
            <div class="meta-item">
              <span class="meta-label">更新人</span>
              <span class="meta-value">{{ record.updater || '-' }}</span>
            </div>
            <div class="meta-item">
              <span class="meta-label">更新时间</span>
              <span class="meta-value">{{ record.updateTime | formatDate }}</span>
            </div>
          </div>
        </footer>
      </article>
    </template>

    <!-- Image Preview Overlay -->
    <transition name="preview">
      <div v-if="previewImageUrl" class="image-preview-overlay" @click="closePreview">
        <div class="image-preview-box" @click.stop>
          <img :src="previewImageUrl" class="image-preview-content" alt="截图预览" />
          <button class="image-preview-close" @click="closePreview" aria-label="关闭预览">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <line x1="18" y1="6" x2="6" y2="18"/>
              <line x1="6" y1="6" x2="18" y2="18"/>
            </svg>
          </button>
        </div>
      </div>
    </transition>
  </div>
</template>

<script>
import { getRecordById, deleteRecord } from '../api/index';
import LoadingSkeleton from '../components/LoadingSkeleton.vue';

export default {
  name: 'Detail',
  components: { LoadingSkeleton },
  data() {
    return {
      record: null,
      loading: true,
      previewImageUrl: ''
    };
  },
  created() {
    this.fetchDetail();
  },
  methods: {
    async fetchDetail() {
      this.loading = true;
      try {
        this.record = await getRecordById(this.$route.params.id);
      } catch (e) {
        console.error('获取详情失败:', e);
        this.$router.push('/');
      } finally {
        this.loading = false;
      }
    },
    async handleDelete() {
      if (!confirm('确定要删除这条记录吗？此操作不可恢复。')) return;
      try {
        await deleteRecord(this.$route.params.id);
        this.$root.$emit('toast', { message: '删除成功', type: 'success' });
        this.$router.push('/');
      } catch (e) {
        this.$root.$emit('toast', {
          message: '删除失败: ' + (e.response?.data?.message || e.message),
          type: 'error'
        });
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
    formatDate(val) {
      if (!val) return '-';
      return val.replace('T', ' ').substring(0, 19);
    }
  }
};
</script>

<style scoped>
.detail-page {
  max-width: 820px;
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

/* === Detail Card === */
.detail-card {
  background: var(--surface-card);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-xl);
  padding: var(--space-8);
  box-shadow: var(--shadow-xs);
}

/* === Action Bar === */
.action-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--space-6);
  padding-bottom: var(--space-5);
  border-bottom: 1px solid var(--border-light);
}

.btn-back {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-2) 0;
  font-size: var(--text-sm);
  font-family: var(--font-sans);
  font-weight: var(--font-medium);
  color: var(--text-secondary);
  background: none;
  border: none;
  cursor: pointer;
  transition: color var(--transition-fast);
}

.btn-back:hover {
  color: var(--color-link);
}

.action-group {
  display: flex;
  gap: var(--space-2);
}

/* === Buttons === */
.btn {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-2) var(--space-4);
  font-size: var(--text-sm);
  font-weight: var(--font-medium);
  font-family: var(--font-sans);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all var(--transition-fast);
  border: none;
  text-decoration: none;
}

.btn-secondary {
  background: var(--btn-secondary-bg);
  color: var(--btn-secondary-text);
  border: 1px solid var(--btn-secondary-border);
}

.btn-secondary:hover {
  background: var(--btn-secondary-hover);
}

.btn-danger {
  background: var(--color-danger-bg);
  color: var(--color-danger);
  border: 1px solid var(--color-danger-border);
}

.btn-danger:hover {
  background: var(--color-danger);
  color: #fff;
}

/* === Header === */
.detail-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-4);
  margin-bottom: var(--space-8);
}

.detail-title-row {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  flex: 1;
  min-width: 0;
}

.detail-title {
  font-size: var(--text-2xl);
  font-weight: var(--font-bold);
  color: var(--text-primary);
  letter-spacing: -0.01em;
  line-height: var(--leading-snug);
}

.badge {
  display: inline-flex;
  align-items: center;
  flex-shrink: 0;
  font-size: var(--text-2xs);
  font-weight: var(--font-semibold);
  padding: 3px 10px;
  border-radius: var(--radius-full);
  text-transform: uppercase;
  letter-spacing: 0.02em;
}

.badge-pending {
  background: var(--badge-pending-bg);
  color: var(--badge-pending-text);
  border: 1px solid var(--badge-pending-border);
}

.badge-resolved {
  background: var(--badge-resolved-bg);
  color: var(--badge-resolved-text);
  border: 1px solid var(--badge-resolved-border);
}

.detail-category {
  flex-shrink: 0;
  font-size: var(--text-xs);
  font-weight: var(--font-medium);
  color: var(--tag-category-text);
  background: var(--tag-category-bg);
  padding: var(--space-1) var(--space-4);
  border-radius: var(--radius-full);
  white-space: nowrap;
}

/* === Detail Section === */
.detail-section {
  margin-bottom: var(--space-6);
}

.section-title {
  font-size: var(--text-sm);
  font-weight: var(--font-semibold);
  color: var(--text-primary);
  margin-bottom: var(--space-3);
  padding-left: var(--space-3);
  border-left: 3px solid var(--color-primary-400);
}

.section-body {
  background: var(--color-neutral-50);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  padding: var(--space-5);
}

/* === Content Text === */
.content-text {
  font-family: var(--font-mono);
  font-size: var(--text-sm);
  color: var(--text-primary);
  line-height: var(--leading-relaxed);
  white-space: pre-wrap;
  word-break: break-word;
  margin: 0;
}

.content-empty {
  font-size: var(--text-sm);
  color: var(--text-tertiary);
  font-style: italic;
}

/* === Screenshot === */
.screenshot-image {
  max-width: 100%;
  max-height: 400px;
  border-radius: var(--radius-md);
  border: 1px solid var(--border-light);
  cursor: zoom-in;
  display: block;
  transition: opacity var(--transition-fast);
}

.screenshot-image:hover {
  opacity: 0.9;
}

.screenshot-hint {
  font-size: var(--text-xs);
  color: var(--text-tertiary);
  margin-top: var(--space-2);
}

/* === Solution === */
.solution-text {
  font-size: var(--text-sm);
  color: var(--text-primary);
  line-height: var(--leading-relaxed);
  white-space: pre-wrap;
}

.solution-empty {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  font-size: var(--text-sm);
  color: var(--color-warning);
}

.inline-link {
  color: var(--color-link);
  font-weight: var(--font-medium);
}

.inline-link:hover {
  text-decoration: underline;
}

/* === Keywords === */
.keywords-list {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
}

.keyword-tag {
  font-size: var(--text-xs);
  color: var(--tag-text);
  background: var(--tag-bg);
  padding: var(--space-1) var(--space-3);
  border-radius: var(--radius-sm);
}

/* === Meta === */
.detail-meta {
  margin-top: var(--space-8);
  padding-top: var(--space-6);
  border-top: 1px solid var(--border-light);
}

.meta-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: var(--space-3);
}

.meta-item {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  font-size: var(--text-sm);
}

.meta-label {
  color: var(--text-tertiary);
  flex-shrink: 0;
}

.meta-value {
  color: var(--text-secondary);
  font-weight: var(--font-medium);
}

/* === Image Preview Overlay === */
.image-preview-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.8);
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
  backdrop-filter: blur(4px);
  -webkit-backdrop-filter: blur(4px);
}

.image-preview-box {
  position: relative;
  max-width: 90vw;
  max-height: 90vh;
}

.image-preview-content {
  max-width: 90vw;
  max-height: 90vh;
  border-radius: var(--radius-md);
  object-fit: contain;
}

.image-preview-close {
  position: absolute;
  top: -40px;
  right: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  background: rgba(255, 255, 255, 0.15);
  color: #fff;
  border: none;
  border-radius: var(--radius-full);
  cursor: pointer;
  transition: background var(--transition-fast);
}

.image-preview-close:hover {
  background: rgba(255, 255, 255, 0.3);
}

/* Preview transition */
.preview-enter-active,
.preview-leave-active {
  transition: opacity var(--transition-base);
}

.preview-enter,
.preview-leave-to {
  opacity: 0;
}

/* === Responsive === */
@media (max-width: 640px) {
  .detail-card {
    padding: var(--space-5);
    border-radius: var(--radius-lg);
  }

  .action-bar {
    flex-wrap: wrap;
    gap: var(--space-3);
  }

  .detail-header {
    flex-direction: column-reverse;
  }

  .detail-title {
    font-size: var(--text-xl);
  }

  .meta-grid {
    grid-template-columns: 1fr;
  }

  .section-body {
    padding: var(--space-4);
  }
}
</style>
