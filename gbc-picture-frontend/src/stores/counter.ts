import { ref, computed } from 'vue'
import { defineStore } from 'pinia'

export const useCounterStore = defineStore('counter', () => {
  // 定义状态初始值
  const count = ref(0)
  // 定义变量计算逻辑getter
  const doubleCount = computed(() => count.value * 2)
  // 定义怎么更改状态的方法
  function increment() {
    count.value++
  }
  // 返回
  return { count, doubleCount, increment }
})
