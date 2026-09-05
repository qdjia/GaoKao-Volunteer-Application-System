<template>
  <section class="excel-page" v-loading="loading">
    <div class="page-heading">
      <h2>{{ store.isAdmin() ? '数据导入导出' : '正式志愿表' }}</h2>
      <el-button :icon="Refresh" circle title="刷新" aria-label="刷新" :disabled="busy" @click="refresh" />
    </div>
    <template v-if="store.isAdmin()">
      <div class="import-toolbar">
        <el-select v-model="batchId" placeholder="选择招生批次" aria-label="招生批次" :disabled="busy" class="batch-select">
          <el-option v-for="batch in context.batches" :key="batch.id" :label="`${batch.admission_year} ${batch.name}`" :value="batch.id" />
        </el-select>
        <el-button v-if="!context.batches?.length" :icon="Plus" :loading="busy" @click="createBatch">创建2026普通本科批</el-button>
      </div>
      <el-tabs v-model="type" @tab-change="clearFile">
        <el-tab-pane label="考生与成绩" name="candidates" :disabled="busy" />
        <el-tab-pane label="院校专业组与计划" name="plans" :disabled="busy" />
      </el-tabs>
      <div class="import-toolbar">
        <el-button :icon="Download" :disabled="busy" @click="downloadTemplate(false)">空白模板 v1.0</el-button>
        <el-button :icon="Download" :disabled="busy" @click="downloadTemplate(true)">{{ type === 'candidates' ? '10人体验数据' : '体验院校计划' }}</el-button>
        <input ref="fileInput" class="file-input" type="file" accept=".xlsx,.xls" aria-label="选择Excel文件" :disabled="busy" @change="selectFile" />
        <el-button :icon="FolderOpened" :disabled="busy" @click="fileInput?.click()">选择文件</el-button>
        <span class="file-name" :title="selectedFile?.name">{{ selectedFile?.name || '未选择文件' }}</span>
        <el-button type="primary" :icon="Upload" :loading="busy" :disabled="!selectedFile || !batchId" @click="upload">整批导入</el-button>
      </div>
      <el-alert v-if="outcome" class="outcome" :type="outcome.success ? 'success' : 'error'" :closable="false" show-icon
        :title="outcome.success ? `导入成功：新增 ${outcome.createdCount}，更新 ${outcome.updatedCount}` : `整批未写入：发现 ${outcome.errors.length} 项错误`" />
      <template v-if="outcome && !outcome.success">
        <div class="section-heading"><h3>校验错误</h3><el-button :icon="Download" @click="downloadErrors(outcome.id)">错误报告</el-button></div>
        <el-table :data="outcome.errors" max-height="360" border>
          <el-table-column prop="sheet" label="工作表" width="110" />
          <el-table-column prop="row" label="Excel行号" width="100" />
          <el-table-column prop="field" label="字段" width="140" />
          <el-table-column prop="message" label="原因" min-width="260" />
        </el-table>
      </template>
      <div class="section-heading"><h3>导入记录</h3></div>
      <el-table :data="context.jobs || []" max-height="300" empty-text="暂无导入记录">
        <el-table-column label="时间" min-width="175"><template #default="{ row }">{{ formatTime(row.created_at) }}</template></el-table-column>
        <el-table-column label="类型" min-width="160"><template #default="{ row }">{{ row.template_type === 'candidates' ? '考生与成绩' : '专业组与计划' }}</template></el-table-column>
        <el-table-column label="状态" width="110"><template #default="{ row }"><el-tag :type="row.status === 'SUCCEEDED' ? 'success' : 'danger'">{{ row.status === 'SUCCEEDED' ? '已导入' : '已拒绝' }}</el-tag></template></el-table-column>
        <el-table-column prop="row_count" label="数据行数" width="100" />
        <el-table-column prop="created_count" label="新增" width="75" />
        <el-table-column prop="updated_count" label="更新" width="75" />
        <el-table-column label="错误报告" width="100"><template #default="{ row }"><el-button v-if="row.status === 'REJECTED'" :icon="Download" circle title="下载错误报告" aria-label="下载错误报告" @click="downloadErrors(row.id)" /></template></el-table-column>
      </el-table>
      <div class="section-heading"><h3>投档运行结果</h3></div>
      <el-table :data="context.runs || []" max-height="300" empty-text="暂无已完成的投档运行">
        <el-table-column prop="id" label="运行ID" width="100" />
        <el-table-column prop="run_no" label="版本" width="80" />
        <el-table-column prop="name" label="批次" min-width="160" />
        <el-table-column label="创建时间" min-width="180"><template #default="{ row }">{{ formatTime(row.created_at) }}</template></el-table-column>
        <el-table-column label="导出" width="80"><template #default="{ row }"><el-button :icon="Download" circle title="导出结果和审计" aria-label="导出结果和审计" @click="downloadRun(row.id)" /></template></el-table-column>
      </el-table>
    </template>
    <div class="section-heading"><h3>正式志愿表</h3></div>
    <el-table :data="context.submissions || []" max-height="400" empty-text="暂无有效的正式提交">
      <el-table-column prop="exam_number" label="准考证号" min-width="135" />
      <el-table-column prop="name" label="姓名" min-width="110" />
      <el-table-column prop="batch_name" label="批次" min-width="130" />
      <el-table-column prop="version_no" label="版本" width="75" />
      <el-table-column label="提交时间" min-width="180"><template #default="{ row }">{{ formatTime(row.submitted_at) }}</template></el-table-column>
      <el-table-column label="导出" width="80"><template #default="{ row }"><el-button :icon="Download" circle title="导出正式志愿表" aria-label="导出正式志愿表" @click="downloadVolunteer(row)" /></template></el-table-column>
    </el-table>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Download, FolderOpened, Plus, Refresh, Upload } from '@element-plus/icons-vue'
import { useUserStore } from '../stores/user'
import { getExcelContext, initializeBatch, importExcel, downloadExcel } from '../api/excel'

const store = useUserStore()
const context = ref({})
const loading = ref(false)
const busy = ref(false)
const type = ref('candidates')
const batchId = ref(null)
const fileInput = ref(null)
const selectedFile = ref(null)
const outcome = ref(null)

async function refresh() {
  loading.value = true
  try {
    context.value = (await getExcelContext()).data
    if (!batchId.value) batchId.value = context.value.batches?.[0]?.id || null
  } catch (_) {} finally { loading.value = false }
}
async function createBatch() {
  busy.value = true
  try { batchId.value = (await initializeBatch()).data; await refresh() } catch (_) {} finally { busy.value = false }
}
function clearFile() {
  selectedFile.value = null
  outcome.value = null
  if (fileInput.value) fileInput.value.value = ''
}
function selectFile(event) {
  const file = event.target.files?.[0]
  outcome.value = null
  if (file && (!/\.(xlsx|xls)$/i.test(file.name) || file.size > 2 * 1024 * 1024 || !file.size)) {
    ElMessage.error('请选择不超过2MB的非空Excel文件')
    clearFile()
    return
  }
  selectedFile.value = file || null
}
async function upload() {
  if (!selectedFile.value || !batchId.value || busy.value) return
  busy.value = true
  outcome.value = null
  try {
    outcome.value = (await importExcel(type.value, batchId.value, selectedFile.value)).data
    if (outcome.value.success) {
      selectedFile.value = null
      fileInput.value.value = ''
    }
    await refresh()
  } catch (_) {} finally { busy.value = false }
}
async function downloadTemplate(demo) {
  try { await downloadExcel(`templates/${type.value}`, `${demo ? 'demo' : 'template'}-${type.value}-v1.xlsx`, { demo }) } catch (_) {}
}
async function downloadErrors(id) {
  try { await downloadExcel(`imports/${id}/errors`, `import-errors-${id}.xlsx`) } catch (_) {}
}
async function downloadRun(id) {
  try { await downloadExcel(`runs/${id}`, `admission-run-${id}.xlsx`) } catch (_) {}
}
async function downloadVolunteer(row) {
  try { await downloadExcel(`volunteers/${row.candidate_id}`, `volunteer-${row.exam_number}.xlsx`, { batchId: row.admission_batch_id }) } catch (_) {}
}
const formatTime = value => value ? String(value).replace('T', ' ').slice(0, 19) : ''
onMounted(refresh)
</script>

<style scoped>
.excel-page { background: #fff; padding: 24px; min-width: 0; }
.page-heading, .section-heading { display: flex; justify-content: space-between; align-items: center; gap: 12px; }
h2 { font-size: 20px; margin: 0 0 16px; }
h3 { font-size: 16px; margin: 0; }
.section-heading { margin: 28px 0 12px; min-height: 32px; }
.import-toolbar { display: flex; align-items: center; flex-wrap: wrap; gap: 12px; margin: 12px 0 20px; }
.import-toolbar .el-button + .el-button { margin-left: 0; }
.batch-select { width: 260px; max-width: 100%; }
.file-input { display: none; }
.file-name { color: #606266; font-size: 14px; overflow-wrap: anywhere; flex: 1; min-width: 120px; }
.outcome { margin-bottom: 16px; }
:deep(.el-table__empty-text) { width: 100%; padding: 14px; line-height: 1.5; box-sizing: border-box; }
@media (max-width: 640px) {
  .excel-page { padding: 14px; }
  .file-name { flex-basis: 100%; }
  h2 { font-size: 18px; }
}
</style>
