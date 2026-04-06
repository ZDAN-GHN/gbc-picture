<template>
  <div class="space-usage-analyze">
    <a-flex gap="middle">
      <a-card title="存储空间" style="width: 50%">
        <div style="height: 320px; text-align: center">
          <h3>
            {{ formatSize(data.usedSize) }} /
            <span v-if="data.maxSize">{{ formatSize(data.maxSize) }}</span>
            <span v-else style="font-size: 20px">∞</span>
          </h3>
          <a-progress :loading="loading" type="dashboard" :percent="data.sizeUsageRatio ?? 0" />
        </div>
      </a-card>
      <a-card title="图片数量" style="width: 50%">
        <div style="height: 320px; text-align: center">
          <h3>
            {{ data.usedCount }} /
            <span v-if="data.maxSize">{{ data.maxCount }}</span>
            <span v-else style="font-size: 20px">∞</span>
          </h3>
          <a-progress :loading="loading" type="dashboard" :percent="data.countUsageRatio ?? 0" />
        </div>
      </a-card>
    </a-flex>
  </div>
</template>

<script setup lang="ts">
import { analyzeSpaceUsageUsingPost } from '@/api/spaceAnalyzeController.ts'
import { ref, watchEffect } from 'vue'
import { message } from 'ant-design-vue'
import { formatSize } from '@/utils'

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
const data = ref<API.SpaceUsageAnalyzeResponse>({})

// 获取渲染数据
const fetchData = async () => {
  loading.value = true
  try {
    const res = await analyzeSpaceUsageUsingPost({
      queryAll: props.queryAll,
      queryPublic: props.queryPublic,
      spaceId: props?.spaceId,
    })
    const response = res.data
    if (response.code === 0) {
      data.value = response.data ?? {}
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
</script>

<style scoped></style>
