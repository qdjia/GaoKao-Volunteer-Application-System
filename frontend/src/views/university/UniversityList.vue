<template>
  <div class="page-container">
    <div class="page-header"><h2>大学院系专业管理</h2></div>

    <el-row :gutter="16">
      <el-col :span="8">
        <el-card>
          <template #header>
            <div style="display:flex;justify-content:space-between;align-items:center">
              <span>大学列表</span>
              <el-button type="primary" size="small" @click="handleAddUniv">新增</el-button>
            </div>
          </template>
          <el-input v-model="univQuery" placeholder="搜索大学" clearable style="margin-bottom:12px" />
          <div v-for="u in filteredUniversities" :key="u.id" class="list-item" :class="{ active: selectedUniv?.id === u.id }"
            @click="selectUniv(u)">
            <span>{{ u.name }}</span>
            <el-tag size="small" :type="u.type === '985' ? 'danger' : u.type === '211' ? 'warning' : 'info'">{{ u.type }}</el-tag>
          </div>
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card>
          <template #header>
            <div style="display:flex;justify-content:space-between;align-items:center">
              <span>院系列表{{ selectedUniv ? ' - ' + selectedUniv.name : '' }}</span>
              <el-button type="primary" size="small" @click="handleAddDept" :disabled="!selectedUniv">新增</el-button>
            </div>
          </template>
          <div v-for="d in departments" :key="d.id" class="list-item" :class="{ active: selectedDept?.id === d.id }"
            @click="selectDept(d)">
            {{ d.name }}
          </div>
          <el-empty v-if="!selectedUniv" description="请先选择大学" :image-size="60" />
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card>
          <template #header>
            <div style="display:flex;justify-content:space-between;align-items:center">
              <span>专业列表{{ selectedDept ? ' - ' + selectedDept.name : '' }}</span>
              <el-button type="primary" size="small" @click="handleAddMajor" :disabled="!selectedDept">新增</el-button>
            </div>
          </template>
          <div v-for="m in majors" :key="m.id" class="major-item" @click="handleEditMajor(m)">
            <div><strong>{{ m.name }}</strong></div>
            <div style="font-size:12px;color:#909399">
              选科要求：{{ m.subjectReq || '无' }} | 招生计划：{{ m.totalQuota }}
            </div>
          </div>
          <el-empty v-if="!selectedDept" description="请先选择院系" :image-size="60" />
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="univDialogVisible" :title="isEdit ? '编辑大学' : '新增大学'" width="500px">
      <el-form :model="univForm" label-width="80px">
        <el-form-item label="大学名称"><el-input v-model="univForm.name" /></el-form-item>
        <el-form-item label="类型">
          <el-select v-model="univForm.type"><el-option label="985" value="985" /><el-option label="211" value="211" /><el-option label="普通" value="普通" /></el-select>
        </el-form-item>
        <el-form-item label="省份">
          <el-select v-model="univForm.provinceId" filterable placeholder="选择省份">
            <el-option v-for="p in provinces" :key="p.id" :label="p.name" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="录取批次">
          <el-select v-model="univForm.batch"><el-option label="本科一批" value="本科一批" /><el-option label="本科二批" value="本科二批" /></el-select>
        </el-form-item>
      </el-form>
      <template #footer><el-button @click="univDialogVisible = false">取消</el-button><el-button type="primary" @click="saveUniv">确定</el-button></template>
    </el-dialog>

    <el-dialog v-model="deptDialogVisible" :title="isEdit ? '编辑院系' : '新增院系'" width="400px">
      <el-form :model="deptForm" label-width="80px">
        <el-form-item label="院系名称"><el-input v-model="deptForm.name" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="deptDialogVisible = false">取消</el-button><el-button type="primary" @click="saveDept">确定</el-button></template>
    </el-dialog>

    <el-dialog v-model="majorDialogVisible" :title="isEdit ? '编辑专业' : '新增专业'" width="500px">
      <el-form :model="majorForm" label-width="100px">
        <el-form-item label="专业名称"><el-input v-model="majorForm.name" /></el-form-item>
        <el-form-item label="选科要求"><el-input v-model="majorForm.subjectReq" placeholder="如：物理 或 物化生" /></el-form-item>
        <el-form-item label="招生计划数"><el-input-number v-model="majorForm.totalQuota" :min="0" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="majorDialogVisible = false">取消</el-button><el-button type="primary" @click="saveMajorItem">确定</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { getUniversities, saveUniversity, getDepartments, saveDepartment, getMajors, saveMajor, getProvinces } from '../../api'
import { ElMessage } from 'element-plus'

const universities = ref([])
const departments = ref([])
const majors = ref([])
const provinces = ref([])
const selectedUniv = ref(null)
const selectedDept = ref(null)
const univQuery = ref('')
const isEdit = ref(false)

const univDialogVisible = ref(false)
const deptDialogVisible = ref(false)
const majorDialogVisible = ref(false)

const univForm = reactive({ id: null, name: '', type: '985', provinceId: null, batch: '本科一批' })
const deptForm = reactive({ id: null, name: '', universityId: null })
const majorForm = reactive({ id: null, name: '', departmentId: null, subjectReq: '', totalQuota: 0 })

const filteredUniversities = computed(() => {
  if (!univQuery.value) return universities.value
  return universities.value.filter(u => u.name.includes(univQuery.value))
})

const loadUniversities = async () => { const res = await getUniversities({}); universities.value = res.data }

const selectUniv = async (u) => {
  selectedUniv.value = u
  selectedDept.value = null
  majors.value = []
  const res = await getDepartments(u.id)
  departments.value = res.data
}

const selectDept = async (d) => {
  selectedDept.value = d
  const res = await getMajors({ departmentId: d.id })
  majors.value = res.data
}

const handleAddUniv = () => { isEdit.value = false; Object.assign(univForm, { id: null, name: '', type: '985', provinceId: null, batch: '本科一批' }); univDialogVisible.value = true }
const saveUniv = async () => { await saveUniversity(univForm); ElMessage.success('保存成功'); univDialogVisible.value = false; loadUniversities() }

const handleAddDept = () => { isEdit.value = false; Object.assign(deptForm, { id: null, name: '', universityId: selectedUniv.value.id }); deptDialogVisible.value = true }
const saveDept = async () => { await saveDepartment(deptForm); ElMessage.success('保存成功'); deptDialogVisible.value = false; selectUniv(selectedUniv.value) }

const handleAddMajor = () => { isEdit.value = false; Object.assign(majorForm, { id: null, name: '', departmentId: selectedDept.value.id, subjectReq: '', totalQuota: 0 }); majorDialogVisible.value = true }
const handleEditMajor = (m) => { isEdit.value = true; Object.assign(majorForm, m); majorDialogVisible.value = true }
const saveMajorItem = async () => { await saveMajor(majorForm); ElMessage.success('保存成功'); majorDialogVisible.value = false; selectDept(selectedDept.value) }

onMounted(async () => { const [uRes, pRes] = await Promise.all([loadUniversities(), getProvinces()]); provinces.value = pRes.data })
</script>

<style scoped>
.page-container { padding: 20px; }
.page-header { margin-bottom: 16px; }
.page-header h2 { margin: 0; }
.list-item { padding: 10px 12px; cursor: pointer; border-radius: 4px; margin-bottom: 4px; display: flex; justify-content: space-between; align-items: center; }
.list-item:hover { background: #f5f7fa; }
.list-item.active { background: #ecf5ff; color: #409eff; }
.major-item { padding: 10px 12px; cursor: pointer; border-radius: 4px; margin-bottom: 4px; border: 1px solid #ebeef5; }
.major-item:hover { border-color: #409eff; }
</style>