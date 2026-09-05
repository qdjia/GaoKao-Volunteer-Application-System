<template>
  <div class="password-page">
    <div class="password-panel">
      <h2>修改密码</h2>
      <p v-if="store.mustChangePassword" class="notice">首次登录需要更换初始密码。</p>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="当前密码" prop="currentPassword">
          <el-input v-model="form.currentPassword" type="password" show-password autocomplete="current-password" />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="form.newPassword" type="password" show-password autocomplete="new-password" />
        </el-form-item>
        <el-form-item label="确认新密码" prop="confirmation">
          <el-input v-model="form.confirmation" type="password" show-password autocomplete="new-password"
            @keyup.enter="submit" />
        </el-form-item>
        <div class="actions">
          <el-button v-if="!store.mustChangePassword" @click="router.back()">取消</el-button>
          <el-button type="primary" :loading="loading" @click="submit">确认修改</el-button>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { changePassword } from '../api'
import { useUserStore } from '../stores/user'

const router = useRouter()
const store = useUserStore()
const formRef = ref()
const loading = ref(false)
const form = reactive({ currentPassword: '', newPassword: '', confirmation: '' })

const confirmPassword = (rule, value, callback) => {
  if (value !== form.newPassword) callback(new Error('两次输入的新密码不一致'))
  else callback()
}

const rules = {
  currentPassword: [{ required: true, message: '请输入当前密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 8, max: 72, message: '密码长度须为8至72位', trigger: 'blur' },
    { pattern: /^(?=.*[A-Za-z])(?=.*\d).+$/, message: '密码必须同时包含字母和数字', trigger: 'blur' }
  ],
  confirmation: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    { validator: confirmPassword, trigger: 'blur' }
  ]
}

const submit = async () => {
  await formRef.value.validate()
  loading.value = true
  try {
    await changePassword({ currentPassword: form.currentPassword, newPassword: form.newPassword })
    ElMessage.success('密码已修改，请重新登录')
    store.logout()
    router.replace('/login')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.password-page { min-height: 100vh; display: grid; place-items: center; background: #f3f5f7; padding: 24px; }
.password-panel { width: min(420px, 100%); background: #fff; border: 1px solid #dcdfe6; padding: 28px; }
.password-panel h2 { margin: 0 0 20px; font-size: 22px; color: #252a31; }
.notice { margin: -8px 0 20px; color: #b54708; font-size: 14px; }
.actions { display: flex; justify-content: flex-end; gap: 10px; }
</style>
