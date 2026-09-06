import { defineStore } from 'pinia'
import { ref } from 'vue'

const storage = sessionStorage
for (const key of ['token', 'role', 'username', 'studentId', 'mustChangePassword', 'expiresAt', 'permanentDemo']) {
  localStorage.removeItem(key)
}

export const useUserStore = defineStore('user', () => {
  const token = ref(storage.getItem('token') || '')
  const role = ref(storage.getItem('role') || '')
  const username = ref(storage.getItem('username') || '')
  const studentId = ref(storage.getItem('studentId') || '')
  const mustChangePassword = ref(storage.getItem('mustChangePassword') === 'true')
  const expiresAt = ref(storage.getItem('expiresAt') || '')
  const permanentDemo = ref(storage.getItem('permanentDemo') === 'true')

  function setLogin(data) {
    token.value = data.token
    role.value = data.role
    username.value = data.username
    studentId.value = data.studentId || ''
    mustChangePassword.value = Boolean(data.mustChangePassword)
    expiresAt.value = data.expiresAt || ''
    permanentDemo.value = Boolean(data.permanentDemo)
    storage.setItem('token', data.token)
    storage.setItem('role', data.role)
    storage.setItem('username', data.username)
    storage.setItem('studentId', data.studentId || '')
    storage.setItem('mustChangePassword', String(Boolean(data.mustChangePassword)))
    storage.setItem('expiresAt', data.expiresAt || '')
    storage.setItem('permanentDemo', String(Boolean(data.permanentDemo)))
  }

  function logout() {
    token.value = ''
    role.value = ''
    username.value = ''
    studentId.value = ''
    mustChangePassword.value = false
    expiresAt.value = ''
    permanentDemo.value = false
    for (const key of ['token', 'role', 'username', 'studentId', 'mustChangePassword', 'expiresAt', 'permanentDemo']) {
      storage.removeItem(key)
    }
  }

  const isAdmin = () => role.value === 'ADMIN'
  const isStudent = () => role.value === 'STUDENT'

  return {
    token, role, username, studentId, mustChangePassword, expiresAt, permanentDemo,
    setLogin, logout, isAdmin, isStudent
  }
})
