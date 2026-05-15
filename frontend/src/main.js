import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import ko from 'element-plus/es/locale/lang/ko'

import App from './App.vue'
import router from './router'
import './assets/main.css'

// ptkt-* → workoop-* localStorage 키 일회성 마이그레이션
if (!localStorage.getItem('workoop-migrated')) {
  const renames = [
    ['ptkt-dark',        'workoop-dark'],
    ['ptkt-memos',       'workoop-memos'],
    ['ptkt-badges',      'workoop-badges'],
    ['ptkt-favorites',   'workoop-favorites'],
    ['ptkt-reviews',     'workoop-reviews'],
    ['ptkt-star-stories','workoop-star-stories'],
  ]
  renames.forEach(([from, to]) => {
    const v = localStorage.getItem(from)
    if (v !== null) { localStorage.setItem(to, v); localStorage.removeItem(from) }
  })
  // ptkt-scrum-*, ptkt-checkin-*, ptkt-ms-* 패턴 키 일괄 이전
  Object.keys(localStorage)
    .filter(k => k.startsWith('ptkt-'))
    .forEach(k => {
      const v = localStorage.getItem(k)
      const newKey = k.replace(/^ptkt-/, 'workoop-')
      if (v !== null) { localStorage.setItem(newKey, v); localStorage.removeItem(k) }
    })
  localStorage.setItem('workoop-migrated', '1')
}

const app = createApp(App)

// Element Plus 아이콘 전역 등록
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.use(createPinia())
app.use(router)
app.use(ElementPlus, { locale: ko })

app.mount('#app')
