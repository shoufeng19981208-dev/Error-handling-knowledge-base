<template>
  <div class="skeleton-list" :style="{ '--count': count }">
    <div v-for="i in count" :key="i" class="skeleton-card">
      <div class="skeleton-line skeleton-line-title"></div>
      <div class="skeleton-line skeleton-line-body"></div>
      <div class="skeleton-line skeleton-line-body skeleton-line-short"></div>
      <div class="skeleton-footer">
        <div class="skeleton-tag"></div>
        <div class="skeleton-tag"></div>
        <div class="skeleton-tag"></div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'LoadingSkeleton',
  props: {
    count: {
      type: Number,
      default: 5
    }
  }
};
</script>

<style scoped>
.skeleton-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.skeleton-card {
  background: var(--surface-card);
  border-radius: var(--radius-lg);
  padding: var(--space-5) var(--space-6);
  border: 1px solid var(--border-light);
}

.skeleton-line {
  height: 14px;
  background: var(--color-neutral-150);
  border-radius: var(--radius-sm);
  margin-bottom: var(--space-3);
  animation: shimmer 1.8s ease-in-out infinite;
  background: linear-gradient(
    90deg,
    var(--color-neutral-150) 25%,
    var(--color-neutral-100) 50%,
    var(--color-neutral-150) 75%
  );
  background-size: 200% 100%;
}

.skeleton-line-title {
  width: 60%;
  height: 16px;
  margin-bottom: var(--space-4);
}

.skeleton-line-body {
  width: 100%;
}

.skeleton-line-short {
  width: 75%;
}

.skeleton-footer {
  display: flex;
  gap: var(--space-2);
  margin-top: var(--space-4);
}

.skeleton-tag {
  width: 56px;
  height: 22px;
  background: var(--color-neutral-150);
  border-radius: var(--radius-sm);
  animation: shimmer 1.8s ease-in-out infinite;
  background: linear-gradient(
    90deg,
    var(--color-neutral-150) 25%,
    var(--color-neutral-100) 50%,
    var(--color-neutral-150) 75%
  );
  background-size: 200% 100%;
}

@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}
</style>
