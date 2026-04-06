/* 全局权限校验 */
import router from '@/router'
import { useLoginUserStore } from '@/stores/useLoginUserStore.ts'
import { message } from 'ant-design-vue'

let firstFetchLoginUser = true

router.beforeEach(async (to, from, next)=> {
  const loginUserStore = useLoginUserStore()
  let loginUser = {...loginUserStore.loginUser}
  if (firstFetchLoginUser) {
    await loginUserStore.fetchLoginUser()
    loginUser = {...loginUserStore.loginUser}
    firstFetchLoginUser = false
  }
  const url = to.fullPath
  if (url.startsWith('/admin')) {
    if (!loginUser || loginUser.userRole !== 'admin') {
      message.error('没有权限')
      next({
        path: '/user/login',
        query: {
          redirect: url
        }
      })
      return
    }
  }
  next()
})
