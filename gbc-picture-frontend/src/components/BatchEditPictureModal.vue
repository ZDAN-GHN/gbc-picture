<template>
  <div>
    <a-modal v-model:open="open" title="批量编辑图片" :footer="false" @cancel="closeModal">
      <a-typography-text type="secondary">* 只对当前页面下的图片列表生效</a-typography-text>
      <a-form layout="vertical" name="basic" :model="formData" @finish="handleSubmit">
        <a-form-item name="category" label="分类">
          <a-auto-complete
            v-model:value="formData.category"
            :options="categoryOptions"
            placeholder="请输入分类"
            auto-clear
          />
        </a-form-item>
        <a-form-item name="tags" label="标签">
          <a-select
            v-model:value="formData.tags"
            mode="tags"
            placeholder="请输入标签，格式为 tag1,tag2..."
            :options="tagOptions"
            auto-clear
          />
        </a-form-item>
        <a-form-item name="nameRule" label="命名规则">
          <a-input
            v-model:value="formData.nameRule"
            placeholder="请输入名称规则，输入 {{序号}} 可动态生成序号"
            allow-clear
          ></a-input>
        </a-form-item>
        <a-form-item>
          <a-button type="primary" html-type="submit" style="width: 100%">确认修改</a-button>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>
<script lang="ts" setup>
import { onMounted, reactive, ref } from 'vue'
import {
  editPictureByBatchUsingPost,
  listPictureTagCategoryUsingGet,
} from '@/api/pictureController.ts'
import { message } from 'ant-design-vue'

const formData = reactive<API.PictureEditByBatchRequest>({
  category: '',
  tags: [],
  nameRule: '',
})

interface Props {
  pictureList: API.PictureVO[]
  spaceId?: string | number
  onSuccess?: () => void
}

const props = withDefaults(defineProps<Props>(), {
  spaceId: '',
  onSuccess: () => {},
})

const open = ref<boolean>(false)

const openModal = () => {
  open.value = true
}

const closeModal = (e: MouseEvent) => {
  open.value = false
}

const handleSubmit = async (values: any) => {
  try {
    const res = await editPictureByBatchUsingPost({
      pictureIdList: props.pictureList.map((picture) => picture.id),
      spaceId: props.spaceId,
      ...values,
    })
    if (res.data.code === 0 && res.data.data) {
      message.success('修改成功')
      props.onSuccess()
      open.value = false
    } else {
      message.error('修改失败')
    }
  } catch (e) {
    message.error('修改失败' + e.message)
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
          lable: data,
          value: data,
        }
      })
      tagOptions.value = (tagCategory.tagList ?? []).map((data: string) => {
        return {
          lable: data,
          value: data,
        }
      })
    } else {
      message.error('获取分类和标签失败')
    }
  } catch (e) {
    message.error('获取分类和标签失败' + e.message)
  }
}

// 加载组件的时候自动获取分类和标签
onMounted(getTagCategoryOptions)

// 暴露函数给父组件
defineExpose({
  openModal,
})
</script>
