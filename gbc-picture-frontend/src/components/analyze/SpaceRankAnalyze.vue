<template>
  <div class="space-rank-analyze">
    <a-card title="空间使用排行分析">
      <v-chart :option="options" style="height: 320px; max-width: 100%" :loading="loading" />
      <template #extra>
        <a-space size="middle" align="center">
          前
          <a-input-number
            v-model:value="nextTopN"
            :min="1"
            @blur="handleBlur"
            @pressEnter="handlePressEnter"
          />
          条
        </a-space>
      </template>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import VChart from 'vue-echarts'
import 'echarts'
import { analyzeSpaceRankUsingPost } from '@/api/spaceAnalyzeController.ts'
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
const dataList = ref<API.Space[]>([])

const topN = ref<number>(10) //后端默认获取前 10 条，这里做标识
const nextTopN = ref<number>(topN.value)

const handleBlur = () => {
  // 只有当值真正改变时才执行搜索
  if (nextTopN.value !== topN.value) {
    topN.value = nextTopN.value
  }
}

const handlePressEnter = () => {
  // 回车时直接执行搜索
  topN.value = nextTopN.value
  fetchData()
}

// 获取渲染数据
const fetchData = async () => {
  loading.value = true
  try {
    const res = await analyzeSpaceRankUsingPost({
      queryAll: props.queryAll,
      queryPublic: props.queryPublic,
      spaceId: props?.spaceId,
      topN: topN.value,
    })
    const response = res.data
    if (response.code === 0) {
      dataList.value = response.data ?? []
    } else {
      message.error('空间使用排行分析数据获取失败 ' + response.message)
    }
  } catch (e: any) {
    message.error('空间使用排行分析数据获取失败 ' + e)
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
  const spaceNames = dataList.value.map((item) => item.spaceName)
  const usageData = dataList.value.map((item) => ((item.totalSize ?? 0) / (1024 * 1024)).toFixed(2)) // 转为 MB

  return {
    tooltip: { trigger: 'axis' },
    xAxis: {
      type: 'category',
      data: spaceNames,
    },
    yAxis: {
      type: 'value',
      name: '空间使用量 (MB)',
    },
    series: [
      {
        name: '空间使用量 (MB)',
        type: 'bar',
        data: usageData,
        itemStyle: {
          color: '#5470C6', // 自定义柱状图颜色
        },
      },
    ],
  }
})
</script>

<style scoped></style>
