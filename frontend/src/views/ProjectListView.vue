<template>
  <div class="page-container">
    <!-- 헤더 -->
    <div class="action-bar">
      <div v-if="!orgStore.currentOrg" class="no-org-msg">
        조직에 참여하면 프로젝트를 만들 수 있습니다.
        <router-link to="/settings">설정에서 팀 참여하기 →</router-link>
      </div>
      <template v-else>
        <div class="org-name">{{ orgStore.currentOrg.name }}의 프로젝트</div>
        <button class="btn-create" @click="openCreate">
          <Plus :size="16" /> 새 프로젝트
        </button>
      </template>
    </div>

    <!-- 프로젝트 그리드 -->
    <div v-if="loading" class="project-grid">
      <div v-for="i in 3" :key="i" class="project-card sk-card">
        <div class="sk-title"></div>
        <div class="sk-desc"></div>
        <div class="sk-meta"></div>
      </div>
    </div>

    <div v-else-if="projects.length === 0 && orgStore.currentOrg" class="empty-state">
      <Kanban :size="48" class="empty-icon" />
      <p>아직 프로젝트가 없습니다.</p>
      <button class="btn-create" @click="openCreate">첫 프로젝트 만들기</button>
    </div>

    <div v-else class="project-grid">
      <div
        v-for="p in projects"
        :key="p.id"
        class="project-card"
        @click="$router.push(`/projects/${p.id}/board`)"
      >
        <div class="card-color-bar" :style="{ background: projectColor(p.id) }"></div>
        <div class="card-body">
          <div class="card-title">{{ p.name }}</div>
          <div class="card-desc">{{ p.description || '설명 없음' }}</div>
          <div class="card-meta">
            <span class="meta-tasks">
              <CheckSquare :size="13" /> {{ p.taskCount }}개 태스크
            </span>
            <span class="meta-owner">{{ p.ownerName }}</span>
          </div>
        </div>
        <button class="card-delete" @click.stop="deleteProject(p)" title="삭제">
          <Trash2 :size="14" />
        </button>
      </div>
    </div>

    <!-- 생성 다이얼로그 -->
    <el-dialog v-model="dialogOpen" title="새 프로젝트" width="420px">
      <div class="form-group">
        <label>프로젝트 이름 *</label>
        <input v-model="form.name" class="form-input" placeholder="예: 2분기 마케팅 캠페인" maxlength="100" />
      </div>
      <div class="form-group mt-3">
        <label>설명</label>
        <textarea v-model="form.description" class="form-input" rows="3" placeholder="프로젝트 목표나 범위를 간략히 적어주세요"></textarea>
      </div>
      <template #footer>
        <el-button @click="dialogOpen = false">취소</el-button>
        <el-button type="primary" :disabled="!form.name.trim() || saving" @click="createProject">
          {{ saving ? '생성 중...' : '만들기' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useOrgStore } from '@/store/orgStore'
import { projectApi } from '@/api/projectApi'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Kanban, CheckSquare, Trash2 } from 'lucide-vue-next'

const router   = useRouter()
const orgStore = useOrgStore()

const projects   = ref([])
const loading    = ref(false)
const dialogOpen = ref(false)
const saving     = ref(false)
const form       = ref({ name: '', description: '' })

const COLORS = ['#3B82F6','#10B981','#8B5CF6','#F59E0B','#EF4444','#06B6D4','#F97316']
const projectColor = (id) => COLORS[id % COLORS.length]

async function load() {
  if (!orgStore.currentOrg) return
  loading.value = true
  try {
    const res = await projectApi.list(orgStore.currentOrg.id)
    projects.value = res.data || []
  } finally {
    loading.value = false
  }
}

function openCreate() {
  form.value = { name: '', description: '' }
  dialogOpen.value = true
}

async function createProject() {
  saving.value = true
  try {
    await projectApi.create({
      name: form.value.name.trim(),
      description: form.value.description.trim(),
      orgId: String(orgStore.currentOrg.id)
    })
    ElMessage.success('프로젝트가 생성되었습니다.')
    dialogOpen.value = false
    await load()
  } finally {
    saving.value = false
  }
}

async function deleteProject(p) {
  await ElMessageBox.confirm(`"${p.name}" 프로젝트를 삭제할까요? 태스크도 모두 삭제됩니다.`, '삭제 확인', {
    confirmButtonText: '삭제', cancelButtonText: '취소', type: 'warning'
  })
  await projectApi.delete(p.id)
  ElMessage.success('삭제되었습니다.')
  await load()
}

onMounted(load)
</script>

<style scoped>
.page-container { padding: 24px 28px; }

.action-bar {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 24px;
}
.org-name { font-size: 1rem; font-weight: 700; color: var(--text-primary); }
.no-org-msg { font-size: 0.9rem; color: var(--text-secondary); }
.no-org-msg a { color: var(--color-project); text-decoration: none; font-weight: 600; }

.btn-create {
  display: flex; align-items: center; gap: 6px;
  padding: 9px 16px; background: var(--color-project); color: white;
  border: none; border-radius: var(--radius-md); font-size: 0.875rem;
  font-weight: 600; cursor: pointer; transition: opacity var(--transition-fast);
}
.btn-create:hover { opacity: 0.88; }

.project-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}

.project-card {
  background: var(--bg-card); border: 1px solid var(--border-color); border-radius: var(--radius-lg);
  overflow: hidden; cursor: pointer; position: relative;
  transition: box-shadow var(--transition-base), transform var(--transition-fast);
  display: flex; flex-direction: column; box-shadow: var(--shadow-xs);
}
.project-card:hover { box-shadow: var(--shadow-md); transform: translateY(-2px); }

.card-color-bar { height: 4px; width: 100%; }

.card-body { padding: 16px 18px 14px; flex: 1; }
.card-title { font-size: 1rem; font-weight: 700; color: var(--text-primary); margin-bottom: 6px; }
.card-desc {
  font-size: 0.825rem; color: var(--text-secondary); margin-bottom: 14px;
  display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden;
}
.card-meta { display: flex; align-items: center; justify-content: space-between; }
.meta-tasks { display: flex; align-items: center; gap: 4px; font-size: 0.78rem; color: var(--text-muted); }
.meta-owner { font-size: 0.75rem; color: var(--text-muted); }

.card-delete {
  position: absolute; top: 12px; right: 12px;
  width: 28px; height: 28px; border-radius: var(--radius-sm);
  background: transparent; border: none; color: var(--border-color);
  cursor: pointer; display: flex; align-items: center; justify-content: center;
  opacity: 0; transition: all var(--transition-fast);
}
.project-card:hover .card-delete { opacity: 1; }
.card-delete:hover { background: rgba(239,68,68,0.08); color: var(--color-danger); }

.empty-state {
  text-align: center; padding: 80px 24px; color: var(--text-muted);
}
.empty-icon { margin-bottom: 16px; opacity: 0.4; }
.empty-state p { margin-bottom: 20px; font-size: 0.95rem; }

.form-group { display: flex; flex-direction: column; gap: 6px; }
.form-group label { font-size: 0.8rem; font-weight: 600; color: var(--text-secondary); }
.form-input {
  padding: 9px 12px; border: 1px solid var(--border-color); border-radius: var(--radius-md);
  font-size: 0.9rem; outline: none; transition: border-color var(--transition-fast); width: 100%; box-sizing: border-box;
  background: var(--bg-card); color: var(--text-primary);
}
.form-input:focus { border-color: var(--color-project); }
.mt-3 { margin-top: 12px; }

/* 스켈레톤 */
.sk-card { cursor: default; padding: 16px 18px; }
.sk-card:hover { transform: none; box-shadow: none; }
.sk-title  { height: 20px; width: 60%; background: var(--bg-hover); border-radius: var(--radius-xs); margin-bottom: 8px; }
.sk-desc   { height: 14px; width: 80%; background: var(--bg-hover); border-radius: var(--radius-xs); margin-bottom: 14px; }
.sk-meta   { height: 12px; width: 40%; background: var(--bg-hover); border-radius: var(--radius-xs); }
</style>
