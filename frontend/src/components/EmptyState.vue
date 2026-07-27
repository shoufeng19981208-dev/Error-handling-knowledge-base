<template>
  <div class="empty-state">
    <div class="empty-icon">
      <svg width="64" height="64" viewBox="0 0 64 64" fill="none" xmlns="http://www.w3.org/2000/svg">
        <rect x="12" y="8" width="40" height="48" rx="4" stroke="currentColor" stroke-width="1.5" fill="none"/>
        <line x1="20" y1="20" x2="44" y2="20" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" opacity="0.4"/>
        <line x1="20" y1="28" x2="38" y2="28" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" opacity="0.3"/>
        <line x1="20" y1="36" x2="34" y2="36" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" opacity="0.2"/>
        <circle cx="46" cy="44" r="10" stroke="currentColor" stroke-width="1.5" fill="none"/>
        <line x1="52.5" y1="50.5" x2="56" y2="54" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
      </svg>
    </div>
    <p class="empty-title">{{ title }}</p>
    <p v-if="description" class="empty-description">{{ description }}</p>
    <slot />
  </div>
</template>

<script>
export default {
  name: 'EmptyState',
  props: {
    title: {
      type: String,
      default: '暂无数据'
    },
    description: {
      type: String,
      default: ''
    }
  }
};
</script>

<style scoped>
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: var(--space-16) var(--space-6);
  text-align: center;
}

.empty-icon {
  color: var(--color-neutral-300);
  margin-bottom: var(--space-6);
}

.empty-title {
  font-size: var(--text-lg);
  font-weight: var(--font-semibold);
  color: var(--text-secondary);
  margin-bottom: var(--space-2);
}

.empty-description {
  font-size: var(--text-sm);
  color: var(--text-tertiary);
  max-width: 320px;
  line-height: var(--leading-relaxed);
  margin-bottom: var(--space-4);
}
</style>
