<template>
  <div class="home-page">
    <!-- Smart Match Section -->
    <section class="search-section">
      <div class="search-wrapper" :class="{ 'search-wrapper--multiline': isMultiline }">
        <svg class="search-icon" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <circle cx="11" cy="11" r="8"/>
          <line x1="21" y1="21" x2="16.65" y2="16.65"/>
        </svg>
        <textarea
          ref="searchInput"
          v-model="keyword"
          class="search-input search-textarea"
          :rows="textareaRows"
          placeholder="粘贴整段报错日志，或输入关键词，自动匹配解决方案"
          @keydown.enter.exact.prevent="handleSearch"
          @paste="handlePaste"
        ></textarea>
        <button v-if="keyword" class="clear-btn" title="清空，返回列表" @click="clearSearch">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <line x1="18" y1="6" x2="6" y2="18"/>
            <line x1="6" y1="6" x2="18" y2="18"/>
          </svg>
        </button>
        <button class="search-btn" @click="handleSearch">
          匹配
        </button>
      </div>
      <p class="search-hint">复制报错日志后直接 Ctrl+V 粘贴，自动识别特征并匹配，无需自己提炼关键词（Shift+Enter 换行）</p>
    </section>

    <!-- Match Result (智能匹配模式) -->
    <template v-if="matchMode">
      <div v-if="matchSignals.length || matchRootCause" class="signal-bar">
        <div v-if="matchSignals.length" class="signal-row">
          <span class="signal-label">识别到的报错特征</span>
          <span v-for="(sig, idx) in matchSignals" :key="idx" class="signal-chip">{{ sig }}</span>
        </div>
        <p v-if="matchRootCause" class="signal-root">根因行：{{ matchRootCause }}</p>
      </div>

      <LoadingSkeleton v-if="matching" :count="3" />

      <template v-else-if="matchItems.length">
        <p class="match-count">为你匹配到 {{ matchItems.length }} 条可能的解决方案，命中特征已高亮</p>
        <div class="record-list match-record-list">
          <div
            v-for="item in matchItems"
            :key="item.record.id"
            class="record-card"
            @click="$router.push('/detail/' + item.record.id)"
          >
            <div class="record-card-inner">
              <div class="record-main">
                <div class="record-header">
                  <h3 class="record-title">
                    <span :class="['badge', levelClass(item.level)]">{{ levelText(item.level) }}</span>
                    <span v-if="item.record.status === 'PENDING'" class="badge badge-pending">待更新</span>
                    <span class="record-title-text">{{ item.record.errorTitle }}</span>
                  </h3>
                  <span class="record-category">{{ item.record.category }}</span>
                </div>
                <p class="record-excerpt">{{ truncateText(item.record.errorContent, 150) }}</p>
              </div>
              <div class="record-meta">
                <div class="record-tags">
                  <span
                    v-for="(term, idx) in item.matchedTerms.slice(0, 6)"
                    :key="'hit' + idx"
                    class="tag tag--hit"
                  >{{ term }}</span>
                </div>
                <div class="record-info">
                  <span class="record-time">{{ item.record.updateTime | formatDate }}</span>
                  <span v-if="item.record.updater" class="record-author">{{ item.record.updater }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
        <div class="match-footer">
          <span class="match-footer-text">都不能解决你的问题？</span>
          <button class="match-footer-btn" @click="registerPending">登记为待处理报错</button>
        </div>
      </template>

      <EmptyState
        v-else
        title="未匹配到现成的解决方案"
        description="可以一键把这条报错登记为「待处理」，日志和已识别的特征会自动带入，等有人补充处理步骤后沉淀为新方案"
      >
        <button class="empty-action empty-action--btn" @click="registerPending">登记为待处理报错</button>
      </EmptyState>
    </template>

    <!-- Browse Mode (浏览模式) -->
    <template v-if="!matchMode">
    <!-- Stats Bar -->
    <div class="stats-bar">
      <div class="stat-card">
        <span class="stat-value">{{ totalElements }}</span>
        <span class="stat-label">记录总数</span>
      </div>
      <div class="stat-card stat-card--warning">
        <span class="stat-value">{{ pendingCount }}</span>
        <span class="stat-label">待更新</span>
      </div>
      <div class="stats-spacer"></div>
      <button
        :class="['stats-action', 'stats-action--btn', { 'stats-action--active': importPanelOpen }]"
        @click="importPanelOpen = !importPanelOpen"
      >批量导入</button>
      <router-link to="/add" class="stats-action">+ 新增记录</router-link>
    </div>

    <!-- Import Panel -->
    <div v-if="importPanelOpen" class="import-panel">
      <div class="import-panel-row">
        <a
          class="import-template-link"
          href="/api/template/error-kb-template"
          download="报错处理知识库-收集模板.xlsx"
        >
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
            <polyline points="7 10 12 15 17 10"/>
            <line x1="12" y1="15" x2="12" y2="3"/>
          </svg>
          下载收集模板（xlsx）
        </a>
        <span class="import-hint">按模板填写后上传；示例行、重复标题会自动跳过，无处理步骤的记录自动标记「待更新」</span>
        <label :class="['import-file-btn', { 'import-file-btn--disabled': importing }]">
          {{ importing ? '导入中...' : '选择文件导入' }}
          <input
            type="file"
            accept=".xlsx"
            class="file-input-hidden"
            :disabled="importing"
            @change="handleImportFile"
          />
        </label>
      </div>
      <div v-if="importResult" class="import-result">
        <p class="import-result-line">
          共 {{ importResult.total }} 行：成功导入
          <b class="import-num-ok">{{ importResult.imported }}</b> 条、跳过
          {{ importResult.skipped }} 条、失败
          <b :class="importResult.failed > 0 ? 'import-num-bad' : ''">{{ importResult.failed }}</b> 条
        </p>
        <ul v-if="importResult.errors && importResult.errors.length" class="import-error-list">
          <li v-for="(err, idx) in importResult.errors.slice(0, 8)" :key="idx">
            第 {{ err.row }} 行{{ err.title ? '「' + err.title + '」' : '' }}：{{ err.reason }}
          </li>
          <li v-if="importResult.errors.length > 8">… 共 {{ importResult.errors.length }} 条明细</li>
        </ul>
      </div>
    </div>

    <!-- Loading State -->
    <LoadingSkeleton v-if="loading" :count="5" />

    <!-- Empty State -->
    <EmptyState
      v-else-if="!loading && records.length === 0"
      title="暂无记录"
      description="还没有报错记录，快去添加第一条吧"
    >
      <router-link to="/add" class="empty-action">新增记录</router-link>
    </EmptyState>

    <!-- Record List -->
    <div v-else class="record-list">
      <div
        v-for="record in records"
        :key="record.id"
        class="record-card"
        @click="$router.push('/detail/' + record.id)"
      >
        <div class="record-card-inner">
          <div class="record-main">
            <div class="record-header">
              <h3 class="record-title">
                <span v-if="record.status === 'PENDING'" class="badge badge-pending">待更新</span>
                <span class="record-title-text">{{ record.errorTitle }}</span>
              </h3>
              <span class="record-category">{{ record.category }}</span>
            </div>
            <p class="record-excerpt">{{ truncateText(record.errorContent, 150) }}</p>
          </div>
          <div class="record-meta">
            <div class="record-tags">
              <span
                v-for="(kw, idx) in parseKeywords(record.keywords)"
                :key="idx"
                class="tag"
              >{{ kw }}</span>
            </div>
            <div class="record-info">
              <span class="record-time">{{ record.updateTime | formatDate }}</span>
              <span v-if="record.updater" class="record-author">{{ record.updater }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Pagination -->
    <div v-if="totalPages > 1" class="pagination">
      <button
        class="page-btn"
        :disabled="currentPage === 0"
        @click="changePage(currentPage - 1)"
      >
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <polyline points="15 18 9 12 15 6"/>
        </svg>
        上一页
      </button>
      <div class="page-info">
        <span v-for="p in pageNumbers" :key="p"
          :class="['page-dot', { 'page-dot--active': p === currentPage }]"
          @click="changePage(p)"
        >{{ p + 1 }}</span>
      </div>
      <button
        class="page-btn"
        :disabled="currentPage >= totalPages - 1"
        @click="changePage(currentPage + 1)"
      >
        下一页
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <polyline points="9 18 15 12 9 6"/>
        </svg>
      </button>
    </div>
    </template>
  </div>
</template>

<script>
import { searchRecords, getPendingList, matchLog, importRecords } from '../api/index';
import EmptyState from '../components/EmptyState.vue';
import LoadingSkeleton from '../components/LoadingSkeleton.vue';

export default {
  name: 'Home',
  components: { EmptyState, LoadingSkeleton },
  data() {
    return {
      keyword: '',
      records: [],
      currentPage: 0,
      totalPages: 0,
      totalElements: 0,
      loading: false,
      pendingCount: 0,
      matching: false,
      matchMode: false,
      matchResult: null,
      importPanelOpen: false,
      importing: false,
      importResult: null
    };
  },
  computed: {
    isMultiline() {
      return this.keyword.indexOf('\n') !== -1;
    },
    textareaRows() {
      if (!this.keyword) return 1;
      return Math.min(10, this.keyword.split('\n').length);
    },
    matchSignals() {
      return (this.matchResult && this.matchResult.signals) || [];
    },
    matchRootCause() {
      return (this.matchResult && this.matchResult.rootCause) || '';
    },
    matchItems() {
      return (this.matchResult && this.matchResult.matches) || [];
    },
    pageNumbers() {
      const pages = [];
      const total = this.totalPages;
      const current = this.currentPage;
      const maxVisible = 7;
      if (total <= maxVisible) {
        for (let i = 0; i < total; i++) pages.push(i);
      } else {
        pages.push(0);
        let start = Math.max(1, current - 2);
        let end = Math.min(total - 2, current + 2);
        if (start > 1) pages.push(-1);
        for (let i = start; i <= end; i++) pages.push(i);
        if (end < total - 2) pages.push(-2);
        pages.push(total - 1);
      }
      return pages;
    }
  },
  created() {
    this.fetchRecords();
    this.fetchPendingCount();
  },
  methods: {
    async fetchRecords(page = 0) {
      this.loading = true;
      try {
        const res = await searchRecords(this.keyword, page, 10);
        this.records = res.content || [];
        this.totalPages = res.totalPages || 0;
        this.totalElements = res.totalElements || 0;
        this.currentPage = res.currentPage || 0;
      } catch (e) {
        console.error('搜索失败:', e);
      } finally {
        this.loading = false;
      }
    },
    async fetchPendingCount() {
      try {
        const list = await getPendingList();
        this.pendingCount = list ? list.length : 0;
      } catch (e) {
        console.error('获取待更新数量失败:', e);
      }
    },
    handleSearch() {
      if (!this.keyword.trim()) {
        this.clearSearch();
        return;
      }
      this.runMatch();
    },
    handlePaste() {
      // paste 事件先于 v-model 更新触发，延后一拍取值；粘贴内容较长时自动匹配（复制→粘贴→出结果）
      setTimeout(() => {
        if (this.keyword.trim().length >= 40) {
          this.runMatch();
        }
      }, 0);
    },
    async runMatch() {
      const text = this.keyword.trim();
      if (!text) return;
      this.matchMode = true;
      this.matching = true;
      try {
        this.matchResult = await matchLog(text);
      } catch (e) {
        console.error('匹配失败:', e);
        this.$root.$emit('toast', { message: '匹配失败，请稍后重试', type: 'error' });
      } finally {
        this.matching = false;
      }
    },
    clearSearch() {
      this.keyword = '';
      this.matchMode = false;
      this.matchResult = null;
      this.fetchRecords(0);
      this.$nextTick(() => {
        if (this.$refs.searchInput) this.$refs.searchInput.focus();
      });
    },
    registerPending() {
      // 未命中闭环：日志与识别特征带入新增页，登记为待处理（不填处理步骤自动 PENDING）
      const firstLine = (this.keyword.split('\n').find(line => line.trim()) || '').trim();
      const prefill = {
        errorTitle: (this.matchRootCause || firstLine).slice(0, 120),
        errorContent: this.keyword,
        keywords: this.matchSignals.slice(0, 10).join(',')
      };
      sessionStorage.setItem('kb_prefill', JSON.stringify(prefill));
      this.$router.push('/add');
    },
    levelClass(level) {
      if (level === 'HIGH') return 'badge-high';
      if (level === 'MEDIUM') return 'badge-medium';
      return 'badge-low';
    },
    levelText(level) {
      if (level === 'HIGH') return '匹配度高';
      if (level === 'MEDIUM') return '匹配度中';
      return '可能相关';
    },
    async handleImportFile(e) {
      const file = e.target.files[0];
      e.target.value = '';
      if (!file) return;
      this.importing = true;
      this.importResult = null;
      try {
        const res = await importRecords(file);
        if (res.success === false) {
          this.$root.$emit('toast', { message: res.message || '导入失败', type: 'error' });
          return;
        }
        this.importResult = res;
        this.$root.$emit('toast', {
          message: '导入完成：成功 ' + res.imported + ' 条',
          type: res.imported > 0 ? 'success' : 'warning'
        });
        this.fetchRecords(0);
        this.fetchPendingCount();
      } catch (err) {
        this.$root.$emit('toast', {
          message: '导入失败: ' + (err.response?.data?.message || err.message),
          type: 'error'
        });
      } finally {
        this.importing = false;
      }
    },
    changePage(page) {
      if (page < 0 || page >= this.totalPages) return;
      this.fetchRecords(page);
      window.scrollTo({ top: 0, behavior: 'smooth' });
    },
    truncateText(text, maxLen) {
      if (!text) return '';
      return text.length > maxLen ? text.slice(0, maxLen) + '...' : text;
    },
    parseKeywords(keywords) {
      if (!keywords) return [];
      return keywords.split(',').filter(k => k.trim()).slice(0, 5);
    }
  },
  filters: {
    formatDate(val) {
      if (!val) return '';
      const s = val.replace('T', ' ').substring(0, 16);
      return s;
    }
  }
};
</script>

<style scoped>
.home-page {
  max-width: 860px;
  margin: 0 auto;
}

/* === Search Section === */
.search-section {
  margin-bottom: var(--space-6);
}

.search-wrapper {
  display: flex;
  align-items: center;
  background: var(--surface-card);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-xl);
  padding: var(--space-1);
  transition: all var(--transition-base);
  box-shadow: var(--shadow-xs);
}

.search-wrapper:focus-within {
  border-color: var(--input-focus-border);
  box-shadow: 0 0 0 3px var(--input-focus-ring), var(--shadow-sm);
}

.search-icon {
  color: var(--text-tertiary);
  margin-left: var(--space-3);
  flex-shrink: 0;
}

.search-input {
  flex: 1;
  border: none;
  outline: none;
  background: transparent;
  padding: var(--space-3) var(--space-3);
  font-size: var(--text-base);
  font-family: var(--font-sans);
  color: var(--text-primary);
  min-width: 0;
}

.search-input::placeholder {
  color: var(--input-placeholder);
}

.search-btn {
  flex-shrink: 0;
  padding: var(--space-2) var(--space-6);
  background: var(--btn-primary-bg);
  color: var(--btn-primary-text);
  border: none;
  border-radius: var(--radius-lg);
  font-size: var(--text-sm);
  font-weight: var(--font-medium);
  font-family: var(--font-sans);
  cursor: pointer;
  transition: background var(--transition-fast);
}

.search-btn:hover {
  background: var(--btn-primary-hover);
}

.search-btn:active {
  background: var(--btn-primary-active);
  transform: scale(0.98);
}

.search-hint {
  font-size: var(--text-xs);
  color: var(--text-tertiary);
  margin-top: var(--space-2);
  padding-left: var(--space-2);
}

/* === Stats Bar === */
.stats-bar {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  margin-bottom: var(--space-6);
}

.stat-card {
  display: flex;
  align-items: baseline;
  gap: var(--space-2);
  background: var(--surface-card);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  padding: var(--space-3) var(--space-4);
}

.stat-value {
  font-size: var(--text-xl);
  font-weight: var(--font-bold);
  color: var(--text-primary);
  line-height: 1;
}

.stat-label {
  font-size: var(--text-xs);
  color: var(--text-tertiary);
}

.stat-card--warning .stat-value {
  color: var(--color-warning);
}

.stats-spacer {
  flex: 1;
}

.stats-action {
  display: inline-flex;
  align-items: center;
  padding: var(--space-2) var(--space-4);
  font-size: var(--text-sm);
  font-weight: var(--font-medium);
  color: var(--color-primary-600);
  background: var(--color-primary-50);
  border-radius: var(--radius-md);
  text-decoration: none;
  transition: all var(--transition-fast);
}

.stats-action:hover {
  background: var(--color-primary-100);
  color: var(--color-primary-700);
}

/* === Record List === */
.record-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.record-card {
  background: var(--surface-card);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  cursor: pointer;
  transition: all var(--transition-base);
}

.record-card:hover {
  border-color: var(--color-primary-200);
  box-shadow: var(--shadow-md);
  transform: translateY(-1px);
}

.record-card-inner {
  padding: var(--space-5) var(--space-6);
}

.record-main {
  margin-bottom: var(--space-3);
}

.record-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-3);
  margin-bottom: var(--space-2);
}

.record-title {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  min-width: 0;
  flex: 1;
}

.record-title-text {
  font-size: var(--text-base);
  font-weight: var(--font-semibold);
  color: var(--text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.badge {
  display: inline-flex;
  align-items: center;
  flex-shrink: 0;
  font-size: var(--text-2xs);
  font-weight: var(--font-semibold);
  padding: 2px 8px;
  border-radius: var(--radius-full);
  text-transform: uppercase;
  letter-spacing: 0.02em;
}

.badge-pending {
  background: var(--badge-pending-bg);
  color: var(--badge-pending-text);
  border: 1px solid var(--badge-pending-border);
}

.record-category {
  flex-shrink: 0;
  font-size: var(--text-2xs);
  font-weight: var(--font-medium);
  color: var(--tag-category-text);
  background: var(--tag-category-bg);
  padding: 3px 10px;
  border-radius: var(--radius-full);
  white-space: nowrap;
}

.record-excerpt {
  font-size: var(--text-sm);
  color: var(--text-secondary);
  line-height: var(--leading-relaxed);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* Record Meta */
.record-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-4);
}

.record-tags {
  display: flex;
  gap: var(--space-1);
  flex-wrap: wrap;
  flex: 1;
  min-width: 0;
}

.tag {
  font-size: var(--text-2xs);
  color: var(--tag-text);
  background: var(--tag-bg);
  padding: 2px 8px;
  border-radius: var(--radius-sm);
  white-space: nowrap;
}

.record-info {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  font-size: var(--text-xs);
  color: var(--text-tertiary);
  flex-shrink: 0;
}

.record-time::before {
  content: '';
}

/* === Pagination === */
.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-4);
  margin-top: var(--space-8);
  padding-top: var(--space-6);
  border-top: 1px solid var(--border-light);
}

.page-btn {
  display: inline-flex;
  align-items: center;
  gap: var(--space-1);
  padding: var(--space-2) var(--space-4);
  font-size: var(--text-sm);
  font-family: var(--font-sans);
  font-weight: var(--font-medium);
  color: var(--text-secondary);
  background: var(--surface-card);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.page-btn:hover:not(:disabled) {
  color: var(--color-primary-600);
  border-color: var(--color-primary-200);
  background: var(--color-primary-50);
}

.page-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.page-info {
  display: flex;
  gap: var(--space-1);
}

.page-dot {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  font-size: var(--text-sm);
  font-weight: var(--font-medium);
  color: var(--text-secondary);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all var(--transition-fast);
  user-select: none;
}

.page-dot:hover {
  background: var(--color-neutral-100);
}

.page-dot--active {
  background: var(--color-primary-600);
  color: #fff;
}

.page-dot--active:hover {
  background: var(--color-primary-700);
}

/* === Empty Action === */
.empty-action {
  display: inline-block;
  padding: var(--space-3) var(--space-6);
  font-size: var(--text-sm);
  font-weight: var(--font-medium);
  color: #fff;
  background: var(--btn-primary-bg);
  border-radius: var(--radius-md);
  text-decoration: none;
  transition: background var(--transition-fast);
}

.empty-action:hover {
  background: var(--btn-primary-hover);
  color: #fff;
}

/* === Import Panel === */
.stats-action--btn {
  border: none;
  cursor: pointer;
  font-family: var(--font-sans);
  font-size: var(--text-sm);
}

.stats-action--active {
  background: var(--color-primary-100);
  color: var(--color-primary-700);
}

.import-panel {
  background: var(--surface-card);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  padding: var(--space-4) var(--space-5);
  margin-bottom: var(--space-6);
  margin-top: calc(-1 * var(--space-3));
}

.import-panel-row {
  display: flex;
  align-items: center;
  gap: var(--space-4);
  flex-wrap: wrap;
}

.import-template-link {
  display: inline-flex;
  align-items: center;
  gap: var(--space-1);
  font-size: var(--text-sm);
  font-weight: var(--font-medium);
  color: var(--color-primary-600);
  text-decoration: none;
  flex-shrink: 0;
}

.import-template-link:hover {
  color: var(--color-primary-700);
  text-decoration: underline;
}

.import-hint {
  flex: 1;
  min-width: 200px;
  font-size: var(--text-xs);
  color: var(--text-tertiary);
}

.import-file-btn {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  padding: var(--space-2) var(--space-4);
  background: var(--btn-primary-bg);
  color: var(--btn-primary-text);
  border-radius: var(--radius-md);
  font-size: var(--text-sm);
  font-weight: var(--font-medium);
  cursor: pointer;
  transition: background var(--transition-fast);
}

.import-file-btn:hover {
  background: var(--btn-primary-hover);
}

.import-file-btn--disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.file-input-hidden {
  display: none;
}

.import-result {
  margin-top: var(--space-3);
  padding-top: var(--space-3);
  border-top: 1px dashed var(--border-default);
}

.import-result-line {
  font-size: var(--text-sm);
  color: var(--text-secondary);
}

.import-num-ok {
  color: var(--color-success);
}

.import-num-bad {
  color: var(--color-danger);
}

.import-error-list {
  margin: var(--space-2) 0 0;
  padding-left: var(--space-5);
  font-size: var(--text-xs);
  color: var(--text-tertiary);
  line-height: 1.8;
}

/* === Smart Match === */
.search-textarea {
  resize: none;
  overflow-y: auto;
  max-height: 220px;
  line-height: var(--leading-relaxed);
}

.search-wrapper--multiline {
  align-items: flex-start;
}

.search-wrapper--multiline .search-icon,
.search-wrapper--multiline .clear-btn,
.search-wrapper--multiline .search-btn {
  margin-top: var(--space-2);
}

.search-wrapper--multiline .search-textarea {
  font-family: var(--font-mono);
  font-size: var(--text-sm);
}

.clear-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  flex-shrink: 0;
  border: none;
  background: transparent;
  color: var(--text-tertiary);
  border-radius: var(--radius-full);
  cursor: pointer;
  transition: all var(--transition-fast);
  margin-right: var(--space-1);
}

.clear-btn:hover {
  background: var(--color-neutral-100);
  color: var(--text-primary);
}

.signal-bar {
  background: var(--surface-card);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  padding: var(--space-3) var(--space-4);
  margin-bottom: var(--space-4);
}

.signal-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: var(--space-2);
}

.signal-label {
  font-size: var(--text-xs);
  font-weight: var(--font-medium);
  color: var(--text-tertiary);
  flex-shrink: 0;
}

.signal-chip {
  font-size: var(--text-2xs);
  font-family: var(--font-mono);
  color: var(--color-primary-700);
  background: var(--color-primary-50);
  border: 1px solid var(--color-primary-200);
  padding: 2px 8px;
  border-radius: var(--radius-full);
  white-space: nowrap;
  max-width: 260px;
  overflow: hidden;
  text-overflow: ellipsis;
}

.signal-root {
  margin-top: var(--space-2);
  font-size: var(--text-xs);
  font-family: var(--font-mono);
  color: var(--text-secondary);
  word-break: break-all;
}

.match-count {
  font-size: var(--text-xs);
  color: var(--text-tertiary);
  margin-bottom: var(--space-3);
  padding-left: var(--space-1);
}

.tag--hit {
  color: var(--color-primary-700);
  background: var(--color-primary-100);
  border: 1px solid var(--color-primary-200);
  font-weight: var(--font-medium);
}

.badge-high {
  background: var(--color-success-bg);
  color: var(--color-success);
  border: 1px solid var(--color-success-border);
}

.badge-medium {
  background: var(--color-warning-bg);
  color: var(--color-warning);
  border: 1px solid var(--color-warning-border);
}

.badge-low {
  background: var(--color-neutral-100);
  color: var(--text-tertiary);
  border: 1px solid var(--border-default);
}

.empty-action--btn {
  border: none;
  cursor: pointer;
  font-family: var(--font-sans);
  font-size: var(--text-sm);
  font-weight: var(--font-medium);
}

.match-footer {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-2);
  margin-top: var(--space-6);
  padding-top: var(--space-5);
  border-top: 1px dashed var(--border-default);
}

.match-footer-text {
  font-size: var(--text-xs);
  color: var(--text-tertiary);
}

.match-footer-btn {
  border: none;
  background: transparent;
  padding: var(--space-1) var(--space-2);
  font-size: var(--text-xs);
  font-family: var(--font-sans);
  font-weight: var(--font-medium);
  color: var(--color-primary-600);
  cursor: pointer;
  border-radius: var(--radius-sm);
  transition: all var(--transition-fast);
}

.match-footer-btn:hover {
  background: var(--color-primary-50);
  color: var(--color-primary-700);
}

/* === Responsive === */
@media (max-width: 640px) {
  .search-wrapper {
    border-radius: var(--radius-lg);
  }

  .search-input {
    font-size: var(--text-sm);
  }

  .search-btn {
    padding: var(--space-2) var(--space-4);
    font-size: var(--text-xs);
  }

  .stats-bar {
    flex-wrap: wrap;
  }

  .stat-card {
    padding: var(--space-2) var(--space-3);
  }

  .stat-value {
    font-size: var(--text-lg);
  }

  .record-card-inner {
    padding: var(--space-4);
  }

  .record-title-text {
    font-size: var(--text-sm);
  }

  .record-meta {
    flex-direction: column;
    align-items: flex-start;
    gap: var(--space-2);
  }

  .record-info {
    width: 100%;
    justify-content: flex-start;
  }

  .pagination {
    flex-wrap: wrap;
    gap: var(--space-2);
  }

  .page-dot {
    width: 28px;
    height: 28px;
    font-size: var(--text-xs);
  }
}
</style>
