<script setup lang="ts">
import { computed, ref, onMounted } from 'vue'
import { get } from '@/api/request'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, BarChart, PieChart } from 'echarts/charts'
import {
  GridComponent,
  TooltipComponent,
  LegendComponent,
  TitleComponent,
} from 'echarts/components'

use([
  CanvasRenderer,
  LineChart,
  BarChart,
  PieChart,
  GridComponent,
  TooltipComponent,
  LegendComponent,
  TitleComponent,
])

interface Dashboard {
  totalUsers: number
  totalOrders: number
  totalProducts: number
  totalSales: number
  todayOrders: number
  todaySales: number
  trend: { date: string; orders: number; sales: number }[]
  statusCount: Record<string, number>
  hotProducts: { title: string; sales: number; revenue: number }[]
}

const data = ref<Dashboard | null>(null)
const loading = ref(true)

onMounted(async () => {
  try {
    data.value = await get<Dashboard>('/admin/dashboard')
  } catch {
    data.value = {
      totalUsers: 128,
      totalOrders: 356,
      totalProducts: 48,
      totalSales: 125680,
      todayOrders: 12,
      todaySales: 4680,
      trend: [
        { date: '06-06', orders: 8, sales: 3200 },
        { date: '06-07', orders: 15, sales: 5800 },
        { date: '06-08', orders: 10, sales: 4100 },
        { date: '06-09', orders: 18, sales: 7200 },
        { date: '06-10', orders: 14, sales: 5600 },
        { date: '06-11', orders: 20, sales: 8900 },
        { date: '06-12', orders: 12, sales: 4680 },
      ],
      statusCount: { pending_payment: 5, paid: 3, shipping: 2, completed: 40, cancelled: 2 },
      hotProducts: [
        { title: 'Auralite 降噪耳机', sales: 120, revenue: 47880 },
        { title: '松谷蓝莓礼盒', sales: 89, revenue: 7921 },
        { title: 'TrailPro 徒步鞋', sales: 56, revenue: 25704 },
      ],
    }
  } finally {
    loading.value = false
  }
})

const trendOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  legend: { data: ['订单数', '销售额(元)'] },
  xAxis: { type: 'category', data: data.value?.trend.map((t) => t.date) || [] },
  yAxis: [
    { type: 'value', name: '订单数' },
    { type: 'value', name: '销售额' },
  ],
  series: [
    {
      name: '订单数',
      type: 'bar',
      data: data.value?.trend.map((t) => t.orders) || [],
      itemStyle: { color: '#10b981' },
    },
    {
      name: '销售额(元)',
      type: 'line',
      yAxisIndex: 1,
      data: data.value?.trend.map((t) => t.sales) || [],
      itemStyle: { color: '#f59e0b' },
    },
  ],
}))

const pieOption = computed(() => ({
  tooltip: { trigger: 'item' },
  legend: { orient: 'vertical', left: 'left' },
  series: [
    {
      type: 'pie',
      radius: '60%',
      data: Object.entries(data.value?.statusCount || {}).map(([k, v]) => ({
        name:
          {
            pending_payment: '待付款',
            paid: '已付款',
            shipping: '配送中',
            completed: '已完成',
            cancelled: '已取消',
          }[k] || k,
        value: v,
      })),
      emphasis: { itemStyle: { shadowBlur: 10, shadowOffsetX: 0, shadowColor: 'rgba(0,0,0,0.5)' } },
    },
  ],
}))

const barOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  xAxis: { type: 'category', data: data.value?.hotProducts.map((h) => h.title) || [] },
  yAxis: { type: 'value' },
  series: [
    {
      name: '销量',
      type: 'bar',
      data: data.value?.hotProducts.map((h) => h.sales) || [],
      itemStyle: { color: '#10b981' },
    },
  ],
}))
</script>

<template>
  <div>
    <h1 style="margin-bottom: 24px">数据看板</h1>

    <a-spin :spinning="loading">
      <!-- 概览卡片 -->
      <a-row :gutter="16" style="margin-bottom: 24px">
        <a-col
          :span="6"
          v-for="card in [
            { label: '总用户', value: data?.totalUsers, icon: 'U' },
            { label: '总订单', value: data?.totalOrders, icon: 'O' },
            { label: '总商品', value: data?.totalProducts, icon: 'P' },
            {
              label: '总销售额',
              value: '￥' + (data?.totalSales || 0).toLocaleString(),
              icon: '¥',
            },
          ]"
          :key="card.label"
        >
          <a-card :bordered="false" style="border-radius: 12px">
            <div class="stat-icon">{{ card.icon }}</div>
            <div style="color: #8c8c8c; margin: 8px 0">{{ card.label }}</div>
            <div style="font-size: 1.5rem; font-weight: 800">{{ card.value }}</div>
          </a-card>
        </a-col>
      </a-row>

      <!-- 今日统计 -->
      <a-row :gutter="16" style="margin-bottom: 24px">
        <a-col :span="12">
          <a-card title="今日概览" :bordered="false" style="border-radius: 12px">
            <a-statistic title="今日订单" :value="data?.todayOrders" style="margin-right: 32px" />
            <a-statistic title="今日销售额" :value="data?.todaySales" prefix="￥" />
          </a-card>
        </a-col>
        <a-col :span="12">
          <a-card title="订单状态分布" :bordered="false" style="border-radius: 12px">
            <v-chart :option="pieOption" autoresize style="height: 220px" />
          </a-card>
        </a-col>
      </a-row>

      <!-- 趋势图 -->
      <a-row :gutter="16" style="margin-bottom: 24px">
        <a-col :span="24">
          <a-card title="近7天趋势" :bordered="false" style="border-radius: 12px">
            <v-chart :option="trendOption" autoresize style="height: 300px" />
          </a-card>
        </a-col>
      </a-row>

      <!-- 热卖排行 -->
      <a-row :gutter="16">
        <a-col :span="24">
          <a-card title="热卖商品 Top 5" :bordered="false" style="border-radius: 12px">
            <v-chart :option="barOption" autoresize style="height: 250px" />
          </a-card>
        </a-col>
      </a-row>
    </a-spin>
  </div>
</template>

<style scoped>
.stat-icon {
  display: inline-grid;
  width: 42px;
  height: 42px;
  place-items: center;
  color: #fff;
  background: linear-gradient(135deg, var(--primary), #a78bfa);
  border-radius: 14px;
  box-shadow: 0 12px 26px rgb(124 58 237 / 0.22);
  font-size: 1.1rem;
  font-weight: 900;
}
</style>
