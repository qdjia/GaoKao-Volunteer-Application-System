<template>
  <el-container class="main-layout">
    <el-aside :width="isCollapse ? '64px' : '220px'" class="aside">
      <div class="logo">
        <h3 v-show="!isCollapse">志愿填报系统</h3>
        <h3 v-show="isCollapse">志愿</h3>
      </div>
      <el-menu :default-active="route.path" :collapse="isCollapse" router background-color="#304156"
        text-color="#bfcbd9" active-text-color="#409EFF">
        <el-menu-item index="/excel">
          <el-icon><Document /></el-icon>
          <template #title>{{ store.isAdmin() ? '数据导入导出' : '正式志愿表' }}</template>
        </el-menu-item>
        <el-menu-item index="/dashboard">
          <el-icon><DataAnalysis /></el-icon>
          <template #title>数据看板</template>
        </el-menu-item>
        <el-sub-menu index="student-mgr" v-if="store.isAdmin()">
          <template #title><el-icon><User /></el-icon><span>学生管理</span></template>
          <el-menu-item index="/students">学生信息</el-menu-item>
          <el-menu-item index="/classes">班级管理</el-menu-item>
        </el-sub-menu>
        <el-sub-menu index="univ-mgr" v-if="store.isAdmin()">
          <template #title><el-icon><School /></el-icon><span>院校管理</span></template>
          <el-menu-item index="/universities">大学院系</el-menu-item>
          <el-menu-item index="/score-lines">分数线</el-menu-item>
        </el-sub-menu>
        <el-menu-item index="/application">
          <el-icon><EditPen /></el-icon>
          <template #title>志愿填报</template>
        </el-menu-item>
        <el-sub-menu index="admission-mgr">
          <template #title><el-icon><Finished /></el-icon><span>录取管理</span></template>
          <el-menu-item index="/admission" v-if="store.isAdmin()">录取分配</el-menu-item>
          <el-menu-item index="/admission-query">录取查询</el-menu-item>
        </el-sub-menu>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <el-icon class="collapse-btn" @click="isCollapse = !isCollapse">
          <Fold v-if="!isCollapse" /><Expand v-else />
        </el-icon>
        <div class="header-right">
          <span class="role-tag">
            <el-tag :type="store.isAdmin() ? 'danger' : store.isStudent() ? 'success' : 'warning'" size="small">
              {{ store.isAdmin() ? '管理员' : '考生' }}
            </el-tag>
          </span>
          <span class="username">{{ store.username }}</span>
          <el-button text @click="router.push('/change-password')">修改密码</el-button>
          <el-button type="danger" text @click="handleLogout">退出</el-button>
        </div>
      </el-header>
      <el-main class="main-content">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { logout as logoutApi } from '../api'

const route = useRoute()
const router = useRouter()
const store = useUserStore()
const isCollapse = ref(window.innerWidth < 768)

const handleLogout = async () => {
  try { await logoutApi() } catch (e) {}
  store.logout()
  router.push('/login')
}
</script>

<style scoped>
.main-layout { height: 100vh; }
.aside { background: #304156; transition: width 0.3s; overflow: hidden; }
.logo { height: 60px; display: flex; align-items: center; justify-content: center; color: #fff; background: #263445; }
.logo h3 { margin: 0; font-size: 16px; white-space: nowrap; }
.header { display: flex; align-items: center; justify-content: space-between; border-bottom: 1px solid #e6e6e6; background: #fff; }
.collapse-btn { font-size: 20px; cursor: pointer; }
.header-right { display: flex; align-items: center; gap: 12px; }
.username { font-size: 14px; color: #333; }
.main-content { background: #f0f2f5; min-height: 0; overflow-y: auto; }
.main-layout > .el-container { min-width: 0; }
@media (max-width: 640px) {
  .main-content { padding: 10px; }
  .header { padding: 0 10px; }
  .header-right { gap: 4px; }
  .username { max-width: 80px; overflow: hidden; text-overflow: ellipsis; }
  .role-tag { display: none; }
}
</style>
