import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '@/views/HomeView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/', name: 'home', component: HomeView },
    { path: '/login', name: 'login', component: () => import('@/views/LoginView.vue') },
    { path: '/products', name: 'products', component: () => import('@/views/ProductsView.vue') },
    { path: '/products/new', name: 'product-publish', component: () => import('@/views/ProductPublishView.vue') },
    { path: '/products/:id', name: 'product-detail', component: () => import('@/views/ProductDetailView.vue') },
    { path: '/cart', name: 'cart', component: () => import('@/views/CartView.vue') },
    { path: '/checkout', name: 'checkout', component: () => import('@/views/CheckoutView.vue') },
    { path: '/orders', name: 'orders', component: () => import('@/views/OrdersView.vue') },
    { path: '/orders/:id', name: 'order-detail', component: () => import('@/views/OrderDetailView.vue') },
    { path: '/member', name: 'member', component: () => import('@/views/member/MemberView.vue') },
    { path: '/member/addresses', name: 'addresses', component: () => import('@/views/member/AddressesView.vue') },
    { path: '/member/browse-history', name: 'browse-history', component: () => import('@/views/member/BrowseHistoryView.vue') },
    { path: '/member/listings', name: 'my-listings', component: () => import('@/views/member/MyListingsView.vue') },
    { path: '/member/reviews', name: 'reviews', component: () => import('@/views/member/ReviewsView.vue') },
    { path: '/member/balance', name: 'balance', component: () => import('@/views/member/BalanceView.vue') },
    { path: '/messages', name: 'messages', component: () => import('@/views/MessagesView.vue') },
    {
      path: '/messages/notifications',
      name: 'message-notifications',
      component: () => import('@/views/MessageNotificationsView.vue'),
    },
    {
      path: '/messages/:id',
      name: 'message-chat',
      component: () => import('@/views/MessageChatView.vue'),
    },

    // 管理后台
    {
      path: '/admin',
      component: () => import('@/views/admin/AdminLayout.vue'),
      meta: { requiresAdmin: true },
      children: [
        { path: '', name: 'admin', component: () => import('@/views/admin/DashboardView.vue') },
        { path: 'products', name: 'admin-products', component: () => import('@/views/admin/ProductsView.vue') },
        { path: 'orders', name: 'admin-orders', component: () => import('@/views/admin/OrdersView.vue') },
        { path: 'users', name: 'admin-users', component: () => import('@/views/admin/UsersView.vue') },
        { path: 'refunds', name: 'admin-refunds', component: () => import('@/views/admin/RefundsView.vue') },
      ],
    },

    { path: '/:pathMatch(.*)*', redirect: '/' },
  ],
})

// 路由守卫：管理员权限检查
router.beforeEach((to) => {
  if (to.meta.requiresAdmin) {
    const role = localStorage.getItem('mall-user-role') || ''
    if (role !== 'admin') {
      return '/login'
    }
  }
  return true
})

export default router
