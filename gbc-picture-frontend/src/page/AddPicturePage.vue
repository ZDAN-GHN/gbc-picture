<template>
  <div id="addPicturePage">
    <h2 style="margin-bottom: 16px">
      {{ route.query?.id ? '修改图片' : '创建图片' }}
    </h2>
    <a-typography-paragraph v-if="spaceId">
      保存至空间：<a :href="`/space/${spaceId}`">{{ spaceName ?? spaceId }}</a>
    </a-typography-paragraph>
    <!-- 选择上传方式 -->
    <a-tabs v-model:activeKey="uploadType">
      <a-tab-pane key="file" tab="文件上传">
        <FilePictureUpload :picture="picture" :spaceId="spaceId" :onSuccess="onSuccess" />
      </a-tab-pane>
      <a-tab-pane key="url" tab="URL上传" force-render>
        <UrlPictureUpload :picture="picture" :spaceId="spaceId" :onSuccess="onSuccess" />
      </a-tab-pane>
    </a-tabs>
    <div v-if="picture" class="edit-bar">
      <a-space size="middle">
        <a-button :icon="h(EditOutlined)" @click="doEditPicture">编辑图片</a-button>
        <a-button
          v-if="canOutPainting"
          type="primary"
          :icon="h(FullscreenOutlined)"
          @click="doPictureOutPainting"
          >AI 扩图</a-button
        >
      </a-space>
      <image-cropper
        ref="imageCropperRef"
        :imageUrl="picture?.url"
        :picture="picture"
        :spaceId="spaceId"
        :space="space"
        :onSuccess="onCropSuccess"
      />
      <image-out-painting
        ref="pictureOutPaintingRef"
        :picture="picture"
        :spaceId="spaceId"
        :onSuccess="onPictureOutPaintingSuccess"
      />
    </div>
    <!--  图片信息表单  -->
    <a-form
      v-if="picture"
      layout="vertical"
      name="basic"
      :model="pictureForm"
      @finish="handleSubmit"
    >
      <a-form-item name="name" label="名称">
        <a-input v-model:value="pictureForm.name" placeholder="请输入名称" allow-clear></a-input>
      </a-form-item>
      <a-form-item name="introduction" label="简介">
        <a-textarea
          v-model:value="pictureForm.introduction"
          placeholder="请输入简介"
          :auto-size="{ minRows: 2, maxRows: 5 }"
          allow-clear
        ></a-textarea>
      </a-form-item>
      <a-form-item name="category" label="分类">
        <a-auto-complete
          v-model:value="pictureForm.category"
          :options="categoryOptions"
          placeholder="请输入分类"
          auto-clear
        />
      </a-form-item>
      <a-form-item name="tags" label="标签">
        <a-select
          v-model:value="pictureForm.tags"
          mode="tags"
          placeholder="请输入标签"
          :options="tagOptions"
          auto-clear
        />
      </a-form-item>
      <a-form-item>
        <a-button type="primary" html-type="submit" style="width: 100%">{{
          route.query?.id ? '修改' : '创建'
        }}</a-button>
      </a-form-item>
    </a-form>
  </div>
</template>

<script setup lang="ts">
import FilePictureUpload from '@/components/FilePictureUpload.vue'
import { h, computed, onMounted, reactive, ref, watchEffect } from 'vue'
import {
  editPictureUsingPost,
  getPictureVoByIdUsingGet,
  listPictureTagCategoryUsingGet,
} from '@/api/pictureController.ts'
import { message } from 'ant-design-vue'
import router from '@/router'
import { useRoute } from 'vue-router'
import UrlPictureUpload from '@/components/UrlPictureUpload.vue'
import ImageCropper from '@/components/ImageCropper.vue'
import { EditOutlined, FullscreenOutlined } from '@ant-design/icons-vue'
import ImageOutPainting from '@/components/ImageOutPainting.vue'
import { getSpaceVoByIdUsingGet } from '@/api/spaceController.ts'

const uploadType = ref<'file' | 'url'>('file')
const picture = ref<API.PictureVO>()
const pictureForm = reactive<API.PictureEditRequest>({})

const canOutPainting = computed(() => {
  // 验证宽度和高度是否在 512 到 4096 之间
  const picWidth = picture.value?.picWidth ?? 0
  const picHeight = picture.value?.picHeight ?? 0
  const widthValid = picWidth >= 512 && picWidth <= 4096
  const heightValid = picHeight >= 512 && picHeight <= 4096
  return widthValid && heightValid
})

/**
 * 上传成功
 * @param newPicture
 */
const onSuccess = (newPicture: API.PictureVO) => {
  picture.value = newPicture
  pictureForm.name = newPicture.name
}

const route = useRoute()
const spaceId = computed(() => picture.value?.spaceId ?? (route.query?.spaceId as string))
const spaceName = computed(() => route.query?.spaceName as string)

/**
 * 提交表单
 * @param values
 */
const handleSubmit = async (values: any) => {
  const pictureId = picture.value?.id
  if (!pictureId) {
    message.error('未上传图片')
    return
  }
  try {
    const res = await editPictureUsingPost({
      id: pictureId,
      spaceId: spaceId.value,
      ...values,
    })
    const redirect: string = (route.query?.redirect as string) ?? `/picture/${pictureId}`

    if (res.data.code === 0 && res.data.data) {
      message.success('创建成功')
      router.push({
        path: redirect,
      })
    } else {
      message.error('创建失败')
    }
  } catch (e: any) {
    message.error('创建失败' + e.message)
  }
}

const categoryOptions = ref<string[]>([])
const tagOptions = ref<string[]>([])

/**
 * 获取标签和分类选项
 */
const getTagCategoryOptions = async () => {
  try {
    const res = await listPictureTagCategoryUsingGet()
    if (res.data.code === 0 && res.data.data) {
      const tagCategory = res.data.data
      categoryOptions.value = (tagCategory.categoryList ?? []).map((data: string) => {
        return {
          label: data,
          value: data,
        }
      })
      tagOptions.value = (tagCategory.tagList ?? []).map((data: string) => {
        return {
          label: data,
          value: data,
        }
      })
    } else {
      message.error('获取分类和标签失败')
    }
  } catch (e: any) {
    message.error('获取分类和标签失败' + e.message)
  }
}
/**
 * 页面打开时获取标签和分类选项
 */
onMounted(getTagCategoryOptions)

/**
 * 获取旧图片信息
 */
const getOldPicture = async () => {
  const id = route.query?.id
  if (id) {
    const res = await getPictureVoByIdUsingGet({ id })
    if (res.data.code === 0 && res.data.data) {
      const oldPicture = res.data.data
      picture.value = oldPicture
      pictureForm.name = oldPicture.name
      pictureForm.introduction = oldPicture.introduction
      pictureForm.category = oldPicture.category
      pictureForm.tags = oldPicture.tags
    }
  }
}
/**
 * 页面打开时获取旧图片信息
 */
onMounted(getOldPicture)

// ----- 图片编辑器引用 ------
const imageCropperRef = ref()

// 编辑图片
const doEditPicture = async () => {
  imageCropperRef.value?.openModal()
}

// 编辑成功事件
const onCropSuccess = (newPicture: API.PictureVO) => {
  picture.value = newPicture

}

// ----- AI 扩图编辑器引用 ------
const pictureOutPaintingRef = ref()

const doPictureOutPainting = async () => {
  pictureOutPaintingRef.value?.openModal()
}

const onPictureOutPaintingSuccess = (newPicture: API.PictureVO) => {
  picture.value = newPicture
}

const space = ref<API.SpaceVO>()

// 获取空间信息
const fetchSpace = async () => {
  console.log('获取空间信息', spaceId.value)
  // 获取数据
  if (spaceId.value) {
    try {
      const res = await getSpaceVoByIdUsingGet({
        id: spaceId.value,
      })
      if (res.data.code === 0 && res.data.data) {
        space.value = res.data.data
      }
    } catch (e: any) {
      message.error('获取空间信息失败' + e.message)
    }
  }
}

watchEffect(() => {
  fetchSpace()
})
</script>

<style>
#addPicturePage {
  max-width: 720px;
  margin: 0 auto;
}

.edit-bar {
  margin: 8px 0;
  text-align: center;
}
</style>
