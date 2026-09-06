import { ref, watch, onBeforeUnmount } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const dirty = ref(false)
const pending = ref(false)

export function useDraftProtection(changed, busy) {
  const stop = watch([changed, busy], ([a, b]) => { dirty.value = a; pending.value = b }, { immediate: true, flush: 'sync' })
  const warn = event => {
    if (!dirty.value && !pending.value) return
    event.preventDefault()
    event.returnValue = ''
  }
  window.addEventListener('beforeunload', warn)
  onBeforeUnmount(() => {
    stop()
    dirty.value = false
    pending.value = false
    window.removeEventListener('beforeunload', warn)
  })
}

export async function confirmLeave() {
  if (pending.value) { ElMessage.warning('保存或提交尚未完成，请稍候'); return false }
  if (!dirty.value) return true
  try {
    await ElMessageBox.confirm('尚有未保存的志愿修改。离开后这些修改将丢失。', '离开填报页面？', {
      type: 'warning', confirmButtonText: '放弃修改并离开', cancelButtonText: '继续填报', closeOnClickModal: false
    })
    return true
  } catch (_) { return false }
}
