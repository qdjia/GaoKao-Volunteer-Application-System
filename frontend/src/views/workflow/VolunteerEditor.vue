<template>
  <section v-loading="loading" class="editor">
    <div class="heading"><h2>志愿填报</h2><div class="heading-tools"><el-tag v-if="candidate.category_code" type="success">{{ categoryName(candidate.category_code) }}</el-tag><el-button :icon="Refresh" circle title="刷新填报数据" aria-label="刷新填报数据" :disabled="busy || loading" @click="refresh" /></div></div>
    <el-alert v-if="loadFailed" type="error" title="填报数据加载失败" :closable="false"><el-button @click="load">重试</el-button></el-alert>
    <template v-else-if="context">
      <div class="identity"><strong>{{ candidate.name }}</strong><span>{{ candidate.exam_number }}</span><span>{{ candidate.combination_name }}</span></div>
      <dl class="scores"><div v-for="score in scoreFields" :key="score[0]"><dt>{{ score[1] }}</dt><dd>{{ candidate[score[0]] ?? '待导入' }}</dd></div></dl>
      <div class="toolbar">
        <el-select :model-value="batchId" aria-label="招生批次" class="batch" :disabled="busy" @change="changeBatch"><el-option v-for="b in context.batches" :key="b.id" :label="`${b.admission_year} ${b.name}`" :value="b.id" /></el-select>
        <el-tag :type="writable ? 'success' : 'info'">{{ windowLabel }}</el-tag>
      </div>
      <p class="deadline">截止时间：{{ formatTime(batch?.endsAt) }}<span v-if="writable"> · 剩余 {{ remaining }}</span></p>
      <el-alert v-if="!context.noticeAccepted" title="尚未确认模拟用途声明" type="warning" :closable="false"><el-button @click="noticeVisible = true">查看并确认</el-button></el-alert>
      <div class="editor-heading"><h3>院校专业组 <span>{{ rows.length }} / 45</span></h3><el-checkbox v-model="eligibleOnly">仅匹配选科</el-checkbox></div>
      <el-empty v-if="!rows.length" :description="batchId ? '暂无志愿草稿' : '暂无招生批次'" :image-size="72" />
      <ol class="preferences">
        <li v-for="(row, index) in rows" :key="row.key" class="preference">
          <div class="preference-top">
            <strong class="order">{{ String(index + 1).padStart(2, '0') }}</strong>
            <el-select v-model="row.planId" filterable placeholder="选择院校专业组" :aria-label="`第${index + 1}志愿专业组`" :disabled="!canEdit" class="plan-select" @change="row.majorIds = []">
              <el-option v-for="plan in options(row)" :key="plan.id" :value="plan.id" :label="planLabel(plan)" :disabled="plan.group_status !== 'ACTIVE' || plan.institution_status !== 'ACTIVE' || rows.some(r => r !== row && r.planId === plan.id)" />
            </el-select>
            <div class="row-tools">
              <el-tooltip content="上移志愿"><el-button :icon="ArrowUp" circle aria-label="上移志愿" :disabled="!canEdit || index === 0" @click="move(index, -1)" /></el-tooltip>
              <el-tooltip content="下移志愿"><el-button :icon="ArrowDown" circle aria-label="下移志愿" :disabled="!canEdit || index === rows.length - 1" @click="move(index, 1)" /></el-tooltip>
              <el-tooltip content="删除志愿"><el-button :icon="Delete" circle aria-label="删除志愿" :disabled="!canEdit" @click="rows.splice(index, 1)" /></el-tooltip>
            </div>
          </div>
          <template v-if="planFor(row)">
            <div class="plan-details"><span>计划 {{ planFor(row).planned_count }} 人</span><span>投档比例 {{ (planFor(row).filing_ratio * 100).toFixed(0) }}%</span><span>再选要求：{{ planFor(row).requiredSubjects.map(subjectName).join('、') || '不限' }}</span></div>
            <p v-if="!eligible(planFor(row))" class="warning" role="alert">选科不匹配，可暂存草稿，但不能正式提交。</p>
            <div class="majors">
              <label v-for="n in 6" :key="n"><span>专业 {{ n }}</span>
                <el-select :model-value="row.majorIds[n - 1] || null" clearable filterable :placeholder="n === 1 ? '请选择专业' : '空'" :aria-label="`第${index + 1}志愿专业${n}`" :disabled="!canEdit || (n > 1 && !row.majorIds[n - 2])" @update:model-value="setMajor(row, n - 1, $event)">
                  <el-option v-for="major in planFor(row).majors" :key="major.id" :value="major.id" :label="`${major.major_code} ${major.name}`" :disabled="major.status !== 'ACTIVE' || (row.majorIds.includes(major.id) && row.majorIds[n - 1] !== major.id)" />
                </el-select>
              </label>
            </div>
            <p v-for="major in selectedWarnings(row)" :key="major.id" class="major-warning">{{ major.name }}：{{ major.description }}</p>
            <el-checkbox v-model="row.acceptAdjustment" :disabled="!canEdit">服从组内专业调剂</el-checkbox>
          </template>
        </li>
      </ol>
      <el-button :icon="Plus" :disabled="!canEdit || rows.length >= 45" @click="addRow">添加院校专业组</el-button>
      <div class="save-bar">
        <div class="save-state"><strong :class="{ warning: dirty }">{{ dirty ? '有未保存修改' : revision ? `草稿已保存 · 修订 ${revision}` : '草稿尚未保存' }}</strong><span>{{ latest ? `正式提交：第 ${latest.version_no} 版` : '尚未正式提交' }}</span></div>
        <div class="save-actions">
          <el-button v-if="latest" :icon="Document" :disabled="busy" @click="viewSubmission">查看正式版本</el-button>
          <el-button :icon="Check" :loading="busy" :disabled="!canEdit || !dirty" @click="save">保存草稿</el-button>
          <el-button type="primary" :icon="Promotion" :loading="busy" :disabled="!canEdit || dirty || !revision || !rows.length || rows.some(r => !r.majorIds.length || !eligible(planFor(r)))" @click="submit">正式提交</el-button>
        </div>
      </div>
    </template>
    <el-dialog v-model="noticeVisible" title="模拟用途声明" width="min(480px, 92vw)" :close-on-click-modal="false" :close-on-press-escape="false">
      <p class="notice-text">{{ context?.notice }}</p><p>声明版本：{{ context?.noticeVersion }}</p>
      <template #footer><el-button type="primary" :loading="busy" @click="acceptNotice">已知悉并同意</el-button></template>
    </el-dialog>
    <SubmissionSheet v-model="sheetVisible" :data="sheet" />
  </section>
</template>

<script setup>
import { computed, onMounted, onBeforeUnmount, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowUp, ArrowDown, Delete, Plus, Check, Promotion, Document, Refresh } from '@element-plus/icons-vue'
import request from '../../utils/request'
import { categoryName, subjectName, formatTime } from '../../utils/workflow'
import { useDraftProtection, confirmLeave } from '../../utils/draftProtection'
import SubmissionSheet from '../../components/SubmissionSheet.vue'

const context = ref(null), batchId = ref(null), plans = ref([]), rows = ref([]), revision = ref(0), latest = ref(null)
const loading = ref(false), busy = ref(false), loadFailed = ref(false), eligibleOnly = ref(true)
const noticeVisible = ref(false), sheetVisible = ref(false), sheet = ref(null), baseline = ref('[]'), now = ref(Date.now())
let clockOffset = 0, ticker, requestId = null
const candidate = computed(() => context.value?.candidate || {})
const batch = computed(() => context.value?.batches.find(b => b.id === batchId.value))
const serialize = () => JSON.stringify(rows.value.map(({ planId, majorIds, acceptAdjustment }) => ({ planId, majorIds, acceptAdjustment })))
const dirty = computed(() => serialize() !== baseline.value)
useDraftProtection(dirty, busy)
const writable = computed(() => batch.value?.status === 'OPEN' && now.value >= Date.parse(batch.value.startsAt) && now.value < Date.parse(batch.value.endsAt))
const canEdit = computed(() => writable.value && context.value?.noticeAccepted && !busy.value && !loading.value)
const windowLabel = computed(() => !batch.value?.startsAt ? '尚未开放' : writable.value ? '填报中' : now.value < Date.parse(batch.value.startsAt) ? '尚未开始' : '填报已关闭')
const remaining = computed(() => { const seconds = Math.max(0, Math.floor((Date.parse(batch.value?.endsAt) - now.value) / 1000)); return `${Math.floor(seconds / 86400)}天 ${Math.floor(seconds / 3600) % 24}时 ${Math.floor(seconds / 60) % 60}分 ${seconds % 60}秒` })
const scoreFields = computed(() => [ ['chinese_score', '语文'], ['mathematics_score', '数学'], ['foreign_language_score', '外语'], ['primary_subject_score', candidate.value.category_code === 'PHYSICS' ? '物理' : '历史'], ['secondary_subject_1_score', subjectName(candidate.value.secondary_subject_1)], ['secondary_subject_2_score', subjectName(candidate.value.secondary_subject_2)], ['culture_total', '文化总分'], ['policy_bonus', '政策加分'], ['final_rank', '最终位次'] ])
const planFor = row => plans.value.find(p => p.id === row.planId)
const eligible = plan => !!plan && plan.requiredSubjects.every(s => [candidate.value.secondary_subject_1, candidate.value.secondary_subject_2].includes(s))
const planLabel = plan => `${plan.institution_code} ${plan.institution_name} / ${plan.group_code} ${plan.group_name}`
const options = row => plans.value.filter(p => p.id === row.planId || !eligibleOnly.value || eligible(p))
const selectedWarnings = row => planFor(row)?.majors.filter(m => row.majorIds.includes(m.id) && m.description) || []
function setMajor(row, index, value) { if (value) row.majorIds[index] = value; else row.majorIds.splice(index, 1) }
function addRow() { rows.value.push({ key: crypto.randomUUID(), planId: null, majorIds: [], acceptAdjustment: false }) }
function move(index, delta) { const row = rows.value.splice(index, 1)[0]; rows.value.splice(index + delta, 0, row) }
function syncTime(value) { clockOffset = Date.parse(value) - Date.now(); now.value = Date.now() + clockOffset }
function setDraft(draft) {
  revision.value = draft.revision
  rows.value = draft.items.map(item => ({ key: crypto.randomUUID(), planId: item.enrollment_plan_id, majorIds: item.majors.map(m => m.group_major_id), acceptAdjustment: item.accept_adjustment }))
  baseline.value = serialize()
  requestId = null
}
async function load() {
  loading.value = true; loadFailed.value = false
  try {
    context.value = (await request.get('/candidate/context')).data
    syncTime(context.value.serverTime)
    if (!batchId.value) batchId.value = context.value.batches[0]?.id || null
    if (batchId.value) {
      const data = (await request.get(`/candidate/batches/${batchId.value}/workspace`)).data
      plans.value = data.plans; setDraft(data.draft); latest.value = data.submissions[0] || null; syncTime(data.serverTime)
    }
    noticeVisible.value = !context.value.noticeAccepted
  } catch (_) { loadFailed.value = true } finally { loading.value = false }
}
async function changeBatch(id) { if (id !== batchId.value && await confirmLeave()) { batchId.value = id; await load() } }
async function refresh() { if (await confirmLeave()) await load() }
async function acceptNotice() {
  busy.value = true
  try { await request.post('/candidate/notice', { version: context.value.noticeVersion }); context.value.noticeAccepted = true; noticeVisible.value = false } catch (_) {} finally { busy.value = false }
}
async function save() {
  if (rows.value.some(r => !r.planId)) { ElMessage.warning('请先选择院校专业组，或删除空白志愿'); return }
  busy.value = true
  try { const data = (await request.put(`/candidate/batches/${batchId.value}/draft`, { revision: revision.value, preferences: JSON.parse(serialize()) })).data; setDraft(data); ElMessage.success('草稿已保存，尚未正式提交') } catch (_) {} finally { busy.value = false }
}
async function submit() {
  try { await ElMessageBox.confirm(`确认正式提交当前 ${rows.value.length} 个院校专业组志愿？`, '正式提交', { confirmButtonText: '确认提交', cancelButtonText: '继续核对', closeOnClickModal: false }) } catch (_) { return }
  busy.value = true
  if (!requestId) requestId = crypto.randomUUID()
  try {
    latest.value = (await request.post(`/candidate/batches/${batchId.value}/submit`, { revision: revision.value, requestId })).data
    ElMessage.success(`正式提交成功，第 ${latest.value.version_no} 版`)
    requestId = null
  } catch (_) {} finally { busy.value = false }
}
async function viewSubmission() {
  try { sheet.value = (await request.get(`/candidate/batches/${batchId.value}/submission`)).data; sheetVisible.value = true } catch (_) {}
}
onMounted(() => { load(); ticker = setInterval(() => { now.value = Date.now() + clockOffset }, 1000) })
onBeforeUnmount(() => clearInterval(ticker))
</script>

<style scoped>
.editor { max-width: 1240px; margin: 0 auto; min-width: 0; }
.heading, .editor-heading { display: flex; justify-content: space-between; align-items: center; gap: 12px; flex-wrap: wrap; }
.heading-tools { display: flex; align-items: center; gap: 12px; }
h2 { font-size: 22px; margin: 0 0 16px; } h3 { font-size: 16px; margin: 0; } h3 span { color: #647078; font-size: 14px; font-weight: 400; }
.identity { display: flex; gap: 16px; flex-wrap: wrap; font-size: 14px; }
.scores { display: grid; grid-template-columns: repeat(9, minmax(0, 1fr)); background: white; border-block: 1px solid #e2e7e9; padding: 16px 0; margin: 18px 0; gap: 12px; }
.scores div { padding: 0 8px; } dt { font-size: 12px; color: #65717a; } dd { font-size: 18px; margin: 6px 0 0; font-weight: 600; }
.toolbar { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }.batch { width: 280px; max-width: 100%; }
.deadline { font-size: 13px; color: #58666c; line-height: 1.8; }
.editor-heading { border-top: 1px solid #dce3e6; padding-top: 20px; margin: 20px 0 12px; }
.preferences { list-style: none; padding: 0; margin: 0; }.preference { background: white; border: 1px solid #dce3e6; border-radius: 6px; padding: 16px; margin-bottom: 12px; }
.preference-top { display: flex; align-items: center; gap: 12px; }.order { width: 28px; flex-shrink: 0; color: #157b6b; font-size: 18px; }.plan-select { flex: 1; min-width: 0; }
.row-tools { display: flex; gap: 6px; }.row-tools .el-button { margin: 0; width: 30px; height: 30px; }
.plan-details { display: flex; gap: 16px; flex-wrap: wrap; color: #66747a; font-size: 12px; margin: 12px 0; }
.majors { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 12px; margin: 12px 0; }.majors label { min-width: 0; }.majors label > span { display: block; font-size: 12px; color: #69747b; margin-bottom: 6px; }
.warning, .major-warning { color: #ad5714; font-size: 13px; line-height: 1.7; }.major-warning { margin: 4px 0; }.notice-text { line-height: 1.9; }
.save-bar { position: sticky; bottom: -24px; background: #fff; border-top: 1px solid #cad6dc; padding: 16px; margin-top: 24px; display: flex; justify-content: space-between; align-items: center; gap: 12px; z-index: 2; flex-wrap: wrap; }
.save-state { display: flex; gap: 6px; flex-direction: column; font-size: 13px; }.save-state span { color: #66747a; }.save-actions { display: flex; flex-wrap: wrap; gap: 8px; }.save-actions .el-button { margin: 0; }
@media (max-width: 900px) { .scores { grid-template-columns: repeat(3, minmax(0, 1fr)); }.majors { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
@media (max-width: 640px) { h2 { font-size: 20px; }.preference { padding: 12px; }.preference-top { flex-wrap: wrap; gap: 8px; }.plan-select { flex-basis: calc(100% - 40px); }.row-tools { margin-left: auto; }.majors { grid-template-columns: 1fr; }.save-bar { bottom: -12px; padding: 12px 0; }.save-actions { width: 100%; }.deadline span { display: block; } }
</style>
