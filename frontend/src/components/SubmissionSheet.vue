<template>
  <el-dialog :model-value="modelValue" title="正式志愿表" width="min(1100px, 96vw)" append-to-body @update:model-value="$emit('update:modelValue', $event)">
    <div v-if="data" class="print-sheet">
      <h2>2026 黑龙江普通本科批志愿表</h2>
      <p>{{ data.candidate?.name || data.submission?.name }} / {{ data.candidate?.exam_number || data.submission?.exam_number }} · 第 {{ data.submission?.version_no }} 版</p>
      <p>提交时间：{{ formatTime(data.submission?.submitted_at) }}</p>
      <div class="sheet-scroll"><table>
        <thead><tr><th>序号</th><th>院校专业组</th><th v-for="n in 6" :key="n">专业 {{ n }}</th><th>调剂</th></tr></thead>
        <tbody><tr v-for="item in data.items" :key="item.id">
          <td>{{ item.preference_no }}</td><td>{{ item.institution_code }} {{ item.institution_name }}<br>{{ item.group_code }} {{ item.group_name }}</td>
          <td v-for="n in 6" :key="n">{{ item.majors[n - 1]?.major_code }} {{ item.majors[n - 1]?.name || '空' }}</td>
          <td>{{ item.accept_adjustment ? '服从' : '不服从' }}</td>
        </tr></tbody>
      </table></div>
      <p class="disclaimer">模拟结果仅供测试，不代表黑龙江省招生考试院正式投档结果</p>
    </div>
    <template #footer><el-button :icon="Printer" @click="print">打印</el-button><el-button @click="$emit('update:modelValue', false)">关闭</el-button></template>
  </el-dialog>
</template>
<script setup>
import { Printer } from '@element-plus/icons-vue'
import { formatTime } from '../utils/workflow'
defineProps({ modelValue: Boolean, data: Object })
defineEmits(['update:modelValue'])
function print() { window.print() }
</script>
<style scoped>
h2 { font-size: 20px; margin: 0 0 16px; }
p { line-height: 1.6; }
.sheet-scroll { overflow-x: auto; }
table { width: 100%; border-collapse: collapse; min-width: 780px; font-size: 12px; }
td, th { border: 1px solid #c9ced2; padding: 10px 6px; text-align: left; overflow-wrap: anywhere; }
th { background: #f4f6f7; }
.disclaimer { font-size: 12px; color: #806218; }
</style>
<style>
@media print {
  @page { size: A4 landscape; margin: 12mm; }
  body * { visibility: hidden !important; }
  .print-sheet, .print-sheet * { visibility: visible !important; }
  .el-overlay:has(.print-sheet), .el-overlay-dialog:has(.print-sheet), .el-dialog:has(.print-sheet), .el-dialog__body:has(.print-sheet) { position: static !important; overflow: visible !important; height: auto !important; width: auto !important; margin: 0 !important; padding: 0 !important; box-shadow: none !important; opacity: 1 !important; transform: none !important; transition: none !important; animation: none !important; }
  .el-dialog__header, .el-dialog__footer, #app, .el-message { display: none !important; }
  .print-sheet .sheet-scroll { overflow: visible; }
  .print-sheet table { min-width: 0; table-layout: fixed; }
  .print-sheet tr { break-inside: avoid; }
}
</style>
