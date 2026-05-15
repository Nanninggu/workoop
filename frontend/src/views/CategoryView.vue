<template>
  <div class="page-container">
    <div class="action-bar">
      <div class="text-sm text-slate-500">{{ categories.length }}개의 카테고리</div>
      <el-button type="primary" @click="openCreate">
        <Plus :size="16" class="mr-1" /> 카테고리 추가
      </el-button>
    </div>

    <div class="category-grid">
      <div
        v-for="cat in categories"
        :key="cat.id"
        class="category-card"
      >
        <div class="cat-color-strip" :style="{ backgroundColor: cat.color }"></div>
        <div class="cat-body">
          <div class="cat-top">
            <div class="cat-icon-wrap" :style="{ background: cat.color + '20', color: cat.color }">
              <component :is="getIconComponent(cat.icon)" :size="22" />
            </div>
            <div class="cat-info">
              <div class="cat-name">{{ cat.name }}</div>
              <div class="cat-desc">{{ cat.description || '설명 없음' }}</div>
            </div>
          </div>
          <div class="cat-actions">
            <el-button size="small" text @click="openEdit(cat)">수정</el-button>
            <el-button size="small" text type="danger" @click="deleteCategory(cat)">삭제</el-button>
          </div>
        </div>
      </div>

      <!-- 추가 카드 -->
      <div class="category-card add-card" @click="openCreate">
        <Plus :size="24" class="text-slate-400" />
        <span class="text-slate-500 text-sm font-medium">카테고리 추가</span>
      </div>
    </div>

    <!-- 카테고리 생성/수정 다이얼로그 -->
    <el-dialog
      v-model="dialogVisible"
      :title="editing ? '카테고리 수정' : '카테고리 추가'"
      width="520px"
      destroy-on-close
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        @submit.prevent="submitForm"
      >
        <el-form-item label="카테고리 이름" prop="name">
          <el-input v-model="form.name" placeholder="예: 건강/운동" maxlength="100" />
        </el-form-item>

        <el-form-item label="색상" prop="color">
          <div class="color-picker-row">
            <el-color-picker v-model="form.color" show-alpha :predefine="presetColors" />
            <div class="color-presets">
              <div
                v-for="c in presetColors"
                :key="c"
                class="preset-dot"
                :style="{ backgroundColor: c, outline: form.color === c ? '3px solid ' + c : 'none', outlineOffset: '2px' }"
                @click="form.color = c"
              ></div>
            </div>
          </div>
        </el-form-item>

        <!-- 아이콘 선택기 -->
        <el-form-item label="아이콘">
          <div class="icon-picker">
            <div
              v-for="iconName in iconOptions"
              :key="iconName"
              class="icon-option"
              :class="{ selected: form.icon === iconName }"
              :style="form.icon === iconName ? { background: form.color + '20', borderColor: form.color, color: form.color } : {}"
              :title="iconName"
              @click="form.icon = iconName"
            >
              <component :is="getIconComponent(iconName)" :size="18" />
            </div>
          </div>
          <div class="icon-selected-label" v-if="form.icon">
            <component :is="getIconComponent(form.icon)" :size="14" style="display:inline-block;vertical-align:middle;margin-right:4px" />
            {{ form.icon }}
          </div>
        </el-form-item>

        <el-form-item label="설명">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="2"
            placeholder="카테고리 설명"
            maxlength="500"
          />
        </el-form-item>

        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">취소</el-button>
          <el-button type="primary" native-type="submit">{{ editing ? '수정' : '생성' }}</el-button>
        </div>
      </el-form>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { categoryApi } from '@/api/categoryApi'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Plus, TrendingUp, TrendingDown, Briefcase, Zap, Users, BookOpen,
  Target, BarChart3, BarChart2, LineChart, PieChart,
  Star, Heart, Flame, Trophy, Medal, Award,
  DollarSign, ShoppingCart, Package, Truck, Building2,
  Code2, GitBranch, Terminal, Database, Globe,
  Brain, Lightbulb, Pencil, FileText, BookMarked,
  Dumbbell, Apple, Moon, Sun, Coffee,
  Mail, MessageSquare, Phone, Calendar, Clock,
  Settings, Shield, Lock, Bell, Search,
  Layers, Layout, Grid, Monitor, Smartphone
} from 'lucide-vue-next'

const iconMap = {
  TrendingUp, TrendingDown, Briefcase, Zap, Users, BookOpen,
  Target, BarChart3, BarChart2, LineChart, PieChart,
  Star, Heart, Flame, Trophy, Medal, Award,
  DollarSign, ShoppingCart, Package, Truck, Building2,
  Code2, GitBranch, Terminal, Database, Globe,
  Brain, Lightbulb, Pencil, FileText, BookMarked,
  Dumbbell, Apple, Moon, Sun, Coffee,
  Mail, MessageSquare, Phone, Calendar, Clock,
  Settings, Shield, Lock, Bell, Search,
  Layers, Layout, Grid, Monitor, Smartphone
}

const iconOptions = Object.keys(iconMap)

function getIconComponent(name) {
  return iconMap[name] || iconMap['BarChart3']
}

const categories = ref([])
const dialogVisible = ref(false)
const editing = ref(null)
const formRef = ref()

const presetColors = [
  '#10B981', '#3B82F6', '#F59E0B', '#8B5CF6',
  '#EC4899', '#EF4444', '#06B6D4', '#F97316'
]

const form = reactive({
  name: '',
  color: '#3B82F6',
  description: '',
  icon: 'BarChart3',
  sortOrder: 0
})

const rules = {
  name: [{ required: true, message: '카테고리 이름을 입력하세요' }],
  color: [{ required: true, message: '색상을 선택하세요' }]
}

async function loadCategories() {
  const res = await categoryApi.getAll()
  categories.value = res.data || []
}

function openCreate() {
  editing.value = null
  Object.assign(form, { name: '', color: '#3B82F6', description: '', icon: 'BarChart3', sortOrder: 0 })
  dialogVisible.value = true
}

function openEdit(cat) {
  editing.value = cat
  Object.assign(form, {
    name: cat.name,
    color: cat.color,
    description: cat.description || '',
    icon: cat.icon || 'BarChart3',
    sortOrder: cat.sortOrder || 0
  })
  dialogVisible.value = true
}

async function submitForm() {
  await formRef.value.validate()
  if (editing.value) {
    await categoryApi.update(editing.value.id, form)
    ElMessage.success('카테고리가 수정되었습니다.')
  } else {
    await categoryApi.create(form)
    ElMessage.success('카테고리가 생성되었습니다.')
  }
  dialogVisible.value = false
  await loadCategories()
}

async function deleteCategory(cat) {
  await ElMessageBox.confirm(
    `"${cat.name}" 카테고리를 삭제하시겠습니까? 연결된 KPI가 없어야 삭제 가능합니다.`,
    '삭제 확인',
    { confirmButtonText: '삭제', cancelButtonText: '취소', type: 'warning' }
  )
  await categoryApi.delete(cat.id)
  ElMessage.success('카테고리가 삭제되었습니다.')
  await loadCategories()
}

onMounted(loadCategories)
</script>

<style scoped>
.page-container { padding: 24px 28px; }

.action-bar {
  display: flex; align-items: center; justify-content: space-between; margin-bottom: 18px;
}

.category-grid {
  display: grid; grid-template-columns: repeat(auto-fill, minmax(250px, 1fr)); gap: 14px;
}

.category-card {
  background: var(--bg-card); border: 1px solid var(--border-color);
  border-radius: var(--radius-lg); overflow: hidden;
  box-shadow: var(--shadow-xs);
  transition: border-color var(--transition-base), box-shadow var(--transition-base);
}
.category-card:hover { border-color: var(--gray-300); box-shadow: var(--shadow-sm); }

.add-card {
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  gap: 8px; padding: 40px; cursor: pointer; border-style: dashed;
  border-color: var(--border-color); transition: all var(--transition-base);
  color: var(--text-muted);
}
.add-card:hover { background: var(--bg-hover); border-color: var(--color-project); color: var(--color-project); }

.cat-color-strip { height: 4px; }
.cat-body { padding: 16px; }
.cat-top { display: flex; align-items: flex-start; gap: 12px; margin-bottom: 12px; }

.cat-icon-wrap {
  width: 40px; height: 40px; border-radius: var(--radius-md);
  display: flex; align-items: center; justify-content: center; flex-shrink: 0;
}

.cat-name { font-size: 15px; font-weight: 600; color: var(--text-primary); margin-bottom: 4px; letter-spacing: -0.01em; }
.cat-desc { font-size: 12px; color: var(--text-muted); line-height: 1.45; }

.cat-actions {
  display: flex; justify-content: flex-end; gap: 4px;
  border-top: 1px solid var(--border-color); padding-top: 10px; margin-top: 10px;
}

.color-picker-row { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.color-presets    { display: flex; gap: 7px; flex-wrap: wrap; }
.preset-dot { width: 22px; height: 22px; border-radius: 50%; cursor: pointer; transition: transform var(--transition-fast); }
.preset-dot:hover { transform: scale(1.2); }

.icon-picker {
  display: grid; grid-template-columns: repeat(10, 1fr); gap: 5px;
  max-height: 180px; overflow-y: auto; padding: 6px;
  border: 1px solid var(--border-color); border-radius: var(--radius-md);
  background: var(--bg-hover);
}
.icon-option {
  width: 34px; height: 34px; border-radius: var(--radius-md);
  display: flex; align-items: center; justify-content: center;
  cursor: pointer; border: 2px solid transparent;
  color: var(--text-secondary); background: var(--bg-card); transition: all var(--transition-fast);
}
.icon-option:hover { background: rgba(48,127,226,0.08); color: var(--color-project); border-color: rgba(48,127,226,0.25); }
.icon-option.selected { font-weight: bold; }

.icon-selected-label { margin-top: 8px; font-size: 12px; color: var(--text-secondary); }

.dialog-footer {
  display: flex; justify-content: flex-end; gap: 10px;
  margin-top: 16px; padding-top: 16px; border-top: 1px solid var(--border-color);
}
</style>
