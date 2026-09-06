<template>
  <ol v-if="stacked" class="trace-list">
    <li v-for="row in rows" :key="row.sequenceNo"><strong class="step">{{ row.sequenceNo }}</strong><div><strong>{{ row.institutionName || '全局校验' }} {{ row.groupCode }} {{ row.groupName }}</strong><p v-if="row.preferenceNo" class="preference">第 {{ row.preferenceNo }} 志愿</p><p>{{ row.detail }}</p></div></li>
  </ol>
  <el-table v-else :data="rows" empty-text="暂无检索轨迹" max-height="500">
    <el-table-column prop="sequenceNo" label="步骤" width="65" />
    <el-table-column prop="preferenceNo" label="志愿" width="65" />
    <el-table-column label="院校专业组" min-width="210"><template #default="{ row }">{{ row.institutionName || '全局校验' }} {{ row.groupCode }} {{ row.groupName }}</template></el-table-column>
    <el-table-column prop="detail" label="检索过程" min-width="280" />
  </el-table>
</template>
<script setup>
defineProps({ rows: { type: Array, default: () => [] }, stacked: Boolean })
</script>
<style scoped>
.trace-list { padding: 0; margin: 0; list-style: none; }
.trace-list li { display: grid; grid-template-columns: 28px minmax(0, 1fr); gap: 12px; padding: 16px 0; border-top: 1px solid #e2e8ea; font-size: 14px; }
.trace-list strong { font-weight: 600; overflow-wrap: anywhere; }
.trace-list .step { color: #18786c; }
.trace-list p { margin: 6px 0 0; line-height: 1.8; overflow-wrap: anywhere; color: #586b73; }
.trace-list .preference { font-size: 12px; color: #7b868b; }
</style>
