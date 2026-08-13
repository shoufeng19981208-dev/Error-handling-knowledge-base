<template>
  <div class="category-page">
    <div class="category-header">
      <div>
        <h1 class="page-title">所属分类配置</h1>
        <p class="page-desc">管理报错记录的所属分类，新增/编辑报错时可从配置的分类中选择</p>
      </div>
      <button type="button" class="btn-primary" @click="openCreate">+ 新增分类</button>
    </div>

    <div v-if="loading" class="category-loading">加载中...</div>

    <div v-else class="category-table-wrap">
      <table class="category-table">
        <thead>
          <tr>
            <th>排序</th>
            <th>分类名称</th>
            <th>描述</th>
            <th>记录数</th>
            <th>状态</th>
            <th>更新时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in categories" :key="item.id">
            <td class="cell-center">
              <button class="icon-btn" title="上移" :disabled="item.sortOrder === 0" @click="move(item, -1)">↑</button>
              <span class="sort-num">{{ item.sortOrder }}</span>
              <button class="icon-btn" title="下移" :disabled="item.sortOrder === maxSort" @click="move(item, 1)">↓</button>
            </td>
            <td class="cell-name">{{ item.name }}</td>
            <td class="cell-desc">{{ item.description || '—' }}</td>
            <td class="cell-center">{{ item.recordCount }}</td>
            <td class="cell-center">
              <span :class="['status-badge', item.enabled ? 'status-on' : 'status-off']">
                {{ item.enabled ? '启用' : '停用' }}
              </span>
            </td>
            <td class="cell-time">{{ formatTime(item.updateTime) }}</td>
            <td class="cell-actions">
              <button class="btn-mini" @click="openEdit(item)">编辑</button>
              <button class="btn-mini btn-mini--danger" @click="remove(item)">删除</button>
            </td>
          </tr>
          <tr v-if="!categories.length">
            <td colspan="7" class="cell-empty">暂无分类，点击右上角「新增分类」创建</td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 新增/编辑弹窗 -->
    <div v-if="showForm" class="modal-mask" @click.self="closeForm">
      <div class="modal-body">
        <h2 class="modal-title">{{ form.id ? '编辑分类' : '新增分类' }}</h2>
        <div class="form-group">
          <label class="form-label">分类名称 <span class="form-required">*</span></label>
          <input v-model="form.name" class="form-input" maxlength="100" placeholder="例如：TO层报错" />
        </div>
        <div class="form-group">
          <label class="form-label">描述</label>
          <input v-model="form.description" class="form-input" maxlength="500" placeholder="分类用途说明（可选）" />
        </div>
        <div class="form-group form-group--row">
          <div class="form-group">
            <label class="form-label">排序号</label>
            <input v-model.number="form.sortOrder" type="number" min="0" class="form-input form-input--small" />
          </div>
          <div class="form-group">
            <label class="form-label">状态</label>
            <select v-model="form.enabled" class="form-input form-input--small">
              <option :value="true">启用</option>
              <option :value="false">停用</option>
            </select>
          </div>
        </div>
        <div class="modal-actions">
          <button type="button" class="btn-plain" @click="closeForm">取消</button>
          <button type="button" class="btn-primary" :disabled="saving" @click="save">
            {{ saving ? '保存中...' : '保存' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { getCategoryConfigList, createCategoryConfig, updateCategoryConfig, deleteCategoryConfig } from '../api/index';

export default {
  name: 'CategoryManage',
  data() {
    return {
      loading: true,
      saving: false,
      categories: [],
      showForm: false,
      form: { id: null, name: '', description: '', sortOrder: 0, enabled: true }
    };
  },
  computed: {
    maxSort() {
      return this.categories.length ? this.categories[this.categories.length - 1].sortOrder : 0;
    }
  },
  created() {
    this.fetchList();
  },
  methods: {
    async fetchList() {
      this.loading = true;
      try {
        this.categories = (await getCategoryConfigList()) || [];
      } catch (e) {
        this.$root.$emit('toast', { message: '加载分类失败: ' + (e.response?.data?.message || e.message), type: 'error' });
      } finally {
        this.loading = false;
      }
    },
    openCreate() {
      this.form = { id: null, name: '', description: '', sortOrder: this.maxSort + 1, enabled: true };
      this.showForm = true;
    },
    openEdit(item) {
      this.form = { id: item.id, name: item.name, description: item.description || '', sortOrder: item.sortOrder, enabled: item.enabled };
      this.showForm = true;
    },
    closeForm() {
      this.showForm = false;
    },
    async save() {
      if (!this.form.name || !this.form.name.trim()) {
        this.$root.$emit('toast', { message: '请填写分类名称', type: 'warning' });
        return;
      }
      this.saving = true;
      try {
        const payload = {
          name: this.form.name.trim(),
          description: this.form.description,
          sortOrder: this.form.sortOrder || 0,
          enabled: this.form.enabled
        };
        const res = this.form.id
          ? await updateCategoryConfig(this.form.id, payload)
          : await createCategoryConfig(payload);
        if (res.success) {
          this.$root.$emit('toast', { message: this.form.id ? '分类已更新' : '分类已新增', type: 'success' });
          this.showForm = false;
          this.fetchList();
        } else {
          this.$root.$emit('toast', { message: res.message || '保存失败', type: 'error' });
        }
      } catch (e) {
        this.$root.$emit('toast', { message: '保存失败: ' + (e.response?.data?.message || e.message), type: 'error' });
      } finally {
        this.saving = false;
      }
    },
    async remove(item) {
      if (!window.confirm('确定删除分类「' + item.name + '」吗？')) return;
      try {
        const res = await deleteCategoryConfig(item.id);
        if (res.success) {
          this.$root.$emit('toast', { message: '分类已删除', type: 'success' });
          this.fetchList();
        } else {
          this.$root.$emit('toast', { message: res.message || '删除失败', type: 'error' });
        }
      } catch (e) {
        this.$root.$emit('toast', { message: '删除失败: ' + (e.response?.data?.message || e.message), type: 'error' });
      }
    },
    async move(item, delta) {
      const target = this.categories.find(c => c.sortOrder === item.sortOrder + delta);
      if (!target) return;
      const orderA = item.sortOrder;
      const orderB = target.sortOrder;
      try {
        await updateCategoryConfig(item.id, { name: item.name, sortOrder: orderB, enabled: item.enabled });
        await updateCategoryConfig(target.id, { name: target.name, sortOrder: orderA, enabled: target.enabled });
        this.fetchList();
      } catch (e) {
        this.$root.$emit('toast', { message: '调整排序失败', type: 'error' });
      }
    },
    formatTime(t) {
      if (!t) return '—';
      return String(t).replace('T', ' ').substring(0, 19);
    }
  }
};
</script>

<style scoped>
.category-page {
  max-width: 980px;
  margin: 0 auto;
  padding: 24px 16px 48px;
}
.category-header {
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
}
.category-loading {
  padding: 48px 0;
  text-align: center;
  color: #9ca3af;
}
.category-table-wrap {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  overflow: hidden;
}
.category-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
}
.category-table th {
  background: #f9fafb;
  padding: 12px 14px;
  text-align: left;
  font-weight: 600;
  color: #374151;
  border-bottom: 1px solid #e5e7eb;
  white-space: nowrap;
}
.category-table td {
  padding: 12px 14px;
  border-bottom: 1px solid #f3f4f6;
  vertical-align: middle;
}
.category-table tr:last-child td {
  border-bottom: none;
}
.cell-center {
  text-align: center;
}
.cell-name {
  font-weight: 600;
}
.cell-desc {
  color: #6b7280;
  max-width: 260px;
}
.cell-time {
  color: #9ca3af;
  font-size: 13px;
  white-space: nowrap;
}
.cell-empty {
  text-align: center;
  color: #9ca3af;
  padding: 36px 0;
}
.status-badge {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 999px;
  font-size: 12px;
}
.status-on {
  background: #dcfce7;
  color: #15803d;
}
.status-off {
  background: #f3f4f6;
  color: #6b7280;
}
.icon-btn {
  width: 26px;
  height: 26px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  background: #fff;
  color: #374151;
  cursor: pointer;
}
.icon-btn:disabled {
  opacity: 0.35;
  cursor: not-allowed;
}
.sort-num {
  display: inline-block;
  min-width: 22px;
  text-align: center;
  font-size: 13px;
  color: #6b7280;
}
.btn-mini {
  padding: 4px 12px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  background: #fff;
  font-size: 13px;
  color: #374151;
  cursor: pointer;
  margin-right: 6px;
}
.btn-mini--danger {
  color: #dc2626;
  border-color: #fecaca;
}
.btn-primary {
  padding: 9px 18px;
  border: none;
  border-radius: 8px;
  background: #4f46e5;
  color: #fff;
  font-size: 14px;
  cursor: pointer;
}
.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.btn-plain {
  padding: 9px 18px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  font-size: 14px;
  cursor: pointer;
}
.modal-mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}
.modal-body {
  width: 440px;
  max-width: 92vw;
  background: #fff;
  border-radius: 14px;
  padding: 24px;
}
.modal-title {
  margin: 0 0 16px;
  font-size: 18px;
}
.form-group {
  margin-bottom: 14px;
}
.form-group--row {
  display: flex;
  gap: 12px;
}
.form-group--row .form-group {
  flex: 1;
}
.form-label {
  display: block;
  margin-bottom: 6px;
  font-size: 13px;
  font-weight: 600;
  color: #374151;
}
.form-required {
  color: #dc2626;
}
.form-input {
  width: 100%;
  padding: 9px 12px;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  font-size: 14px;
  box-sizing: border-box;
}
.form-input--small {
  width: 100%;
}
.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 18px;
}
</style>
