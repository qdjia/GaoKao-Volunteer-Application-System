<template>
  <div class="page-container">
    <div class="page-header">
      <h2>班级管理</h2>
      <el-button type="primary" @click="handleAdd" v-if="store.isAdmin()"><el-icon><Plus /></el-icon>新增班级</el-button>
    </div>
    <el-card>
      <el-table :data="classes" stripe border>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="班级名称" width="160" />
        <el-table-column prop="grade" label="年级" width="100" />
        <el-table-column prop="teacher" label="班主任" width="120" />
        <el-table-column prop="provinceName" label="省份" width="120" />
        <el-table-column prop="studentCount" label="学生数" width="100" />
        <el-table-column label="操作" width="180">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)" v-if="store.isAdmin()">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑班级' : '新增班级'" width="500px">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="80px">
        <el-form-item label="班级名称" prop="name"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="年级"><el-input v-model="form.grade" /></el-form-item>
        <el-form-item label="班主任"><el-input v-model="form.teacher" /></el-form-item>
        <el-form-item label="省份">
          <el-select v-model="form.provinceId" placeholder="选择省份" filterable>
            <el-option v-for="p in provinces" :key="p.id" :label="p.name" :value="p.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useUserStore } from '../../stores/user'
import { getClasses, saveClass, deleteClass, getProvinces } from '../../api'
import { ElMessage, ElMessageBox } from 'element-plus'

const store = useUserStore()
const classes = ref([])
const provinces = ref([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref()

const form = reactive({ id: null, name: '', grade: '', teacher: '', provinceId: null })
const formRules = { name: [{ required: true, message: '请输入班级名称', trigger: 'blur' }] }

const loadData = async () => { const res = await getClasses({}); classes.value = res.data }

const handleAdd = () => { isEdit.value = false; Object.assign(form, { id: null, name: '', grade: '', teacher: '', provinceId: null }); dialogVisible.value = true }
const handleEdit = (row) => { isEdit.value = true; Object.assign(form, row); dialogVisible.value = true }
const handleSave = async () => { await formRef.value.validate(); await saveClass(form); ElMessage.success('保存成功'); dialogVisible.value = false; loadData() }
const handleDelete = async (row) => { await ElMessageBox.confirm(`确定删除班级 ${row.name}？`, '提示', { type: 'warning' }); await deleteClass(row.id); ElMessage.success('删除成功'); loadData() }

onMounted(async () => { const [cRes, pRes] = await Promise.all([loadData(), getProvinces()]); provinces.value = pRes.data })
</script>

<style scoped>
.page-container { padding: 20px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.page-header h2 { margin: 0; }
</style>