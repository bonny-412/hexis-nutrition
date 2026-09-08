import { defineStore } from 'pinia'
import { ref } from 'vue'

const STORAGE_KEY = 'hexis-theme'

export const useThemeStore = defineStore('theme', () => {
  const isDark = ref(localStorage.getItem(STORAGE_KEY) === 'dark')

  function applica() {
    document.documentElement.classList.toggle('dark', isDark.value)
  }

  function toggleTheme() {
    isDark.value = !isDark.value
    localStorage.setItem(STORAGE_KEY, isDark.value ? 'dark' : 'light')
    applica()
  }

  applica()

  return { isDark, toggleTheme }
})
