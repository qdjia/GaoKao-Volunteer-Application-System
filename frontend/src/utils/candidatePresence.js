import { watch, onMounted, onBeforeUnmount } from 'vue'
import { useUserStore } from '../stores/user'
import request from './request'

export function useCandidatePresence() {
  const store = useUserStore()
  let timer, inFlight = false
  async function heartbeat() {
    if (!store.isStudent() || !store.token || inFlight) return
    inFlight = true
    try { await request.post('/candidate/heartbeat', {}, { silent: true }) } catch (_) {} finally { inFlight = false }
  }
  function offline() {
    if (!store.isStudent() || !store.token) return
    fetch('/api/candidate/offline', { method: 'POST', keepalive: true, headers: { Authorization: `Bearer ${store.token}` } }).catch(() => {})
  }
  watch(() => store.token, heartbeat)
  onMounted(() => {
    heartbeat()
    timer = setInterval(heartbeat, 10000)
    window.addEventListener('pagehide', offline)
    window.addEventListener('pageshow', heartbeat)
  })
  onBeforeUnmount(() => {
    clearInterval(timer)
    offline()
    window.removeEventListener('pagehide', offline)
    window.removeEventListener('pageshow', heartbeat)
  })
}
