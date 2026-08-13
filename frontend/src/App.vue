<template>
  <div id="app">
    <header class="app-header">
      <div class="header-content">
        <router-link to="/" class="app-brand">
          <span class="brand-icon">
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"/>
              <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"/>
              <line x1="8" y1="7" x2="16" y2="7"/>
              <line x1="8" y1="11" x2="14" y2="11"/>
            </svg>
          </span>
          <span class="brand-text">报错处理知识库</span>
        </router-link>
        <nav class="header-nav">
          <router-link to="/" class="nav-link" exact-active-class="nav-link--active">首页</router-link>
          <router-link to="/categories" class="nav-link" active-class="nav-link--active">分类配置</router-link>
          <router-link to="/add" class="nav-link nav-link--cta" active-class="nav-link--active">新增记录</router-link>
        </nav>
      </div>
    </header>
    <main class="app-body">
      <router-view />
    </main>
  </div>
</template>

<script>
export default {
  name: 'App'
};
</script>

<style>
/* === App Container === */
#app {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

/* === Header === */
.app-header {
  position: sticky;
  top: 0;
  z-index: 100;
  background: rgba(255, 255, 255, 0.88);
  backdrop-filter: saturate(180%) blur(16px);
  -webkit-backdrop-filter: saturate(180%) blur(16px);
  border-bottom: 1px solid var(--border-light);
}

.header-content {
  max-width: 1200px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 52px;
  padding: 0 var(--space-6);
}

/* === Brand === */
.app-brand {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  text-decoration: none;
  color: var(--text-primary);
  transition: opacity var(--transition-fast);
}

.app-brand:hover {
  opacity: 0.8;
}

.brand-icon {
  display: flex;
  align-items: center;
  color: var(--color-primary-600);
}

.brand-text {
  font-size: var(--text-lg);
  font-weight: var(--font-semibold);
  letter-spacing: -0.01em;
}

/* === Navigation === */
.header-nav {
  display: flex;
  align-items: center;
  gap: var(--space-1);
}

.nav-link {
  padding: var(--space-2) var(--space-3);
  font-size: var(--text-sm);
  font-weight: var(--font-medium);
  color: var(--text-secondary);
  text-decoration: none;
  border-radius: var(--radius-md);
  transition: all var(--transition-fast);
}

.nav-link:hover {
  color: var(--text-primary);
  background: var(--color-neutral-100);
}

.nav-link--active {
  color: var(--color-primary-600);
  background: var(--color-primary-50);
}

.nav-link--cta {
  margin-left: var(--space-2);
  background: var(--color-primary-600);
  color: #fff !important;
  padding: var(--space-2) var(--space-4);
}

.nav-link--cta:hover {
  background: var(--color-primary-700);
}

.nav-link--cta.nav-link--active {
  background: var(--color-primary-700);
}

/* === Body === */
.app-body {
  flex: 1;
  max-width: 1200px;
  width: 100%;
  margin: 0 auto;
  padding: var(--space-8) var(--space-6);
}

/* Responsive */
@media (max-width: 640px) {
  .header-content {
    padding: 0 var(--space-4);
    height: 48px;
  }
  .brand-text {
    font-size: var(--text-base);
  }
  .nav-link--cta {
    padding: var(--space-1) var(--space-3);
    font-size: var(--text-xs);
  }
  .app-body {
    padding: var(--space-4) var(--space-4);
  }
}
</style>
