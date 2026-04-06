<template>
  <div id="pictureManagePage">
    <a-flex justify="space-between">
      <h2>图片管理</h2>
      <a-space>
        <a-button type="primary" href="/add_picture" target="_blank">+ 创建图片</a-button>
        <a-button type="primary" href="/add_picture/batch" target="_blank" ghost>+ 批量创建图片</a-button>
      </a-space>
    </a-flex>
    <div style="margin-bottom: 16px"></div>
    <!--  搜索表单  -->
    <a-form layout="inline" :model="searchParams" @finish="doSearch">
      <a-form-item label="关键词">
        <a-input
          v-model:value="searchParams.searchText"
          placeholder="从名称和简介搜素"
          allow-clear
        ></a-input>
      </a-form-item>
      <a-form-item label="类型">
        <a-input
          v-model:value="searchParams.category"
          placeholder="请输入分类"
          auto-clear
        ></a-input>
      </a-form-item>
      <a-form-item label="标签">
        <a-select
          v-model:value="searchParams.tags"
          mode="tags"
          placeholder="请输入标签"
          style="min-width: 180px"
          auto-clear
        />
      </a-form-item>
      <a-form-item label="审核状态" name="reviewStatus">
        <a-select
          v-model:value="searchParams.reviewStatus"
          :options="PIC_REVIEW_STATUS_OPTIONS"
          placeholder="请选择审核状态"
          style="min-width: 180px"
          allow-clear
          @change="doSearch"
        />
      </a-form-item>
      <a-form-item>
        <a-button type="primary" html-type="submit">搜索</a-button>
      </a-form-item>
    </a-form>
    <!--  分隔  -->
    <div style="margin-bottom: 16px"></div>
    <!--  表格  -->
    <a-table
      :columns="columns"
      :data-source="dataList"
      :pagination="pagination"
      @change="doTableChange"
      :scroll="{ x: 'max-content' }"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'url'">
          <a-image :src="record.url" width="60px"></a-image>
        </template>
        <template v-if="column.dataIndex === 'introduction' && record.introduction === null">
          --------------
        </template>
        <template v-if="column.dataIndex === 'category'">
          <a-tag color="green">
            {{ record.category }}
          </a-tag>
        </template>
        <template v-if="column.dataIndex === 'tags'">
          <a-space wrap>
            <a-tag v-if="record.tags" color="blue" v-for="tag in JSON.parse(record.tags || '[]')">
              {{ tag }}
            </a-tag>
            <a-tag v-else color="blue"></a-tag>
          </a-space>
        </template>
        <template v-if="column.dataIndex === 'picInfo'">
          <div>格式：{{ record.picFormat }}</div>
          <div>大小：{{ record.picSize }}</div>
          <div>宽度：{{ record.picWidth }}</div>
          <div>高度：{{ record.picHeight }}</div>
          <div>宽高比：{{ record.picScale }}</div>
        </template>
        <template v-if="column.dataIndex === 'reviewInfo'">
          <div>审核人：{{ record.reviewerId }}</div>
          <div>审核状态：{{ PIC_REVIEW_STATUS_MAP[record.reviewStatus] }}</div>
          <div>审核信息：{{ record.reviewMessage }}</div>
          <div v-if="record.reviewTime">
            审核时间：{{ dayjs(record.reviewTime).format('YYYY-MM-DD HH-mm-ss') }}
          </div>
        </template>
        <template v-if="column.dataIndex === 'createTime'">
          {{ dayjs(record.createTime).format('YYYY-MM-DD HH-mm-ss') }}
        </template>
        <template v-if="column.dataIndex === 'editTime'">
          {{ dayjs(record.editTime).format('YYYY-MM-DD HH-mm-ss') }}
        </template>
        <template v-if="column.key === 'action'">
          <a-space wrap>
            <a-button
              type="link"
              v-if="record.reviewStatus !== PIC_REVIEW_STATUS_ENUM.PASS"
              @click="handleReview(record, PIC_REVIEW_STATUS_ENUM.PASS)"
            >
              通过
            </a-button>
            <a-button
              type="link"
              danger
              v-if="record.reviewStatus !== PIC_REVIEW_STATUS_ENUM.REJECT"
              @click="handleReview(record, PIC_REVIEW_STATUS_ENUM.REJECT)"
            >
              拒绝
            </a-button>
            <a-button
              type="link"
              style="border-color: dodgerblue"
              :href="`/add_picture?id=${record.id}`"
              target="_blank"
            >
              编辑
            </a-button>
            <a-button danger @click="doDelete(record.id)">删除</a-button>
          </a-space>
        </template>
      </template>
    </a-table>
  </div>
</template>
<script lang="ts" setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'
import { doPictureReviewUsingPost, listPictureByPageUsingPost } from '@/api/pictureController.ts'
import {
  PIC_REVIEW_STATUS_ENUM,
  PIC_REVIEW_STATUS_MAP,
  PIC_REVIEW_STATUS_OPTIONS,
} from '@/constant/picture.ts'

// 表格列，dataIndex是展示数据源的字段名，key是本列的唯一索引，
// 可以将dataIndex看成是key的升级版
const columns = [
  {
    title: 'id',
    dataIndex: 'id',
    width: 80,
  },
  {
    title: '图片',
    dataIndex: 'url',
  },
  {
    title: '名称',
    dataIndex: 'name',
  },
  {
    title: '简介',
    dataIndex: 'introduction',
    ellipsis: true,
  },
  {
    title: '类型',
    dataIndex: 'category',
  },
  {
    title: '标签',
    dataIndex: 'tags',
  },
  {
    title: '图片信息',
    dataIndex: 'picInfo',
  },
  {
    title: '用户 id',
    dataIndex: 'userId',
    width: 80,
  },  {
    title: '空间 id',
    dataIndex: 'spaceId',
    width: 80,
  },
  {
    title: '审核信息',
    dataIndex: 'reviewInfo',
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
  },
  {
    title: '编辑时间',
    dataIndex: 'editTime',
  },
  {
    title: '操作',
    key: 'action',
  },
]

// 表格渲染内容
const dataList = ref<API.Picture>([])
const total = ref(0)

// 搜索条件
const searchParams = reactive<API.PictureQueryRequest>({
  current: 1,
  pageSize: 10,
  sortField: 'createTime',
  sortOrder: 'ascend',
})

// 获取渲染数据
const fetchData = async () => {
  try {
    // searchParams是响应式数据较为影响体验，
    // 为了不污染到这个变量，传值时不要传引用，而是解包传新对象
    const res = await listPictureByPageUsingPost({ ...searchParams })
    if (res.data.code === 0 && res.data.data) {
      console.log(res.data.data)
      dataList.value = res.data.data.records ?? []
      total.value = res.data.data.total
    } else {
      message.error('获取数据失败' + res.data.message + res.data.data)
    }
  } catch (e) {
    message.error('发送请求发生异常' + e.message)
  }
}

const doDelete = async (id: string) => {
  if (!id) return
  try {
    const res = await { id }
    if (res.data.code === 0) {
      message.info('删除成功')
    } else {
      message.error('删除失败')
    }
  } catch (e) {
    message.error('发送删除请求失败' + e.message)
  }
}

// 页面加载时自动请求一次数据
onMounted(() => {
  fetchData()
})

// 分页参数
const pagination = computed(() => {
  return {
    current: searchParams.current,
    pageSize: searchParams.pageSize,
    total: total.value,
    showSizeChanger: true,
    showTotal: (total: any) => `共 ${total} 条`,
  }
})

// 表格变化之后重新获取数据
const doTableChange = (page: any) => {
  searchParams.current = page.current
  searchParams.pageSize = page.pageSize
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

const handleReview = async (record: API.Picture, reviewStatus: number) => {
  if (!record || !reviewStatus) {
    message.error('参数错误')
  }
  try {
    const res = await doPictureReviewUsingPost({ ...record, reviewStatus })
    if (res.data.code === 0 && res.data.data) {
      fetchData()
    } else {
      message.error('审核失败')
    }
  } catch (e) {
    message.error('请求失败' + e)
  }
}
</script>
