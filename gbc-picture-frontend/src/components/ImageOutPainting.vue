<template>
  <a-modal
    class="image-out-painting"
    :style="{
      textAlign: 'center',
    }"
    v-model:open="open"
    title="AI 扩图"
    :footer="false"
    @cancel="closeModal"
  >
    <div style="margin-bottom: 16px"></div>
    <a-row gutter="16">
      <a-col :span="resultImageUrl ? 12 : 24">
        <h4 v-show="resultImageUrl">原图</h4>
        <img :src="picture.url" :alt="`${picture.name}--原图`" style="max-width: 100%" />
        <div style="margin-bottom: 16px"></div>
        <a-button type="primary" ghost :loading="!!taskId" @click="createTask">
          生成图片
        </a-button>
      </a-col>
      <a-col v-if="resultImageUrl" :span="12">
        <h4>AI扩图结果</h4>
        <img :src="resultImageUrl" :alt="`${picture.name}--AI扩图`" style="max-width: 100%" />
        <div style="margin-bottom: 16px"></div>
        <a-flex justify="center" gap="16">
          <a-button type="primary" ghost  @click="(e)=>handleCreateOrUpdate(false)"> 创建新图片 </a-button>
          <a-button type="primary" @click="(e)=>handleCreateOrUpdate(true)"> 替换原图 </a-button>
        </a-flex>
      </a-col>
    </a-row>
    <div style="margin-bottom: 16px"></div>
  </a-modal>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { message } from 'ant-design-vue'
import {
  createPictureOutPaintingTaskUsingPost,
  getPictureOutPaintingTaskResponseUsingGet, uploadPictureByUrlUsingPost
} from '@/api/pictureController.ts'
import { PIC_OUT_PAINTING_STATUS_MAP } from '@/constant/picture.ts'

interface Props {
  picture: API.PictureVO
  spaceId: string | number | undefined
  onSuccess?: (newPicture: API.PictureVO) => void
}

const props = withDefaults(defineProps<Props>(), {
  onSuccess: () => {},
})

// AI 扩图结果URL
const resultImageUrl = ref<string>()

// 编辑器组件的引用
const taskId = ref<string>()

/**
 * 创建任务
 */
const createTask = async () => {
  const pictureId = props.picture?.id
  if (!pictureId) return
  try {
    const res = await createPictureOutPaintingTaskUsingPost({
      pictureId,
      parameters: { xScale: 2, yScale: 2 },
    })
    if (res.data.code === 0 && res.data.data) {
      message.success('AI扩图进行中，请耐心等待...')
      taskId.value = res.data.data.output?.taskId
      console.log(taskId.value)
      // 开启轮询请求后端查看任务执行是否已经生成结果
      startPolling()
    } else {
      message.error('AI扩图失败')
    }
  } catch (e: any) {
    message.error('AI扩图失败', e)
  }
}

// 轮询定时器
let pollingTimer: NodeJS.Timeout = null

/**
 * 轮询请求后端 AI 扩图结果
 */
const startPolling = () => {
  const paramTaskId = taskId.value
  if (!paramTaskId) return
  pollingTimer = setInterval(async () => {
    try {
      const res = await getPictureOutPaintingTaskResponseUsingGet({ taskId: paramTaskId })
      if (res.data.code === 0 && res.data.data) {
        const output = res.data.data.output
        const taskStatus = output?.taskStatus
        const outputImageUrl = output?.outputImageUrl
        console.log(taskStatus)
        if (taskStatus === PIC_OUT_PAINTING_STATUS_MAP.SUCCEEDED) {
          message.success('AI扩图成功')
          resultImageUrl.value = outputImageUrl
        } else if (taskStatus === PIC_OUT_PAINTING_STATUS_MAP.FAILED) {
          message.error('AI扩图失败 '+ output?.message)
        } else {
          return
        }
        // 清理轮询
        clearPolling()
      }
    } catch (e: any) {
      message.error('获取AI扩图结果失败')
      clearPolling()
    }
  }, 3000)
}

/**
 * 关闭定时轮询
 */
const clearPolling = () => {
  if(pollingTimer) clearInterval(pollingTimer)
  pollingTimer = null
  taskId.value = null
}

const loading = ref<boolean>(false)

/**
 * 创建新图
 * @param file
 */
const handleCreateOrUpdate = async (update: boolean) => {
  // loading用于控制等待效果 true 表示等待中， false 表示完成
  loading.value = true
  try {
    const params: API.PictureUploadRequest = update ? { id: props.picture.id } : {}
    params.spaceId = props.spaceId
    params.fileUrl = resultImageUrl.value
    const res = await uploadPictureByUrlUsingPost(params)
    if (res.data.code === 0 && res.data.data) {
      message.success('图片上传成功')
      // 将上传成功的图片信息传递给父组件
      props.onSuccess?.(res.data.data)
      closeModal()
    } else {
      message.error('图片上传失败')
    }
  } catch (error: any) {
    message.error('图片上传失败' + error.message)
  }
  loading.value = false
}

const open = ref<boolean>(false)
const openModal = () => {
  open.value = true
}

const closeModal = () => {
  open.value = false
}

defineExpose({
  openModal,
})
</script>

<style scoped>

</style>
