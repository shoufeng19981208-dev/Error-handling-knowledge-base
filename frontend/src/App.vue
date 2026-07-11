<template>
  <div id="app">
    <div class="app-header">
      <div class="header-content">
        <h1 class="app-title" @click="$router.push('/')">报错处理知识库</h1>
        <div class="header-nav">
          <router-link to="/" class="nav-link">首页</router-link>
          <router-link to="/add" class="nav-link nav-link-add">+ 新增记录</router-link>
        </div>
      </div>
    </div>
    <div class="app-body">
      <router-view />
    </div>
  </div>
</template>

<script>
export default {
  name: 'App'
};
</script>

<style>
#app {
  min-height: 100vh;
  background-color: #f5f6fa;
}

.app-header {
  background: linear-gradient(135deg, #1a73e8, #0d47a1);
  color: #fff;
  padding: 0 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  position: sticky;
  top: 0;
  z-index: 100;
}

.header-content {
  max-width: 1200px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 56px;
}

.app-title {
  font-size: 20px;
  font-weight: 600;
  cursor: pointer;
  user-select: none;
}

.header-nav {
  display: flex;
  gap: 16px;
}

.nav-link {
  color: rgba(255, 255, 255, 0.9);
  text-decoration: none;
  font-size: 14px;
  padding: 6px 12px;
  border-radius: 4px;
  transition: background 0.2s;
}

.nav-link:hover {
  background: rgba(255, 255, 255, 0.15);
  color: #fff;
}

.nav-link-add {
  background: rgba(255, 255, 255, 0.2);
}

.app-body {
  max-width: 1200px;
  margin: 24px auto;
  padding: 0 24px;
}
</style>
