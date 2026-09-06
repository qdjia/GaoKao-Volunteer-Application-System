import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '../stores/user'
import { confirmLeave } from '../utils/draftProtection'

const home = () => useUserStore().isAdmin() ? '/admin' : '/application'
const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', component: () => import('../views/Login.vue') },
    { path: '/change-password', component: () => import('../views/ChangePassword.vue') },
    {
      path: '/', component: () => import('../layout/MainLayout.vue'), redirect: home,
      children: [
        { path: 'admin', component: () => import('../views/workflow/AdminDesk.vue'), meta: { roles: ['ADMIN'] } },
        { path: 'application', component: () => import('../views/workflow/VolunteerEditor.vue'), meta: { roles: ['STUDENT'] } },
        { path: 'results', component: () => import('../views/workflow/CandidateResults.vue'), meta: { roles: ['STUDENT'] } },
        { path: 'excel', component: () => import('../views/ExcelData.vue'), meta: { roles: ['ADMIN', 'STUDENT'] } }
      ]
    },
    { path: '/:pathMatch(.*)*', redirect: home }
  ]
})

router.beforeEach(async (to, from) => {
  const store = useUserStore()
  if (!sessionStorage.getItem('token') && store.token) store.logout()
  if (store.token && to.path !== from.path && !(await confirmLeave())) return false
  if (!store.token) return to.path === '/login' ? true : '/login'
  if (store.permanentDemo && to.path === '/change-password') return home()
  if (store.mustChangePassword && to.path !== '/change-password') return '/change-password'
  if (to.path === '/login' || (to.meta.roles && !to.meta.roles.includes(store.role))) return home()
  return true
})
export default router
