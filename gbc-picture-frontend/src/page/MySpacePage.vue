<template>
  <div id="mySpacePage">
    <p v-if="'' === spaceId">正在加载个人空间...</p>
    <space-detail-page v-else :spaceId="spaceId as string" />
  </div>
</template>

<script lang="ts" setup>
import { useRouter } from 'vue-router'
import { useLoginUserStore } from '@/stores/useLoginUserStore.ts'
import { listSpaceVoByPageUsingPost } from '@/api/spaceController.ts'
import { message } from 'ant-design-vue'
import { onMounted, ref } from 'vue'
import { SPACE_TYPE_ENUM, SPACE_TYPE_MAP } from '@/constant/space.ts'
import SpaceDetailPage from '@/page/SpaceDetailPage.vue'

const router = useRouter()
const loginUser = useLoginUserStore().loginUser

const spaceId = ref<string | number>('')

const checkUserSpace = async () => {
  // 用户未登录，跳转到登录页面
  if (!loginUser?.id) {
    router.push('/user/login')
    return
  }
  // 用户已登录，查询是否有个人空间
  const res = await listSpaceVoByPageUsingPost({
    userId: loginUser.id,
    spaceType: SPACE_TYPE_ENUM.PRIVATE,
    current: 1,
    pageSize: 1,
  })
  if (res.data.code === 0) {
    if (res.data.data?.records && res.data.data.records.length > 0) {
      // 有私有空间，进入到第一个空间
      const space = res.data.data.records[0]
      spaceId.value = space.id ?? ''
      // router.replace(`space/${space.id}`)
    } else {
      // 没有个人空间，跳转到创建空间页面
      router.replace('/add_space')
      message.warn('请先创建空间')
    }
  } else {
    message.error('加载我的空间失败 ' + res.data.message)
  }
}

onMounted(checkUserSpace)
</script>

<style scoped></style>
