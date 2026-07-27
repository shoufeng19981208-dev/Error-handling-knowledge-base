<template>
  <transition-group name="toast" tag="div" class="toast-container">
    <div
      v-for="item in messages"
      :key="item.id"
      :class="['toast-item', 'toast-' + item.type]"
      role="alert"
    >
      <span class="toast-icon">
        <template v-if="item.type === 'success'">&#10003;</template>
        <template v-else-if="item.type === 'error'">&#10007;</template>
        <template v-else-if="item.type === 'warning'">&#9888;</template>
        <template v-else>&#8505;</template>
      </span>
      <span class="toast-message">{{ item.message }}</span>
    </div>
  </transition-group>
</template>

<script>
let toastId = 0;

export default {
  name: 'Toast',
  data() {
    return {
      messages: []
    };
  },
  methods: {
    show(message, type = 'info', duration = 3000) {
      const id = ++toastId;
      this.messages.push({ id, message, type });
      setTimeout(() => {
        this.remove(id);
      }, duration);
    },
    remove(id) {
      const idx = this.messages.findIndex(m => m.id === id);
      if (idx > -1) {
        this.messages.splice(idx, 1);
      }
    },
    success(msg, duration) {
      this.show(msg, 'success', duration);
    },
    error(msg, duration) {
      this.show(msg, 'error', duration || 5000);
    },
    warning(msg, duration) {
      this.show(msg, 'warning', duration);
    },
    info(msg, duration) {
      this.show(msg, 'info', duration);
    }
  }
};
</script>

<style scoped>
.toast-container {
  position: fixed;
  top: 72px;
  right: 24px;
  z-index: 9999;
  display: flex;
  flex-direction: column;
  gap: 8px;
  pointer-events: none;
}

.toast-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 20px;
  border-radius: var(--radius-lg);
  font-size: var(--text-sm);
  font-weight: var(--font-medium);
  line-height: var(--leading-snug);
  box-shadow: var(--shadow-lg);
  pointer-events: auto;
  max-width: 360px;
  word-break: break-word;
}

.toast-icon {
  flex-shrink: 0;
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  border-radius: var(--radius-full);
}

.toast-success {
  background: var(--color-success-bg);
  color: var(--color-success);
  border: 1px solid var(--color-success-border);
}
.toast-success .toast-icon {
  background: var(--color-success);
  color: #fff;
}

.toast-error {
  background: var(--color-danger-bg);
  color: var(--color-danger);
  border: 1px solid var(--color-danger-border);
}
.toast-error .toast-icon {
  background: var(--color-danger);
  color: #fff;
}

.toast-warning {
  background: var(--color-warning-bg);
  color: var(--color-warning);
  border: 1px solid var(--color-warning-border);
}
.toast-warning .toast-icon {
  background: var(--color-warning);
  color: #fff;
}

.toast-info {
  background: var(--color-info-bg);
  color: var(--color-info);
  border: 1px solid var(--color-info-border);
}
.toast-info .toast-icon {
  background: var(--color-info);
  color: #fff;
}

/* Transition */
.toast-enter-active {
  transition: all var(--transition-base);
}
.toast-leave-active {
  transition: all var(--transition-fast);
}
.toast-enter {
  opacity: 0;
  transform: translateX(40px) scale(0.95);
}
.toast-leave-to {
  opacity: 0;
  transform: translateX(20px) scale(0.98);
}
</style>
