import Vue from 'vue';
import App from './App.vue';
import router from './router';
import Toast from './components/Toast.vue';
import './assets/variables.css';

Vue.config.productionTip = false;

// 全局 Toast 事件总线
Vue.prototype.$toast = function(message, type, duration) {
  this.$root.$emit('toast', { message, type, duration });
};

const app = new Vue({
  router,
  render: h => h(App)
}).$mount('#app');

// Mount Toast component on app
const ToastComponent = Vue.extend(Toast);
const toastInstance = new ToastComponent().$mount();
document.body.appendChild(toastInstance.$el);

// Listen for toast events
app.$on('toast', ({ message, type, duration }) => {
  if (toastInstance[type]) {
    toastInstance[type](message, duration);
  } else {
    toastInstance.show(message, type, duration);
  }
});
