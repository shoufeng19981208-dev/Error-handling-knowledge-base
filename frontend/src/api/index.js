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

export function uploadScreenshot(file) {
  var formData = new FormData();
  formData.append('file', file);
  return api.post('/error-record/upload-screenshot', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  });
}

export default api;
