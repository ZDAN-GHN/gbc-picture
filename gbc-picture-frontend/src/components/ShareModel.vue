<template>
  <div>
    <a-modal v-model:open="open" :title="title" :footer="false" @cancel="closeModal">
      <h4>复制分享链接</h4>
      <a-typography-link copyable>
        {{ link }}
      </a-typography-link>
      <h4>手机端扫码查看</h4>
      <a-qrcode error-level="H" :value="link" icon="https://www.codefather.cn/logo.png" />
    </a-modal>
  </div>
</template>
<script lang="ts" setup>
import { ref } from 'vue'

interface Props {
  title?: string
  link?: string
}

const props = withDefaults(defineProps<Props>(), {
  title: '分享图片',
  link: 'www.codefather.cn',
})

const open = ref<boolean>(false)

const openModal = () => {
  open.value = true
}

const closeModal = (e: MouseEvent) => {
  open.value = false
}

// 暴露函数给父组件
defineExpose({
  openModal,
})
</script>
