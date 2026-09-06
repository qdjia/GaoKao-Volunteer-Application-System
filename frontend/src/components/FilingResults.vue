<template>
  <el-table :data="rows" empty-text="暂无投档结果" max-height="440">
    <el-table-column prop="examNumber" label="准考证号" min-width="125" />
    <el-table-column prop="candidateName" label="姓名" min-width="100" />
    <el-table-column label="科类" width="90"><template #default="{ row }">{{ categoryName(row.category) }}</template></el-table-column>
    <el-table-column label="状态" min-width="135"><template #default="{ row }"><el-tag :type="row.status === 'FILED' ? 'success' : 'warning'">{{ resultName(row.status) }}</el-tag></template></el-table-column>
    <el-table-column label="院校专业组" min-width="230"><template #default="{ row }">{{ row.institutionName || '未投档' }} {{ row.groupCode }} {{ row.groupName }}</template></el-table-column>
    <el-table-column prop="matchedPreferenceNo" label="志愿序号" width="90" />
    <el-table-column prop="reason" label="原因" min-width="220" />
    <el-table-column label="检索轨迹" width="90"><template #default="{ row }"><el-button :icon="Search" circle title="查看检索轨迹" aria-label="查看检索轨迹" @click="$emit('trace', row)" /></template></el-table-column>
  </el-table>
</template>
<script setup>
import { Search } from '@element-plus/icons-vue'
import { categoryName, resultName } from '../utils/workflow'
defineProps({ rows: { type: Array, default: () => [] } })
defineEmits(['trace'])
</script>
