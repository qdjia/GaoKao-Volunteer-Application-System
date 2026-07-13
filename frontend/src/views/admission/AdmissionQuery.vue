<template>
  <div class="page-container">
    <div class="page-header"><h2>录取查询</h2></div>

    <el-card class="filter-card">
      <el-form :inline="true" :model="query">
        <el-form-item label="查询方式">
          <el-radio-group v-model="queryType">
            <el-radio value="university">按学校查询</el-radio>
            <el-radio value="student">按学生查询</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="queryType === 'university'" label="大学">
          <el-select v-model="query.universityId" placeholder="选择大学" filterable clearable>
            <el-option v-for="u in universities" :key="u.id" :label="u.name" :value="u.id" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="queryType === 'student'" label="学号/姓名">
          <el-input v-model="studentKeyword" placeholder="输入学号或姓名" clearable />
        </el-form-item>
        <el-form-item label="录取状态">
          <el-select v-model="query.status" placeholder="全部" clearable>
            <el-option label="已录取" value="ADMITTED" /><el-option label="未录取" value="UNADMITTED" />
            <el-option label="未填报" value="NO_APPLICATION" />
          </el-select>
        </el-form-item>
        <el-form-item><el-button type="primary" @click="search">查询</el-button></el-form-item>
      </el-form>
    </el-card>

    <el-card>
      <el-table :data="results" stripe border>
        <el-table-column prop="studentNo" label="学号" width="120" />
        <el-table-column prop="studentName" label="姓名" width="100" />
        <el-table-column prop="className" label="班级" width="120" />
        <el-table-column prop="universityName" label="录取大学" width="140" />
        <el-table-column prop="majorName" label="录取专业" width="160" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ADMITTED' ? 'success' : row.status === 'UNADMITTED' ? 'danger' : 'info'" size="small">
              {{ row.status === 'ADMITTED' ? '已录取' : row.status === 'UNADMITTED' ? '未录取' : row.status === 'NO_APPLICATION' ? '未填报' : row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="isAdjusted" label="调剂" width="80">
          <template #default="{ row }">
            <el-tag v-if="row.isAdjusted" type="warning" size="small">是</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="reason" label="分配课程/原因" min-width="200" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getAdmissionResults, getUniversities, getStudents } from '../../api'

const queryType = ref('university')
const studentKeyword = ref('')
const universities = ref([])
const results = ref([])
const query = reactive({ universityId: null, studentId: null, status: null })

const search = async () => {
  const params = { ...query }
  if (queryType.value === 'student' && studentKeyword.value) {
    const sRes = await getStudents({ studentNo: studentKeyword.value, name: studentKeyword.value })
    if (sRes.data.length > 0) {
      params.studentId = sRes.data[0].id
    }
  }
  const res = await getAdmissionResults(params)
  results.value = res.data
}

onMounted(async () => {
  const res = await getUniversities({})
  universities.value = res.data
  search()
})
</script>

<style scoped>
.page-container { padding: 20px; }
.page-header { margin-bottom: 16px; }
.page-header h2 { margin: 0; }
.filter-card { margin-bottom: 16px; }
</style>