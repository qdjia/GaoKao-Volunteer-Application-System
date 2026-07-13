<template>
  <div class="page-container">
    <div class="page-header"><h2>分数线管理</h2></div>

    <el-tabs v-model="activeTab">
      <el-tab-pane label="省控线" name="provincial">
        <el-card class="filter-card">
          <el-form :inline="true" :model="pQuery">
            <el-form-item label="省份">
              <el-select v-model="pQuery.provinceId" placeholder="全部" clearable filterable>
                <el-option v-for="p in provinces" :key="p.id" :label="p.name" :value="p.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="年份"><el-input-number v-model="pQuery.year" :min="2020" :max="2026" /></el-form-item>
            <el-form-item><el-button type="primary" @click="loadProvincial">查询</el-button></el-form-item>
          </el-form>
        </el-card>
        <el-card>
          <el-table :data="provincialLines" stripe border>
            <el-table-column prop="provinceName" label="省份" width="100" />
            <el-table-column prop="year" label="年份" width="80" />
            <el-table-column prop="batch" label="批次" width="120" />
            <el-table-column prop="subjectType" label="科类" width="80" />
            <el-table-column prop="score" label="分数线" width="100" />
            <el-table-column label="操作" width="150">
              <template #default="{ row }">
                <el-button size="small" type="danger" @click="delProvincial(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="大学投档线" name="university">
        <el-card class="filter-card">
          <el-form :inline="true" :model="uQuery">
            <el-form-item label="大学">
              <el-select v-model="uQuery.universityId" placeholder="全部" clearable filterable>
                <el-option v-for="u in universities" :key="u.id" :label="u.name" :value="u.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="年份"><el-input-number v-model="uQuery.year" :min="2020" :max="2026" /></el-form-item>
            <el-form-item><el-button type="primary" @click="loadUniversity">查询</el-button></el-form-item>
          </el-form>
        </el-card>
        <el-card>
          <el-table :data="universityLines" stripe border>
            <el-table-column prop="universityName" label="大学" width="140" />
            <el-table-column prop="provinceName" label="省份" width="100" />
            <el-table-column prop="year" label="年份" width="80" />
            <el-table-column prop="majorName" label="专业" width="160" />
            <el-table-column prop="minScore" label="最低分" width="100" />
            <el-table-column prop="avgScore" label="平均分" width="100" />
            <el-table-column label="操作" width="150">
              <template #default="{ row }">
                <el-button size="small" type="danger" @click="delUniversity(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getProvincialScoreLines, deleteProvincialScoreLine, getUniversityScoreLines, deleteUniversityScoreLine, getProvinces, getUniversities } from '../../api'
import { ElMessage, ElMessageBox } from 'element-plus'

const activeTab = ref('provincial')
const provinces = ref([])
const universities = ref([])
const provincialLines = ref([])
const universityLines = ref([])

const pQuery = reactive({ provinceId: null, year: 2024 })
const uQuery = reactive({ universityId: null, year: 2024 })

const loadProvincial = async () => { const res = await getProvincialScoreLines(pQuery); provincialLines.value = res.data }
const loadUniversity = async () => { const res = await getUniversityScoreLines(uQuery); universityLines.value = res.data }
const delProvincial = async (row) => { await ElMessageBox.confirm('确定删除？', '提示', { type: 'warning' }); await deleteProvincialScoreLine(row.id); ElMessage.success('删除成功'); loadProvincial() }
const delUniversity = async (row) => { await ElMessageBox.confirm('确定删除？', '提示', { type: 'warning' }); await deleteUniversityScoreLine(row.id); ElMessage.success('删除成功'); loadUniversity() }

onMounted(async () => {
  const [pRes, uRes] = await Promise.all([getProvinces(), getUniversities({})])
  provinces.value = pRes.data
  universities.value = uRes.data
  loadProvincial()
  loadUniversity()
})
</script>

<style scoped>
.page-container { padding: 20px; }
.page-header { margin-bottom: 16px; }
.page-header h2 { margin: 0; }
.filter-card { margin-bottom: 16px; }
</style>