<template>
  <div class="register-container">
    <div class="register-card">
      <h2 class="register-title">用户注册</h2>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px" size="large">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名（学生即学号）" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="form.confirmPassword" type="password" placeholder="请再次输入密码" show-password />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-radio-group v-model="form.role">
            <el-radio value="STUDENT">学生</el-radio>
            <el-radio value="TEACHER">教师</el-radio>
          </el-radio-group>
        </el-form-item>
        <template v-if="form.role === 'STUDENT'">
          <el-form-item label="姓名" prop="name">
            <el-input v-model="form.name" placeholder="请输入真实姓名" />
          </el-form-item>
          <el-form-item label="性别" prop="gender">
            <el-radio-group v-model="form.gender">
              <el-radio value="男">男</el-radio>
              <el-radio value="女">女</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="省份" prop="provinceId">
            <el-select v-model="form.provinceId" placeholder="请选择省份" filterable style="width: 100%">
              <el-option v-for="p in provinces" :key="p.id" :label="p.name" :value="p.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="选科组合" prop="subjectCombo">
            <el-select v-model="form.subjectCombo" placeholder="请选择选科组合" style="width: 100%">
              <el-option value="物化生" />
              <el-option value="物化地" />
              <el-option value="物政生" />
              <el-option value="物政地" />
              <el-option value="史政地" />
              <el-option value="史政生" />
              <el-option value="史化地" />
              <el-option value="史化生" />
            </el-select>
          </el-form-item>
          <el-form-item label="联系电话" prop="phone">
            <el-input v-model="form.phone" placeholder="请输入手机号" />
          </el-form-item>
        </template>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="handleRegister" style="width: 100%">注 册</el-button>
        </el-form-item>
      </el-form>
      <div class="register-footer">
        已有账号？<el-link type="primary" @click="router.push('/login')">返回登录</el-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { register, getProvinces } from '../api'
import { ElMessage } from 'element-plus'

const router = useRouter()
const formRef = ref()
const loading = ref(false)
const provinces = ref([])

const form = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  role: 'STUDENT',
  name: '',
  gender: '男',
  provinceId: null,
  subjectCombo: '',
  phone: ''
})

const validateConfirm = (rule, value, callback) => {
  if (value !== form.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirm, trigger: 'blur' }
  ],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }],
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  gender: [{ required: true, message: '请选择性别', trigger: 'change' }],
  provinceId: [{ required: true, message: '请选择省份', trigger: 'change' }],
  subjectCombo: [{ required: true, message: '请选择选科组合', trigger: 'change' }]
}

onMounted(async () => {
  try {
    const res = await getProvinces()
    provinces.value = res.data || []
  } catch (e) { /* ignore */ }
})

const handleRegister = async () => {
  await formRef.value.validate()
  loading.value = true
  try {
    const data = {
      username: form.username,
      password: form.password,
      role: form.role
    }
    if (form.role === 'STUDENT') {
      data.name = form.name
      data.gender = form.gender
      data.provinceId = form.provinceId
      data.subjectCombo = form.subjectCombo
      data.phone = form.phone
    }
    await register(data)
    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.register-container { height: 100vh; display: flex; align-items: center; justify-content: center; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); overflow-y: auto; padding: 20px 0; }
.register-card { width: 520px; padding: 36px; background: #fff; border-radius: 12px; box-shadow: 0 20px 60px rgba(0,0,0,0.3); }
.register-title { text-align: center; margin-bottom: 24px; color: #333; font-size: 22px; }
.register-footer { text-align: center; margin-top: 12px; font-size: 14px; color: #666; }
</style>