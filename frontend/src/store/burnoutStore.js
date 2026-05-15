import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import api from '@/api/axios'
import dayjs from 'dayjs'

export const useBurnoutStore = defineStore('burnout', () => {
  const mySignals   = ref([])   // 개인 위험 신호 목록
  const teamRisks   = ref([])   // 팀원별 위험도
  const serverData  = ref(null) // 서버에서 받은 태스크 기반 데이터
  const loading     = ref(false)

  // 개인 번아웃 위험도
  const myRiskLevel = computed(() => {
    if (mySignals.value.length === 0) return 'SAFE'
    if (mySignals.value.length === 1) return 'CAUTION'
    return 'RISK'
  })

  const teamRiskMembers = computed(() =>
    teamRisks.value.filter(m => m.riskLevel !== 'SAFE')
  )

  // localStorage에서 최근 N일 에너지/블로커 데이터 읽기
  function readLocalHistory(days = 7) {
    const result = []
    for (let i = 0; i < days; i++) {
      const date = dayjs().subtract(i, 'day').format('YYYY-MM-DD')
      let energy = 0, blockerSeverity = 'LOW', taskDoneRate = null

      try {
        const checkin = JSON.parse(localStorage.getItem(`workoop-checkin-${date}`))
        if (checkin) energy = checkin.energy || 0
      } catch {}

      try {
        const scrum = JSON.parse(localStorage.getItem(`workoop-scrum-${date}`))
        if (scrum) {
          blockerSeverity = scrum.blockerSeverity || 'LOW'
          const tasks = scrum.tasks || []
          if (tasks.length > 0) {
            const done = tasks.filter(t => t.done).length
            taskDoneRate = Math.round((done / tasks.length) * 100)
          }
        }
      } catch {}

      result.push({ date, energy, blockerSeverity, taskDoneRate })
    }
    return result
  }

  // 개인 번아웃 신호 분석
  async function analyzeMyBurnout() {
    loading.value = true
    mySignals.value = []

    try {
      const history = readLocalHistory(7)

      // 신호 1: 에너지 3일 연속 2 이하
      const lowEnergyDays = countConsecutive(
        history.filter(d => d.energy > 0).slice(0, 5),
        d => d.energy > 0 && d.energy <= 2
      )
      if (lowEnergyDays >= 3) {
        mySignals.value.push({
          type: 'energy',
          icon: '🔋',
          message: `최근 ${lowEnergyDays}일 연속 에너지 레벨이 낮습니다.`,
          severity: 'high'
        })
      }

      // 신호 2: HIGH 블로커 2일 이상
      const highBlockerDays = history.filter(d => d.blockerSeverity === 'HIGH').length
      if (highBlockerDays >= 2) {
        mySignals.value.push({
          type: 'blocker',
          icon: '🚧',
          message: `최근 ${highBlockerDays}일 HIGH 블로커가 지속되고 있습니다.`,
          severity: 'high'
        })
      }

      // 신호 3: 스크럼 태스크 완료율 3일 연속 40% 미만
      const lowCompletionDays = history
        .filter(d => d.taskDoneRate !== null && d.taskDoneRate < 40).length
      if (lowCompletionDays >= 3) {
        mySignals.value.push({
          type: 'completion',
          icon: '📉',
          message: `최근 ${lowCompletionDays}일 태스크 완료율이 40% 미만입니다.`,
          severity: 'medium'
        })
      }

      // 신호 4: 서버 — 기한 초과 태스크 + 과다 업무량
      const res = await api.get('/burnout/me')
      serverData.value = res.data

      if (res.data.overdueTasks >= 3) {
        mySignals.value.push({
          type: 'overdue',
          icon: '⏰',
          message: `기한이 지난 태스크가 ${res.data.overdueTasks}개 있습니다.`,
          severity: 'high'
        })
      }
      if (res.data.pendingHours >= 30) {
        mySignals.value.push({
          type: 'workload',
          icon: '📦',
          message: `잔여 예상 업무량이 ${Math.round(res.data.pendingHours)}시간입니다.`,
          severity: 'medium'
        })
      }
    } catch {}

    loading.value = false
  }

  // 팀 번아웃 분석
  async function analyzeTeamBurnout(orgId) {
    if (!orgId) return
    try {
      const res = await api.get(`/burnout/team?orgId=${orgId}`)
      teamRisks.value = res.data || []
    } catch {}
  }

  // 연속 일수 계산 (앞에서부터)
  function countConsecutive(arr, predFn) {
    let count = 0
    for (const item of arr) {
      if (predFn(item)) count++
      else break
    }
    return count
  }

  return {
    mySignals, myRiskLevel, teamRisks, teamRiskMembers, serverData, loading,
    analyzeMyBurnout, analyzeTeamBurnout, readLocalHistory,
  }
})
