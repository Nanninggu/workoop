<template>
  <div class="onboarding-page">
    <div class="onboarding-card">
      <div class="step-badge">조직 설정</div>
      <h1 class="ob-title">팀 이름을 알려주세요</h1>
      <p class="ob-sub">조직을 만들고 팀원을 초대해서 함께 KPI를 관리하세요.</p>

      <form @submit.prevent="handleCreate" class="ob-form">
        <div class="form-group">
          <label>조직 이름</label>
          <input
            v-model="orgName"
            type="text"
            placeholder="예: 스타트업 A팀, 마케팅 부서"
            required
            autofocus
          />
        </div>
        <button type="submit" class="btn-primary" :disabled="loading || !orgName.trim()">
          {{ loading ? '생성 중...' : '조직 만들기 →' }}
        </button>
      </form>

      <button class="btn-skip" @click="skipToApp">
        개인으로 먼저 시작하기
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useOrgStore } from '@/store/orgStore'

const router  = useRouter()
const orgStore = useOrgStore()
const orgName  = ref('')
const loading  = ref(false)

async function handleCreate() {
  loading.value = true
  try {
    await orgStore.createOrg(orgName.value.trim())
    router.push('/dashboard')
  } finally {
    loading.value = false
  }
}

function skipToApp() {
  router.push('/dashboard')
}
</script>

<style scoped>
.onboarding-page {
  min-height: 100vh;
  background: #1D1E20;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}
.onboarding-card {
  width: 100%;
  max-width: 480px;
  background: rgba(255,255,255,0.05);
  border-radius: var(--radius-2xl);
  padding: 48px 40px;
  border: 1px solid rgba(255,255,255,0.10);
  box-shadow: 0 24px 48px rgba(0,0,0,0.4);
}
.step-badge {
  display: inline-block;
  background: rgba(48,127,226,0.18);
  color: #93C5FD;
  font-size: 0.75rem;
  font-weight: 700;
  padding: 4px 12px;
  border-radius: 20px;
  margin-bottom: 16px;
  letter-spacing: 0.5px;
  text-transform: uppercase;
}
.ob-title {
  font-size: 1.6rem;
  font-weight: 800;
  color: white;
  margin: 0 0 8px;
}
.ob-sub {
  font-size: 0.9rem;
  color: rgba(255,255,255,0.45);
  margin: 0 0 32px;
  line-height: 1.6;
}
.ob-form {
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
  padding: 12px 16px;
  background: rgba(0,0,0,0.25);
  border: 1px solid rgba(255,255,255,0.12);
  border-radius: var(--radius-md);
  color: white;
  font-size: 0.95rem;
  outline: none;
  transition: border-color var(--transition-fast);
  font-family: inherit;
}
.form-group input:focus { border-color: var(--color-project); }
.form-group input::placeholder { color: rgba(255,255,255,0.25); }
.btn-primary {
  padding: 14px;
  background: var(--color-project);
  color: white;
  border: none;
  border-radius: var(--radius-md);
  font-size: 1rem;
  font-weight: 700;
  cursor: pointer;
  transition: opacity var(--transition-fast);
}
.btn-primary:hover:not(:disabled) { opacity: 0.88; }
.btn-primary:disabled { opacity: 0.5; cursor: not-allowed; }
.btn-skip {
  width: 100%;
  margin-top: 12px;
  padding: 12px;
  background: transparent;
  color: rgba(255,255,255,0.35);
  border: none;
  border-radius: var(--radius-md);
  font-size: 0.875rem;
  cursor: pointer;
  transition: color var(--transition-fast);
}
.btn-skip:hover { color: rgba(255,255,255,0.55); }
</style>
