import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { taskApi } from '@/api/projectApi'

// 포모도로 설정 (초)
const WORK_SEC  = 25 * 60
const SHORT_SEC =  5 * 60
const LONG_SEC  = 15 * 60
const SETS_BEFORE_LONG = 4

export const useFocusStore = defineStore('focus', () => {
  const active       = ref(false)
  const myTasks      = ref([])
  const currentTask  = ref(null)

  // 포모도로 상태
  const phase        = ref('work')   // 'work' | 'short' | 'long'
  const remaining    = ref(WORK_SEC)
  const setsDone     = ref(0)
  const todayPomos   = ref(0)
  const running      = ref(false)
  let   _timer       = null

  // 알림 차단 목록 (집중 모드 종료 후 보여줄 것)
  const queuedAlerts = ref([])

  const phaseLabel = computed(() => ({
    work: '집중', short: '짧은 휴식', long: '긴 휴식'
  }[phase.value]))

  const totalSec = computed(() =>
    phase.value === 'work' ? WORK_SEC : phase.value === 'short' ? SHORT_SEC : LONG_SEC
  )

  const progress = computed(() =>
    Math.round((1 - remaining.value / totalSec.value) * 100)
  )

  const timeLabel = computed(() => {
    const m = Math.floor(remaining.value / 60).toString().padStart(2, '0')
    const s = (remaining.value % 60).toString().padStart(2, '0')
    return `${m}:${s}`
  })

  async function enter() {
    active.value  = true
    running.value = false
    phase.value   = 'work'
    remaining.value = WORK_SEC
    try {
      const res = await taskApi.mine()
      myTasks.value = res.data || []
      if (!currentTask.value && myTasks.value.length > 0) {
        currentTask.value = myTasks.value[0]
      }
    } catch {}
  }

  function exit() {
    active.value  = false
    running.value = false
    clearInterval(_timer)
    _timer = null
  }

  function selectTask(task) { currentTask.value = task }

  function startTimer() {
    if (running.value) return
    running.value = true
    _timer = setInterval(() => {
      if (remaining.value > 0) {
        remaining.value--
      } else {
        _onPhaseEnd()
      }
    }, 1000)
  }

  function pauseTimer() {
    running.value = false
    clearInterval(_timer)
    _timer = null
  }

  function resetTimer() {
    pauseTimer()
    remaining.value = totalSec.value
  }

  function _onPhaseEnd() {
    pauseTimer()
    _beep()
    if (phase.value === 'work') {
      setsDone.value++
      todayPomos.value++
      if (setsDone.value >= SETS_BEFORE_LONG) {
        setsDone.value = 0
        phase.value    = 'long'
        remaining.value = LONG_SEC
      } else {
        phase.value    = 'short'
        remaining.value = SHORT_SEC
      }
    } else {
      phase.value    = 'work'
      remaining.value = WORK_SEC
    }
    startTimer()
  }

  function _beep() {
    try {
      const ctx = new (window.AudioContext || window.webkitAudioContext)()
      const osc = ctx.createOscillator()
      const gain = ctx.createGain()
      osc.connect(gain)
      gain.connect(ctx.destination)
      osc.type = 'sine'
      osc.frequency.value = 880
      gain.gain.setValueAtTime(0.3, ctx.currentTime)
      gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + 1)
      osc.start(ctx.currentTime)
      osc.stop(ctx.currentTime + 1)
    } catch {}
  }

  function queueAlert(msg) {
    if (active.value) queuedAlerts.value.push(msg)
  }

  function flushAlerts() {
    const list = [...queuedAlerts.value]
    queuedAlerts.value = []
    return list
  }

  return {
    active, myTasks, currentTask,
    phase, phaseLabel, remaining, timeLabel, totalSec, progress,
    setsDone, todayPomos, running, queuedAlerts,
    enter, exit, selectTask,
    startTimer, pauseTimer, resetTimer, queueAlert, flushAlerts,
  }
})
