<template>
  <div class="space-size-analyze">
    <a-card title="空间图片大小分析">
      <v-chart :option="options" style="height: 320px; max-width: 100%" :loading="loading" />
    </a-card>
  </div>
</template>

<script setup lang="ts">
import VChart from 'vue-echarts'
import 'echarts'
import { analyzeSpaceSizeUsingPost } from '@/api/spaceAnalyzeController.ts'
import { computed, ref, watchEffect } from 'vue'
import { message } from 'ant-design-vue'

interface Props {
  queryAll?: boolean
  queryPublic?: boolean
  spaceId?: string | number | undefined
}

const props = withDefaults(defineProps<Props>(), {
  queryAll: false,
  queryPublic: false,
})

const loading = ref<boolean>(false)
const dataList = ref<API.SpaceSizeAnalyzeResponse[]>([])

// 获取渲染数据
const fetchData = async () => {
  loading.value = true
  try {
    const res = await analyzeSpaceSizeUsingPost({
      queryAll: props.queryAll,
      queryPublic: props.queryPublic,
      spaceId: props?.spaceId,
    })
    const response = res.data
    if (response.code === 0) {
      dataList.value = response.data ?? []
    } else {
      message.error('空间图片大小分析数据获取失败 ' + response.message)
    }
  } catch (e: any) {
    message.error('空间图片大小分析数据获取失败 ' + e)
  }
  loading.value = false
}

/**
 * 监听 props 属性，参数改变的时候触发数据的重新加载
 */
watchEffect(() => {
  fetchData()
})

/**
 * 图表选项
 */
const options = computed(() => {
  const pieData = dataList.value.map((item) => ({
    name: item.sizeRange,
    value: item.count ?? 0,
  }))

  return {
    tooltip: {
      trigger: 'item',
      formatter: '{a} <br{b}: {c} ({d}%)',
    },
    legend: {
      top: 'bottom',
    },
    series: [
      {
        name: '图片大小',
        type: 'pie',
        radius: '50%',
        data: pieData,
      },
    ],
  }
})
</script>

<style scoped></style>
