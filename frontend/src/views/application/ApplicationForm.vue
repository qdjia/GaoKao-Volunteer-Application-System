<template>
  <div class="page-container">
    <div class="page-header"><h2>志愿填报</h2></div>

    <el-alert v-if="!currentStudent" title="请先以学生账号登录" type="warning" show-icon :closable="false" style="margin-bottom:16px" />

    <template v-if="currentStudent">
      <el-alert
        :title="windowTitle"
        :type="applicationWindow.open ? 'success' : 'warning'"
        show-icon
        :closable="false"
        style="margin-bottom:16px"
      />
      <el-card style="margin-bottom:16px">
        <div style="display:flex;justify-content:space-between;align-items:center">
          <div>
            <span style="font-size:16px;font-weight:bold">{{ currentStudent.name }}</span>
            <el-tag style="margin-left:8px">{{ currentStudent.studentNo }}</el-tag>
            <span style="margin-left:16px;color:#909399">总分：{{ currentStudent.totalScore }}</span>
            <span style="margin-left:16px;color:#909399">选科：{{ currentStudent.subjectCombo }}</span>
          </div>
          <div>
            <el-button type="warning" @click="loadRecommend" :loading="recLoading">智能推荐</el-button>
            <el-button type="success" @click="saveDraft" :disabled="isLocked">保存草稿</el-button>
            <el-button type="primary" @click="handleSubmit" :disabled="applications.length === 0 || isLocked">正式提交</el-button>
          </div>
        </div>
      </el-card>

      <el-dialog v-model="recDialogVisible" title="智能推荐 - 冲稳保方案" width="700px">
        <el-table :data="recommendList" stripe>
          <el-table-column prop="universityName" label="大学" width="160" />
          <el-table-column prop="level" label="推荐等级" width="100">
            <template #default="{ row }">
              <el-tag :type="row.level === '冲刺' ? 'danger' : row.level === '稳妥' ? 'warning' : row.level === '保底' ? 'success' : 'info'">{{ row.level }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="minScore" label="最低分" width="90" />
          <el-table-column prop="avgScore" label="平均分" width="90" />
          <el-table-column label="操作" width="100">
            <template #default="{ row }">
              <el-button size="small" type="primary" @click="addFromRecommend(row)">添加</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-dialog>

      <el-card>
        <div style="margin-bottom:16px;display:flex;justify-content:space-between;align-items:center">
          <span>志愿列表（{{ applications.length }}/10）</span>
          <el-button type="primary" size="small" @click="addApplication" :disabled="isLocked">添加志愿</el-button>
        </div>

        <div v-for="(app, idx) in applications" :key="idx" class="application-item">
          <div class="app-header">
            <span class="app-priority">第{{ idx + 1 }}志愿</span>
            <div>
              <el-switch v-model="app.acceptAdjust" :disabled="isLocked" active-text="同意调剂" inactive-text="不同意调剂" style="margin-right:12px" />
              <el-button type="danger" size="small" text :disabled="isLocked" @click="applications.splice(idx, 1)">删除</el-button>
            </div>
          </div>
          <div class="app-body">
            <div style="margin-bottom:8px">
              <span style="font-weight:bold;margin-right:8px">大学：</span>
              <el-select v-model="app.universityId" :disabled="isLocked" placeholder="选择大学" filterable @change="onUnivChange(app)" style="width:300px">
                <el-option v-for="u in universities" :key="u.id" :label="u.name + ' (' + u.type + ')'" :value="u.id" />
              </el-select>
            </div>
            <div>
              <span style="font-weight:bold;margin-right:8px">专业：</span>
              <div v-for="(m, mi) in app.majors" :key="mi" style="display:flex;align-items:center;gap:8px;margin-bottom:6px">
                <el-tag size="small">专业{{ mi + 1 }}</el-tag>
                <el-select v-model="m.majorId" :disabled="isLocked" placeholder="选择专业" filterable style="width:260px" @change="onMajorChange(app, m)">
                  <el-option v-for="major in getAvailableMajors(app)" :key="major.id" :label="major.name" :value="major.id">
                    <span>{{ major.name }}</span>
                    <el-tag v-if="!checkSubjectOk(major)" size="small" type="danger" style="margin-left:8px">选科不符</el-tag>
                  </el-option>
                </el-select>
                <el-tag v-if="m.majorId && !m.subjectMatch" type="danger" size="small">选科不符</el-tag>
                <el-button size="small" type="danger" text :disabled="isLocked" @click="app.majors.splice(mi, 1)">移除</el-button>
              </div>
              <el-button size="small" @click="addMajor(app)" :disabled="app.majors.length >= 3 || isLocked">+ 添加专业（{{ app.majors.length }}/3）</el-button>
            </div>
          </div>
        </div>

        <el-empty v-if="applications.length === 0" description="暂未填报志愿，请点击上方添加" />
      </el-card>
    </template>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useUserStore } from '../../stores/user'
import { getStudent, getUniversities, getMajors, getApplications, getApplicationWindow, submitApplication, getRecommend, checkSubject } from '../../api'
import { ElMessage, ElMessageBox } from 'element-plus'

const store = useUserStore()
const currentStudent = ref(null)
const universities = ref([])
const applications = ref([])
const recommendList = ref([])
const recDialogVisible = ref(false)
const recLoading = ref(false)
const majorCache = ref({})
const applicationWindow = ref({ open: false, start: null, end: null, message: '正在读取填报时间' })
const hasSubmitted = computed(() => applications.value.some(app => app.status === 'SUBMITTED'))
const isLocked = computed(() => !applicationWindow.value.open || hasSubmitted.value)
const formatTime = value => value ? value.replace('T', ' ') : '--'
const windowTitle = computed(() => {
  if (hasSubmitted.value) return '志愿已正式提交并锁定'
  const window = applicationWindow.value
  return `${window.message}（${formatTime(window.start)} 至 ${formatTime(window.end)}）`
})

const loadApplicationWindow = async () => {
  const res = await getApplicationWindow()
  applicationWindow.value = res.data
}

const loadStudent = async () => {
  if (store.studentId) {
    const res = await getStudent(store.studentId)
    currentStudent.value = res.data
  }
}

const loadUniversities = async () => {
  const res = await getUniversities({})
  universities.value = res.data
}

const loadApplications = async () => {
  if (!store.studentId) return
  const res = await getApplications(store.studentId)
  if (res.data && res.data.length > 0) {
    applications.value = res.data.map(app => ({
      universityId: app.universityId,
      acceptAdjust: app.acceptAdjust || false,
      majors: (app.majors || []).map(m => ({ majorId: m.majorId, subjectMatch: true })),
      status: app.status
    }))
  }
}

const addApplication = () => {
  if (applications.value.length >= 10) { ElMessage.warning('最多填报10个志愿'); return }
  applications.value.push({ universityId: null, acceptAdjust: false, majors: [], status: 'DRAFT' })
}

const addMajor = (app) => {
  if (app.majors.length >= 3) return
  app.majors.push({ majorId: null, subjectMatch: true })
}

const onUnivChange = async (app) => {
  app.majors = []
  if (!app.universityId) return
  if (!majorCache.value[app.universityId]) {
    const res = await getMajors({ universityId: app.universityId })
    majorCache.value[app.universityId] = res.data
  }
}

const getAvailableMajors = (app) => {
  return majorCache.value[app.universityId] || []
}

const checkSubjectOk = (major) => {
  if (!currentStudent.value) return false
  const combo = currentStudent.value.subjectCombo || ''
  const primary = combo.startsWith('物') ? '物理' : combo.startsWith('史') ? '历史' : null
  if (major.subjectType && major.subjectType !== primary) return false
  if (!major.subjectReq) return true
  const subjectAliases = [
    ['物理', '物'], ['历史', '史'], ['化学', '化'],
    ['生物', '生'], ['政治', '政'], ['地理', '地']
  ]
  return subjectAliases
    .filter(([full, short]) => major.subjectReq.includes(full) || major.subjectReq.includes(short))
    .every(([, short]) => combo.includes(short))
}

const onMajorChange = async (app, m) => {
  if (m.majorId && currentStudent.value) {
    try {
      const res = await checkSubject(currentStudent.value.id, m.majorId)
      m.subjectMatch = res.data.match
    } catch { m.subjectMatch = true }
  }
}

const addFromRecommend = (row) => {
  if (isLocked.value) return
  if (applications.value.length >= 10) { ElMessage.warning('最多10个志愿'); return }
  applications.value.push({ universityId: row.universityId, acceptAdjust: false, majors: [], status: 'DRAFT' })
  ElMessage.success('已添加 ' + row.universityName)
}

const loadRecommend = async () => {
  if (!currentStudent.value) return
  recLoading.value = true
  try {
    const res = await getRecommend(currentStudent.value.id)
    recommendList.value = res.data
    recDialogVisible.value = true
  } finally { recLoading.value = false }
}

const saveDraft = async () => {
  if (!currentStudent.value) return
  await submitApplication({
    studentId: currentStudent.value.id,
    status: 'DRAFT',
    applications: applications.value.map((app, idx) => ({
      universityId: app.universityId,
      priority: idx + 1,
      acceptAdjust: app.acceptAdjust,
      majors: app.majors.map((m, mi) => ({ majorId: m.majorId, priority: mi + 1 }))
    }))
  })
  ElMessage.success('草稿已保存')
}

const handleSubmit = async () => {
  const valid = applications.value.every(a => a.universityId && a.majors.length > 0)
  if (!valid) { ElMessage.warning('请完善所有志愿的大学和专业信息'); return }
  const mismatch = applications.value.some(app => app.majors.some(major => major.subjectMatch === false))
  if (mismatch) { ElMessage.warning('存在选科要求不匹配的专业，请调整后再提交'); return }
  await ElMessageBox.confirm('系统将预检院校、专业、顺序和选科要求；提交后志愿锁定不可修改。确定提交？', '确认提交', { type: 'warning' })
  await submitApplication({
    studentId: currentStudent.value.id,
    status: 'SUBMITTED',
    applications: applications.value.map((app, idx) => ({
      universityId: app.universityId,
      priority: idx + 1,
      acceptAdjust: app.acceptAdjust,
      majors: app.majors.map((m, mi) => ({ majorId: m.majorId, priority: mi + 1 }))
    }))
  })
  ElMessage.success('志愿已提交')
  loadApplications()
}

onMounted(async () => { await Promise.all([loadStudent(), loadUniversities(), loadApplicationWindow()]); loadApplications() })
</script>

<style scoped>
.page-container { padding: 20px; }
.page-header { margin-bottom: 16px; }
.page-header h2 { margin: 0; }
.application-item { border: 1px solid #e4e7ed; border-radius: 8px; margin-bottom: 12px; overflow: hidden; }
.app-header { display: flex; justify-content: space-between; align-items: center; padding: 12px 16px; background: #f5f7fa; border-bottom: 1px solid #e4e7ed; }
.app-priority { font-weight: bold; font-size: 15px; color: #409eff; }
.app-body { padding: 16px; }
</style>
