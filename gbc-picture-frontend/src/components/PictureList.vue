<template>
  <div class="picture-list">
    <!--  图片列表  -->
    <a-list
      :grid="{ gutter: 16, xs: 1, sm: 2, md: 3, lg: 4, xl: 5, xxl: 6 }"
      :data-source="dataList"
      :loading="loading"
    >
      <template #renderItem="{ item: picture }">
        <a-list-item style="padding: 0">
          <a-card hoverable @click="doClickPicture(picture)">
            <template #cover>
              <img
                :alt="picture.name"
                :src="picture.thumbnailUrl ?? picture.url"
                style="height: 180px; object-fit: cover"
              />
            </template>
            <a-card-meta :title="picture.name">
              <template #description>
                <a-flex>
                  <a-tag color="green">
                    {{ picture.category ?? '默认' }}
                  </a-tag>
                  <a-tag v-if="picture.tags" color="blue" v-for="tag in picture.tags">
                    {{ tag }}
                  </a-tag>
                </a-flex>
              </template>
            </a-card-meta>
            <template #actions v-if="showAction">
              <share-alt-outlined
                title="分享"
                style="color: yellowgreen"
                @click="(e) => doShare(picture, e)"
              />
              <search-outlined
                title="搜索"
                style="color: orange"
                @click="(e) => doSearch(picture, e)"
              />
              <edit-outlined
                title="编辑"
                v-if="canEdit"
                style="color: dodgerblue"
                @click="(e) => doEdit(picture, e)"
              />
              <delete-outlined
                title="删除"
                v-if="canDelete"
                style="color: red"
                @click="(e) => doDelete(picture, e)"
              />
            </template>
          </a-card>
        </a-list-item>
      </template>
    </a-list>
    <share-model ref="shareModalRef" :link="shareLink"></share-model>
  </div>
</template>

<script lang="ts" setup>
import { useRoute, useRouter } from 'vue-router'
import {
  ShareAltOutlined,
  SearchOutlined,
  DeleteOutlined,
  EditOutlined,
} from '@ant-design/icons-vue'
import { deletePictureUsingPost } from '@/api/pictureController.ts'
import { message } from 'ant-design-vue'
import { useLoginUserStore } from '@/stores/useLoginUserStore.ts'
import ShareModel from '@/components/ShareModel.vue'
import { ref } from 'vue'

// 父组件需要传递的属性
interface Props {
  dataList?: API.PictureVO[]
  loading?: boolean
  showAction?: boolean
  canEdit?: boolean
  canDelete?: boolean
  attachment?: Record<string, any>
  deletePictureCallback?: (id: string | number) => any
}

// 为 props 设定默认值
const props = withDefaults(defineProps<Props>(), {
  dataList: () => [],
  loading: false,
  showAction: false,
  canEdit: false,
  canDelete: false,
  attachment: () => ({}),
  deletePictureCallback: () => {},
})

const router = useRouter()
const route = useRoute()

/**
 * 点击图片后跳转到图片详情页
 */
const doClickPicture = (picture: API.PictureVO) => {
  router.push({
    path: `/picture/${picture.id}`,
  })
}

// 搜索图片回调
const doSearch = (picture: API.PictureVO, e: Event) => {
  console.log(picture)
  // 阻止冒泡
  e.stopPropagation()
  // 打开新的页面
  window.open(`/search_picture?pictureId=${picture.id}`)
}

// 编辑图片回调
const doEdit = (picture: API.PictureVO, e: Event) => {
  // 阻止事件冒泡
  e.stopPropagation()
  console.log(picture)
  const pictureId = picture.id
  // 图片不存在直接取消操作
  if (!pictureId) {
    message.error('图片不存在')
    return
  }
  router.push({
    path: `/add_picture`,
    query: {
      id: pictureId,
      spaceId: picture.spaceId,
      spaceName: props.attachment['spaceName'],
      redirect: route.path,
    },
  })
  console.log('编辑图片')
}

// 删除图片回调
const doDelete = async (picture: API.PictureVO, e: Event) => {
  // 阻止事件冒泡
  e.stopPropagation()
  const pictureId = picture.id
  // 图片不存在直接取消操作
  if (!pictureId) {
    message.error('图片不存在')
    return
  }
  // 删除后端数据
  const res = await deletePictureUsingPost({ id: pictureId })
  if (res.data.code === 0) {
    message.success('删除成功')
    // 删除回调，删除前端数据
    props.deletePictureCallback(pictureId)
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
  console.log('删除图片')
}

/* 分享图片 */
const shareModalRef = ref()
const shareLink = ref<string>()

const doShare = async (picture: API.PictureVO, e: Event) => {
  e.stopPropagation()
  const location = window.location
  shareLink.value = `${location.protocol}://${location.host}/picture/${picture.id}`
  shareModalRef.value?.openModal()
}
</script>

<style scoped></style>
