<template>
  <div class="page-container">
    <div class="page-header">
      <h2>学生信息管理</h2>
      <el-button type="primary" @click="handleAdd" v-if="store.isAdmin()">
        <el-icon><Plus /></el-icon>新增学生
      </el-button>
    </div>

    <el-card class="filter-card">
      <el-form :inline="true" :model="query">
        <el-form-item label="姓名"><el-input v-model="query.name" placeholder="搜索姓名" clearable /></el-form-item>
        <el-form-item label="学号"><el-input v-model="query.studentNo" placeholder="搜索学号" clearable /></el-form-item>
        <el-form-item label="班级">
          <el-select v-model="query.classId" placeholder="全部" clearable>
            <el-option v-for="c in classes" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item><el-button type="primary" @click="loadData">查询</el-button></el-form-item>
      </el-form>
    </el-card>

    <el-card>
      <el-table :data="students" stripe border style="width: 100%">
        <el-table-column prop="studentNo" label="学号" width="120" />
        <el-table-column prop="name" label="姓名" width="100" />
        <el-table-column prop="gender" label="性别" width="70" />
        <el-table-column prop="totalScore" label="高考总分" width="100" sortable />
        <el-table-column prop="provinceName" label="省份" width="100" />
        <el-table-column prop="className" label="班级" width="120" />
        <el-table-column prop="subjectCombo" label="选科组合" width="120" />
        <el-table-column prop="phone" label="联系电话" width="130" />
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="handleInterest(row)">兴趣课</el-button>
            <el-button size="small" type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)" v-if="store.isAdmin()">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑学生' : '新增学生'" width="600px">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="学号" prop="studentNo"><el-input v-model="form.studentNo" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="姓名" prop="name"><el-input v-model="form.name" /></el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="性别">
              <el-radio-group v-model="form.gender"><el-radio value="男">男</el-radio><el-radio value="女">女</el-radio></el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="高考总分" prop="totalScore"><el-input-number v-model="form.totalScore" :min="0" :max="750" /></el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="省份">
              <el-select v-model="form.provinceId" placeholder="选择省份" filterable>
                <el-option v-for="p in provinces" :key="p.id" :label="p.name" :value="p.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="班级">
              <el-select v-model="form.classId" placeholder="选择班级">
                <el-option v-for="c in classes" :key="c.id" :label="c.name" :value="c.id" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="选科组合">
              <el-select v-model="form.subjectCombo" placeholder="选择科组">
                <el-option label="物化生" value="物化生" /><el-option label="物化地" value="物化地" />
                <el-option label="物政生" value="物政生" /><el-option label="史政地" value="史政地" />
                <el-option label="史政生" value="史政生" /><el-option label="史地生" value="史地生" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系电话"><el-input v-model="form.phone" /></el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="interestVisible" title="兴趣课程" width="500px">
      <div style="margin-bottom: 10px;">
        <el-tag v-for="(c, i) in interestList" :key="i" closable @close="interestList.splice(i, 1)"
          style="margin-right: 8px; margin-bottom: 8px;">{{ c }}</el-tag>
      </div>
      <div style="display: flex; gap: 8px;">
        <el-input v-model="newInterest" placeholder="输入兴趣课程" @keyup.enter="addInterest" />
        <el-button type="primary" @click="addInterest">添加</el-button>
      </div>
      <template #footer>
        <el-button @click="interestVisible = false">取消</el-button>
        <el-button type="primary" @click="saveInterest">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useUserStore } from '../../stores/user'
import { getStudents, saveStudent, deleteStudent, getInterestCourses, saveInterestCourses, getAllClasses, getProvinces } from '../../api'
import { ElMessage, ElMessageBox } from 'element-plus'

const store = useUserStore()
const students = ref([])
const classes = ref([])
const provinces = ref([])
const dialogVisible = ref(false)
const interestVisible = ref(false)
const isEdit = ref(false)
const formRef = ref()
const interestList = ref([])
const newInterest = ref('')
const currentStudentId = ref(null)

const query = reactive({ name: '', studentNo: '', classId: null })
const form = reactive({ id: null, studentNo: '', name: '', gender: '男', totalScore: null, provinceId: null, classId: null, subjectCombo: '', phone: '' })
const formRules = {
  studentNo: [{ required: true, message: '请输入学号', trigger: 'blur' }],
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  totalScore: [{ required: true, message: '请输入高考总分', trigger: 'blur' }]
}

const loadData = async () => {
  const res = await getStudents(query)
  students.value = res.data
}

const handleAdd = () => {
  isEdit.value = false
  Object.assign(form, { id: null, studentNo: '', name: '', gender: '男', totalScore: null, provinceId: null, classId: null, subjectCombo: '', phone: '' })
  dialogVisible.value = true
}

const handleEdit = (row) => {
  isEdit.value = true
  Object.assign(form, row)
  dialogVisible.value = true
}

const handleSave = async () => {
  await formRef.value.validate()
  await saveStudent(form)
  ElMessage.success('保存成功')
  dialogVisible.value = false
  loadData()
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm(`确定删除学生 ${row.name}？`, '提示', { type: 'warning' })
  await deleteStudent(row.id)
  ElMessage.success('删除成功')
  loadData()
}

const handleInterest = async (row) => {
  currentStudentId.value = row.id
  const res = await getInterestCourses(row.id)
  interestList.value = res.data.map(c => c.name)
  newInterest.value = ''
  interestVisible.value = true
}

const addInterest = () => {
  if (newInterest.value.trim()) {
    interestList.value.push(newInterest.value.trim())
    newInterest.value = ''
  }
}

const saveInterest = async () => {
  await saveInterestCourses(currentStudentId.value, { courses: interestList.value })
  ElMessage.success('保存成功')
  interestVisible.value = false
}

onMounted(async () => {
  const [sRes, cRes, pRes] = await Promise.all([getStudents(query), getAllClasses(), getProvinces()])
  students.value = sRes.data
  classes.value = cRes.data
  provinces.value = pRes.data
})
</script>

<style scoped>
.page-container { padding: 20px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.page-header h2 { margin: 0; }
.filter-card { margin-bottom: 16px; }
</style>