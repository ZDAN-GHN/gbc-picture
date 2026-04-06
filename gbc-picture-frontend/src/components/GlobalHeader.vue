<template>
  <div id="globalHeader">
    <a-row :wrap="false">
      <a-col flex="240px">
        <router-link to="/">
          <div class="title-bar">
            <img class="logo" src="@/assets/gbc.png" alt="gbc" style=""/>
            <div class="title">TOGE 智能协作相册</div>
          </div>
        </router-link>
      </a-col>
      <a-col flex="auto">
        <a-menu
          v-model:selectedKeys="current"
          mode="horizontal"
          :items="items"
          @click="doMenuClick"
        />
      </a-col>
      <a-col flex="120px">
        <div class="user-login-status">
          <div v-if="loginUserStore.loginUser.id">
            <a-dropdown>
              <a class="ant-dropdown-link" @click.prevent>
                <a-space>
                  <a-avatar :src="loginUserStore.loginUser.userAvatar"></a-avatar>
                  {{ loginUserStore.loginUser.userName ?? '无名' }}
                </a-space>
              </a>
              <template #overlay>
                <a-menu class="dropdown-menu">
                  <a-menu-item>
                    <router-link to="/my_space">
                      <UserOutlined />
                      我的空间
                    </router-link>
                  </a-menu-item>
                  <a-menu-item>
                    <router-link :to="`/add_space?type=${SPACE_TYPE_ENUM.TEAM}`">
                      <TeamOutlined />
                      创建团队
                    </router-link>
                  </a-menu-item>
                  <a-menu-item>
                    <LogoutOutlined />
                    <!-- 取消时间冒泡以阻止点击后下拉菜单自动消失的默认效果 -->
                    <a @click.stop="doLogout"> 退出登录</a>
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
          </div>
          <div v-else>
            <a-button type="primary" href="/user/login">登录</a-button>
          </div>
        </div>
      </a-col>
    </a-row>
  </div>
</template>

<script lang="ts" setup>
import { computed, h, ref } from 'vue'
import { HomeOutlined, LogoutOutlined, UserOutlined, TeamOutlined } from '@ant-design/icons-vue'
import { type MenuProps, message } from 'ant-design-vue'
import { useRouter } from 'vue-router'
import { useLoginUserStore } from '@/stores/useLoginUserStore.ts'
import { userLogoutUsingPost } from '@/api/userController.ts'
import { SPACE_TYPE_ENUM } from '@/constant/space.ts'

// 用户登录状态
const loginUserStore = useLoginUserStore()

// 获取路由器
const router = useRouter()

// 菜单点击时间回调
const doMenuClick = ({ key }) => {
  router.push({
    path: key,
  })
}

// 退出登录点击回调
const doLogout = async () => {
  try {
    const res = await userLogoutUsingPost()
    if (res.data.code === 0) {
      message.success('退出登录成功')
      useLoginUserStore().setLoginUser({ username: '未登录' })
      router.push({
        path: '/user/login',
        replace: true,
      })
    }
  } catch (e) {
    message.error('退出登录失败')
  }
}

// 高亮页面
const current = ref<string[]>([])

// 路由钩子，每次路由结束后执行
// router.afterEach((to, from, fialed)=>{
router.afterEach((to, from, next) => {
  current.value = [to.path]
})

// 未经过滤的菜单项
const originItems = [
  {
    key: '/',
    icon: () => h(HomeOutlined),
    label: '主页',
    title: '主页',
  },
  {
    key: '/add_picture',
    label: '创建图片',
    title: '创建图片',
  },
  {
    key: '/add_space',
    label: '创建空间',
    title: '创建空间',
  },
  {
    key: '/admin/userManage',
    label: '用户管理',
    title: '用户管理',
  },
  {
    key: '/admin/pictureManage',
    label: '图片管理',
    title: '图片管理',
  },
  {
    key: '/admin/spaceManage',
    label: '空间管理',
    title: '空间管理',
  },
  {
    key: '/about',
    label: '关于',
    title: '关于',
  },
  {
    key: 'others',
    label: h('a', { href: 'https://www.codefather.cn', target: '_blank' }, '编程导航'),
    title: '编程导航',
  },
]

// 根据权限过滤菜单项
const filterMenus = (menus = [] as MenuProps['items']) => {
  return menus?.filter((menu) => {
    if (menu?.key?.startsWith('/admin')) {
      const loginUser = { ...loginUserStore.loginUser }
      return loginUser && loginUser.userRole === 'admin'
    }
    return true
  })
}

// 过滤菜单
const items = computed(() => filterMenus(originItems))
</script>

<style scoped>
#globalHeader .title-bar {
  display: flex;
  align-items: center;
}

#globalHeader .title {
  color: black;
  font-size: 18px;
  margin-left: 16px;
}

#globalHeader .logo {
  height: 60px;
}

.dropdown-menu {
  margin-top: 10px;
}
</style>
