<template>
  <teleport to="body">
    <transition name="notif-slide">
      <div v-if="open" class="notif-overlay" @click.self="$emit('close')">
        <div class="notif-panel">

          <!-- 헤더 -->
          <div class="np-header">
            <div class="np-title">
              <Bell :size="18" />
              알림
              <span v-if="store.unreadCount > 0" class="np-badge">{{ store.unreadCount }}</span>
            </div>
            <div class="np-actions">
              <button
                v-if="store.unreadCount > 0"
                class="btn-mark-all"
                @click="store.markAllRead()"
              >모두 읽음</button>
              <button @click="$emit('close')" class="btn-np-close">
                <X :size="18" />
              </button>
            </div>
          </div>

          <!-- 알림 목록 -->
          <div class="np-list" v-if="store.notifications.length > 0">
            <div
              v-for="n in store.notifications"
              :key="n.id"
              class="notif-item"
              :class="{ unread: !n.readAt }"
              @click="onClickNotif(n)"
            >
              <!-- 타입 아이콘 -->
              <div class="notif-icon" :style="{ background: iconBg(n.type) }">
                {{ store.typeIcon(n.type) }}
              </div>

              <!-- 내용 -->
              <div class="notif-content">
                <div class="notif-title">{{ n.title }}</div>
                <div class="notif-body" v-if="n.body">{{ n.body }}</div>
                <div class="notif-time">{{ timeAgo(n.createdAt) }}</div>
              </div>

              <!-- 읽지 않음 점 -->
              <div v-if="!n.readAt" class="unread-dot"></div>
            </div>
          </div>

          <!-- 빈 상태 -->
          <div v-else class="np-empty">
            <Bell :size="36" class="np-empty-icon" />
            <p>알림이 없습니다</p>
            <span>태스크를 완료하거나 KPI를 달성하면 알림이 표시됩니다.</span>
          </div>

        </div>
      </div>
    </transition>
  </teleport>
</template>

<script setup>
import { useNotificationStore } from '@/store/notificationStore'
import { useRouter } from 'vue-router'
import { Bell, X } from 'lucide-vue-next'
import dayjs from 'dayjs'
import relativeTime from 'dayjs/plugin/relativeTime'
import 'dayjs/locale/ko'

dayjs.extend(relativeTime)
dayjs.locale('ko')

defineProps({ open: Boolean })
defineEmits(['close'])

const store  = useNotificationStore()
const router = useRouter()

const TYPE_BG = {
  TASK_ASSIGNED: '#EFF6FF',
  TASK_DONE:     '#F0FDF4',
  KPI_ACHIEVED:  '#FFFBEB',
  BLOCKER_HIGH:  '#FEF2F2',
}
function iconBg(type) { return TYPE_BG[type] || '#F8FAFC' }

function timeAgo(dt) {
  if (!dt) return ''
  return dayjs(dt).fromNow()
}

async function onClickNotif(n) {
  if (!n.readAt) await store.markRead(n.id)
  if (n.link) router.push(n.link)
}
</script>

<style scoped>
.notif-overlay {
  position: fixed; inset: 0; z-index: 8000;
  background: rgba(17,24,39,0.35);
  display: flex; justify-content: flex-end;
}
.notif-slide-enter-active, .notif-slide-leave-active { transition: transform 0.25s ease; }
.notif-slide-enter-from, .notif-slide-leave-to { transform: translateX(100%); }

.notif-panel {
  width: 380px; max-width: 95vw; height: 100%;
  background: var(--bg-card); display: flex; flex-direction: column;
  box-shadow: var(--shadow-xl);
}

/* 헤더 */
.np-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 18px 20px; border-bottom: 1px solid var(--border-color); flex-shrink: 0;
}
.np-title {
  display: flex; align-items: center; gap: 8px;
  font-size: 1rem; font-weight: 700; color: var(--text-primary);
}
.np-badge {
  background: var(--color-danger); color: white;
  font-size: 0.7rem; font-weight: 700;
  padding: 1px 6px; border-radius: 10px; min-width: 18px; text-align: center;
}
.np-actions { display: flex; align-items: center; gap: 8px; }
.btn-mark-all {
  padding: 5px 10px; background: var(--bg-hover); border: none;
  border-radius: var(--radius-sm); font-size: 0.78rem; color: var(--text-secondary);
  cursor: pointer; font-weight: 600;
}
.btn-mark-all:hover { background: var(--border-color); }
.btn-np-close {
  width: 30px; height: 30px; border-radius: var(--radius-md);
  background: transparent; border: none; cursor: pointer;
  color: var(--text-muted); display: flex; align-items: center; justify-content: center;
}
.btn-np-close:hover { background: var(--bg-hover); color: var(--text-primary); }

/* 목록 */
.np-list { flex: 1; overflow-y: auto; }

.notif-item {
  display: flex; align-items: flex-start; gap: 12px;
  padding: 14px 20px; cursor: pointer;
  border-bottom: 1px solid var(--bg-main); transition: background var(--transition-fast);
  position: relative;
}
.notif-item:hover { background: var(--bg-hover); }
.notif-item.unread { background: rgba(48,127,226,0.03); }

.notif-icon {
  width: 38px; height: 38px; border-radius: var(--radius-md); flex-shrink: 0;
  display: flex; align-items: center; justify-content: center;
  font-size: 1.1rem;
}
.notif-content { flex: 1; min-width: 0; }
.notif-title {
  font-size: 0.875rem; font-weight: 600; color: var(--text-primary);
  margin-bottom: 3px; line-height: 1.4;
}
.notif-body {
  font-size: 0.78rem; color: var(--text-secondary); margin-bottom: 4px;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.notif-time { font-size: 0.72rem; color: var(--text-muted); }

.unread-dot {
  width: 8px; height: 8px; border-radius: 50%;
  background: var(--color-project); flex-shrink: 0; margin-top: 6px;
}

/* 빈 상태 */
.np-empty {
  flex: 1; display: flex; flex-direction: column;
  align-items: center; justify-content: center;
  padding: 48px 24px; color: var(--text-muted); text-align: center; gap: 10px;
}
.np-empty-icon { opacity: 0.2; margin-bottom: 6px; }
.np-empty p { font-size: 0.95rem; font-weight: 600; color: var(--text-secondary); margin: 0; }
.np-empty span { font-size: 0.8rem; line-height: 1.6; }
</style>
