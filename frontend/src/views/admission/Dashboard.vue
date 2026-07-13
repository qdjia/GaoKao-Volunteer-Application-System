<template>
  <div class="page-container">
    <div class="page-header"><h2>数据看板</h2></div>

    <el-row :gutter="16" style="margin-bottom:16px">
      <el-col :span="8">
        <el-card class="stat-card">
          <div class="stat-number" style="color:#409eff">{{ dashboard.totalStudents || 0 }}</div>
          <div class="stat-label">总考生数</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="stat-card">
          <div class="stat-number" style="color:#67c23a">{{ dashboard.admittedStudents || 0 }}</div>
          <div class="stat-label">已录取</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="stat-card">
          <div class="stat-number" style="color:#f56c6c">{{ dashboard.unadmittedStudents || 0 }}</div>
          <div class="stat-label">未录取</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :span="12">
        <el-card>
          <template #header><span>各校录取人数分布</span></template>
          <div ref="barChartRef" style="height:400px"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header><span>分数段分布</span></template>
          <div ref="pieChartRef" style="height:400px"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { getDashboard } from '../../api'

const dashboard = ref({})
const barChartRef = ref(null)
const pieChartRef = ref(null)

const loadDashboard = async () => {
  const res = await getDashboard()
  dashboard.value = res.data
  await nextTick()
  renderCharts()
}

const renderCharts = () => {
  if (barChartRef.value) {
    const barChart = echarts.init(barChartRef.value)
    barChart.setOption({
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'category', data: (dashboard.value.universityStats || []).map(s => s.universityName), axisLabel: { rotate: 30 } },
      yAxis: { type: 'value' },
      series: [{ type: 'bar', data: (dashboard.value.universityStats || []).map(s => s.count), itemStyle: { color: '#409eff' } }],
      grid: { bottom: 80 }
    })
    window.addEventListener('resize', () => barChart.resize())
  }

  if (pieChartRef.value) {
    const pieChart = echarts.init(pieChartRef.value)
    const colors = ['#409eff', '#67c23a', '#e6a23c', '#f56c6c', '#909399', '#b37feb']
    pieChart.setOption({
      tooltip: { trigger: 'item' },
      legend: { orient: 'vertical', left: 'left' },
      series: [{
        type: 'pie', radius: ['40%', '70%'],
        data: (dashboard.value.scoreRanges || []).map((s, i) => ({ value: s.count, name: s.range, itemStyle: { color: colors[i % colors.length] } })),
        label: { formatter: '{b}: {c}人' }
      }]
    })
    window.addEventListener('resize', () => pieChart.resize())
  }
}

onMounted(() => { loadDashboard() })
</script>

<style scoped>
.page-container { padding: 20px; }
.page-header { margin-bottom: 16px; }
.page-header h2 { margin: 0; }
.stat-card { text-align: center; padding: 20px 0; }
.stat-number { font-size: 42px; font-weight: bold; }
.stat-label { font-size: 16px; color: #909399; margin-top: 8px; }
</style>