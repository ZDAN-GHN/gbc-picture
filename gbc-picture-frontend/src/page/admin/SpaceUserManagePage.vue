<template>
  <div id="spaceUserManagePage">
    <a-flex justify="space-between">
      <h2>
        <div style="margin-bottom: 8px; color: royalblue"><EditFilled /> 空间成员管理</div>
        <div>
          <span v-if="spaceName && spaceName !== ''"> {{ spaceName }} -- </span>
          {{ spaceId }}
        </div>
      </h2>
    </a-flex>
    <div style="margin-bottom: 16px"></div>
    <!--  搜索表单  -->
    <a-form layout="inline" :model="formData" @finish="handleSubmit">
      <a-form-item label="用户 id" name="userId">
        <a-input v-model:value="formData.userId" placeholder="请输入用户 id" allow-clear />
      </a-form-item>
      <a-form-item>
        <a-button type="primary" html-type="submit" :loading="loading">添加用户</a-button>
      </a-form-item>
    </a-form>
    <!--  分隔  -->
    <div style="margin-bottom: 16px"></div>
    <!--  表格  -->
    <a-table :columns="columns" :data-source="dataList">
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'userInfo'">
          <a-space>
            <a-avatar :src="record.user?.userAvatar" />
            {{ record.user?.userName }}
          </a-space>
        </template>
        <template v-if="column.dataIndex === 'spaceRole'">
          <a-select
            v-model:value="record.spaceRole"
            :options="SPACE_ROLE_OPTIONS"
            @change="(value: string) => editSpaceRole(value, record)"
          />
        </template>
        <template v-else-if="column.dataIndex === 'createTime'">
          {{ dayjs(record.createTime).format('YYYY-MM-DD HH:mm:ss') }}
        </template>
        <template v-else-if="column.key === 'action'">
          <a-space wrap>
            <a-button type="link" danger @click="doDelete(record.id)">删除</a-button>
          </a-space>
        </template>
      </template>
    </a-table>
  </div>
</template>
<script lang="ts" setup>
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'
import { SPACE_ROLE_OPTIONS } from '@/constant/space.ts'
import { EditFilled } from '@ant-design/icons-vue'
import {
  addSpaceUserUsingPost,
  deleteSpaceUserUsingPost,
  editSpaceUserUsingPost,
  listSpaceUserVoByPageUsingPost,
} from '@/api/spaceUserController.ts'
import { useRoute } from 'vue-router'

const route = useRoute()
const spaceId = computed(() => route.query?.spaceId as number)
const spaceName = computed(() => route.query?.spaceName as string)

// 表格列，dataIndex是展示数据源的字段名，key是本列的唯一索引，
// 可以将dataIndex看成是key的升级版
// 表格列
const columns = [
  {
    title: '用户',
    dataIndex: 'userInfo',
  },
  {
    title: '角色',
    dataIndex: 'spaceRole',
  },
  {
    title: '加入时间',
    dataIndex: 'createTime',
  },
  {
    title: '操作',
    key: 'action',
  },
]

// 表格渲染内容
const dataList = ref<API.SpaceUserVO[]>([])

// 获取渲染数据
const fetchData = async () => {
  try {
    const res = await listSpaceUserVoByPageUsingPost({ spaceId: spaceId.value })
    if (res.data.code === 0 && res.data.data) {
      dataList.value = res.data.data ?? []
    } else {
      message.error('获取数据失败 ' + res.data.message)
    }
  } catch (e: any) {
    message.error('发送请求发生异常 ' + e.message)
  }
}

// 表单数据
const formData = reactive<API.SpaceUserAddRequest>({})

const loading = ref<boolean>(false)
// 新增成员
const handleSubmit = async () => {
  loading.value = true
  try {
    const res = await addSpaceUserUsingPost({ spaceId: spaceId.value, ...formData })
    if (res.data.code === 0 && res.data.data) {
      message.success('添加成功')
      await fetchData()
    } else {
      message.error('添加失败 ' + res.data.message)
    }
  } catch (e: any) {
    message.error('发送请求发生异常' + e.message)
  }
  loading.value = false
}

// 删除空间内的成员
const doDelete = async (id: string) => {
  if (!id) return
  try {
    const res = await deleteSpaceUserUsingPost({ id })
    if (res.data.code === 0 && res.data.data) {
      message.info('删除成功')
    } else {
      message.error('删除失败 ' + res.data.message)
    }
  } catch (e: any) {
    message.error('发送删除请求失败' + e.message)
  }
}

// 更改成员角色
const editSpaceRole = async (value: string, spaceUser: API.SpaceUserVO) => {
  try {
    const res = await editSpaceUserUsingPost({
      id: spaceUser.id,
      spaceRole: value,
    })
    if (res.data.code === 0) {
      message.success('修改成功')
    }
    dataList.value.forEach((user: API.SpaceUserVO) => {
      if (user.id === spaceUser.id) user.spaceRole = value
    })
  } catch (e: any) {
    message.error('发送请求失败' + e.message)
  }
}

// 页面加载时自动请求一次数据
onMounted(() => {
  fetchData()
})
</script>
