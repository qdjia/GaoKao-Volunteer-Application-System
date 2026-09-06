<template>
  <section v-loading="loading" class="desk">
    <div class="heading"><h2>管理工作台</h2><div class="status"><el-tag :type="overview.mode === 'DEMO' ? 'warning' : 'success'">{{ overview.mode === 'DEMO' ? '体验模式' : '正式模式' }}</el-tag><span>在线考生 {{ online ?? '暂不可用' }}</span><el-button :icon="Refresh" circle title="刷新工作台" aria-label="刷新工作台" :disabled="busy" @click="refresh" /></div></div>
    <el-alert v-if="failed" title="工作台加载失败，请刷新重试" type="error" :closable="false" />
    <div class="toolbar">
      <el-select v-model="batchId" placeholder="选择招生批次" aria-label="招生批次" class="batch" :disabled="busy" @change="selectBatch"><el-option v-for="b in overview.batches" :key="b.id" :value="b.id" :label="`${b.admission_year} ${b.name}`" /></el-select>
      <el-button :icon="Upload" @click="router.push('/excel')">数据导入导出</el-button>
    </div>
    <el-tabs v-model="tab">
      <el-tab-pane label="填报设置" name="window">
        <el-empty v-if="!batch" description="暂无招生批次" />
        <el-form v-else class="window-form" label-position="top" :disabled="busy">
          <div class="form-grid">
            <el-form-item label="填报开始时间"><el-date-picker v-model="windowForm.startsAt" type="datetime" aria-label="填报开始时间" placeholder="开始时间" /></el-form-item>
            <el-form-item label="填报截止时间"><el-date-picker v-model="windowForm.endsAt" type="datetime" aria-label="填报截止时间" placeholder="截止时间" /></el-form-item>
            <el-form-item label="物理类控制线"><el-input-number v-model="windowForm.physicsLine" :min="0" :max="750" :precision="2" :step="1" aria-label="物理类控制线" /></el-form-item>
            <el-form-item label="历史类控制线"><el-input-number v-model="windowForm.historyLine" :min="0" :max="750" :precision="2" :step="1" aria-label="历史类控制线" /></el-form-item>
          </div>
          <el-button type="primary" :icon="Check" :loading="busy" @click="saveWindow">保存填报设置</el-button>
        </el-form>
      </el-tab-pane>
      <el-tab-pane label="招生计划" name="plans">
        <div class="toolbar"><el-input v-model="planSearch" :prefix-icon="Search" clearable placeholder="院校名称或专业组代码" aria-label="搜索招生计划" class="search" /><el-segmented v-model="category" :options="[{ label: '全部', value: '' }, { label: '物理类', value: 'PHYSICS' }, { label: '历史类', value: 'HISTORY' }]" /></div>
        <el-table :data="filteredPlans" max-height="540" empty-text="暂无计划">
          <el-table-column prop="institution_name" label="院校" min-width="180" />
          <el-table-column label="专业组" min-width="180"><template #default="{ row }">{{ row.group_code }} {{ row.group_name }}</template></el-table-column>
          <el-table-column label="科类" width="90"><template #default="{ row }">{{ categoryName(row.category_code) }}</template></el-table-column>
          <el-table-column label="选科要求" min-width="110"><template #default="{ row }">{{ row.requiredSubjects.map(subjectName).join('、') || '不限' }}</template></el-table-column>
          <el-table-column label="计划人数" width="165"><template #default="{ row }"><el-input-number v-model="row.planned_count" :min="1" :max="2147483647" :precision="0" :disabled="busy" aria-label="计划人数" /></template></el-table-column>
          <el-table-column label="投档比例" width="165"><template #default="{ row }"><el-input-number v-model="row.filing_ratio" :min="1" :max="1.05" :step="0.01" :precision="4" :disabled="busy" aria-label="投档比例" /></template></el-table-column>
          <el-table-column label="保存" width="70"><template #default="{ row }"><el-button :icon="Check" circle title="保存计划" aria-label="保存计划" :disabled="busy" @click="savePlan(row)" /></template></el-table-column>
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="考生与版本" name="candidates">
        <div class="toolbar"><el-input v-model="candidateSearch" :prefix-icon="Search" clearable placeholder="姓名或准考证号" aria-label="搜索考生" class="search" /></div>
        <el-table :data="filteredCandidates" max-height="540" empty-text="暂无考生">
          <el-table-column prop="exam_number" label="准考证号" min-width="130" /><el-table-column prop="name" label="姓名" min-width="120" />
          <el-table-column label="科类" width="90"><template #default="{ row }">{{ categoryName(row.category_code) }}</template></el-table-column>
          <el-table-column prop="culture_total" label="文化总分" width="90" /><el-table-column prop="final_rank" label="位次" width="90" />
          <el-table-column label="来源" width="90"><template #default="{ row }">{{ row.data_origin === 'DEMO' ? '体验' : '正式' }}</template></el-table-column>
          <el-table-column label="账号状态" min-width="150"><template #default="{ row }"><el-switch :model-value="row.account_status === 'ACTIVE'" active-text="启用" inactive-text="禁用" :disabled="busy || !row.user_id" @change="toggleAccount(row, $event)" /></template></el-table-column>
          <el-table-column label="提交版本" width="95"><template #default="{ row }"><el-button :icon="Document" circle title="查看全部提交版本" aria-label="查看全部提交版本" :disabled="!batchId" @click="viewVersions(row)" /></template></el-table-column>
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="投档运行" name="runs">
        <div class="toolbar"><el-button type="primary" :icon="VideoPlay" :loading="busy" :disabled="!batchId || !batch?.endsAt || Date.parse(batch.endsAt) > now" @click="execute">执行本批次投档</el-button><span class="muted">截止时间：{{ formatTime(batch?.endsAt) }}</span></div>
        <el-table :data="batchRuns" max-height="520" empty-text="暂无投档运行">
          <el-table-column prop="id" label="运行 ID" width="90" /><el-table-column prop="run_no" label="版本" width="75" />
          <el-table-column label="状态" width="100"><template #default="{ row }">{{ runStatus[row.status] || row.status }}</template></el-table-column>
          <el-table-column label="完成时间" min-width="190"><template #default="{ row }">{{ formatTime(row.completed_at) }}</template></el-table-column>
          <el-table-column label="结果与导出" min-width="120"><template #default="{ row }"><el-button :icon="Search" circle title="查看投档结果" aria-label="查看投档结果" @click="viewRun(row)" /><el-button :icon="Download" circle title="导出运行结果" aria-label="导出运行结果" :disabled="row.status !== 'COMPLETED'" @click="downloadRun(row.id)" /></template></el-table-column>
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="操作审计" name="audit">
        <el-table :data="overview.audit || []" max-height="540" empty-text="暂无操作记录">
          <el-table-column label="时间" min-width="190"><template #default="{ row }">{{ formatTime(row.created_at) }}</template></el-table-column>
          <el-table-column prop="operator_user_id" label="操作账号 ID" width="120" />
          <el-table-column label="操作" min-width="130"><template #default="{ row }">{{ actions[row.action] || row.action }}</template></el-table-column>
          <el-table-column prop="target_id" label="对象 ID" width="90" />
          <el-table-column label="详情" width="80"><template #default="{ row }"><el-button :icon="Search" circle title="查看变更记录" aria-label="查看变更记录" @click="auditRow = row; auditVisible = true" /></template></el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>
    <div v-if="overview.mode === 'DEMO'" class="reset-area"><h3>体验数据</h3><el-button type="danger" plain :icon="Delete" :disabled="busy" @click="resetDemo">彻底删除体验数据</el-button></div>
    <el-dialog v-model="versionsVisible" :title="`${selectedCandidate?.name || ''} · 提交版本`" width="min(760px, 96vw)">
      <el-table :data="versions" empty-text="暂无正式提交"><el-table-column prop="version_no" label="版本" width="80" /><el-table-column label="提交时间" min-width="180"><template #default="{ row }">{{ formatTime(row.submitted_at) }}</template></el-table-column><el-table-column label="查看" width="80"><template #default="{ row }"><el-button :icon="Document" circle title="查看志愿表" aria-label="查看志愿表" @click="viewSubmission(row.id)" /></template></el-table-column></el-table>
    </el-dialog>
    <SubmissionSheet v-model="sheetVisible" :data="sheet" />
    <el-dialog v-model="resultsVisible" :title="`第 ${selectedRun?.run_no || ''} 次投档结果`" width="min(1200px, 96vw)"><FilingResults :rows="results" @trace="viewTrace" /></el-dialog>
    <el-dialog v-model="traceVisible" title="考生检索轨迹" width="min(1000px, 96vw)"><FilingTrace :rows="traces" /></el-dialog>
    <el-dialog v-model="auditVisible" title="变更记录" width="min(800px, 96vw)"><h3>修改前</h3><pre>{{ pretty(auditRow?.before_json) }}</pre><h3>修改后</h3><pre>{{ pretty(auditRow?.after_json) }}</pre></el-dialog>
  </section>
</template>

<script setup>
import { computed, onMounted, onBeforeUnmount, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Search, Check, Upload, Document, VideoPlay, Download, Delete } from '@element-plus/icons-vue'
import request from '../../utils/request'
import { downloadExcel } from '../../api/excel'
import { categoryName, subjectName, formatTime } from '../../utils/workflow'
import SubmissionSheet from '../../components/SubmissionSheet.vue'
import FilingResults from '../../components/FilingResults.vue'
import FilingTrace from '../../components/FilingTrace.vue'
const router = useRouter(), overview = ref({}), batchId = ref(null), tab = ref('window'), plans = ref([]), windowForm = ref({})
const loading = ref(false), busy = ref(false), failed = ref(false), online = ref(null), now = ref(Date.now())
const planSearch = ref(''), candidateSearch = ref(''), category = ref('')
const versionsVisible = ref(false), versions = ref([]), selectedCandidate = ref(null), sheetVisible = ref(false), sheet = ref(null)
const resultsVisible = ref(false), results = ref([]), selectedRun = ref(null), traceVisible = ref(false), traces = ref([]), auditVisible = ref(false), auditRow = ref(null)
let timer, ticker, onlinePending = false, timeOffset = 0
const runStatus = { COMPLETED: '已完成', RUNNING: '运行中', CREATED: '待运行', FAILED: '失败' }
const actions = { BATCH_CONFIGURED: '批次设置', PLAN_CHANGED: '计划变更', ADMISSION_EXECUTED: '执行投档', DEMO_RESET: '体验重置' }
const batch = computed(() => overview.value.batches?.find(b => b.id === batchId.value))
const batchRuns = computed(() => overview.value.runs?.filter(r => r.admission_batch_id === batchId.value) || [])
const filteredPlans = computed(() => plans.value.filter(p => (!category.value || p.category_code === category.value) && `${p.institution_name} ${p.group_name} ${p.group_code}`.includes(planSearch.value.trim())))
const filteredCandidates = computed(() => overview.value.candidates?.filter(c => `${c.name} ${c.exam_number}`.includes(candidateSearch.value.trim())) || [])
function pretty(value) { try { return JSON.stringify(JSON.parse(value), null, 2) } catch (_) { return value || '{}' } }
async function refresh() {
  loading.value = true; failed.value = false
  try { overview.value = (await request.get('/admin/workflow')).data; online.value = overview.value.onlineCount; timeOffset = Date.parse(overview.value.serverTime) - Date.now(); now.value = Date.now() + timeOffset; batchId.value ||= overview.value.batches[0]?.id; await selectBatch() } catch (_) { failed.value = true } finally { loading.value = false }
}
async function selectBatch() {
  if (!batch.value) return
  windowForm.value = { revision: batch.value.config_revision, startsAt: batch.value.startsAt ? new Date(batch.value.startsAt) : null, endsAt: batch.value.endsAt ? new Date(batch.value.endsAt) : null, physicsLine: batch.value.controlLines.find(c => c.category_code === 'PHYSICS')?.score, historyLine: batch.value.controlLines.find(c => c.category_code === 'HISTORY')?.score }
  try { plans.value = (await request.get(`/admin/workflow/batches/${batchId.value}/plans`)).data } catch (_) { plans.value = [] }
}
async function saveWindow() {
  const value = windowForm.value
  if (!value.startsAt || !value.endsAt || value.physicsLine == null || value.historyLine == null || new Date(value.startsAt) >= new Date(value.endsAt)) { ElMessage.warning('请填写有效起止时间与两类控制线'); return }
  busy.value = true
  try { await request.put(`/admin/workflow/batches/${batchId.value}`, { ...value, startsAt: new Date(value.startsAt).toISOString(), endsAt: new Date(value.endsAt).toISOString() }); ElMessage.success('填报设置已保存'); await refresh() } catch (_) {} finally { busy.value = false }
}
async function savePlan(row) {
  busy.value = true
  try { await request.put(`/admin/workflow/plans/${row.id}`, { plannedCount: row.planned_count, filingRatio: row.filing_ratio }); ElMessage.success('招生计划已保存'); await refresh() } catch (_) {} finally { busy.value = false }
}
async function toggleAccount(row, enabled) {
  try { await ElMessageBox.confirm(`确认${enabled ? '启用' : '禁用'}考生 ${row.name} 的账号？`, '账号状态', { type: 'warning' }) } catch (_) { return }
  busy.value = true
  try { await request.patch(`/admin/accounts/${row.user_id}/status`, { status: enabled ? 'ACTIVE' : 'DISABLED' }); await refresh() } catch (_) {} finally { busy.value = false }
}
async function viewVersions(row) { try { selectedCandidate.value = row; versions.value = (await request.get(`/admin/workflow/candidates/${row.id}/submissions`, { params: { batchId: batchId.value } })).data; versionsVisible.value = true } catch (_) {} }
async function viewSubmission(id) { try { sheet.value = (await request.get(`/admin/workflow/submissions/${id}`)).data; sheetVisible.value = true } catch (_) {} }
async function execute() {
  try { await ElMessageBox.confirm('确认使用本批次当前计划、成绩和截止前最后一次正式志愿创建新投档运行？', '执行投档', { type: 'warning', confirmButtonText: '确认执行', cancelButtonText: '取消', closeOnClickModal: false }) } catch (_) { return }
  busy.value = true
  try { await request.post(`/admin/workflow/batches/${batchId.value}/execute`, {}, { timeout: 300000 }); ElMessage.success('投档运行已完成'); await refresh() } catch (_) {} finally { busy.value = false }
}
async function viewRun(row) { try { selectedRun.value = row; results.value = (await request.get(`/admission-runs/${row.id}/results`)).data; resultsVisible.value = true } catch (_) {} }
async function viewTrace(row) { try { traces.value = (await request.get(`/admission-runs/${selectedRun.value.id}/candidates/${row.candidateId}/traces`)).data; traceVisible.value = true } catch (_) {} }
async function downloadRun(id) { try { await downloadExcel(`runs/${id}`, `admission-run-${id}.xlsx`) } catch (_) {} }
async function resetDemo() {
  if (overview.value.mode !== 'DEMO') return
  try {
    await ElMessageBox.confirm(`当前在线考生 ${online.value ?? '未知'} 人。体验账号、草稿、正式志愿及其投档运行将永久删除，无法恢复。`, '彻底删除体验数据？', { type: 'error', confirmButtonText: '继续确认', cancelButtonText: '取消', closeOnClickModal: false })
    const { value } = await ElMessageBox.prompt('输入“删除体验数据”确认本次操作。', '最后确认', { inputValidator: text => text === '删除体验数据' || '确认文字不匹配', confirmButtonText: '彻底删除', cancelButtonText: '取消', closeOnClickModal: false })
    busy.value = true
    const data = (await request.post('/admin/workflow/demo-reset', { confirmation: value })).data
    ElMessage.success(`已删除 ${data.deletedCandidates} 个体验考生及 ${data.deletedRuns} 次运行`)
    await refresh()
  } catch (_) {} finally { busy.value = false }
}
async function pollOnline() {
  if (onlinePending) return
  onlinePending = true
  try { online.value = (await request.get('/admin/workflow/online', { silent: true })).data.onlineCount } catch (_) { online.value = null } finally { onlinePending = false }
}
onMounted(() => { refresh(); timer = setInterval(pollOnline, 10000); ticker = setInterval(() => { now.value = Date.now() + timeOffset }, 1000) })
onBeforeUnmount(() => { clearInterval(timer); clearInterval(ticker) })
</script>

<style scoped>
.desk { background: white; padding: 24px; min-width: 0; }.heading, .status, .toolbar { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }.heading { justify-content: space-between; }.status { font-size: 13px; color: #50636b; }h2 { font-size: 22px; margin: 0 0 12px; }h3 { font-size: 16px; }.toolbar { margin: 20px 0; }.batch { width: 280px; max-width: 100%; }.search { width: 300px; max-width: 100%; }.form-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 0 24px; max-width: 700px; }.window-form { margin: 20px 0; }.form-grid :deep(.el-input-number), .form-grid :deep(.el-date-editor) { width: 100%; }.muted { font-size: 13px; color: #69767b; }.reset-area { border-top: 1px solid #e5e8eb; margin-top: 32px; padding-top: 16px; display: flex; align-items: center; gap: 20px; flex-wrap: wrap; }.el-table :deep(.el-input-number) { width: 145px; }pre { white-space: pre-wrap; overflow-wrap: anywhere; background: #f5f7f8; padding: 12px; font-size: 12px; }
@media (max-width: 700px) { .desk { padding: 12px; }h2 { font-size: 20px; }.form-grid { grid-template-columns: 1fr; }.toolbar .el-button { margin-left: 0; }.status { gap: 8px; } }
</style>
