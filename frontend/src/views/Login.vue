<template>
  <div class="login-container">
    <div class="login-card">
      <h2 class="login-title">黑龙江投档模拟</h2>
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
    router.push(res.data.mustChangePassword ? '/change-password' : res.data.role === 'ADMIN' ? '/admin' : '/application')
  } catch (_) {
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container { min-height: 100dvh; box-sizing: border-box; padding: 20px; display: flex; align-items: center; justify-content: center; background: #eef3f4; }
.login-card { width: min(420px, 100%); box-sizing: border-box; padding: 28px; background: #fff; border: 1px solid #dce5e7; border-radius: 6px; }
.login-title { text-align: center; margin-bottom: 30px; color: #333; font-size: 22px; }
.login-tips { margin-top: 16px; padding-top: 12px; border-top: 1px solid #e3e8ea; font-size: 13px; color: #66757a; line-height: 1.8; }
</style>
