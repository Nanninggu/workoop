<template>
  <!-- 개인 번아웃 배너 -->
  <div v-if="burnout.mySignals.length > 0 && !dismissed" class="burnout-banner" :class="bannerClass">
    <div class="banner-left">
      <div class="banner-emoji">{{ bannerEmoji }}</div>
      <div class="banner-text">
        <div class="banner-title">{{ bannerTitle }}</div>
        <div class="banner-signals">
          <span v-for="s in burnout.mySignals" :key="s.type" class="signal-chip" :class="s.severity">
            {{ s.icon }} {{ s.message }}
          </span>
        </div>
      </div>
    </div>
    <div class="banner-actions">
      <a href="/review" class="banner-link">회고 작성하기</a>
      <button @click="dismissed = true" class="banner-dismiss">✕</button>
    </div>
  </div>

  <!-- 팀 번아웃 카드 (관리자) -->
  <div v-if="burnout.teamRiskMembers.length > 0" class="team-burnout-card">
    <div class="tbc-header">
      <AlertTriangle :size="16" class="tbc-icon" />
      <span>번아웃 위험 팀원</span>
      <span class="tbc-count">{{ burnout.teamRiskMembers.length }}명</span>
    </div>
    <div class="tbc-list">
      <div v-for="m in burnout.teamRiskMembers" :key="m.USERID || m.userId" class="tbc-member">
        <div class="tbc-avatar">{{ nameInitial(m.USERNAME || m.userName) }}</div>
        <div class="tbc-info">
          <div class="tbc-name">{{ m.USERNAME || m.userName }}</div>
          <div class="tbc-chips">
            <span v-for="sig in (m.signals || [])" :key="sig" class="tbc-chip">{{ sig }}</span>
          </div>
        </div>
        <span class="tbc-risk" :class="(m.riskLevel || '').toLowerCase()">
          {{ riskLabel(m.riskLevel) }}
        </span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useBurnoutStore } from '@/store/burnoutStore'
import { AlertTriangle } from 'lucide-vue-next'

const burnout   = useBurnoutStore()
const dismissed = ref(false)

const bannerClass = computed(() => ({
  risk:    burnout.myRiskLevel === 'RISK',
  caution: burnout.myRiskLevel === 'CAUTION',
}))

const bannerEmoji = computed(() =>
  burnout.myRiskLevel === 'RISK' ? '⚠️' : '🌿'
)

const bannerTitle = computed(() =>
  burnout.myRiskLevel === 'RISK'
    ? '번아웃 위험 신호가 감지됐어요. 오늘은 잠깐 쉬어가세요.'
    : '약간 지친 것 같아요. 조금 여유를 가져보세요 🍃'
)

function nameInitial(name) {
  return (name || '?').charAt(0).toUpperCase()
}

function riskLabel(level) {
  return level === 'RISK' ? '위험' : level === 'CAUTION' ? '주의' : '정상'
}
</script>

<style scoped>
/* 개인 배너 */
.burnout-banner {
  display: flex; align-items: flex-start; justify-content: space-between;
  padding: 16px 20px; border-radius: var(--radius-lg); margin-bottom: 20px;
  border: 1px solid; gap: 16px; flex-wrap: wrap;
}
.burnout-banner.risk {
  background: rgba(239,68,68,0.06); border-color: rgba(239,68,68,0.25);
}
.burnout-banner.caution {
  background: rgba(245,158,11,0.06); border-color: rgba(245,158,11,0.25);
}
.banner-left { display: flex; align-items: flex-start; gap: 12px; flex: 1; }
.banner-emoji { font-size: 1.6rem; line-height: 1; flex-shrink: 0; }
.banner-title { font-size: 0.9rem; font-weight: 700; color: var(--text-primary); margin-bottom: 8px; }
.banner-signals { display: flex; flex-wrap: wrap; gap: 6px; }
.signal-chip {
  padding: 3px 10px; border-radius: 20px; font-size: 0.75rem; font-weight: 500;
}
.signal-chip.high   { background: rgba(220,38,38,0.12); color: var(--color-danger); }
.signal-chip.medium { background: rgba(217,119,6,0.12); color: #D97706; }

.banner-actions { display: flex; align-items: center; gap: 8px; flex-shrink: 0; }
.banner-link {
  padding: 7px 14px; background: var(--text-primary); color: var(--bg-main);
  border-radius: var(--radius-md); font-size: 0.8rem; font-weight: 600;
  text-decoration: none; white-space: nowrap;
}
.banner-link:hover { opacity: 0.85; }
.banner-dismiss {
  width: 28px; height: 28px; border-radius: 50%; background: transparent;
  border: 1px solid var(--border-color); color: var(--text-muted); cursor: pointer; font-size: 0.75rem;
}
.banner-dismiss:hover { background: var(--bg-hover); }

/* 팀 번아웃 카드 */
.team-burnout-card {
  background: var(--bg-card); border: 1px solid rgba(245,158,11,0.3);
  border-radius: var(--radius-lg); overflow: hidden; margin-bottom: 20px; box-shadow: var(--shadow-xs);
}
.tbc-header {
  display: flex; align-items: center; gap: 8px;
  padding: 12px 16px; background: rgba(245,158,11,0.06);
  border-bottom: 1px solid rgba(245,158,11,0.2);
  font-size: 0.875rem; font-weight: 700; color: var(--color-warning);
}
.tbc-icon { color: var(--color-warning); }
.tbc-count {
  margin-left: auto; background: var(--color-warning); color: white;
  font-size: 0.72rem; font-weight: 700;
  padding: 1px 8px; border-radius: 10px;
}

.tbc-list { display: flex; flex-direction: column; }
.tbc-member {
  display: flex; align-items: center; gap: 12px;
  padding: 12px 16px; border-bottom: 1px solid var(--border-color);
}
.tbc-member:last-child { border-bottom: none; }
.tbc-avatar {
  width: 34px; height: 34px; border-radius: 50%; flex-shrink: 0;
  background: linear-gradient(135deg, var(--color-warning), var(--color-danger));
  color: white; font-size: 0.875rem; font-weight: 700;
  display: flex; align-items: center; justify-content: center;
}
.tbc-info { flex: 1; }
.tbc-name { font-size: 0.875rem; font-weight: 600; color: var(--text-primary); margin-bottom: 4px; }
.tbc-chips { display: flex; flex-wrap: wrap; gap: 4px; }
.tbc-chip {
  padding: 2px 8px; background: rgba(245,158,11,0.10); color: var(--color-warning);
  border-radius: 10px; font-size: 0.7rem; font-weight: 500;
}
.tbc-risk {
  padding: 3px 10px; border-radius: 10px;
  font-size: 0.75rem; font-weight: 700; flex-shrink: 0;
}
.tbc-risk.risk    { background: rgba(220,38,38,0.12); color: var(--color-danger); }
.tbc-risk.caution { background: rgba(217,119,6,0.12); color: #D97706; }
.tbc-risk.safe    { background: rgba(22,163,74,0.12); color: #16A34A; }
</style>
