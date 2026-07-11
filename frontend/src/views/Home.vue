<template>
  <div class="home-page">
    <!-- 搜索区域 -->
    <div class="search-card">
      <div class="search-box">
        <input
          v-model="keyword"
          class="search-input"
          placeholder="输入报错关键词进行搜索，如：ORA-01555、DataStage、分区字段..."
          @keyup.enter="handleSearch"
        />
        <button class="search-btn" @click="handleSearch">搜索</button>
      </div>
      <div class="search-tips">
        <span>支持模糊搜索：报错标题、报错内容、关键字、处理步骤、分类</span>
      </div>
    </div>

    <!-- 统计信息 -->
    <div class="stats-bar">
      <span class="stat-item">共 <strong>{{ totalElements }}</strong> 条记录</span>
      <span class="stat-item stat-pending">
        待更新 <strong>{{ pendingCount }}</strong> 条
      </span>
    </div>

    <!-- 结果列表 -->
    <div class="record-list">
      <div v-if="loading" class="loading-text">加载中...</div>
      <div v-else-if="records.length === 0" class="empty-text">
        暂无数据，如有新报错请<router-link to="/add">新增记录</router-link>
      </div>
      <div v-else>
        <div
          v-for="record in records"
          :key="record.id"
          class="record-card"
          @click="$router.push('/detail/' + record.id)"
        >
          <div class="record-header">
            <h3 class="record-title">
              <span v-if="record.status === 'PENDING'" class="badge badge-pending">待更新</span>
              {{ record.errorTitle }}
            </h3>
            <span class="record-category">{{ record.category }}</span>
          </div>
          <div class="record-content">
            <p class="record-text">{{ truncateText(record.errorContent, 120) }}</p>
          </div>
          <div class="record-screenshot" v-if="record.errorScreenshot">
            <img :src="record.errorScreenshot" class="record-screenshot-img" alt="截图" />
          </div>
          <div class="record-footer">
            <span class="record-keywords">
              <span v-for="(kw, idx) in parseKeywords(record.keywords)" :key="idx" class="keyword-tag">{{ kw }}</span>
            </span>
            <span class="record-meta">
              <span>{{ record.updateTime | formatDateTime }}</span>
              <span class="meta-divider">|</span>
              <span>{{ record.updater }}</span>
            </span>
          </div>
        </div>
      </div>
    </div>

    <!-- 分页 -->
    <div v-if="totalPages > 1" class="pagination">
      <button :disabled="currentPage === 0" @click="changePage(currentPage - 1)">上一页</button>
      <span class="page-info">第 {{ currentPage + 1 }} / {{ totalPages }} 页</span>
      <button :disabled="currentPage >= totalPages - 1" @click="changePage(currentPage + 1)">下一页</button>
    </div>
  </div>
</template>

<script>
import { searchRecords, getPendingList } from '../api/index';

export default {
  name: 'Home',
  data() {
    return {
      keyword: '',
      records: [],
      currentPage: 0,
      totalPages: 0,
      totalElements: 0,
      loading: false,
      pendingCount: 0
    };
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
        this.records = res.content;
        this.totalPages = res.totalPages;
        this.totalElements = res.totalElements;
        this.currentPage = res.currentPage;
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
      this.fetchRecords(0);
    },
    changePage(page) {
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
    formatDateTime(val) {
      if (!val) return '';
      return val.replace('T', ' ').substring(0, 19);
    }
  }
};
</script>

<style scoped>
.home-page {
  max-width: 900px;
  margin: 0 auto;
}

.search-card {
  background: #fff;
  border-radius: 8px;
  padding: 24px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
  margin-bottom: 16px;
}

.search-box {
  display: flex;
  gap: 12px;
}

.search-input {
  flex: 1;
  padding: 10px 16px;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  font-size: 14px;
  outline: none;
  transition: border-color 0.2s;
}

.search-input:focus {
  border-color: #1a73e8;
}

.search-btn {
  padding: 10px 28px;
  background: #1a73e8;
  color: #fff;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  cursor: pointer;
  transition: background 0.2s;
}

.search-btn:hover {
  background: #1557b0;
}

.search-tips {
  margin-top: 10px;
  font-size: 12px;
  color: #909399;
}

.stats-bar {
  display: flex;
  gap: 24px;
  margin-bottom: 16px;
  font-size: 14px;
  color: #606266;
}

.stat-pending {
  color: #e6a23c;
}

.record-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.loading-text, .empty-text {
  text-align: center;
  padding: 40px;
  color: #909399;
  font-size: 14px;
}

.record-card {
  background: #fff;
  border-radius: 8px;
  padding: 16px 20px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
  cursor: pointer;
  transition: box-shadow 0.2s, transform 0.1s;
}

.record-card:hover {
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  transform: translateY(-1px);
}

.record-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.record-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  display: flex;
  align-items: center;
  gap: 8px;
}

.badge {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 10px;
  font-weight: 500;
}

.badge-pending {
  background: #fdf6ec;
  color: #e6a23c;
  border: 1px solid #faecd8;
}

.record-category {
  font-size: 12px;
  color: #fff;
  background: #1a73e8;
  padding: 2px 10px;
  border-radius: 10px;
  white-space: nowrap;
}

.record-content {
  margin-bottom: 10px;
}

.record-text {
  font-size: 13px;
  color: #606266;
  line-height: 1.6;
}

.record-screenshot {
  margin-bottom: 10px;
}

.record-screenshot-img {
  max-width: 120px;
  max-height: 80px;
  border-radius: 4px;
  border: 1px solid #ebeef5;
  object-fit: cover;
}

.record-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 8px;
}

.record-keywords {
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
}

.keyword-tag {
  font-size: 11px;
  background: #f0f2f5;
  color: #909399;
  padding: 1px 8px;
  border-radius: 3px;
}

.record-meta {
  font-size: 12px;
  color: #c0c4cc;
}

.meta-divider {
  margin: 0 6px;
}

.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 16px;
  margin-top: 24px;
  padding: 16px 0;
}

.pagination button {
  padding: 6px 16px;
  border: 1px solid #dcdfe6;
  background: #fff;
  border-radius: 4px;
  cursor: pointer;
  font-size: 13px;
}

.pagination button:hover:not(:disabled) {
  border-color: #1a73e8;
  color: #1a73e8;
}

.pagination button:disabled {
  color: #c0c4cc;
  cursor: not-allowed;
}

.page-info {
  font-size: 13px;
  color: #606266;
}
</style>
