<template>
  <div id="homePage">
    <!--  搜索框  -->
    <div class="search-bar">
      <a-input-search
        v-model:value="searchParams.searchText"
        placeholder="从海量图片中搜索"
        enter-button="搜索"
        size="large"
        @search="doSearch"
      />
    </div>
    <!--  分页和标签筛选  -->
    <a-tabs v-model:activeKey="selectedCategory" @change="doSearch">
      <a-tab-pane key="all" tab="全部" />
      <a-tab-pane v-for="category in categoryList" :key="category" :tab="category" />
    </a-tabs>
    <div class="tag-bar">
      <span style="margin-right: 8px; color: dodgerblue">标签:</span>
      <a-space :size="[0, 8]" wrap>
        <a-checkable-tag
          v-for="(tag, index) in tagList"
          :key="tag"
          v-model:checked="selectedTagList[index]"
          @change="doSearch"
        >
          {{ tag }}
        </a-checkable-tag>
      </a-space>
    </div>
    <!--  图片列表  -->
    <picture-list :data-list="dataList" :loading="loading"/>
    <!-- 分页组件 -->
    <a-pagination
      style="float: right; margin-bottom: 12px"
      v-model:current="searchParams.current"
      v-model:page-size="searchParams.pageSize"
      :total="total"
      @change="onPageChange"
    />
  </div>
</template>

<script lang="ts" setup>
import { onMounted, reactive, ref } from 'vue'
import {
  listPictureTagCategoryUsingGet,
  listPictureVoByPageUsingPost,
} from '@/api/pictureController.ts'
import { message } from 'ant-design-vue'
import PictureList from '@/components/PictureList.vue'

// 搜索条件
const searchParams = reactive<API.PictureQueryRequest>({
  current: 1,
  pageSize: 20,
  sortField: 'createTime',
  sortOrder: 'ascend',
})

const categoryList = ref<string[]>([])
const selectedCategory = ref<string>('all')
const tagList = ref<string[]>([])
const selectedTagList = ref<boolean[]>([])

/**
 * 获取分类和标签
 */
const getTagCategoryList = async () => {
  try {
    const res = await listPictureTagCategoryUsingGet()
    if (res.data.code === 0 && res.data.data) {
      const categoryTag = res.data.data
      categoryList.value = categoryTag.categoryList ?? []
      tagList.value = categoryTag.tagList ?? []
    } else {
      message.error('获取分类和标签失败')
    }
  } catch (e:any) {
    message.error('获取分类和标签失败' + e.message)
  }
}

/**
 * 网页打开时获取分类和标签
 */
onMounted(getTagCategoryList)

// 控制加载效果的字段
const loading = ref<boolean>(true)
const dataList = ref<API.PictureVO[]>([])
const total = ref<any>(0)

// 获取渲染数据
const fetchData = async () => {
  loading.value = true
  try {
    const params = {
      ...searchParams,
      tags: [...tagList.value].filter((_, index) => {
        return selectedTagList.value[index]
      }) as string[],
    }
    if (selectedCategory.value != 'all') params.category = selectedCategory.value
    const res = await listPictureVoByPageUsingPost(params)
    if (res.data.code === 0 && res.data.data) {
      console.log(res.data.data)
      dataList.value = res.data.data.records ?? []
      total.value = res.data.data.total
    } else {
      message.error('获取数据失败' + res.data.message + res.data.data)
    }
  } catch (e:any) {
    message.error('发送请求发生异常' + e.message)
  }
  loading.value = false
}

/**
 * 网页打开时获取数据
 */
onMounted(fetchData)

// 分页组件回调函数
const onPageChange = (current: number, pageSize: number) => {
  searchParams.current = current
  searchParams.pageSize = pageSize
  fetchData()
}

/**
 * 搜索
 */
const doSearch = () => {
  // 重置页码
  searchParams.current = 1
  fetchData()
}
</script>

<style scoped>
#homePage {
  margin-bottom: 16px;
}
.search-bar {
  max-width: 640px;
  margin: 0 auto 16px;
}
.tag-bar {
  margin-bottom: 16px;
}
</style>
