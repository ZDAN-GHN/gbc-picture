<template>
  <div class="space-user-analyze">
    <a-card title="空间用户上传行为分析">
      <v-chart :option="options" style="height: 320px; max-width: 100%" :loading="loading" />
      <template #extra>
        <a-space>
          <a-segmented v-model:value="timeDimension" :options="timeDimensionOptions" />
          <a-input-search placeholder="请输入用户id" enter-button="搜索用户" @search="doSearch" />
        </a-space>
      </template>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import VChart from 'vue-echarts'
import 'echarts'
import { analyzeSpaceUserUsingPost } from '@/api/spaceAnalyzeController.ts'
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
const dataList = ref<API.SpaceUserAnalyzeResponse[]>([])
// 时间维度
const timeDimension = ref<'day' | 'week' | 'month'>('day')
// 用户 ID 选项
const userId = ref<string | number | undefined>(undefined)

// 时间维度可供选项（分段选择器）
const timeDimensionOptions = [
  {
    label: '日',
    value: 'day',
  },
  {
    label: '周',
    value: 'week',
  },
  {
    label: '月',
    value: 'month',
  },
]

const doSearch = async (value: string) => {
  userId.value = value
}

// 获取渲染数据
const fetchData = async () => {
  loading.value = true
  try {
    const res = await analyzeSpaceUserUsingPost({
      queryAll: props.queryAll,
      queryPublic: props.queryPublic,
      spaceId: props?.spaceId,
      timeDimension: timeDimension.value,
      userId: userId.value,
    })
    const response = res.data
    if (response.code === 0) {
      dataList.value = response.data ?? []
    } else {
      message.error('空间用户上传行为分析数据获取失败 ' + response.message)
    }
  } catch (e: any) {
    message.error('空间用户上传行为分析数据获取失败 ' + e)
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
  const periods = dataList.value.map((item) => item.period) // 时间区间
  const counts = dataList.value.map((item) => item.count) // 上传数量

  return {
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: periods, name: '时间区间' },
    yAxis: { type: 'value', name: '上传数量' },
    series: [
      {
        name: '上传数量',
        type: 'line',
        data: counts,
        smooth: true, // 平滑折线
        emphasis: {
          focus: 'series',
        },
      },
    ],
  }
})
</script>

<style scoped></style>
