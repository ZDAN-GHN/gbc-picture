<template>
  <div id="globalSider">
    <a-layout-sider
      width="240"
      v-if="loginUserStore.loginUser.id"
      breakpoint="lg"
      collapsedWidth="0"
    >
      <a-menu
        v-model:selectedKeys="current"
        mode="inline"
        :items="menuItems"
        @click="doMenuClick"
      />
    </a-layout-sider>
  </div>
</template>

<script lang="ts" setup>
import { computed, h, ref, watchEffect } from 'vue'
import { PictureOutlined, UserOutlined, TeamOutlined } from '@ant-design/icons-vue'
import { useRouter } from 'vue-router'
import { useLoginUserStore } from '@/stores/useLoginUserStore.ts'
import { SPACE_TYPE_ENUM } from '@/constant/space.ts'
import { listMyTeamSpaceUsingPost } from '@/api/spaceUserController.ts'
import { message } from 'ant-design-vue'

const loginUserStore = useLoginUserStore()

// 固定的菜单列表
const fiexedMenuItems = [
  {
    key: '/',
    label: '公共图库',
    icon: () => h(PictureOutlined),
  },
  {
    key: '/my_space',
    label: '我的空间',
    icon: () => h(UserOutlined),
  },
  {
    key: '/add_space?type=' + SPACE_TYPE_ENUM.TEAM,
    label: '创建团队',
    icon: () => h(TeamOutlined),
  },
]

const teamSpaceList = ref<API.SpaceUserVO[]>([])
const menuItems = computed(() => {
  if (teamSpaceList.value.length === 0) {
    // 用户没有团队空间直接返回固定菜单列表
    return fiexedMenuItems
  } else {
    // 用户有团队空间，返回动态菜单列表
    const teamSpaceSubMenus = teamSpaceList.value.map((spaceUser) => {
      const space = spaceUser.space as API.SpaceVO
      console.log('space', space)
      return {
        key: '/space/' + spaceUser.spaceId,
        label: space.spaceName,
      }
    })
    const teamSpaceSubMenuGroup = {
      type: 'group',
      key: 'teamSpace',
      label: '我的团队',
      children: teamSpaceSubMenus,
    }
    return [...fiexedMenuItems, teamSpaceSubMenuGroup]
  }
})

// 加兹团队空间列表
const fetchTeamSpaceList = async () => {
  try {
    const res = await listMyTeamSpaceUsingPost({})
    console.log('res', res)
    if (res.data.code === 0) {
      teamSpaceList.value = res.data.data ?? []
    } else {
      message.error('加载团队空间失败 ' + res.data.message)
    }
  } catch (e: any) {
    message.error('请求加载团队失败 ' + e.message)
  }
}

/**
 * 监听变量，改变时触发数据的重新加载
 */
watchEffect(() => {
  // 登录才加载
  if (loginUserStore.loginUser.id) {
    fetchTeamSpaceList()
  }
})

const router = useRouter()

// 当前选中菜单
const current = ref<string[]>([])
// 监听路由变化,更新当前选中菜单
router.afterEach((to, from, failure) => {
  current.value = [to.path]
})

// 路由跳转事件
const doMenuClick = ({ key }: { key: string }) => {
  router.push(key)
}
</script>

<style scoped>
#globalSider .ant-layout-sider {
  background: none;
}
</style>
