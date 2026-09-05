import { defineStore } from 'pinia'
import { ref } from 'vue'

const storage = sessionStorage
for (const key of ['token', 'role', 'username', 'studentId', 'mustChangePassword', 'expiresAt']) {
  localStorage.removeItem(key)
}

export const useUserStore = defineStore('user', () => {
  const token = ref(storage.getItem('token') || '')
  const role = ref(storage.getItem('role') || '')
  const username = ref(storage.getItem('username') || '')
  const studentId = ref(storage.getItem('studentId') || '')
  const mustChangePassword = ref(storage.getItem('mustChangePassword') === 'true')
  const expiresAt = ref(storage.getItem('expiresAt') || '')

  function setLogin(data) {
    token.value = data.token
    role.value = data.role
    username.value = data.username
    studentId.value = data.studentId || ''
    mustChangePassword.value = Boolean(data.mustChangePassword)
    expiresAt.value = data.expiresAt || ''
    storage.setItem('token', data.token)
    storage.setItem('role', data.role)
    storage.setItem('username', data.username)
    storage.setItem('studentId', data.studentId || '')
    storage.setItem('mustChangePassword', String(Boolean(data.mustChangePassword)))
    storage.setItem('expiresAt', data.expiresAt || '')
  }

  function logout() {
    token.value = ''
    role.value = ''
    username.value = ''
    studentId.value = ''
    mustChangePassword.value = false
    expiresAt.value = ''
    for (const key of ['token', 'role', 'username', 'studentId', 'mustChangePassword', 'expiresAt']) {
      storage.removeItem(key)
    }
  }

  const isAdmin = () => role.value === 'ADMIN'
  const isStudent = () => role.value === 'STUDENT'

  return {
    token, role, username, studentId, mustChangePassword, expiresAt,
    setLogin, logout, isAdmin, isStudent
  }
})
