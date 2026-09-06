<template>
  <el-container class="main-layout">
    <el-aside :width="collapsed ? '56px' : '196px'" class="aside">
      <div class="brand">{{ collapsed ? '志愿' : '黑龙江投档模拟' }}</div>
      <el-menu :default-active="route.path" :collapse="collapsed" router>
        <el-menu-item v-if="store.isAdmin()" index="/admin"><el-icon><Setting /></el-icon><template #title>管理工作台</template></el-menu-item>
        <el-menu-item v-if="store.isStudent()" index="/application"><el-icon><EditPen /></el-icon><template #title>志愿填报</template></el-menu-item>
        <el-menu-item v-if="store.isStudent()" index="/results"><el-icon><Finished /></el-icon><template #title>投档结果</template></el-menu-item>
        <el-menu-item index="/excel"><el-icon><Document /></el-icon><template #title>{{ store.isAdmin() ? '数据导入导出' : '正式志愿表' }}</template></el-menu-item>
      </el-menu>
    </el-aside>
    <el-container class="body">
      <el-header class="header">
        <el-button :icon="collapsed ? Expand : Fold" text aria-label="展开或收起导航" title="展开或收起导航" @click="collapsed = !collapsed" />
        <div class="account">
          <span class="username">{{ store.username }}</span>
          <el-tag size="small" :type="store.isAdmin() ? 'warning' : 'success'">{{ store.isAdmin() ? '本机管理' : store.permanentDemo ? '固定体验' : '考生' }}</el-tag>
          <el-button v-if="!store.permanentDemo" :icon="Lock" text title="修改密码" aria-label="修改密码" @click="router.push('/change-password')" />
          <el-button :icon="SwitchButton" text title="退出登录" aria-label="退出登录" @click="handleLogout" />
        </div>
      </el-header>
      <div class="notice">模拟结果仅供测试，不代表黑龙江省招生考试院正式投档结果</div>
      <el-main class="main-content"><router-view /></el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Fold, Expand, Lock, SwitchButton } from '@element-plus/icons-vue'
import { useUserStore } from '../stores/user'
import { logout as logoutApi } from '../api'
import { confirmLeave } from '../utils/draftProtection'

const route = useRoute(), router = useRouter(), store = useUserStore()
const collapsed = ref(window.innerWidth < 768)
async function handleLogout() {
  if (!(await confirmLeave())) return
  try { await logoutApi() } catch (_) {}
  store.logout()
  router.push('/login')
}
</script>

<style scoped>
.main-layout { height: 100dvh; background: #f4f6f7; }
.aside { background: #fff; border-right: 1px solid #e3e7e9; overflow: hidden; }
.brand { height: 60px; display: flex; align-items: center; justify-content: center; font-size: 16px; font-weight: 700; color: #16776d; white-space: nowrap; }
.el-menu { border: 0; }
.el-menu--collapse { width: 56px; }
.body { min-width: 0; }
.header { display: flex; align-items: center; justify-content: space-between; border-bottom: 1px solid #e3e7e9; background: white; padding: 0 16px; }
.account { display: flex; gap: 8px; align-items: center; min-width: 0; }
.account .el-button + .el-button { margin: 0; }
.username { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-size: 14px; }
.notice { padding: 8px 20px; font-size: 12px; line-height: 1.6; color: #845711; background: #fff7df; }
.main-content { min-height: 0; padding: 24px; }
@media (max-width: 640px) {
  .header { padding: 0 4px; }
  .account { gap: 2px; }
  .username { max-width: 78px; }
  .notice { padding: 8px 12px; }
  .main-content { padding: 12px; }
}
</style>
