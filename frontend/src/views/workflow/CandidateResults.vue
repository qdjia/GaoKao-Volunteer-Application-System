<template>
  <section v-loading="loading" class="results-page">
    <div class="heading"><h2>投档结果</h2><el-button :icon="Refresh" circle title="刷新投档结果" aria-label="刷新投档结果" :disabled="loading" @click="load" /></div>
    <el-select v-model="batchId" aria-label="招生批次" class="batch" @change="loadResults"><el-option v-for="b in batches" :key="b.id" :value="b.id" :label="`${b.admission_year} ${b.name}`" /></el-select>
    <p v-if="data.run" class="run">第 {{ data.run.run_no }} 次投档 · {{ formatTime(data.run.completed_at) }}</p>
    <el-empty v-if="!data.results?.length" :description="failed ? '结果加载失败，请重试' : '暂无已发布的本人投档结果'" />
    <template v-else>
      <div v-for="result in data.results" :key="result.candidateId" class="filing-summary">
        <el-tag :type="result.status === 'FILED' ? 'success' : 'warning'" size="large">{{ resultName(result.status) }}</el-tag>
        <h3 v-if="result.institutionName">{{ result.institutionName }}</h3>
        <p v-if="result.groupName">{{ result.groupCode }} {{ result.groupName }}<span v-if="result.matchedPreferenceNo"> · 第 {{ result.matchedPreferenceNo }} 志愿</span></p>
        <p>{{ result.reason }}</p>
        <p class="candidate">{{ result.candidateName }} · {{ result.examNumber }} · {{ categoryName(result.category) }}</p>
      </div>
      <h3>检索轨迹</h3><FilingTrace :rows="data.traces" stacked />
    </template>
  </section>
</template>
<script setup>
import { onMounted, ref } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import request from '../../utils/request'
import { formatTime, resultName, categoryName } from '../../utils/workflow'
import FilingTrace from '../../components/FilingTrace.vue'
const loading = ref(false), failed = ref(false), batches = ref([]), batchId = ref(null), data = ref({})
async function loadResults() {
  loading.value = true; failed.value = false; data.value = {}
  try { if (batchId.value) data.value = (await request.get(`/candidate/batches/${batchId.value}/results`)).data } catch (_) { failed.value = true } finally { loading.value = false }
}
async function load() {
  loading.value = true
  try { batches.value = (await request.get('/candidate/context')).data.batches; batchId.value ||= batches.value[0]?.id; await loadResults() } catch (_) { failed.value = true } finally { loading.value = false }
}
onMounted(load)
</script>
<style scoped>
.results-page { padding: 20px; background: white; min-width: 0; }.heading { display: flex; align-items: center; justify-content: space-between; gap: 12px; }h2 { font-size: 22px; margin: 0 0 20px; }h3 { font-size: 16px; margin-top: 28px; }.batch { width: 280px; max-width: 100%; }.run { font-size: 13px; color: #63757a; margin: 20px 0; }@media (max-width: 640px) { .results-page { padding: 12px; }h2 { font-size: 20px; } }
.filing-summary { padding: 20px 0; border-block: 1px solid #e2e8ea; }
.filing-summary h3 { margin: 14px 0 6px; font-size: 18px; }
.filing-summary p { margin: 8px 0 0; line-height: 1.8; overflow-wrap: anywhere; }
.filing-summary .candidate { margin-top: 16px; color: #718088; font-size: 12px; }
</style>
