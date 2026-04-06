<template>
  <div id="pictureDetailPage">
    <a-row :gutter="[16, 16]">
      <!--  横向和纵向的间距都为16  -->
      <!-- 图片展示区 -->
      <a-col :sm="24" :md="16" :xl="18">
        <a-card title="图片预览">
          <a-image style="max-height: 600px; object-fit: contain" :src="picture.compressedUrl" />
        </a-card>
      </a-col>
      <!-- 图片信息区 -->
      <a-col :sm="24" :md="8" :xl="6">
        <a-card title="图片信息">
          <a-descriptions :column="1">
            <a-descriptions-item label="作者">
              <a-space>
                <a-avatar :size="24" :src="picture.userVO?.userAvatar" />
                <div>{{ picture.userVO?.userName }}</div>
              </a-space>
            </a-descriptions-item>
            <a-descriptions-item label="名称">
              {{ picture.name ?? '未命名' }}
            </a-descriptions-item>
            <a-descriptions-item label="简介">
              {{ picture.introduction ?? '------' }}
            </a-descriptions-item>
            <a-descriptions-item label="分类">
              <a-tag color="green">{{ picture.category ?? '默认' }}</a-tag>
            </a-descriptions-item>
            <a-descriptions-item label="标签">
              <a-tag color="blue" v-for="tag in picture.tags" :key="tag">
                {{ tag }}
              </a-tag>
            </a-descriptions-item>
            <a-descriptions-item label="格式">
              {{ picture.picFormat ?? '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="宽度">
              {{ picture.picWidth ?? '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="高度">
              {{ picture.picHeight ?? '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="宽高比">
              {{ picture.picScale ?? '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="大小">
              {{ formatSize(picture.picSize) }}
            </a-descriptions-item>
            <a-descriptions-item label="主色调">
              <a-space>
                {{ picture.picColor ?? '-' }}
                <div
                  v-if="picture.picColor"
                  :style="{
                    width: '16px',
                    height: '16px',
                    border: '1px',
                    backgroundColor: toHexColor(picture.picColor),
                  }"
                ></div>
              </a-space>
            </a-descriptions-item>
          </a-descriptions>
          <a-space wrap>
            <a-space>
              <a-button type="primary" :icon="h(DownloadOutlined)" @click="doDownload">
                免费下载
              </a-button>
              <a-button
                type="default"
                style="color: yellowgreen"
                :icon="h(ShareAltOutlined)"
                @click="(e: Event) => doShare(picture, e)"
              >
                分享
              </a-button>
            </a-space>
            <a-space>
              <a-button v-if="canEdit" :icon="h(EditOutlined)" type="primary" ghost @click="doEdit">
                编辑
              </a-button>
              <a-button v-if="canDelete" :icon="h(DeleteOutlined)" danger @click="doDelete">
                删除
              </a-button>
            </a-space>
          </a-space>
        </a-card>
      </a-col>
    </a-row>
    <share-model ref="shareModalRef" :link="shareLink"></share-model>
  </div>
</template>

<script lang="ts" setup>
import { deletePictureUsingPost, getPictureVoByIdUsingGet } from '@/api/pictureController.ts'
import { message } from 'ant-design-vue'
import { computed, h, onMounted, ref } from 'vue'
import { downloadImage, formatSize, toHexColor } from '@/utils'
import { useLoginUserStore } from '@/stores/useLoginUserStore.ts'
import {
  DeleteOutlined,
  EditOutlined,
  DownloadOutlined,
  ShareAltOutlined,
} from '@ant-design/icons-vue'
import router from '@/router'
import { useRoute } from 'vue-router'
import ShareModel from '@/components/ShareModel.vue'
import { SPACE_PERMISSION_ENUM } from '@/constant/space.ts'

interface Props {
  pictureId: string | number
}

const props = defineProps<Props>()
const picture = ref<API.PictureVO>({})

// 通用权限检查函数
function createPermissionChecker(permission: string) {
  return computed(() => {
    return (picture.value.permissionList ?? []).includes(permission)
  })
}

// 定义权限检查
const canEdit = createPermissionChecker(SPACE_PERMISSION_ENUM.PICTURE_EDIT)
const canDelete = createPermissionChecker(SPACE_PERMISSION_ENUM.PICTURE_DELETE)

/**
 * 获取旧图片信息
 */
const fetchPictureDetail = async () => {
  const id = props.pictureId
  try {
    if (id) {
      const res = await getPictureVoByIdUsingGet({ id })
      if (res.data.code === 0 && res.data.data) {
        picture.value = res.data.data
      } else {
        message.error('获取图片信息失败 ', res.data.message)
      }
    } else {
      message.error('缺少路径参数')
    }
  } catch (e: any) {
    message.error('获取图片信息失败' + e.message)
  }
}
// 打开页面时获取图片信息
onMounted(fetchPictureDetail)

// 下载图片
const doDownload = () => {
  downloadImage(picture.value.url)
}

const route = useRoute()
// 编辑图片
const doEdit = () => {
  const id = picture.value.id
  console.log(id)
  debugger
  if (!id) {
    message.error('图片不存在')
    router.push('/')
    return
  } else {
    router.push({
      path: '/add_picture',
      query: {
        id,
        redirect: route.path,
      },
    })
  }
}

// 删除逻辑
const doDelete = async () => {
  try {
    const id = picture.value.id
    if (!id) return
    const res = await deletePictureUsingPost({ id })
    if (res.data.code === 0) {
      message.success('删除成功')
    } else {
      message.error('删除失败')
      if (useLoginUserStore().loginUser.userName == '未登录') {
        router.push({
          path: '/user/login',
          query: {
            redirect: route.path,
          },
        })
      }
    }
    const spaceId = picture.value.spaceId
    const path: string = spaceId ? `/space/${spaceId}` : '/'
    router.push({
      path,
    })
  } catch (e: any) {
    message.error('删除失败' + e.message)
  }
}

/* 分享图片 */
const shareModalRef = ref()
const shareLink = ref<string>()

const doShare = async (picture: API.PictureVO, e: Event) => {
  e.stopPropagation()
  const location = window.location
  shareLink.value = `${location.protocol}//${location.host}/picture/${picture.id}`
  console.log(shareModalRef)
  if (shareModalRef.value) {
    shareModalRef.value.openModal()
  }
}
</script>

<style scoped>
#pictureDetailPage {
  max-width: 960px;
  margin: 0 auto 16px;
}
</style>
