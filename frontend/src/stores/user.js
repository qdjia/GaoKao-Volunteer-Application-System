import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const role = ref(localStorage.getItem('role') || '')
  const username = ref(localStorage.getItem('username') || '')
  const studentId = ref(localStorage.getItem('studentId') || '')

  function setLogin(data) {
    token.value = data.token
    role.value = data.role
    username.value = data.username
    studentId.value = data.studentId || ''
    localStorage.setItem('token', data.token)
    localStorage.setItem('role', data.role)
    localStorage.setItem('username', data.username)
    localStorage.setItem('studentId', data.studentId || '')
  }

  function logout() {
    token.value = ''
    role.value = ''
    username.value = ''
    studentId.value = ''
    localStorage.removeItem('token')
    localStorage.removeItem('role')
    localStorage.removeItem('username')
    localStorage.removeItem('studentId')
  }

  const isAdmin = () => role.value === 'ADMIN'
  const isStudent = () => role.value === 'STUDENT'

  return { token, role, username, studentId, setLogin, logout, isAdmin, isStudent }
})