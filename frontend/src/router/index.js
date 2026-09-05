import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '../stores/user'

const routes = [
  { path: '/login', name: 'Login', component: () => import('../views/Login.vue') },
  { path: '/change-password', name: 'ChangePassword', component: () => import('../views/ChangePassword.vue'), meta: { title: '修改密码' } },
  {
    path: '/',
    component: () => import('../layout/MainLayout.vue'),
    redirect: '/dashboard',
    children: [
      { path: 'excel', name: 'ExcelData', component: () => import('../views/ExcelData.vue'), meta: { title: '数据导入导出', roles: ['ADMIN', 'STUDENT'] } },
      { path: 'dashboard', name: 'Dashboard', component: () => import('../views/admission/Dashboard.vue'), meta: { title: '数据看板' } },
      { path: 'students', name: 'StudentList', component: () => import('../views/student/StudentList.vue'), meta: { title: '学生管理', roles: ['ADMIN'] } },
      { path: 'classes', name: 'ClassList', component: () => import('../views/student/ClassList.vue'), meta: { title: '班级管理', roles: ['ADMIN'] } },
      { path: 'universities', name: 'UniversityList', component: () => import('../views/university/UniversityList.vue'), meta: { title: '大学管理', roles: ['ADMIN'] } },
      { path: 'score-lines', name: 'ScoreLineList', component: () => import('../views/university/ScoreLineList.vue'), meta: { title: '分数线管理', roles: ['ADMIN'] } },
      { path: 'application', name: 'ApplicationForm', component: () => import('../views/application/ApplicationForm.vue'), meta: { title: '志愿填报', roles: ['ADMIN', 'STUDENT'] } },
      { path: 'admission', name: 'AdmissionProcess', component: () => import('../views/admission/AdmissionProcess.vue'), meta: { title: '录取分配', roles: ['ADMIN'] } },
      { path: 'admission-query', name: 'AdmissionQuery', component: () => import('../views/admission/AdmissionQuery.vue'), meta: { title: '录取查询', roles: ['ADMIN', 'STUDENT'] } },
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const store = useUserStore()
  if (to.path !== '/login') {
    if (!store.token) {
      next('/login')
    } else if (store.mustChangePassword && to.path !== '/change-password') {
      next('/change-password')
    } else if (to.meta.roles && !to.meta.roles.includes(store.role)) {
      next('/dashboard')
    } else {
      next()
    }
  } else if (store.token) {
    next(store.mustChangePassword ? '/change-password' : '/dashboard')
  } else {
    next()
  }
})

export default router
