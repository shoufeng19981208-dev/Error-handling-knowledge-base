import Vue from 'vue';
import VueRouter from 'vue-router';

Vue.use(VueRouter);

const routes = [
  {
    path: '/',
    name: 'Home',
    component: () => import('../views/Home.vue')
  },
  {
    path: '/add',
    name: 'Add',
    component: () => import('../views/AddRecord.vue')
  },
  {
    path: '/edit/:id',
    name: 'Edit',
    component: () => import('../views/EditRecord.vue')
  },
  {
    path: '/detail/:id',
    name: 'Detail',
    component: () => import('../views/Detail.vue')
  }
];

const router = new VueRouter({
  mode: 'hash',
  routes
});

export default router;
