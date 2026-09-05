<template>
  <div class="login-container">
    <div class="login-card">
      <h2 class="login-title">高考平行志愿填报管理系统</h2>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="0">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" prefix-icon="User" size="large" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" prefix-icon="Lock" size="large"
            show-password @keyup.enter="handleLogin" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="large" :loading="loading" @click="handleLogin" style="width: 100%">登 录</el-button>
        </el-form-item>
      </el-form>
      <div class="login-tips">
        本系统仅用于黑龙江省普通高考志愿投档模拟，不代表省招考院或高校正式录取结果。
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { login } from '../api'
import { ElMessage } from 'element-plus'

const router = useRouter()
const store = useUserStore()
const formRef = ref()
const loading = ref(false)

const form = reactive({ username: '', password: '' })
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const handleLogin = async () => {
  await formRef.value.validate()
  loading.value = true
  try {
    const res = await login(form)
    store.setLogin(res.data)
    ElMessage.success('登录成功')
    router.push(res.data.mustChangePassword ? '/change-password' : '/dashboard')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container { height: 100vh; display: flex; align-items: center; justify-content: center; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); }
.login-card { width: 420px; padding: 40px; background: #fff; border-radius: 12px; box-shadow: 0 20px 60px rgba(0,0,0,0.3); }
.login-title { text-align: center; margin-bottom: 30px; color: #333; font-size: 22px; }
.login-tips { margin-top: 16px; padding: 12px; background: #f5f7fa; border-radius: 6px; font-size: 13px; color: #909399; line-height: 1.8; }
</style>
