<template>
  <a-modal
    class="image-cropper"
    v-model:open="open"
    title="编辑图片"
    :footer="false"
    @cancel="closeModal"
  >
    <vue-cropper
      ref="imageCropperRef"
      :img="pictureClone.url"
      :autoCrop="true"
      :fixedBox="false"
      :centerBox="true"
      :can-move="true"
      :canMoveBox="true"
      :info="true"
      outputType="png"
    />
    <div style="margin-bottom: 16px" />
    <!-- 协同编辑操作 -->
    <div class="image-edit-actions" v-if="isTeamSpace">
      <a-space>
        <a-button v-if="editingUser" disabled>{{ editingUser.userName }}正在编辑</a-button>
        <a-button v-if="canEnterEdit" type="primary" ghost @click="enterEdit">进入编辑</a-button>
        <a-button v-if="canExitEdit" danger ghost @click="exitEdit">退出编辑</a-button>
      </a-space>
    </div>
    <!-- 图片操作 -->
    <a-space>
      <a-button @click="rotateLeft" :disabled="!canEdit">向左旋转</a-button>
      <a-button @click="rotateRight" :disabled="!canEdit">向右旋转</a-button>
      <a-button @click="changeScale(1)" :disabled="!canEdit">放大</a-button>
      <a-button @click="changeScale(-1)" :disabled="!canEdit">缩小</a-button>
      <a-button type="primary" :loading="loading" :disabled="!canEdit" @click="handleConfirm">
        确认
      </a-button>
    </a-space>
  </a-modal>
</template>

<script setup lang="ts">
import { computed, onUnmounted, ref, watchEffect } from 'vue'
import { message } from 'ant-design-vue'
import { getPictureVoByIdUsingGet, uploadPictureUsingPost } from '@/api/pictureController.ts'
import { useLoginUserStore } from '@/stores/useLoginUserStore.ts'
import { PICTURE_EDIT_ACTION_ENUM, PICTURE_EDIT_MESSAGE_TYPE_ENUM } from '@/constant/picture.ts'
import PictureEditWebSocket from '@/utils/pictureEditWebSocket.ts'
import { SPACE_TYPE_ENUM } from '@/constant/space.ts'

interface Props {
  imageUrl?: string
  picture: API.PictureVO
  spaceId?: string | number
  space?: API.SpaceVO
  onSuccess?: (newPicture: API.PictureVO) => void
}

const props = withDefaults(defineProps<Props>(), {
  onSuccess: () => {},
})

const pictureClone = ref<API.PictureVO>({ ...props.picture })

// 是否为团队空间
const isTeamSpace = computed(() => {
  console.log(props.space + ' :: ' + props.space?.spaceType)
  return props.space?.spaceType === SPACE_TYPE_ENUM.TEAM
})

// 编辑器组件的引用
const imageCropperRef = ref()

// 向左旋转
const rotateLeft = () => {
  imageCropperRef.value.rotateLeft()
  editAction(PICTURE_EDIT_ACTION_ENUM.ROTATE_LEFT)
}

// 向右旋转
const rotateRight = () => {
  imageCropperRef.value.rotateRight()
  editAction(PICTURE_EDIT_ACTION_ENUM.ROTATE_RIGHT)
}

// 缩放
const changeScale = (num: number) => {
  imageCropperRef.value.changeScale(num)
  if (num > 0) {
    editAction(PICTURE_EDIT_ACTION_ENUM.ZOOM_IN)
  } else {
    editAction(PICTURE_EDIT_ACTION_ENUM.ZOOM_OUT)
  }
}

const loading = ref<boolean>(false)
// 确认裁剪
const handleConfirm = () => {
  imageCropperRef.value?.getCropBlob((blob: Blob) => {
    // blob 为已裁切的文件
    const fileName = (props.picture?.name ?? 'image') + '.png'
    const file = new File([blob], fileName, { type: blob.type })
    // 上传图片
    handleUpload({ file })
    // 向后端发送保存编辑消息
    saveEdit()
    closeModal()
  })
}

const handleUpload = async ({ file }: any) => {
  // loading用于控制等待效果 true 表示等待中， false 表示完成
  loading.value = true
  try {
    const params: API.PictureUploadRequest = props.picture ? { id: props.picture.id } : {}
    params.spaceId = props.spaceId
    const res = await uploadPictureUsingPost(params, {}, file)
    if (res.data.code === 0 && res.data.data) {
      message.success('图片上传成功')
      // 将上传成功的图片信息传递给父组件
      props.onSuccess?.(res.data.data)
    } else {
      message.error('图片上传失败')
    }
  } catch (error: any) {
    message.error('图片上传失败' + error.message)
  }
  loading.value = false
}

const open = ref<boolean>(false)
const openModal = () => {
  open.value = true
}

// 关闭弹窗
const closeModal = () => {
  open.value = false
  if (websocket) {
    websocket.disconnect()
  }
  editingUser.value = undefined
}

defineExpose({
  openModal,
})

// -------- 实时编辑 --------
const loginUserStore = useLoginUserStore()
let loginUser = loginUserStore.loginUser

// 正在编辑的用户
const editingUser = ref<API.UserVO>()

// 没有用户正在编辑中，可进入编辑
const canEnterEdit = computed(() => {
  return !editingUser.value
})

// 正在编辑的用户是本人，可退出编辑
const canExitEdit = computed(() => {
  return editingUser.value?.id === loginUser.id
})

// 可以编辑
const canEdit = computed(() => {
  if (!isTeamSpace.value) return true
  // 团队空间只有，编辑者才能协同编辑
  return editingUser.value?.id === loginUser.id
})

let websocket: PictureEditWebSocket | null

// 初始化 WebSocket 连接，绑定事件
const initWebsocket = () => {
  const pictureId = props.picture?.id
  if (!pictureId || !open.value) {
    return
  }
  // 防止之前的连接未释放
  if (websocket) {
    websocket.disconnect()
  }
  // 创建 WebSocket 实例
  websocket = new PictureEditWebSocket(pictureId)
  // 建立 WebSocket 连接
  websocket.connect()

  // 监听通知消息
  websocket.on(PICTURE_EDIT_MESSAGE_TYPE_ENUM.INFO, (msg) => {
    console.log('收到通知消息：', msg)
    message.info(msg.message)
    // 同步初始编辑的用户
    if (msg?.editUser) {
      editingUser.value = msg.editUser
    }
  })

  // 监听错误消息
  websocket.on(PICTURE_EDIT_MESSAGE_TYPE_ENUM.ERROR, (msg) => {
    console.log('收到错误消息：', msg)
    message.error(msg.message)
  })
  // 监听进入编辑状态消息
  websocket.on(PICTURE_EDIT_MESSAGE_TYPE_ENUM.ENTER_EDIT, (msg) => {
    console.log('收到进入编辑状态消息：', msg)
    message.info(msg.message)
    editingUser.value = msg.editUser
  })

  // 监听编辑操作消息
  websocket.on(PICTURE_EDIT_MESSAGE_TYPE_ENUM.EDIT_ACTION, (msg) => {
    console.log('收到编辑操作消息：', msg)
    // message.info(msg.message) // 测试的时候方便调试，实际上线不应该展示不然页面很花
    switch (msg.editAction) {
      case PICTURE_EDIT_ACTION_ENUM.ROTATE_LEFT:
        imageCropperRef.value.rotateLeft()
        break
      case PICTURE_EDIT_ACTION_ENUM.ROTATE_RIGHT:
        imageCropperRef.value.rotateRight()
        break
      case PICTURE_EDIT_ACTION_ENUM.ZOOM_IN:
        imageCropperRef.value.changeScale(1)
        break
      case PICTURE_EDIT_ACTION_ENUM.ZOOM_OUT:
        imageCropperRef.value.changeScale(-1)
        break
    }
  })

  // 监听退出编辑状态消息
  websocket.on(PICTURE_EDIT_MESSAGE_TYPE_ENUM.EXIT_EDIT, (msg) => {
    console.log('收到退出编辑状态消息：', msg)
    message.info(msg.message)
    if (!msg.editUser) {
      editingUser.value = undefined
    }
  })

  websocket.on(PICTURE_EDIT_MESSAGE_TYPE_ENUM.SAVE_EDIT, (msg) => {
    console.log('收到保存编辑消息：', msg)
    message.info(msg.message)
    let newPicture: API.PictureVO
    if (msg?.picUrl) {
      newPicture = { ...props.picture, url: msg.picUrl }
    } else {
      try {
        const res = getPictureVoByIdUsingGet(props.picture?.id)
        if (res.data.code === 0 && res.data.data) {
          newPicture = res.data.data as API.PictureVO
        } else {
          message.error('更新数据失败，请尝试刷新页面')
        }
      } catch (e: any) {
        message.error('更新数据失败，请尝试刷新页面', e)
      }
    }
    pictureClone.value = newPicture
    props.onSuccess(newPicture)
  })
}

// 监听 open 和 pictureId 的状态，改变时触发
watchEffect(() => {
  // 只有团队空间才需要 websocket
  if (isTeamSpace.value) initWebsocket()
})

// 组件被卸载后要关闭连接
onUnmounted(() => {
  // 断开连接
  if (websocket) {
    websocket.disconnect()
  }
  editingUser.value = undefined
})

// 进入编辑状态
const enterEdit = () => {
  if (websocket) {
    // 发送进入编辑状态的消息
    websocket.sendMessage({
      type: PICTURE_EDIT_MESSAGE_TYPE_ENUM.ENTER_EDIT,
    })
  }
}

// 退出编辑状态
const exitEdit = () => {
  if (websocket) {
    // 发送退出编辑状态的消息
    websocket.sendMessage({
      type: PICTURE_EDIT_MESSAGE_TYPE_ENUM.EXIT_EDIT,
    })
  }
}

// 编辑图片操作
const editAction = (action: string) => {
  if (websocket) {
    // 发送编辑操作的请求
    websocket.sendMessage({
      type: PICTURE_EDIT_MESSAGE_TYPE_ENUM.EDIT_ACTION,
      editAction: action,
    })
  }
}

const saveEdit = () => {
  if (websocket) {
    websocket.sendMessage({
      type: PICTURE_EDIT_MESSAGE_TYPE_ENUM.SAVE_EDIT,
    })
  }
}
</script>

<style scoped>
.image-cropper {
  text-align: center;
}

.image-cropper .vue-cropper {
  min-height: 400px !important;
  max-height: 600px !important;
}

.image-cropper-actions {
  text-align: center;
}

.image-edit-actions {
  text-align: center;
  margin-bottom: 12px;
}
</style>
