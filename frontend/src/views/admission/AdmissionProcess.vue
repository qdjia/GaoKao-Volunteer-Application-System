<template>
  <div class="page-container">
    <div class="page-header"><h2>录取分配</h2></div>

    <el-card style="margin-bottom:16px">
      <el-alert title="点击下方按钮执行平行志愿录取分配，系统将按分数优先、遵循志愿原则自动录取" type="info" show-icon :closable="false" style="margin-bottom:16px" />
      <el-button type="danger" size="large" @click="execute" :loading="loading" style="width:200px;height:50px;font-size:18px">
        执行录取分配
      </el-button>
    </el-card>

    <el-card v-if="result">
      <el-result :icon="result.includes('完成') ? 'success' : 'error'" :title="result" />
    </el-card>

    <el-card style="margin-top:16px">
      <template #header><span>录取日志</span></template>
      <el-table :data="logs" stripe border max-height="500">
        <el-table-column prop="studentName" label="学生" width="100" />
        <el-table-column prop="universityName" label="大学" width="140" />
        <el-table-column prop="majorName" label="专业" width="160" />
        <el-table-column prop="action" label="动作" width="130">
          <template #default="{ row }">
            <el-tag :type="getActionType(row.action)" size="small">{{ getActionLabel(row.action) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="detail" label="详情" />
        <el-table-column prop="createdAt" label="时间" width="180" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { executeAdmission, getAdmissionLogs } from '../../api'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const result = ref('')
const logs = ref([])

const execute = async () => {
  await ElMessageBox.confirm('执行录取分配将清除之前的录取结果，确定继续？', '确认', { type: 'warning' })
  loading.value = true
  try {
    const res = await executeAdmission()
    result.value = res.data.message
    ElMessage.success('录取分配完成')
    loadLogs()
  } finally { loading.value = false }
}

const loadLogs = async () => { const res = await getAdmissionLogs(); logs.value = res.data }

const getActionType = (action) => {
  const map = { ADMITTED: 'success', ADMITTED_ADJUST: 'warning', REJECT: 'danger', SKIP: 'info', NO_APPLICATION: 'info' }
  return map[action] || 'info'
}
const getActionLabel = (action) => {
  const map = { ADMITTED: '录取', ADMITTED_ADJUST: '调剂录取', REJECT: '退档', SKIP: '跳过', NO_APPLICATION: '未填报' }
  return map[action] || action
}

onMounted(() => { loadLogs() })
</script>

<style scoped>
.page-container { padding: 20px; }
.page-header { margin-bottom: 16px; }
.page-header h2 { margin: 0; }
</style>