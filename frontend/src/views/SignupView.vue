<template>
  <div class="auth-page">
    <div class="auth-card">
      <div class="auth-logo">
        <svg viewBox="0 0 28 28" fill="none" xmlns="http://www.w3.org/2000/svg" width="36" height="36">
          <rect x="1" y="18" width="6" height="9" rx="1.5" fill="rgba(52,211,153,0.9)"/>
          <rect x="8.5" y="13" width="6" height="14" rx="1.5" fill="rgba(251,146,60,0.9)"/>
          <rect x="16" y="7.5" width="6" height="19.5" rx="1.5" fill="rgba(147,197,253,0.95)"/>
          <line x1="3" y1="25" x2="20" y2="7" stroke="white" stroke-width="2.2" stroke-linecap="round"/>
          <polyline points="13.5,7 20,7 20,14" stroke="white" stroke-width="2.2" fill="none"
            stroke-linecap="round" stroke-linejoin="round"/>
          <path d="M20 4.5 L21 6.5 L23.2 7.3 L21 8.1 L20 10.1 L19 8.1 L16.8 7.3 L19 6.5 Z"
            fill="#FDE047" opacity="0.95"/>
        </svg>
        <span class="auth-brand">Workoop</span>
      </div>

      <h1 class="auth-title">회원가입</h1>
      <p class="auth-sub">무료로 시작하고 팀을 초대하세요</p>

      <form @submit.prevent="handleSignup" class="auth-form">
        <div class="form-group">
          <label>이름</label>
          <input v-model="name" type="text" placeholder="홍길동" required />
        </div>
        <div class="form-group">
          <label>이메일</label>
          <input v-model="email" type="email" placeholder="you@company.com" required />
        </div>
        <div class="form-group">
          <label>비밀번호</label>
          <input v-model="password" type="password" placeholder="8자 이상" minlength="8" required />
        </div>
        <button type="submit" class="btn-primary" :disabled="loading">
          {{ loading ? '가입 중...' : '시작하기' }}
        </button>
      </form>

      <p class="auth-switch">
        이미 계정이 있으신가요?
        <router-link to="/login">로그인</router-link>
      </p>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/store/authStore'

const router   = useRouter()
const auth     = useAuthStore()
const name     = ref('')
const email    = ref('')
const password = ref('')
const loading  = ref(false)

async function handleSignup() {
  loading.value = true
  try {
    await auth.signup(email.value, password.value, name.value)
    router.push('/onboarding')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  background: #1D1E20;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}
.auth-card {
  width: 100%;
  max-width: 420px;
  background: rgba(255,255,255,0.05);
  border-radius: var(--radius-xl);
  padding: 40px;
  border: 1px solid rgba(255,255,255,0.10);
  box-shadow: 0 24px 48px rgba(0,0,0,0.4);
}
.auth-logo {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 28px;
}
.auth-brand {
  font-size: 1.4rem;
  font-weight: 800;
  color: white;
  letter-spacing: -0.5px;
}
.auth-title {
  font-size: 1.5rem;
  font-weight: 700;
  color: white;
  margin: 0 0 6px;
}
.auth-sub {
  font-size: 0.875rem;
  color: rgba(255,255,255,0.45);
  margin: 0 0 28px;
}
.auth-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.form-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.form-group label {
  font-size: 0.813rem;
  font-weight: 600;
  color: rgba(255,255,255,0.55);
}
.form-group input {
  padding: 10px 14px;
  background: rgba(0,0,0,0.25);
  border: 1px solid rgba(255,255,255,0.12);
  border-radius: var(--radius-md);
  color: white;
  font-size: 0.9rem;
  outline: none;
  transition: border-color var(--transition-fast);
  font-family: inherit;
}
.form-group input:focus { border-color: var(--color-project); }
.form-group input::placeholder { color: rgba(255,255,255,0.25); }
.btn-primary {
  margin-top: 4px;
  padding: 12px;
  background: var(--color-project);
  color: white;
  border: none;
  border-radius: var(--radius-md);
  font-size: 0.95rem;
  font-weight: 600;
  cursor: pointer;
  transition: opacity var(--transition-fast);
}
.btn-primary:hover:not(:disabled) { opacity: 0.88; }
.btn-primary:disabled { opacity: 0.5; cursor: not-allowed; }
.auth-switch {
  margin-top: 20px;
  text-align: center;
  font-size: 0.875rem;
  color: rgba(255,255,255,0.40);
}
.auth-switch a {
  color: #93C5FD;
  text-decoration: none;
  font-weight: 600;
}
.auth-switch a:hover { text-decoration: underline; }
</style>
