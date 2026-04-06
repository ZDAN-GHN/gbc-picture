<template>
  <div class="space-tag-analyze">
    <a-card title="空间图片标签分析">
      <v-chart :option="options" style="height: 320px; max-width: 100%" :loading="loading" />
    </a-card>
  </div>
</template>

<script setup lang="ts">
import VChart from 'vue-echarts'
import 'echarts'
import 'echarts-wordcloud'
import { analyzeSpaceTagUsingPost } from '@/api/spaceAnalyzeController.ts'
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
const dataList = ref<API.SpaceTagAnalyzeResponse[]>([])

// 获取渲染数据
const fetchData = async () => {
  loading.value = true
  try {
    const res = await analyzeSpaceTagUsingPost({
      queryAll: props.queryAll,
      queryPublic: props.queryPublic,
      spaceId: props?.spaceId,
    })
    const response = res.data
    if (response.code === 0) {
      dataList.value = response.data ?? []
    } else {
      message.error('获取数据失败 ' + response.message)
    }
  } catch (e: any) {
    message.error('获取数据失败 ' + e)
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
  const tagData = dataList.value.map((item) => ({
    name: item.tag,
    value: item.count,
  }))

  return {
    tooltip: {
      trigger: 'item',
      formatter: (params: any) => `${params.name}: ${params.value} 次`,
    },
    series: [
      {
        type: 'wordCloud',
        gridSize: 10,
        sizeRange: [12, 50], // 字体大小范围
        rotationRange: [-90, 90],
        shape: 'circle',
        textStyle: {
          color: () =>
            `rgb(${Math.round(Math.random() * 255)}, ${Math.round(
              Math.random() * 255,
            )}, ${Math.round(Math.random() * 255)})`, // 随机颜色
        },
        data: tagData,
      },
    ],
  }
})
</script>

<style scoped></style>
