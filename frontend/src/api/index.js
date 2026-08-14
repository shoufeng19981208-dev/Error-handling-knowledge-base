import axios from 'axios';

const api = axios.create({
  baseURL: '/api',
  timeout: 15000
});

api.interceptors.response.use(
  response => response.data,
  error => {
    console.error('API请求失败:', error);
    return Promise.reject(error);
  }
);

export function searchRecords(keyword, page = 0, size = 10) {
  return api.get('/error-record/search', {
    params: { keyword, page, size }
  });
}

export function getRecordById(id) {
  return api.get("/error-record/" + id);
}

export function createRecord(data) {
  return api.post('/error-record', data);
}

export function updateRecord(id, data) {
  return api.put("/error-record/" + id, data);
}

export function deleteRecord(id) {
  return api.delete("/error-record/" + id);
}

export function getPendingList() {
  return api.get('/error-record/pending');
}

export function getCategories() {
  return api.get('/error-record/categories');
}

export function matchLog(logText) {
  return api.post('/error-record/match', { logText });
}

export function extractKeywords(text) {
  return api.post('/error-record/extract-keywords', { text });
}

export function uploadScreenshot(file) {
  var formData = new FormData();
  formData.append('file', file);
  return api.post('/error-record/upload-screenshot', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  });
}

export function importRecords(file) {
  var formData = new FormData();
  formData.append('file', file);
  return api.post('/error-record/import', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 60000
  });
}

// ===== 分类配置管理 =====
export function getCategoryConfigList() {
  return api.get('/category-config');
}

export function createCategoryConfig(data) {
  return api.post('/category-config', data);
}

export function updateCategoryConfig(id, data) {
  return api.put('/category-config/' + id, data);
}

export function deleteCategoryConfig(id) {
  return api.delete('/category-config/' + id);
}

// ===== 文档管理 =====
export function getDocuments() {
  return api.get('/documents');
}

export function uploadDocument(file) {
  var formData = new FormData();
  formData.append('file', file);
  return api.post('/documents', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 120000
  });
}

export function deleteDocument(id) {
  return api.delete('/documents/' + id);
}

export function getDocumentPreview(id) {
  return api.get('/documents/' + id + '/preview', { timeout: 180000 });
}

export default api;
