<template>
  <div class="picture-search-form">
    <!--  搜索表单  -->
    <a-form layout="inline" :model="searchParams" @finish="doSearch">
      <a-form-item label="关键词" name="searchText">
        <a-input
          v-model:value="searchParams.searchText"
          placeholder="从名称和简介搜素"
          allow-clear
        ></a-input>
      </a-form-item>
      <a-form-item name="category" label="分类">
        <a-auto-complete
          v-model:value="searchParams.category"
          :options="categoryOptions"
          placeholder="请输入分类"
          auto-clear
        />
      </a-form-item>
      <a-form-item name="tags" label="标签">
        <a-select
          v-model:value="searchParams.tags"
          mode="tags"
          placeholder="请输入标签"
          :options="tagOptions"
          auto-clear
        />
      </a-form-item>
      <a-form-item label="日期" name="dateRange">
        <a-range-picker
          style="max-width: 400px"
          show-time
          v-model:value="dateRange"
          :placeholder="['编辑开始时间', '编辑结束时间']"
          format="YYYY-MM-DD HH:mm:ss"
          :presets="rangePresets"
          @change="onRangeChange"
        />
      </a-form-item>
      <a-form-item label="名称" name="name">
        <a-input v-model:value="searchParams.name" placeholder="请输入名称" allow-clear></a-input>
      </a-form-item>
      <a-form-item label="简介" name="introduction">
        <a-input
          v-model:value="searchParams.introduction"
          placeholder="请输入简介"
          allow-clear
        ></a-input>
      </a-form-item>
      <a-form-item label="宽度" name="picWidth">
        <a-input-number v-model:value="searchParams.picWidth" allow-clear></a-input-number>
      </a-form-item>
      <a-form-item label="高度" name="picHeight">
        <a-input-number v-model:value="searchParams.picHeight" allow-clear></a-input-number>
      </a-form-item>
      <a-form-item label="格式" name="picFormat">
        <a-input
          v-model:value="searchParams.picFormat"
          placeholder="请输入格式"
          allow-clear
        ></a-input>
      </a-form-item>
      <a-form-item>
        <a-space>
          <a-button type="primary" html-type="submit">搜索</a-button>
          <a-button html-type="reset" @click="doClear">重置</a-button>
        </a-space>
      </a-form-item>
    </a-form>
  </div>
</template>
<script lang="ts" setup>
import { onMounted, reactive, ref } from 'vue'
import dayjs from 'dayjs'
import { listPictureTagCategoryUsingGet } from '@/api/pictureController.ts'
import { message } from 'ant-design-vue'



interface Props {
  onSearch: (searchParams: API.PictureQueryRequest) => any
}

const props = withDefaults(defineProps<Props>(), {
  onSearch: () => {},
})

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
/**
 * 页面打开时获取标签和分类选项
 */
onMounted(getTagCategoryOptions)

// 搜索条件
const searchParams = reactive<API.PictureQueryRequest>({})

/**
 * 搜索
 */
const doSearch = () => {
  // 重置页码
  props.onSearch(searchParams)
}

const dateRange = ref<any[]>([])

/**
 * 重置搜索条件
 */
const doClear = () => {
  Object.keys(searchParams).forEach((key) => {
    searchParams[key] = undefined
  })
  dateRange.value = []
  props.onSearch(searchParams)
}

// 事件日期范围发生改变
const onRangeChange = (dates: any[], dateStrings: string[]) => {
  if (dates?.length === 2) {
    searchParams.startEditTime = dates[0].toDate()
    searchParams.endEditTime = dates[1].toDate()
  }
}

// 时间范围可供快捷选项
const rangePresets = ref([
  { label: '过去 7  天', value: [dayjs().add(-7, 'd'), dayjs()] },
  { label: '过去 14 天', value: [dayjs().add(-14, 'd'), dayjs()] },
  { label: '过去 30 天', value: [dayjs().add(-30, 'd'), dayjs()] },
  { label: '过去 90 天', value: [dayjs().add(-90, 'd'), dayjs()] },
])
</script>

<style scoped>
.picture-search-form .ant-form-item {
  margin-top: 8px;
  min-width: 180px;
}
</style>
