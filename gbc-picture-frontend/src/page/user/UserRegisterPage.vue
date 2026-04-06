<template>
  <div id="userRegisterPage">
    <h2 class="title">TOGE 智能协作相册 - 注册新用户</h2>
    <div class="desc">企业级智能协同云图库</div>
    <a-form
      :model="formState"
      name="basic"
      autocomplete="off"
      @finish="handlerSubmit"
    >
      <a-form-item
        name="userAccount"
        :rules="[{ required: true, message: '请输入账号！' }]"
      >
        <a-input v-model:value="formState.userAccount" placeholder="请输入账号" />
      </a-form-item>

      <a-form-item
        name="userPassword"
        :rules="[
          { required: true, message: '请输入密码' },
          { min: 6, message: '密码长度不能小于6位' },
          { max: 20, message: '密码长度不能大于20位' }
        ]"
      >
        <a-input-password v-model:value="formState.userPassword" placeholder="请输入密码" />
      </a-form-item>

      <a-form-item
        name="checkPassword"
        :rules="[
          { required: true, message: '请输入确认密码' },
          {
            // 校验确认两次密码是否一致
            validator: validateCheckPassword,
            trigger: 'blur' // 或 'change'，根据需要
          }
        ]"
      >
        <a-input-password v-model:value="formState.checkPassword" placeholder="请输入确认密码" />
      </a-form-item>

      <div class="tips">
        已有账号？
        <router-link to="/user/login">去登录</router-link>
      </div>

      <a-form-item>
        <a-button type="primary" html-type="submit" style="width: 100%">注册</a-button>
      </a-form-item>
    </a-form>
  </div>
</template>
<script lang="ts" setup>
import { reactive } from 'vue'
import { useLoginUserStore } from '@/stores/useLoginUserStore.ts'
import { message } from 'ant-design-vue'
import router from '@/router'
import { userRegisterUsingPost } from '@/api/userController.ts'

const loginUserStore = useLoginUserStore()

// 用于接收表单输入的值
const formState = reactive<API.UserRegisterRequest>({
  userAccount: '',
  userPassword: '',
  checkPassword: ''
})
const validateCheckPassword  = async (rule:any, value:any) => {
  if (value.length < 6) {
    throw new Error('密码长度不能小于8位')
  }
  if (value.length > 20) {
    throw new Error('密码长度不能大于20位')
  }
  if (value !== formState.userPassword) {
    throw new Error('两次输入的密码不一致')
  }
}
// 发送注册请求
const handlerSubmit = async (values: any) => {
  try {
    const res = await userRegisterUsingPost(values)
    if (res.data.code === 0 && res.data.data) {
      await loginUserStore.fetchLoginUser()
      message.success()
      router.push({
        path: '/user/login',
        replace: true
      })
    } else {
      message.error('登录失败' + res.data.message)
    }
  } catch (e) {
    message.error('登录失败' + e)
  }
}
</script>
<style scoped>
#userRegisterPage {
  max-width: 360px;
  margin: 0 auto;
}

.title {
  text-align: center;
}

.desc {
  color: #bbb;
  margin-bottom: 16px;
  text-align: center;
}

.tips {
  color: #bbb;
  font-size: 13px;
  margin-bottom: 16px;
  text-align: right;
}
</style>

