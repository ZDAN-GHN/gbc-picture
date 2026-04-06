<template>
  <div id="addPictureBatchPage">
    <!--  图片信息表单  -->
    <a-form layout="vertical" name="basic" :model="formData" @finish="handleSubmit">
      <a-form-item name="name" label="关键词">
        <a-input
          v-model:value="formData.searchText"
          placeholder="请输入关键词"
          allow-clear
        ></a-input>
      </a-form-item>
      <a-form-item name="category" label="抓取数量">
        <a-input-number
          v-model:value="formData.count"
          placeholder="请输入抓取数量"
          style="min-width: 180px"
          :min="1"
          :max="30"
          allow-clear
        >
        </a-input-number>
      </a-form-item>
      <a-form-item name="tags" label="名称前缀">
        <a-input
          v-model:value="formData.namePrefix"
          placeholder="请输入名称前缀，会自动补齐序号"
          allow-clear
        ></a-input>
      </a-form-item>
      <a-form-item>
        <a-button type="primary" html-type="submit" style="width: 100%" :loading="loading"
          >执行任务</a-button
        >
      </a-form-item>
    </a-form>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { uploadPictureByBatchUsingPost } from '@/api/pictureController.ts'
import { message } from 'ant-design-vue'

const formData = reactive<API.PictureUploadByBatchRequest>({})

// 提交任务状态
const loading = ref<boolean>(false)
/**
 * 提交表单
 * @param values
 */
const handleSubmit = async (values: any) => {
  loading.value = true
  try {
    const tmpFormData = {...formData}
    const namePrefix = tmpFormData.namePrefix
    if (!(namePrefix && namePrefix.trim() !== '')) {
      tmpFormData.namePrefix = tmpFormData.searchText
    }
    const res = await uploadPictureByBatchUsingPost(tmpFormData)
    if (res.data.code === 0 && res.data.data) {
      message.success(`抓取成功，共${res.data.data}条`)
    } else {
      message.error('抓取失败')
    }
  } catch (e) {
    message.error('抓取失败' + e.message)
  }
  loading.value = false
}
</script>

<style>
#addPictureBatchPage {
  max-width: 720px;
  margin: 0 auto;
}
</style>
