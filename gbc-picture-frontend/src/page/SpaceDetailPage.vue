<template>
  <div id="spaceDetailPage">
    <a-flex justify="space-between">
      <h2 style="color: royalblue">
        <UserOutlined v-if="SPACE_TYPE_ENUM.PRIVATE === space.spaceType" />
        <TeamOutlined v-else />
        {{ space.spaceName }} -- {{ spaceId }}
      </h2>
      <a-space>
        <a-button
          v-if="canUploadPicture"
          type="primary"
          :href="`/add_picture/?spaceId=${space.id}&spaceName=${space.spaceName}&redirect=${route.path}`"
          target="_blank"
        >
          + 创建图片
        </a-button>
        <a-button
          v-if="canManageSpaceUser"
          type="primary"
          ghost
          :icon="h(TeamOutlined)"
          :href="`/spaceUserManage?spaceId=${space.id}&spaceName=${space.spaceName}`"
          target="_blank"
        >
          成员管理
        </a-button>
        <a-button
          v-if="canManageSpaceUser"
          type="primary"
          ghost
          :icon="h(BarChartOutlined)"
          :href="`/space_analyze?spaceId=${spaceId}`"
          target="_blank"
        >
          空间分析
        </a-button>
        <a-button v-if="canEditPicture" type="default" @click="(e: Event) => doBatchEdit(e)">
          <edit-outlined />
          批量编辑
        </a-button>
        <a-tooltip
          :title="`占用空间 ${formatSize(space.totalSize)} / ${formatSize(space.maxSize)} `"
        >
          <a-progress
            type="circle"
            :size="42"
            :percent="((space.totalSize * 100) / space.maxSize).toFixed(1)"
          >
          </a-progress>
        </a-tooltip>
      </a-space>
    </a-flex>
    <div style="margin-bottom: 16px"></div>
    <picture-search-form :on-search="onSearch"></picture-search-form>
    <div style="margin-bottom: 16px"></div>
    <!--   按颜色搜索，跟其他搜索条件独立   -->
    <a-form-item label="按颜色搜索">
      <color-picker format="hex" @pureColorChange="onColorChange" />
    </a-form-item>
    <picture-list
      :data-list="dataList"
      :loading="loading"
      :show-action="true"
      :can-edit="canEditPicture"
      :can-delete="canDeletePicture"
      :attachment="{ spaceName: space.spaceName }"
      :delete-picture-callback="deletePictureCallback"
    />
    <!-- 分页组件 -->
    <a-pagination
      v-if="dataList.length"
      style="float: right; margin-bottom: 12px"
      v-model:current="searchParams.current"
      v-model:page-size="searchParams.pageSize"
      :total="total"
      @change="onPageChange"
    />
    <!-- 批量编辑组件 -->
    <batch-edit-picture-modal
      ref="batchEditModalRef"
      :spaceId="space.id"
      :picture-list="dataList"
      :on-success="onBatchEditSuccess"
    ></batch-edit-picture-modal>
  </div>
</template>

<script lang="ts" setup>
import { EditOutlined, BarChartOutlined, UserOutlined, TeamOutlined } from '@ant-design/icons-vue'
import { getSpaceVoByIdUsingGet } from '@/api/spaceController.ts'
import { message } from 'ant-design-vue'
import { computed, h, onMounted, ref, watch } from 'vue'
import {
  listPictureVoByPageUsingPost,
  searchPictureByColorUsingPost,
} from '@/api/pictureController.ts'
import PictureList from '@/components/PictureList.vue'
import { formatSize } from '@/utils'
import { useRoute } from 'vue-router'
import PictureSearchForm from '@/components/PictureSearchForm.vue'
import { ColorPicker } from 'vue3-colorpicker'
import 'vue3-colorpicker/style.css'
import BatchEditPictureModal from '@/components/BatchEditPictureModal.vue'
import { SPACE_PERMISSION_ENUM, SPACE_TYPE_ENUM } from '@/constant/space.ts'
import router from '@/router'

interface Props {
  spaceId: string | number
}

const props = defineProps<Props>()
const space = ref<API.SpaceVO>({})

// 通用权限检查函数
function createPermissionChecker(permission: string) {
  return computed(() => {
    return (space.value.permissionList ?? []).includes(permission)
  })
}

// 定义权限检查
const canManageSpaceUser = createPermissionChecker(SPACE_PERMISSION_ENUM.SPACE_USER_MANAGE)
const canUploadPicture = createPermissionChecker(SPACE_PERMISSION_ENUM.PICTURE_UPLOAD)
const canEditPicture = createPermissionChecker(SPACE_PERMISSION_ENUM.PICTURE_EDIT)
const canDeletePicture = createPermissionChecker(SPACE_PERMISSION_ENUM.PICTURE_DELETE)

const route = useRoute()

/**
 * 获取空间信息
 */
const fetchSpaceDetail = async () => {
  const id = props.spaceId
  try {
    if (id) {
      const res = await getSpaceVoByIdUsingGet({ id })
      if (res.data.code === 0 && res.data.data) {
        space.value = res.data.data
      } else {
        message.error('获取空间信息失败')
      }
    } else {
      message.error('缺少路径参数, id = ', id)
    }
  } catch (e: any) {
    message.error('发送请求发生异常 ' + e.message)
  }
}

// 打开页面时获取空间信息
onMounted(fetchSpaceDetail)

// 搜索条件
const searchParams = ref<API.PictureQueryRequest>({
  current: 1,
  pageSize: 20,
  sortField: 'createTime',
  sortOrder: 'ascend',
})

// 控制加载效果的字段
const loading = ref<boolean>(true)
const dataList = ref<API.PictureVO[]>([])
const total = ref<number>(0)

// 获取渲染数据
const fetchData = async () => {
  loading.value = true
  try {
    const params = {
      spaceId: props.spaceId,
      ...searchParams.value,
    }
    const res = await listPictureVoByPageUsingPost(params as API.PictureQueryRequest)
    if (res.data.code === 0 && res.data.data) {
      dataList.value = res.data.data.records ?? []
      total.value = res.data.data.total as number
    } else {
      message.error('获取数据失败' + res.data.message + res.data.data)
    }
  } catch (e: any) {
    message.error('发送请求发生异常 ' + e.message)
  }
  loading.value = false
}

const onSearch = (newSearchParams: API.PictureQueryRequest) => {
  searchParams.value = {
    ...searchParams.value,
    ...newSearchParams,
    current: 1,
  }
  fetchData()
}

/**
 * 删除图片后的回调
 */
const deletePictureCallback = async (id: string | number) => {
  dataList.value = dataList.value.filter((item) => item.id !== id)
}

/**
 * 网页打开时获取数据
 */
onMounted(fetchData)

// 分页组件回调函数
const onPageChange = (current: number, pageSize: number) => {
  searchParams.value.current = current
  searchParams.value.pageSize = pageSize
  fetchData()
}

/**
 * 颜色搜索
 * @param color
 */
const onColorChange = async (color: string) => {
  loading.value = true
  try {
    const params = {
      spaceId: props.spaceId,
      picColor: color,
    }
    const res = await searchPictureByColorUsingPost(params)
    if (res.data.code === 0 && res.data.data) {
      dataList.value = res.data.data ?? []
    } else {
      message.error('获取数据失败' + res.data.message + res.data.data)
    }
  } catch (e) {
    message.error('发送请求发生异常' + e.message)
  }
  loading.value = false
}

const batchEditModalRef = ref()

// 批量编辑成功回调
const onBatchEditSuccess = async () => {
  fetchData()
}

// 弹出批量编辑窗口
const doBatchEdit = (e: Event) => {
  batchEditModalRef.value?.openModal()
}

// 空间 id 改变时候，必须重新获取数据
watch(
  () => props.spaceId,
  () => {
    fetchSpaceDetail()
    fetchData()
  },
)
</script>

<style scoped>
#spaceDetailPage {
  max-width: 1440px;
  margin: 0 auto 16px;
}
</style>
