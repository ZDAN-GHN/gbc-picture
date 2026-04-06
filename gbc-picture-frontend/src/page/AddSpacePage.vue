<template>
  <div id="addSpacePage">
    <h2 style="margin-bottom: 16px">
      {{ route.query?.id ? '修改' : '创建' }} <span style="color: dodgerblue">{{ SPACE_TYPE_MAP[spaceType] }}</span>
    </h2>
    <!--  空间信息表单  -->
    <a-form layout="vertical" name="basic" :model="spaceForm" @finish="handleSubmit">
      <a-form-item name="spaceName" label="空间名称">
        <a-input
          v-model:value="spaceForm.spaceName"
          placeholder="请输入空间名称"
          allow-clear
        ></a-input>
      </a-form-item>
      <a-form-item name="spaceLevel" label="空间级别">
        <a-select
          v-model:value="spaceForm.spaceLevel"
          :options="SPACE_LEVEL_OPTIONS"
          placeholder="请选择空间级别"
          style="min-width: 180px"
          allow-clear
        />
      </a-form-item>
      <a-form-item>
        <a-button type="primary" :loading="loading" html-type="submit" style="width: 100%">{{
          route.query?.id ? '修改' : '创建'
        }}</a-button>
      </a-form-item>
    </a-form>
    <a-card title="空间级别介绍">
      <a-typography-paragraph>
        * 目前仅支持开通普通版，如需升级空间，请联系
        <a href="https://github.com/ZDAN-GHN" target="_blank">ZDAN-GHN</a>。
      </a-typography-paragraph>

      <a-typography-paragraph v-for="spaceLevel in spaceLevelList">
        {{ spaceLevel.text }}：可存容量
        <i style="color: indianred">{{ formatSize(spaceLevel.maxSize) }}</i
        >， 最大存入图片数量 <i style="color: indianred">{{ spaceLevel.maxCount }}</i>
      </a-typography-paragraph>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { useRoute } from 'vue-router'
import {
  addSpaceUsingPost,
  getSpaceVoByIdUsingGet,
  listSpaceLevelUsingGet,
} from '@/api/spaceController.ts'
import {
  SPACE_LEVEL_OPTIONS,
  SPACE_TYPE_ENUM,
  SPACE_TYPE_MAP,
  SPACE_TYPE_OPTIONS,
} from '@/constant/space.ts'
import { formatSize } from '@/utils'
import router from '@/router'
import { updatePictureUsingPost } from '@/api/pictureController.ts'

const loading = ref(false)
const space = ref<API.SpaceVO>()
const spaceForm = reactive<API.SpaceAddRequest>({})
const spaceLevelList = reactive<API.SpaceLevel[]>([])
const route = useRoute()

// 空间类型，默认为私有空间
const spaceType = computed(() => route.query?.type ?? SPACE_TYPE_ENUM.PRIVATE)

/**
 * 提交表单
 * @param values
 */
const handleSubmit = async (values: any) => {
  const spaceId = space.value?.id
  loading.value = true
  let res
  try {
    if (spaceId) {
      res = await updatePictureUsingPost({
        id: spaceId,
        ...values,
      })
    } else {
      res = await addSpaceUsingPost({
        ...values,
        spaceType: spaceType.value,
      })
    }
    if (res.data.code === 0 && res.data.data) {
      message.success('操作成功')
      router.push({
        path: `/space/${spaceId}`,
      })
    } else {
      message.error('操作失败 ' + res.data.message)
    }
  } catch (e) {
    message.error('操作失败' + e.message)
  }
  loading.value = false
}

/**
 * 获取旧空间信息
 */
const getOldSpace = async () => {
  const id = route.query?.id
  if (id) {
    const res = await getSpaceVoByIdUsingGet({ id })
    if (res.data.code === 0 && res.data.data) {
      const oldSpace = res.data.data
      space.value = oldSpace
      spaceForm.spaceLevel = oldSpace.spaceLevel
      spaceForm.spaceName = oldSpace.spaceName
    }
  }
}

const fetchSpaceLevelList = async () => {
  const res = await listSpaceLevelUsingGet()
  spaceLevelList.length = 0
  spaceLevelList.push(...res.data.data)
}

/**
 * 页面打开时获取旧空间信息
 */
onMounted(() => {
  getOldSpace()
  fetchSpaceLevelList()
})
</script>

<style>
#addSpacePage {
  max-width: 720px;
  margin: 0 auto;
}
</style>
