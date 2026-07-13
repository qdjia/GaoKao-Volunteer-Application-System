import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '../stores/user'

const routes = [
  { path: '/login', name: 'Login', component: () => import('../views/Login.vue') },
  { path: '/register', name: 'Register', component: () => import('../views/Register.vue') },
  {
    path: '/',
    component: () => import('../layout/MainLayout.vue'),
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', name: 'Dashboard', component: () => import('../views/admission/Dashboard.vue'), meta: { title: '数据看板' } },
      { path: 'students', name: 'StudentList', component: () => import('../views/student/StudentList.vue'), meta: { title: '学生管理' } },
      { path: 'classes', name: 'ClassList', component: () => import('../views/student/ClassList.vue'), meta: { title: '班级管理' } },
      { path: 'universities', name: 'UniversityList', component: () => import('../views/university/UniversityList.vue'), meta: { title: '大学管理' } },
      { path: 'score-lines', name: 'ScoreLineList', component: () => import('../views/university/ScoreLineList.vue'), meta: { title: '分数线管理' } },
      { path: 'application', name: 'ApplicationForm', component: () => import('../views/application/ApplicationForm.vue'), meta: { title: '志愿填报' } },
      { path: 'admission', name: 'AdmissionProcess', component: () => import('../views/admission/AdmissionProcess.vue'), meta: { title: '录取分配' } },
      { path: 'admission-query', name: 'AdmissionQuery', component: () => import('../views/admission/AdmissionQuery.vue'), meta: { title: '录取查询' } },
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  if (to.path !== '/login' && to.path !== '/register') {
    const store = useUserStore()
    if (!store.token) {
      next('/login')
    } else {
      next()
    }
  } else {
    next()
  }
})

export default router