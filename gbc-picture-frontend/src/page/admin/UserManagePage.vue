<template>
  <div id="userManagePage">
    <!--  搜索表单  -->
    <a-form layout="inline" :model="searchParams" @finish="doSearch">
      <a-form-item label="账号">
        <a-input
          v-model:value="searchParams.userAccount"
          placeholder="请输入账号"
          allow-clear
        ></a-input>
      </a-form-item>
      <a-form-item label="用户名">
        <a-input
          v-model:value="searchParams.userName"
          placeholder="请输入用户名"
          allow-clear
        ></a-input>
      </a-form-item>
      <a-form-item>
        <a-button type="primary" html-type="submit">搜索</a-button>
      </a-form-item>
    </a-form>
    <div style="margin-bottom: 16px"></div>
    <!--  表格  -->
    <a-table
      :columns="columns"
      :data-source="dataList"
      :pagination="pagination"
      @change="doTableChange"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'userAvatar'">
          <a-image :src="record.userAvatar" width="60px"></a-image>
        </template>
        <template v-if="column.dataIndex === 'userRole'">
          <div v-if="record.userRole === 'admin'">
            <a-tag color="green" style="font-size: 16px">管理员</a-tag>
          </div>
          <div v-else>
            <a-tag color="blue" style="font-size: 16px">普通用户</a-tag>
          </div>
        </template>
        <template v-if="column.dataIndex === 'createTime'">
          {{ dayjs(record.createTime).format('YYYY-MM-DD HH:mm:ss') }}
        </template>
        <template v-if="column.key === 'action'">
          <a-button danger @click="doDelete(record.id)">删除</a-button>
        </template>
      </template>
    </a-table>
  </div>
</template>
<script lang="ts" setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { listUserVoByPageUsingPost, userDeleteUsingPost } from '@/api/userController.ts'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'

// 表格列，dataIndex是展示数据源的字段名，key是本列的唯一索引，
// 可以将dataIndex看成是key的升级版
const columns = [
  {
    title: 'id',
    dataIndex: 'id',
  },
  {
    title: '账号',
    dataIndex: 'userAccount',
  },
  {
    title: '用户名',
    dataIndex: 'userName',
  },
  {
    title: '头像',
    dataIndex: 'userAvatar',
  },
  {
    title: '简介',
    dataIndex: 'userProfile',
  },
  {
    title: '用户角色',
    dataIndex: 'userRole',
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
  },
  {
    title: '操作',
    key: 'action',
  },
]

// 表格渲染内容
const dataList = ref<API.UserVO[]>([])
const total = ref<string | number>(0)

// 搜索条件
const searchParams = reactive<API.UserQueryRequest>({
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
    const res = await listUserVoByPageUsingPost({ ...searchParams })
    if (res.data.code === 0 && res.data.data) {
      dataList.value = res.data.data.records ?? []
      total.value = res.data.data.total ?? dataList.value.length
    } else {
      message.error('获取数据失败')
    }
  } catch (e: any) {
    message.error('发送请求发生异常' + e.message)
  }
}

const doDelete = async (id: string) => {
  if (!id) return
  try {
    const res = await userDeleteUsingPost({ id })
    if (res.data.code === 0) {
      message.info('删除成功')
    } else {
      message.error('删除失败')
    }
  } catch (e: any) {
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

const doSearch = () => {
  // 重置页码
  searchParams.current = 1
  fetchData()
}
</script>
