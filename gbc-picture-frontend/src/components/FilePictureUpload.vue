<template>
  <div class="file-picture-upload">
    <a-upload
      list-type="picture-card"
      :show-upload-list="false"
      class="picture-upload"
      :custom-request="handleUpload"
      :before-upload="beforeUpload"
    >
      <img v-if="picture?.url" :src="picture?.url" :alt="picture?.name" />
      <div v-else>
        <loading-outlined v-if="loading"></loading-outlined>
        <plus-outlined v-else></plus-outlined>
        <div class="ant-upload-text">点击或拖拽上传图片</div>
      </div>
    </a-upload>
  </div>
</template>
<script lang="ts" setup>
import { ref } from 'vue'
import { PlusOutlined, LoadingOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import type { UploadProps } from 'ant-design-vue'
import { uploadPictureUsingPost } from '@/api/pictureController.ts'

interface Props {
  picture?: API.PictureVO
  spaceId?: Number | String
  onSuccess?: (newPicture: API.PictureVO) => void
}

const props = defineProps<Props>()
const loading = ref<boolean>(false)
/**
 * 上传图片
 * @Param file
 */
const handleUpload = async ({ file }: any) => {
  // loading用于控制等待效果 true 表示等待中， false 表示完成
  loading.value = true
  try {
    const params: API.PictureUploadRequest = props.picture ? { id: props.picture.id } : {}
    params.spaceId = props.spaceId
    const res = await uploadPictureUsingPost(params, {}, file)
    if (res.data.code === 0 && res.data.data) {
      message.success('图片上传成功')
      // 将上传成功的图片信息传递给父组件
      props.onSuccess?.(res.data.data)
    } else {
      message.error('图片上传失败')
    }
  } catch (error: any) {
    message.error('图片上传失败' + error.message)
  }
  loading.value = false
}
const beforeUpload = (file: UploadProps['fileList'][number]) => {
  const picFormat = ['image/jpeg', 'image/jpg', 'image/png', 'image/webp']
  // 校验图片格式
  const isLglFmt = picFormat.indexOf(file.type) !== -1
  if (!isLglFmt) {
    message.error('不支持该格式的图片，推荐 jpg 或 png')
  }
  // 校验图片大小
  const isLt3M = file.size / 1024 / 1024 < 3
  if (!isLt3M) {
    message.error('不能上传超过3M的图片')
  }
  return isLglFmt && isLt3M
}
</script>
<style scoped>
.file-picture-upload :deep(.ant-upload) {
  width: 100% !important;
  height: 100% !important;
  min-width: 152px;
  min-height: 152px;
}

.picture-upload img {
  max-width: 100%;
  max-height: 480px;
}

.avatar-uploader > .ant-upload {
  width: 128px;
  height: 128px;
}
.ant-upload-select-picture-card i {
  font-size: 32px;
  color: #999;
}

.ant-upload-select-picture-card .ant-upload-text {
  margin-top: 8px;
  color: #666;
}
</style>
