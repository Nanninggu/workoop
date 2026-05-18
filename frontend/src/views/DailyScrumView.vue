<template>
  <div class="scrum-page">

    <!-- ── 내 스크럼 / 팀 스크럼 탭 ── -->
    <div class="scrum-mode-tabs">
      <button class="smt-btn" :class="{ active: scrumTab === 'my' }" @click="scrumTab = 'my'">
        <ClipboardList :size="15" /> 내 스크럼
      </button>
      <button class="smt-btn" :class="{ active: scrumTab === 'team' }" @click="scrumTab = 'team'">
        <Users :size="15" /> 팀 스크럼
        <span v-if="orgStore.currentOrg" class="smt-org">{{ orgStore.currentOrg.name }}</span>
      </button>
    </div>

    <!-- 팀 스크럼 패널 -->
    <div v-if="scrumTab === 'team'" class="team-scrum-wrap">
      <TeamScrumPanel />
    </div>

    <!-- 내 스크럼 (기존 전체 UI) -->
    <template v-if="scrumTab === 'my'">

    <!-- ── 날짜 네비게이션 바 ── -->
    <div class="date-nav-bar">
      <button class="date-nav-btn" @click="goPrev" title="이전 날">
        <ChevronLeft :size="16" />
      </button>
      <div class="date-nav-center">
        <CalendarDays :size="14" class="date-nav-icon" />
        <span class="date-nav-label" :class="{ 'is-today': isViewingToday }">
          {{ isViewingToday ? '오늘' : todayFullDate }}
        </span>
        <span v-if="!isViewingToday" class="date-nav-sub">{{ todayWeekday }}</span>
      </div>
      <button class="date-nav-btn" @click="goNext" :disabled="viewDate >= maxFutureDate" title="다음 날">
        <ChevronRight :size="16" />
      </button>
      <button v-if="!isViewingToday" class="date-today-btn" @click="goToToday" title="오늘로 이동">
        오늘로
      </button>
      <button class="sync-btn" @click="syncSlack" :disabled="syncing" title="Slack 메시지 자동 입력">
        <RefreshCw :size="14" :class="{ spinning: syncing }" />
        <span class="sync-btn-label">{{ syncing ? '동기화 중...' : 'Slack 동기화' }}</span>
      </button>
      <button class="export-btn" @click="showExportModal = true" title="CSV 내보내기">
        <Download :size="14" />
        <span class="export-btn-label">내보내기</span>
      </button>
    </div>

    <!-- ── CSV 내보내기 모달 ── -->
    <Transition name="modal-fade">
      <div v-if="showExportModal" class="modal-overlay" @click.self="showExportModal = false">
        <div class="export-modal">
          <div class="modal-header">
            <div class="modal-title">
              <Download :size="16" class="modal-title-icon" />
              데일리 스크럼 CSV 내보내기
            </div>
            <button class="modal-close" @click="showExportModal = false"><X :size="16" /></button>
          </div>

          <div class="modal-body">
            <div class="export-presets">
              <div class="preset-label">빠른 선택</div>
              <div class="preset-btns">
                <button class="preset-btn" @click="applyPreset('today')">오늘</button>
                <button class="preset-btn" @click="applyPreset('week')">최근 7일</button>
                <button class="preset-btn" @click="applyPreset('month')">최근 30일</button>
                <button class="preset-btn" @click="applyPreset('all')">전체 기간</button>
              </div>
            </div>

            <div class="export-range">
              <div class="range-label">날짜 범위 직접 지정</div>
              <div class="range-inputs">
                <div class="range-field">
                  <label>시작일</label>
                  <input type="date" v-model="exportFrom" :max="exportTo" />
                </div>
                <span class="range-sep">~</span>
                <div class="range-field">
                  <label>종료일</label>
                  <input type="date" v-model="exportTo" :min="exportFrom" :max="realToday" />
                </div>
              </div>
            </div>

            <div class="export-info">
              <FileText :size="13" class="info-icon" />
              <span>태스크별 1행 · BOM UTF-8 (Excel 한글 호환)</span>
            </div>
          </div>

          <div class="modal-footer">
            <button class="modal-cancel-btn" @click="showExportModal = false">취소</button>
            <button class="modal-export-btn" @click="runExport" :disabled="!exportFrom || !exportTo">
              <Download :size="14" />
              CSV 다운로드
            </button>
          </div>
        </div>
      </div>
    </Transition>

    <!-- ── 헤더: 날짜 + 인사 + 에너지 체크인 ── -->
    <div class="scrum-header">
      <div class="header-left">
        <div class="header-date-group">
          <span class="header-weekday">{{ isViewingToday ? todayWeekday : todayFullDate }}</span>
          <span class="header-full-date">{{ isViewingToday ? todayFullDate : todayWeekday }}</span>
        </div>
        <p class="header-greeting">{{ greeting }}</p>
      </div>
      <div class="header-center">
        <div class="focus-goal-wrap">
          <Target :size="14" class="focus-icon" />
          <input
            v-model="checkIn.focus"
            class="focus-input"
            placeholder="오늘의 집중 목표..."
            @input="saveCheckIn"
            maxlength="60"
          />
        </div>
      </div>
      <div class="header-right">
        <!-- 에너지 레벨 -->
        <div class="energy-section">
          <span class="energy-label">에너지</span>
          <div class="energy-btns">
            <button
              v-for="n in 5"
              :key="n"
              class="energy-btn"
              :class="{ active: checkIn.energy >= n }"
              @click="setEnergy(n)"
              :title="`에너지 레벨 ${n}`"
            >
              <Zap :size="13" />
            </button>
          </div>
        </div>
        <!-- 7일 미니 히트맵 (클릭으로 날짜 이동) -->
        <div class="week-heatmap">
          <span class="week-label">7일</span>
          <div class="heatmap-cols">
            <div
              v-for="day in weekHistory"
              :key="day.date"
              class="heatmap-col"
              :class="{ 'heatmap-col-selected': day.isSelected }"
              :title="`${day.dateLabel}: ${day.rate}% — 클릭으로 이동`"
              style="cursor:pointer"
              @click="viewDate = day.date"
            >
              <div
                class="heatmap-bar"
                :class="day.levelClass"
                :style="{ height: Math.max(day.rate, 6) + '%' }"
              ></div>
              <span class="heatmap-label">{{ day.dayLabel }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- ── 일일 KPI 체크 섹션 ── -->
    <div v-if="isEditable && dailyKpis.length" class="daily-kpi-bar">
      <div class="dkpi-header">
        <Zap :size="13" class="dkpi-header-icon" />
        <span class="dkpi-title">일일 KPI 체크</span>
        <span class="dkpi-progress">{{ dailyKpiDoneCount }}/{{ dailyKpis.length }}</span>
        <div class="dkpi-mini-bar">
          <div
            class="dkpi-mini-fill"
            :style="{ width: (dailyKpis.length ? Math.round(dailyKpiDoneCount / dailyKpis.length * 100) : 0) + '%' }"
          ></div>
        </div>
      </div>
      <div class="dkpi-list">
        <div v-for="kpi in dailyKpis" :key="kpi.id" class="dkpi-item">
          <!-- BOOLEAN 타입 -->
          <template v-if="kpi.kpiType === 'BOOLEAN'">
            <button
              class="dkpi-bool-btn"
              :class="{ done: dailyKpiRecords[kpi.id]?.booleanValue }"
              :disabled="dailyKpiSaving[kpi.id]"
              @click="toggleDailyBoolean(kpi)"
            >
              <Check :size="12" v-if="dailyKpiRecords[kpi.id]?.booleanValue" />
            </button>
          </template>
          <!-- NUMERIC / PERCENTAGE 타입 -->
          <template v-else>
            <input
              type="number"
              class="dkpi-num-input"
              :value="dailyKpiRecords[kpi.id]?.actualValue ?? ''"
              :max="kpi.kpiType === 'PERCENTAGE' ? 100 : undefined"
              min="0"
              placeholder="0"
              @change="onDailyNumChange(kpi, $event)"
            />
          </template>
          <span class="dkpi-name">{{ kpi.name }}</span>
          <span v-if="kpi.unit" class="dkpi-unit">{{ kpi.unit }}</span>
          <!-- 달성률 -->
          <template v-if="kpi.kpiType !== 'BOOLEAN' && kpi.targetValue && dailyKpiRecords[kpi.id]?.actualValue != null">
            <div class="dkpi-rate-bar">
              <div class="dkpi-rate-fill"
                :style="{
                  width: Math.min(dailyKpiRecords[kpi.id].actualValue / kpi.targetValue * 100, 100) + '%',
                  background: dailyKpiRecords[kpi.id].actualValue / kpi.targetValue >= 1 ? '#10b981' : '#3b82f6'
                }"
              ></div>
            </div>
            <span class="dkpi-rate-pct">
              {{ Math.min(Math.round(dailyKpiRecords[kpi.id].actualValue / kpi.targetValue * 100), 100) }}%
            </span>
          </template>
        </div>
      </div>
    </div>

    <!-- ── KPI 스트립 ── -->
    <div class="kpi-strip" v-if="todayKpis.length">
      <span class="kpi-strip-label">오늘 KPI</span>
      <router-link
        v-for="kpi in todayKpis"
        :key="kpi.kpiId"
        :to="`/kpis/${kpi.kpiId}`"
        class="kpi-chip"
      >
        <span class="kpi-dot" :style="{ background: kpi.categoryColor }"></span>
        <span class="kpi-name">{{ kpi.kpiName }}</span>
        <span class="kpi-rate" :class="rateClass(kpi.achievementRate)">
          {{ Math.round(kpi.achievementRate) }}%
        </span>
      </router-link>
    </div>

    <!-- ── 과거 보기 안내 배너 ── -->
    <div v-if="isPastDate" class="readonly-banner">
      <CalendarDays :size="14" />
      <span>{{ todayFullDate }} 기록을 보고 있습니다 — 읽기 전용</span>
    </div>

    <!-- ── 스마트 이월 배너 ── -->
    <div v-if="isViewingToday && showCarryBanner && pendingCarryCount > 0" class="carry-banner">
      <ArrowRightCircle :size="15" />
      <span>어제 미완료 태스크 <strong>{{ pendingCarryCount }}개</strong>가 남아 있습니다.</span>
      <button class="carry-all-btn" @click="carryAllOver">전체 이월</button>
      <button class="carry-dismiss-btn" @click="showCarryBanner = false">
        <X :size="12" />
      </button>
    </div>

    <!-- ── 3컬럼 ── -->
    <div class="scrum-columns">

      <!-- ─── 어제 한 일 ─── -->
      <div class="scrum-col col-yesterday">
        <div class="col-header">
          <div class="col-header-left">
            <ClipboardList :size="16" class="col-icon" />
            <span class="col-title">{{ isViewingToday ? '어제 한 일' : isFutureDate ? '이전날' : '이전날' }}</span>
            <span class="col-badge">{{ yesterdayDate }}</span>
          </div>
          <span class="col-count">{{ doneCount(yesterdayTasks) }}/{{ yesterdayTasks.length }}</span>
        </div>

        <div
          class="task-list drag-zone"
          @dragover.prevent="dragOverZone($event, yesterdayTasks)"
          @drop.prevent="dropTask($event, yesterdayTasks, 'yesterday')"
        >
          <div
            v-for="(task, idx) in yesterdayTasks"
            :key="task.id"
            class="task-card"
            :class="{ done: task.done, dragging: draggingId === task.id }"
            draggable="true"
            :data-idx="idx"
            @dragstart="dragStart($event, task, idx, 'yesterday')"
            @dragend="dragEnd"
          >
            <div class="task-top">
              <div class="drag-handle" title="오늘로 드래그해서 이동">
                <GripVertical :size="12" />
              </div>
              <div class="task-check" :class="{ checked: task.done }" @click="toggleYesterday(task)">
                <Check v-if="task.done" :size="11" />
              </div>
              <span class="task-title">{{ task.title }}</span>
            </div>
            <div class="task-footer">
              <div class="task-tags">
                <span v-if="task.kpiLinked" class="tag tag-kpi">KPI</span>
                <span v-if="task.priority" class="tag" :class="`tag-${task.priority.toLowerCase()}`">
                  {{ task.priority }}
                </span>
                <span v-if="!task.done" class="tag tag-carry">미완료</span>
              </div>
              <div class="yesterday-actions" v-if="isEditable">
                <span v-if="carriedIds.has(task.id)" class="carried-label">
                  <Check :size="10" /> 이월됨
                </span>
                <button
                  v-else-if="!task.done"
                  class="carry-btn"
                  @click="carryOver(task)"
                >
                  이월 →
                </button>
              </div>
            </div>
          </div>
        </div>

        <div v-if="!yesterdayTasks.length" class="col-empty">어제 기록이 없습니다.</div>

        <div v-if="yesterdayTasks.length" class="progress-card">
          <div class="progress-header">
            <span>달성률</span>
            <span class="progress-value" :style="{ color: rateColor(yesterdayRate) }">
              {{ yesterdayRate }}%
            </span>
          </div>
          <div class="progress-bar">
            <div class="progress-fill" :style="{ width: yesterdayRate + '%', background: rateColor(yesterdayRate) }"></div>
          </div>
        </div>
      </div>

      <!-- ─── 오늘 할 일 ─── -->
      <div class="scrum-col col-today">
        <div class="col-header">
          <div class="col-header-left">
            <Zap :size="16" class="col-icon" />
            <span class="col-title">{{ isViewingToday ? '오늘 할 일' : isFutureDate ? '해당일 계획' : '해당일 기록' }}</span>
            <span class="col-badge today-badge">{{ isViewingToday ? 'TODAY' : dayjs(viewDate).format('M/D') }}</span>
          </div>
          <span class="col-count">{{ doneCount(todayTasks) }}/{{ todayTasks.length }}</span>
        </div>

        <!-- 드래그 가능한 태스크 목록 -->
        <div
          class="task-list drag-zone"
          :class="{ 'drop-target': draggingId && draggingCol !== 'today' }"
          @dragover.prevent="dragOverZone($event, todayTasks)"
          @drop.prevent="dropTask($event, todayTasks, 'today')"
        >
          <div
            v-for="(task, idx) in todayTasks"
            :key="task.id"
            class="task-card"
            :class="{
              done: task.done,
              'carried-over': task.carried,
              dragging: draggingId === task.id,
              expanded: expandedId === task.id
            }"
            draggable="true"
            :data-idx="idx"
            @dragstart="dragStart($event, task, idx, 'today')"
            @dragend="dragEnd"
          >
            <div class="task-top">
              <div class="drag-handle" title="드래그로 순서 변경">
                <GripVertical :size="12" />
              </div>
              <div class="task-check" :class="{ checked: task.done }" @click="toggleToday(task)">
                <Check v-if="task.done" :size="11" />
              </div>
              <input
                v-if="editingId === task.id"
                class="task-edit-input"
                v-model="task.title"
                @blur="finishEdit(task)"
                @keyup.enter="finishEdit(task)"
                @keydown.escape="finishEdit(task)"
                :ref="el => { if (el) editInputRef = el }"
              />
              <span v-else class="task-title" @click.stop="startEdit(task)" title="클릭으로 편집">{{ task.title }}</span>
              <div class="task-actions">
                <button
                  class="task-action-btn"
                  @click="toggleExpand(task)"
                  :title="expandedId === task.id ? '접기' : '메모/시간 추가'"
                >
                  <ChevronDown :size="12" :class="{ 'icon-rotate': expandedId === task.id }" />
                </button>
                <button v-if="isEditable" class="task-delete" @click="deleteToday(task.id)" title="삭제">
                  <X :size="12" />
                </button>
              </div>
            </div>

            <!-- 확장 패널: 메모 + 시간 추정 -->
            <div v-if="expandedId === task.id" class="task-expand">
              <textarea
                class="task-note-input"
                v-model="task.note"
                placeholder="메모나 컨텍스트를 적어두세요..."
                rows="3"
                @input="saveTodayData"
                @keydown.enter.stop
              ></textarea>
              <div class="expand-footer">
                <Clock :size="11" />
                <span class="expand-label">예상 시간</span>
                <input
                  type="number"
                  class="time-est-input"
                  v-model.number="task.estimatedHours"
                  min="0.5" max="16" step="0.5"
                  placeholder="0"
                  @input="saveTodayData"
                />
                <span class="expand-label">시간</span>
              </div>
            </div>

            <div class="task-footer">
              <div class="task-tags">
                <span v-if="task.kpiLinked" class="tag tag-kpi">KPI</span>
                <span v-if="task.carried" class="tag tag-carry">이월</span>
                <span
                  class="tag tag-priority"
                  :class="task.priority ? `tag-${task.priority.toLowerCase()}` : 'tag-none'"
                  @click="cyclePriority(task)"
                  title="클릭으로 우선순위 변경"
                >{{ task.priority || '우선순위' }}</span>
                <span v-if="task.note" class="tag tag-note">
                  <FileText :size="9" />&nbsp;메모
                </span>
              </div>
              <span v-if="task.estimatedHours" class="task-time">
                <Clock :size="10" />&nbsp;{{ task.estimatedHours }}h
              </span>
            </div>
          </div>
        </div>

        <!-- 블로커 / 이슈 -->
        <div class="blocker-area" :class="`blocker-${blockerSeverity.toLowerCase()}`">
          <div class="blocker-header">
            <AlertTriangle :size="13" />
            <span>블로커 / 이슈</span>
            <div class="blocker-severity">
              <button
                v-for="s in ['LOW', 'MED', 'HIGH']"
                :key="s"
                class="severity-btn"
                :class="{ active: blockerSeverity === s, [`sev-${s.toLowerCase()}`]: true }"
                @click="blockerSeverity = s; saveTodayData()"
              >{{ s }}</button>
            </div>
          </div>
          <textarea
            v-model="todayBlocker"
            class="blocker-input"
            placeholder="방해 요소나 해결이 필요한 이슈를 입력하세요..."
            rows="3"
            @input="saveTodayData"
            @keydown.enter.stop
          ></textarea>
        </div>

        <!-- 오늘 진행률 -->
        <div v-if="todayTasks.length" class="progress-card today-progress">
          <div class="progress-header">
            <span>오늘 진행률</span>
            <span class="progress-value" style="color: #3b82f6">{{ todayRate }}%</span>
          </div>
          <div class="progress-bar">
            <div class="progress-fill today-fill" :style="{ width: todayRate + '%' }"></div>
          </div>
          <div class="progress-summary">
            <span>완료 {{ doneCount(todayTasks) }}개</span>
            <span>·</span>
            <span>예상 {{ totalEstHours }}h</span>
          </div>
        </div>

        <!-- 태스크 추가 (오늘만) -->
        <div v-if="isEditable && !addingToday" class="add-task-btn" @click="startAddToday">
          <Plus :size="14" />
          태스크 추가...
          <kbd class="shortcut-hint">N</kbd>
        </div>
        <div v-else-if="isEditable" class="add-task-form" @focusout="handleTodayFormBlur">
          <input
            ref="addInputRef"
            v-model="newTodayTitle"
            class="add-input"
            placeholder="할 일을 입력하고 Enter"
            @keyup.enter="submitTodayTask"
            @keydown.escape="cancelAddToday"
          />
          <div class="add-form-opts">
            <button
              v-for="p in ['P1','P2','P3']"
              :key="p"
              class="priority-btn"
              :class="{ active: newTodayPriority === p, [`priority-btn-${p.toLowerCase()}`]: newTodayPriority === p }"
              @mousedown.prevent
              @click="newTodayPriority = newTodayPriority === p ? '' : p"
            >{{ p }}</button>
            <div class="time-est-inline">
              <Clock :size="11" class="time-est-icon" />
              <input
                type="number"
                class="time-est-input small"
                v-model.number="newTodayHours"
                min="0.5" max="16" step="0.5"
                placeholder="시간"
              />
            </div>
            <label class="kpi-check-label" @mousedown.prevent>
              <input type="checkbox" v-model="newTodayKpiLinked" />
              KPI
            </label>
          </div>
          <div class="add-form-actions">
            <button class="add-submit-btn" @mousedown.prevent @click="submitTodayTask">추가</button>
            <button class="add-cancel-btn" @mousedown.prevent @click="cancelAddToday">취소</button>
          </div>
        </div>
      </div>

      <!-- ─── 내일 계획 ─── -->
      <div class="scrum-col col-tomorrow">
        <div class="col-header">
          <div class="col-header-left">
            <CalendarPlus :size="16" class="col-icon" />
            <span class="col-title">{{ isViewingToday ? '내일 계획' : '다음날' }}</span>
            <span class="col-badge">{{ tomorrowDate }}</span>
          </div>
          <span class="col-count">{{ tomorrowTasks.length }}개</span>
        </div>

        <div
          class="task-list drag-zone"
          :class="{ 'drop-target': draggingId && draggingCol !== 'tomorrow' }"
          @dragover.prevent="dragOverZone($event, tomorrowTasks)"
          @drop.prevent="dropTask($event, tomorrowTasks, 'tomorrow')"
        >
          <div
            v-for="(task, idx) in tomorrowTasks"
            :key="task.id"
            class="task-card task-card-tomorrow"
            :class="{ dragging: draggingId === task.id }"
            draggable="true"
            :data-idx="idx"
            @dragstart="dragStart($event, task, idx, 'tomorrow')"
            @dragend="dragEnd"
          >
            <div class="task-top">
              <div class="drag-handle"><GripVertical :size="12" /></div>
              <div class="task-check task-check-tomorrow"></div>
              <input
                v-if="editingId === task.id"
                class="task-edit-input"
                v-model="task.title"
                @blur="finishEditTomorrow(task)"
                @keyup.enter="finishEditTomorrow(task)"
                @keydown.escape="finishEditTomorrow(task)"
                :ref="el => { if (el) editInputRef = el }"
              />
              <span v-else class="task-title" @click.stop="startEdit(task)" title="클릭으로 편집">{{ task.title }}</span>
              <button v-if="isEditable" class="task-delete" @click="deleteTomorrow(task.id)" title="삭제">
                <X :size="12" />
              </button>
            </div>
            <div class="task-footer">
              <div class="task-tags">
                <span v-if="task.kpiLinked" class="tag tag-kpi">KPI</span>
                <span
                  class="tag tag-priority"
                  :class="task.priority ? `tag-${task.priority.toLowerCase()}` : 'tag-none'"
                  @click="cycleTomorrowPriority(task)"
                  title="클릭으로 우선순위 변경"
                >{{ task.priority || '우선순위' }}</span>
              </div>
              <span v-if="task.estimatedHours" class="task-time">
                <Clock :size="10" />&nbsp;{{ task.estimatedHours }}h
              </span>
            </div>
          </div>
        </div>

        <div v-if="!tomorrowTasks.length" class="col-empty">내일 계획을 미리 작성해보세요.</div>

        <!-- 내일 태스크 추가 (오늘만) -->
        <div v-if="isEditable && !addingTomorrow" class="add-task-btn" @click="startAddTomorrow">
          <Plus :size="14" />
          내일 할 일 추가...
        </div>
        <div v-else-if="isEditable" class="add-task-form" @focusout="handleTomorrowFormBlur">
          <input
            ref="addTomorrowRef"
            v-model="newTomorrowTitle"
            class="add-input"
            placeholder="내일 할 일을 입력하고 Enter"
            @keyup.enter="submitTomorrowTask"
            @keydown.escape="cancelAddTomorrow"
          />
          <div class="add-form-opts">
            <button
              v-for="p in ['P1','P2','P3']"
              :key="p"
              class="priority-btn"
              :class="{ active: newTomorrowPriority === p, [`priority-btn-${p.toLowerCase()}`]: newTomorrowPriority === p }"
              @mousedown.prevent
              @click="newTomorrowPriority = newTomorrowPriority === p ? '' : p"
            >{{ p }}</button>
            <div class="time-est-inline">
              <Clock :size="11" class="time-est-icon" />
              <input
                type="number"
                class="time-est-input small"
                v-model.number="newTomorrowHours"
                min="0.5" max="16" step="0.5"
                placeholder="시간"
              />
            </div>
            <label class="kpi-check-label" @mousedown.prevent>
              <input type="checkbox" v-model="newTomorrowKpiLinked" />
              KPI
            </label>
          </div>
          <div class="add-form-actions">
            <button class="add-submit-btn" @mousedown.prevent @click="submitTomorrowTask">추가</button>
            <button class="add-cancel-btn" @mousedown.prevent @click="cancelAddTomorrow">취소</button>
          </div>
        </div>

        <!-- 내일 KPI 예보 -->
        <div v-if="tomorrowKpis.length" class="forecast-card">
          <div class="forecast-title">내일 KPI 예보</div>
          <div v-for="kpi in tomorrowKpis.slice(0,3)" :key="kpi.kpiId" class="forecast-row">
            <span class="forecast-name">{{ kpi.kpiName }}</span>
            <div class="forecast-bar-wrap">
              <div class="forecast-bar">
                <div class="forecast-fill" :style="{ width: Math.min(kpi.achievementRate, 100) + '%' }"></div>
              </div>
              <span class="forecast-rate">{{ Math.round(kpi.achievementRate) }}%</span>
            </div>
          </div>
        </div>

        <!-- 주간 달성 요약 -->
        <div class="weekly-summary-card">
          <div class="weekly-summary-title">
            <BarChart2 :size="13" />
            이번 주 달성 현황
          </div>
          <div class="weekly-bars">
            <div v-for="day in weekHistory" :key="day.date" class="weekly-day-col">
              <div class="weekly-bar-track">
                <div
                  class="weekly-bar-fill"
                  :class="day.levelClass"
                  :style="{ height: Math.max(day.rate, 4) + '%' }"
                  :title="`${day.dateLabel}: ${day.rate}%`"
                ></div>
              </div>
              <span class="weekly-day-label" :class="{ today: day.date === today }">{{ day.dayLabel }}</span>
            </div>
          </div>
        </div>
      </div>

    </div>

    <!-- ── 저장 토스트 ── -->
    <Transition name="toast">
      <div v-if="savedToast" class="saved-toast">
        <Check :size="13" />
        저장됨
      </div>
    </Transition>

    </template> <!-- end 내 스크럼 -->

  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useRoute } from 'vue-router'
import {
  Check, X, Plus, AlertTriangle, ClipboardList, Zap, CalendarPlus,
  GripVertical, ChevronDown, Clock, FileText, Target, ArrowRightCircle,
  BarChart2, ChevronLeft, ChevronRight, CalendarDays, Download, Users, RefreshCw
} from 'lucide-vue-next'
import { useKpiStore } from '@/store/kpiStore'
import { useOrgStore } from '@/store/orgStore'
import { recordApi } from '@/api/kpiApi'
import { scrumApi, syncApi } from '@/api/scrumApi'
import TeamScrumPanel from '@/components/TeamScrumPanel.vue'
import dayjs from 'dayjs'
import 'dayjs/locale/ko'

dayjs.locale('ko')

const route    = useRoute()
const orgStore = useOrgStore()

// 내 스크럼 / 팀 스크럼 탭
const scrumTab = ref('my')  // 'my' | 'team'

// 서버 동기화 (디바운스: 저장 후 1.5초 뒤 서버에 반영)
let _syncTimer = null
function scheduleSyncToServer() {
  clearTimeout(_syncTimer)
  _syncTimer = setTimeout(() => syncDateToServer(
    today.value, todayTasks.value,
    todayBlocker.value, blockerSeverity.value,
    checkIn.value.energy, checkIn.value.focus
  ), 1500)
}

async function syncDateToServer(date, tasks, blocker, blockerSeverity, energy, focus) {
  if (!orgStore.currentOrg) return
  try {
    await scrumApi.save({
      date, orgId: orgStore.currentOrg.id,
      tasksJson:       JSON.stringify(tasks),
      blocker:         blocker         || '',
      blockerSeverity: blockerSeverity || 'LOW',
      energy:          energy          || 0,
      focus:           focus           || '',
    })
  } catch {
    // 서버 동기화 실패해도 localStorage는 유지됨
  }
}
const store = useKpiStore()

// ── 날짜 ──
const realToday = dayjs().format('YYYY-MM-DD')
const viewDate  = ref(realToday)

const isViewingToday = computed(() => viewDate.value === realToday)
const isPastDate     = computed(() => viewDate.value < realToday)
const isFutureDate   = computed(() => viewDate.value > realToday)
const isEditable     = computed(() => !isPastDate.value)   // 오늘·미래 모두 편집 가능
const maxFutureDate  = dayjs().add(30, 'day').format('YYYY-MM-DD')

const today     = computed(() => viewDate.value)
const yesterday = computed(() => dayjs(viewDate.value).subtract(1, 'day').format('YYYY-MM-DD'))
const tomorrow  = computed(() => dayjs(viewDate.value).add(1, 'day').format('YYYY-MM-DD'))

const todayWeekday  = computed(() => dayjs(viewDate.value).format('dddd'))
const todayFullDate = computed(() => dayjs(viewDate.value).format('YYYY년 M월 D일'))
const yesterdayDate = computed(() => dayjs(viewDate.value).subtract(1, 'day').format('M/D (ddd)'))
const tomorrowDate  = computed(() => dayjs(viewDate.value).add(1, 'day').format('M/D (ddd)'))

// ── 날짜 네비게이션 ──
function goPrev() {
  viewDate.value = dayjs(viewDate.value).subtract(1, 'day').format('YYYY-MM-DD')
}
function goNext() {
  if (viewDate.value >= maxFutureDate) return
  viewDate.value = dayjs(viewDate.value).add(1, 'day').format('YYYY-MM-DD')
}
function goToToday() {
  viewDate.value = realToday
}

const greeting = computed(() => {
  const h = dayjs().hour()
  if (h < 10) return '좋은 아침입니다! 오늘 하루도 파이팅하세요.'
  if (h < 14) return '오전 업무 잘 진행되고 있나요?'
  if (h < 18) return '오후도 힘차게 달려봐요!'
  return '수고하셨습니다. 오늘 하루를 정리해보세요.'
})

// ── KPI 데이터 ──
const todayKpis    = computed(() => (store.dashboard?.kpiSummaries || []).filter(k => k.status === 'ACTIVE'))
const tomorrowKpis = computed(() => todayKpis.value)

// ── localStorage 키 ──
const KEY         = (date) => `workoop-scrum-${date}`
const checkinKey  = computed(() => `workoop-checkin-${today.value}`)

// ── 저장 토스트 ──
const savedToast = ref(false)
let toastTimer = null
function showSavedToast() {
  savedToast.value = true
  clearTimeout(toastTimer)
  toastTimer = setTimeout(() => { savedToast.value = false }, 1800)
}

function loadDay(date) {
  try {
    return JSON.parse(localStorage.getItem(KEY(date))) || { tasks: [], blocker: '', blockerSeverity: 'LOW' }
  } catch {
    return { tasks: [], blocker: '', blockerSeverity: 'LOW' }
  }
}
function saveDay(date, data) {
  localStorage.setItem(KEY(date), JSON.stringify(data))
}

// 서버 우선 로드 → localStorage 캐시 갱신 → 폴백
async function loadDayFromServer(date) {
  if (orgStore.currentOrg) {
    try {
      const res = await scrumApi.me(date)
      const scrum = res.data
      if (scrum) {
        const tasks = scrum.tasksJson ? JSON.parse(scrum.tasksJson) : []
        // localStorage 캐시 갱신 (weekHistory / CSV export가 읽음)
        localStorage.setItem(KEY(date), JSON.stringify({
          tasks,
          blocker: scrum.blocker || '',
          blockerSeverity: scrum.blockerSeverity || 'LOW'
        }))
        localStorage.setItem(`workoop-checkin-${date}`, JSON.stringify({
          energy: scrum.energy || 0,
          focus:  scrum.focus  || ''
        }))
        return {
          tasks,
          blocker:         scrum.blocker         || '',
          blockerSeverity: scrum.blockerSeverity  || 'LOW',
          energy:          scrum.energy           || 0,
          focus:           scrum.focus            || ''
        }
      }
    } catch { /* fall through */ }
  }
  // 폴백: localStorage
  const day = loadDay(date)
  const ci  = loadCheckInData(date)
  return { ...day, energy: ci.energy, focus: ci.focus }
}

// ── 체크인 (에너지 + 집중목표) ──
const checkIn = ref({ energy: 0, focus: '' })

function loadCheckInData(date) {
  try {
    return JSON.parse(localStorage.getItem(`workoop-checkin-${date}`)) || { energy: 0, focus: '' }
  } catch {
    return { energy: 0, focus: '' }
  }
}
function loadCheckIn() {
  const ci = loadCheckInData(today.value)
  checkIn.value = { energy: ci.energy || 0, focus: ci.focus || '' }
}
function saveCheckIn() {
  if (isPastDate.value) return
  localStorage.setItem(checkinKey.value, JSON.stringify(checkIn.value))
  showSavedToast()
  scheduleSyncToServer()
}
function setEnergy(n) {
  checkIn.value.energy = checkIn.value.energy === n ? 0 : n
  saveCheckIn()
}

// ── 7일 히트맵 데이터 ──
const weekHistory = computed(() => {
  const realYesterday = dayjs(realToday).subtract(1, 'day').format('YYYY-MM-DD')
  const viewingToday  = viewDate.value === realToday
  return Array.from({ length: 7 }, (_, i) => {
    const d = dayjs().subtract(6 - i, 'day')
    const date = d.format('YYYY-MM-DD')
    let tasks = []
    if (viewingToday && date === realToday) {
      tasks = todayTasks.value
    } else if (viewingToday && date === realYesterday) {
      tasks = yesterdayTasks.value
    } else {
      try { tasks = loadDay(date).tasks || [] } catch { /* */ }
    }
    const rate = tasks.length === 0 ? 0 : Math.round((tasks.filter(t => t.done).length / tasks.length) * 100)
    let levelClass = 'heat-0'
    if (rate >= 80) levelClass = 'heat-3'
    else if (rate >= 50) levelClass = 'heat-2'
    else if (rate > 0) levelClass = 'heat-1'
    return {
      date,
      dateLabel: d.format('M/D'),
      dayLabel: d.format('dd'),
      rate,
      levelClass,
      isSelected: date === viewDate.value
    }
  })
})

// ── 태스크 상태 ──
const yesterdayTasks  = ref([])
const todayTasks      = ref([])
const tomorrowTasks   = ref([])
const todayBlocker    = ref('')
const blockerSeverity = ref('LOW')

async function loadAll() {
  const [yd, td, tm] = await Promise.all([
    loadDayFromServer(yesterday.value),
    loadDayFromServer(today.value),
    loadDayFromServer(tomorrow.value),
  ])
  yesterdayTasks.value  = yd.tasks || []
  todayTasks.value      = td.tasks || []
  tomorrowTasks.value   = tm.tasks || []
  todayBlocker.value    = td.blocker || ''
  blockerSeverity.value = td.blockerSeverity || 'LOW'
  checkIn.value         = { energy: td.energy || 0, focus: td.focus || '' }
}

function saveTodayData() {
  if (isPastDate.value) return
  saveDay(today.value, {
    tasks: todayTasks.value,
    blocker: todayBlocker.value,
    blockerSeverity: blockerSeverity.value
  })
  showSavedToast()
  scheduleSyncToServer()
}
function saveTomorrowData() {
  if (isPastDate.value) return
  saveDay(tomorrow.value, { tasks: tomorrowTasks.value, blocker: '', blockerSeverity: 'LOW' })
  showSavedToast()
  syncDateToServer(tomorrow.value, tomorrowTasks.value, '', 'LOW', 0, '')
}
function saveYesterdayData() {
  if (isPastDate.value) return
  saveDay(yesterday.value, { tasks: yesterdayTasks.value, blocker: '' })
  showSavedToast()
  syncDateToServer(yesterday.value, yesterdayTasks.value, '', 'LOW', 0, '')
}

watch(todayTasks,    saveTodayData,    { deep: true })
watch(tomorrowTasks, saveTomorrowData, { deep: true })
watch(viewDate, async () => { await loadAll(); if (!isPastDate.value) loadDailyKpiRecords() })

// ── 진행률 ──
function doneCount(tasks) {
  return tasks.filter(t => t.done).length
}
const yesterdayRate = computed(() => {
  if (!yesterdayTasks.value.length) return 0
  return Math.round((doneCount(yesterdayTasks.value) / yesterdayTasks.value.length) * 100)
})
const todayRate = computed(() => {
  if (!todayTasks.value.length) return 0
  return Math.round((doneCount(todayTasks.value) / todayTasks.value.length) * 100)
})
const totalEstHours = computed(() => {
  const sum = todayTasks.value.reduce((acc, t) => acc + (t.estimatedHours || 0), 0)
  return sum % 1 === 0 ? sum : sum.toFixed(1)
})

function rateClass(rate) {
  if (rate >= 80) return 'rate-high'
  if (rate >= 50) return 'rate-mid'
  return 'rate-low'
}
function rateColor(rate) {
  if (rate >= 80) return '#10b981'
  if (rate >= 50) return '#f59e0b'
  return '#ef4444'
}

// ── 스마트 이월 배너 ──
const carriedIds = ref(new Set())
const showCarryBanner = ref(true)

const pendingCarryCount = computed(() =>
  yesterdayTasks.value.filter(t => !t.done && !carriedIds.value.has(t.id)).length
)

function carryOver(task) {
  const exists = todayTasks.value.some(t => t.title === task.title)
  if (!exists) {
    todayTasks.value.unshift({
      id: Date.now().toString(),
      title: task.title,
      done: false,
      priority: task.priority || '',
      kpiLinked: task.kpiLinked || false,
      carried: true,
      note: '',
      estimatedHours: task.estimatedHours || null
    })
  }
  carriedIds.value = new Set([...carriedIds.value, task.id])
  saveYesterdayData()
}

function carryAllOver() {
  const pending = yesterdayTasks.value.filter(t => !t.done && !carriedIds.value.has(t.id))
  pending.forEach(task => carryOver(task))
  showCarryBanner.value = false
}

// ── 체크 토글 ──
function toggleYesterday(task) {
  task.done = !task.done
  saveYesterdayData()
}
function toggleToday(task) {
  task.done = !task.done
  saveTodayData()
}

// ── 드래그 앤 드롭 ──
const draggingId   = ref(null)
const draggingCol  = ref(null)
const draggingIdx  = ref(null)
let   dragOverIdx  = null

// 컬럼명 → 실제 배열 매핑
function getColArray(col) {
  if (col === 'today')    return todayTasks.value
  if (col === 'tomorrow') return tomorrowTasks.value
  if (col === 'yesterday') return yesterdayTasks.value
  return null
}

function dragStart(e, task, idx, col) {
  draggingId.value  = task.id
  draggingCol.value = col
  draggingIdx.value = idx
  e.dataTransfer.effectAllowed = 'move'
  e.dataTransfer.setData('text/plain', task.id)
}
function dragEnd() {
  draggingId.value  = null
  draggingCol.value = null
  draggingIdx.value = null
  dragOverIdx       = null
}
function dragOverZone(e, tasks) {
  const cards = e.currentTarget.querySelectorAll('.task-card:not(.dragging)')
  let nearestIdx = tasks.length
  let minDist    = Infinity
  cards.forEach((card, i) => {
    const rect = card.getBoundingClientRect()
    const mid  = rect.top + rect.height / 2
    const dist = Math.abs(e.clientY - mid)
    if (dist < minDist) { minDist = dist; nearestIdx = i }
  })
  dragOverIdx = nearestIdx
}
function dropTask(_e, targetArr, targetCol) {
  const fromCol = draggingCol.value
  const fromIdx = draggingIdx.value
  if (fromIdx === null || !fromCol) { dragEnd(); return }

  const sourceArr = getColArray(fromCol)
  if (!sourceArr) { dragEnd(); return }

  // yesterday 컬럼으로는 드롭 불가 (과거 데이터 보호)
  if (targetCol === 'yesterday') { dragEnd(); return }

  const toIdx = dragOverIdx ?? targetArr.length

  if (fromCol === targetCol) {
    // ── 같은 컬럼: 순서 재정렬 ──
    if (fromIdx === toIdx) { dragEnd(); return }
    const [item] = sourceArr.splice(fromIdx, 1)
    const insertAt = toIdx > fromIdx ? toIdx - 1 : toIdx
    sourceArr.splice(Math.max(insertAt, 0), 0, item)
  } else {
    // ── 다른 컬럼: 태스크 이동 ──
    const [item] = sourceArr.splice(fromIdx, 1)
    // 어제 → 오늘로 이동하면 이월 처리
    if (fromCol === 'yesterday' && targetCol === 'today') {
      item.carried = true
      carriedIds.value = new Set([...carriedIds.value, item.id])
    }
    // 오늘 → 내일 이동 시 carried 플래그 제거
    if (fromCol === 'today' && targetCol === 'tomorrow') {
      item.carried = false
    }
    targetArr.splice(Math.min(toIdx, targetArr.length), 0, item)
  }

  dragEnd()
}

// ── 오늘 태스크 추가 ──
const addingToday       = ref(false)
const newTodayTitle     = ref('')
const newTodayPriority  = ref('')
const newTodayKpiLinked = ref(false)
const newTodayHours     = ref(null)
const addInputRef       = ref(null)

const editingId    = ref(null)
const editInputRef = ref(null)

function startAddToday() {
  addingToday.value = true
  nextTick(() => addInputRef.value?.focus())
}
function submitTodayTask() {
  const title = newTodayTitle.value.trim()
  if (title) {
    todayTasks.value.push({
      id: Date.now().toString(),
      title,
      done: false,
      priority: newTodayPriority.value,
      kpiLinked: newTodayKpiLinked.value,
      carried: false,
      note: '',
      estimatedHours: newTodayHours.value || null
    })
  }
  newTodayTitle.value     = ''
  newTodayPriority.value  = ''
  newTodayKpiLinked.value = false
  newTodayHours.value     = null
  addingToday.value       = false
}
function cancelAddToday() {
  newTodayTitle.value     = ''
  newTodayPriority.value  = ''
  newTodayKpiLinked.value = false
  newTodayHours.value     = null
  addingToday.value       = false
}
function handleTodayFormBlur(e) {
  // Enter로 이미 제출된 경우 중복 실행 방지
  if (!addingToday.value) return
  // 포커스가 폼 내부 다른 요소로 이동하면 닫지 않음
  if (e.currentTarget.contains(e.relatedTarget)) return
  submitTodayTask()
}
function deleteToday(id) {
  todayTasks.value = todayTasks.value.filter(t => t.id !== id)
}

// ── 내일 태스크 추가 ──
const addingTomorrow       = ref(false)
const newTomorrowTitle     = ref('')
const newTomorrowPriority  = ref('')
const newTomorrowKpiLinked = ref(false)
const newTomorrowHours     = ref(null)
const addTomorrowRef       = ref(null)

function startAddTomorrow() {
  addingTomorrow.value = true
  nextTick(() => addTomorrowRef.value?.focus())
}
function submitTomorrowTask() {
  const title = newTomorrowTitle.value.trim()
  if (title) {
    tomorrowTasks.value.push({
      id: Date.now().toString(),
      title,
      done: false,
      priority: newTomorrowPriority.value,
      kpiLinked: newTomorrowKpiLinked.value,
      estimatedHours: newTomorrowHours.value || null
    })
  }
  newTomorrowTitle.value     = ''
  newTomorrowPriority.value  = ''
  newTomorrowKpiLinked.value = false
  newTomorrowHours.value     = null
  addingTomorrow.value       = false
}
function cancelAddTomorrow() {
  newTomorrowTitle.value     = ''
  newTomorrowPriority.value  = ''
  newTomorrowKpiLinked.value = false
  newTomorrowHours.value     = null
  addingTomorrow.value       = false
}
function handleTomorrowFormBlur(e) {
  if (!addingTomorrow.value) return
  if (e.currentTarget.contains(e.relatedTarget)) return
  submitTomorrowTask()
}
function deleteTomorrow(id) {
  tomorrowTasks.value = tomorrowTasks.value.filter(t => t.id !== id)
}

// ── 태스크 확장 (메모 / 시간) ──
const expandedId = ref(null)
function toggleExpand(task) {
  expandedId.value = expandedId.value === task.id ? null : task.id
}

// ── 인라인 편집 ──
function startEdit(task) {
  if (isPastDate.value || draggingId.value) return
  editingId.value = task.id
  nextTick(() => editInputRef.value?.focus())
}
function finishEdit(task) {
  if (!task.title.trim()) task.title = '(제목 없음)'
  editingId.value = null
  saveTodayData()
}
function finishEditTomorrow(task) {
  if (!task.title.trim()) task.title = '(제목 없음)'
  editingId.value = null
  saveTomorrowData()
}

// ── 우선순위 순환 ──
const priorities = ['P1', 'P2', 'P3', '']
function cyclePriority(task) {
  const idx = priorities.indexOf(task.priority || '')
  task.priority = priorities[(idx + 1) % priorities.length]
  saveTodayData()
}
function cycleTomorrowPriority(task) {
  const idx = priorities.indexOf(task.priority || '')
  task.priority = priorities[(idx + 1) % priorities.length]
  saveTomorrowData()
}

// ── 일일 KPI 체크 ──
const dailyKpis = computed(() =>
  (store.kpis || []).filter(k => k.frequency === 'DAILY' && k.status === 'ACTIVE')
)
const dailyKpiRecords = ref({})  // { [kpiId]: { actualValue, booleanValue } }
const dailyKpiSaving  = ref({})  // { [kpiId]: boolean }

const dailyKpiDoneCount = computed(() =>
  dailyKpis.value.filter(k => {
    const r = dailyKpiRecords.value[k.id]
    if (!r) return false
    if (k.kpiType === 'BOOLEAN') return r.booleanValue
    return r.actualValue != null && k.targetValue && r.actualValue >= k.targetValue
  }).length
)

async function loadDailyKpiRecords() {
  if (!dailyKpis.value.length) return
  try {
    const res = await recordApi.getByDate(realToday)
    const map = {}
    ;(res.data || []).forEach(r => {
      if (dailyKpis.value.some(k => k.id === r.kpiId)) {
        map[r.kpiId] = { actualValue: r.actualValue, booleanValue: r.booleanValue ?? false }
      }
    })
    dailyKpiRecords.value = map
  } catch { /* ignore */ }
}

async function saveDailyKpi(kpi) {
  dailyKpiSaving.value = { ...dailyKpiSaving.value, [kpi.id]: true }
  try {
    const rec = dailyKpiRecords.value[kpi.id] || {}
    await recordApi.upsert({
      kpiId:        kpi.id,
      recordedDate: realToday,
      actualValue:  kpi.kpiType !== 'BOOLEAN' ? (rec.actualValue ?? null) : null,
      booleanValue: kpi.kpiType === 'BOOLEAN'  ? (rec.booleanValue ?? false) : null
    })
    showSavedToast()
  } finally {
    dailyKpiSaving.value = { ...dailyKpiSaving.value, [kpi.id]: false }
  }
}

function toggleDailyBoolean(kpi) {
  const cur = dailyKpiRecords.value[kpi.id]?.booleanValue ?? false
  dailyKpiRecords.value = {
    ...dailyKpiRecords.value,
    [kpi.id]: { booleanValue: !cur }
  }
  saveDailyKpi(kpi)
}

function onDailyNumChange(kpi, e) {
  const val = e.target.value === '' ? null : Number(e.target.value)
  dailyKpiRecords.value = {
    ...dailyKpiRecords.value,
    [kpi.id]: { ...(dailyKpiRecords.value[kpi.id] || {}), actualValue: val }
  }
  saveDailyKpi(kpi)
}

// ── Slack 동기화 ──
const syncing = ref(false)

async function syncSlack() {
  if (syncing.value) return
  syncing.value = true
  try {
    const orgId = orgStore.currentOrg?.id ?? null
    const res = await syncApi.slack(orgId)
    const msg = res.data?.message || 'Slack 동기화 완료'
    alert(msg)
    // loadDayFromServer는 orgStore 조건이 있으므로 직접 API 호출
    try {
      const scrumRes = await scrumApi.me(today.value)
      const scrum = scrumRes.data
      if (scrum?.tasksJson) {
        todayTasks.value = JSON.parse(scrum.tasksJson)
      }
    } catch (e) {
      console.error('스크럼 갱신 실패', e)
    }
  } catch (e) {
    alert('Slack 동기화 실패: ' + (e.response?.data?.message || e.message))
  } finally {
    syncing.value = false
  }
}

// ── CSV 내보내기 ──
const showExportModal = ref(false)
const exportFrom = ref(realToday)
const exportTo   = ref(realToday)

function applyPreset(type) {
  exportTo.value = realToday
  if (type === 'today') {
    exportFrom.value = realToday
  } else if (type === 'week') {
    exportFrom.value = dayjs().subtract(6, 'day').format('YYYY-MM-DD')
  } else if (type === 'month') {
    exportFrom.value = dayjs().subtract(29, 'day').format('YYYY-MM-DD')
  } else if (type === 'all') {
    // localStorage에서 실제 존재하는 가장 오래된 날짜 탐색
    const keys = Object.keys(localStorage)
      .filter(k => k.startsWith('workoop-scrum-'))
      .map(k => k.replace('workoop-scrum-', ''))
      .filter(d => dayjs(d).isValid())
      .sort()
    exportFrom.value = keys.length ? keys[0] : realToday
  }
}

function runExport() {
  const from  = dayjs(exportFrom.value)
  const to    = dayjs(exportTo.value)
  const rows  = [['날짜', '에너지(1-5)', '집중목표', '태스크', '완료', '우선순위', '예상시간(h)', '메모', '이월', '블로커', '블로커심각도']]

  let cur = from
  while (!cur.isAfter(to, 'day')) {
    const date = cur.format('YYYY-MM-DD')

    let scrum   = null
    let checkIn = null
    try { scrum   = JSON.parse(localStorage.getItem(`workoop-scrum-${date}`))   } catch { /* ignore */ }
    try { checkIn = JSON.parse(localStorage.getItem(`workoop-checkin-${date}`)) } catch { /* ignore */ }

    const energy   = checkIn?.energy  || ''
    const focus    = checkIn?.focus   || ''
    const blocker  = scrum?.blocker   || ''
    const severity = scrum?.blockerSeverity || ''

    if (scrum?.tasks?.length) {
      for (const t of scrum.tasks) {
        rows.push([
          date, energy, focus,
          t.title          || '',
          t.done ? '완료' : '미완료',
          t.priority       || '',
          t.estimatedHours != null ? t.estimatedHours : '',
          t.note           || '',
          t.carried        ? '이월' : '',
          blocker, severity
        ])
      }
    } else if (energy || focus || blocker) {
      rows.push([date, energy, focus, '', '', '', '', '', '', blocker, severity])
    }
    cur = cur.add(1, 'day')
  }

  // CSV 직렬화 (RFC 4180, BOM UTF-8)
  const csv = rows.map(row =>
    row.map(v => {
      const s = String(v ?? '')
      return (s.includes(',') || s.includes('"') || s.includes('\n'))
        ? `"${s.replace(/"/g, '""')}"` : s
    }).join(',')
  ).join('\r\n')

  const blob = new Blob(['﻿' + csv], { type: 'text/csv;charset=utf-8;' })
  const url  = URL.createObjectURL(blob)
  const a    = Object.assign(document.createElement('a'), {
    href: url,
    download: `workoop-scrum-${exportFrom.value}-${exportTo.value}.csv`
  })
  a.click()
  URL.revokeObjectURL(url)
  showExportModal.value = false
}

// ── 키보드 단축키 ──
function onKeyDown(e) {
  if (e.key === 'n' && !['INPUT', 'TEXTAREA'].includes(e.target.tagName)) {
    e.preventDefault()
    if (!addingToday.value && !isPastDate.value) startAddToday()
  }
}

onMounted(async () => {
  // 캘린더에서 날짜를 지정해서 이동한 경우
  const queryDate = route.query.date
  if (queryDate && dayjs(queryDate).isValid()) {
    viewDate.value = dayjs(queryDate).format('YYYY-MM-DD')
  }
  await loadAll()
  if (!store.dashboard) store.fetchDashboard()
  if (!store.kpis?.length) await store.fetchKpis()
  loadDailyKpiRecords()
  document.addEventListener('keydown', onKeyDown)
})
onUnmounted(() => {
  document.removeEventListener('keydown', onKeyDown)
})
</script>

<style scoped>
/* ── 저장 토스트 ── */
.saved-toast {
  position: fixed; bottom: 28px; right: 32px;
  display: flex; align-items: center; gap: 6px;
  padding: 8px 16px;
  background: var(--gray-800); color: #F9FAFB;
  border-radius: var(--radius-lg); font-size: 13px; font-weight: 600;
  box-shadow: var(--shadow-lg); pointer-events: none; z-index: 9999;
}
.toast-enter-active, .toast-leave-active { transition: opacity .2s, transform .2s; }
.toast-enter-from, .toast-leave-to { opacity: 0; transform: translateY(8px); }

/* ── Slack 동기화 버튼 ── */
.sync-btn {
  display: inline-flex; align-items: center; gap: 5px;
  padding: 5px 11px; border-radius: 7px; font-size: 12px; font-weight: 600;
  border: 1px solid #4A5568; color: #A0AEC0; background: transparent;
  cursor: pointer; transition: all .15s;
}
.sync-btn:hover:not(:disabled) { border-color: #4F9CF9; color: #4F9CF9; background: rgba(79,156,249,0.07); }
.sync-btn:disabled { opacity: 0.5; cursor: default; }
.sync-btn .spinning { animation: spin 1s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }

/* ── 내보내기 버튼 ── */
.export-btn {
  display: flex; align-items: center; gap: 5px;
  padding: 5px 11px; border: 1px solid var(--border-color); border-radius: var(--radius-md);
  background: var(--bg-card); color: var(--text-secondary); cursor: pointer;
  font-size: 12px; font-weight: 600; transition: all var(--transition-fast); font-family: inherit;
}
.export-btn:hover { border-color: var(--color-kpi); color: var(--color-kpi); background: rgba(38,201,129,0.06); }
.export-btn-label { display: inline; }

/* ── 모달 ── */
.modal-overlay {
  position: fixed; inset: 0; background: rgba(0,0,0,.4);
  display: flex; align-items: center; justify-content: center;
  z-index: 9000; backdrop-filter: blur(2px);
}
.modal-fade-enter-active, .modal-fade-leave-active { transition: opacity .15s; }
.modal-fade-enter-from, .modal-fade-leave-to { opacity: 0; }

.export-modal {
  background: var(--bg-card); border-radius: var(--radius-xl);
  width: 420px; max-width: calc(100vw - 32px);
  box-shadow: var(--shadow-xl); border: 1px solid var(--border-color);
  display: flex; flex-direction: column; overflow: hidden;
}
.modal-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 16px 20px 14px; border-bottom: 1px solid var(--border-color);
}
.modal-title { display: flex; align-items: center; gap: 8px; font-size: 15px; font-weight: 600; color: var(--text-primary); }
.modal-title-icon { color: var(--color-kpi); }
.modal-close {
  width: 28px; height: 28px; border: none; background: var(--bg-hover);
  border-radius: var(--radius-md); cursor: pointer; display: flex; align-items: center; justify-content: center;
  color: var(--text-secondary); transition: all var(--transition-fast);
}
.modal-close:hover { background: var(--border-color); }

.modal-body { padding: 18px 20px; display: flex; flex-direction: column; gap: 16px; }

/* 빠른 선택 */
.preset-label, .range-label {
  font-size: 11px; font-weight: 700; color: var(--text-muted);
  text-transform: uppercase; letter-spacing: .06em; margin-bottom: 8px;
}
.preset-btns { display: flex; gap: 6px; flex-wrap: wrap; }
.preset-btn {
  padding: 5px 13px; border: 1px solid var(--border-color); border-radius: var(--radius-md);
  background: var(--bg-card); color: var(--text-secondary); cursor: pointer;
  font-size: 13px; font-weight: 600; transition: all var(--transition-fast); font-family: inherit;
}
.preset-btn:hover { border-color: var(--color-kpi); color: var(--color-kpi); background: rgba(38,201,129,0.06); }

/* 날짜 범위 */
.range-inputs { display: flex; align-items: center; gap: 10px; }
.range-field { display: flex; flex-direction: column; gap: 4px; flex: 1; }
.range-field label { font-size: 11px; color: var(--text-muted); font-weight: 600; }
.range-field input[type="date"] {
  padding: 7px 10px; border: 1px solid var(--border-color); border-radius: var(--radius-md);
  font-size: 13px; color: var(--text-primary); outline: none; background: var(--bg-card);
  font-family: inherit; width: 100%; cursor: pointer; transition: border-color var(--transition-fast);
}
.range-field input[type="date"]:focus { border-color: var(--color-project); box-shadow: 0 0 0 3px rgba(48,127,226,0.12); }
.range-sep { color: var(--text-muted); font-weight: 700; flex-shrink: 0; }

/* 안내 */
.export-info {
  display: flex; align-items: center; gap: 7px;
  padding: 9px 12px; background: var(--bg-hover); border-radius: var(--radius-md);
  font-size: 12px; color: var(--text-secondary); border: 1px solid var(--border-color);
}
.info-icon { color: var(--text-muted); flex-shrink: 0; }

/* 모달 푸터 */
.modal-footer {
  display: flex; gap: 8px; padding: 12px 20px 16px;
  border-top: 1px solid var(--border-color); justify-content: flex-end;
}
.modal-cancel-btn {
  padding: 7px 16px; border: 1px solid var(--border-color); border-radius: var(--radius-md);
  background: var(--bg-card); color: var(--text-secondary); cursor: pointer;
  font-size: 13px; font-weight: 600; transition: all var(--transition-fast); font-family: inherit;
}
.modal-cancel-btn:hover { background: var(--bg-hover); }
.modal-export-btn {
  display: flex; align-items: center; gap: 6px;
  padding: 7px 16px; border: none; border-radius: var(--radius-md);
  background: var(--color-kpi); color: white; cursor: pointer;
  font-size: 13px; font-weight: 700; transition: all var(--transition-fast); font-family: inherit;
}
.modal-export-btn:hover:not(:disabled) { background: #1EA876; }
.modal-export-btn:disabled { opacity: 0.4; cursor: default; }

/* ── 날짜 네비게이션 바 ── */
.date-nav-bar {
  display: flex; align-items: center; gap: 8px;
  padding: 9px 16px; background: var(--bg-card);
  border-bottom: 1px solid var(--border-color); flex-shrink: 0;
}
.date-nav-btn {
  width: 28px; height: 28px;
  border: 1px solid var(--border-color); border-radius: var(--radius-md);
  background: var(--bg-card); color: var(--text-secondary);
  cursor: pointer; display: flex; align-items: center; justify-content: center;
  transition: all var(--transition-fast);
}
.date-nav-btn:hover:not(:disabled) { border-color: var(--color-project); color: var(--color-project); background: rgba(48,127,226,0.06); }
.date-nav-btn:disabled { opacity: 0.35; cursor: default; }
.date-nav-center { display: flex; align-items: center; gap: 6px; flex: 1; justify-content: center; }
.date-nav-icon  { color: var(--text-muted); }
.date-nav-label { font-size: 14px; font-weight: 600; color: var(--text-primary); }
.date-nav-label.is-today { color: var(--color-project); }
.date-nav-sub   { font-size: 12px; color: var(--text-muted); }
.date-today-btn {
  padding: 4px 12px; border: 1px solid var(--color-project); border-radius: var(--radius-md);
  background: rgba(48,127,226,0.08); color: var(--color-project); cursor: pointer;
  font-size: 12px; font-weight: 600; transition: all var(--transition-fast); font-family: inherit;
}
.date-today-btn:hover { background: var(--color-project); color: white; }

/* ── 읽기 전용 배너 ── */
.readonly-banner {
  display: flex; align-items: center; gap: 8px; padding: 7px 20px;
  background: rgba(245,158,11,0.08); border-bottom: 1px solid rgba(245,158,11,0.25);
  color: #92400E; font-size: 13px; font-weight: 500; flex-shrink: 0;
}

/* ── 히트맵 선택 표시 ── */
.heatmap-col-selected .heatmap-bar { outline: 2px solid var(--color-project); outline-offset: 1px; }
.heatmap-col-selected .heatmap-label { color: var(--color-project); font-weight: 700; }

/* ── 탭 ── */
.scrum-mode-tabs {
  display: flex; gap: 4px; padding: 12px 20px 0;
  border-bottom: 1px solid var(--border-color); background: var(--bg-card); flex-shrink: 0;
}
.smt-btn {
  display: flex; align-items: center; gap: 6px;
  padding: 9px 16px; background: transparent;
  border: none; border-bottom: 2px solid transparent; margin-bottom: -1px;
  font-size: 14px; font-weight: 600; color: var(--text-muted);
  cursor: pointer; transition: all var(--transition-fast); font-family: inherit;
}
.smt-btn:hover { color: var(--text-secondary); }
.smt-btn.active { color: var(--color-project); border-bottom-color: var(--color-project); }
.smt-org {
  background: rgba(48,127,226,0.1); color: var(--color-project);
  font-size: 10px; font-weight: 700; padding: 1px 7px; border-radius: var(--radius-sm);
}
.team-scrum-wrap { flex: 1; overflow-y: auto; padding: 20px; }

.scrum-page { display: flex; flex-direction: column; height: 100%; overflow: hidden; background: var(--bg-main); }

/* ── 헤더 ── */
.scrum-header {
  display: flex; align-items: center; gap: 20px;
  padding: 12px 20px; background: var(--bg-card);
  border-bottom: 1px solid var(--border-color); flex-shrink: 0;
}
.header-left { flex-shrink: 0; }
.header-date-group { display: flex; align-items: baseline; gap: 8px; }
.header-weekday   { font-size: 15px; font-weight: 700; color: var(--text-primary); }
.header-full-date { font-size: 12px; color: var(--text-muted); }
.header-greeting  { font-size: 11px; color: var(--text-secondary); margin: 2px 0 0; white-space: nowrap; }
.header-center    { flex: 1; }
.focus-goal-wrap {
  display: flex; align-items: center; gap: 8px;
  background: var(--bg-hover); border: 1px solid var(--border-color);
  border-radius: var(--radius-md); padding: 7px 12px;
}
.focus-icon  { color: var(--color-project); flex-shrink: 0; }
.focus-input {
  flex: 1; border: none; outline: none; background: transparent;
  font-size: 13px; color: var(--text-primary); font-family: inherit;
}
.focus-input::placeholder { color: var(--text-muted); }

.header-right { display: flex; align-items: center; gap: 20px; flex-shrink: 0; }

/* 에너지 레벨 */
.energy-section { display: flex; flex-direction: column; gap: 5px; align-items: flex-end; }
.energy-label   { font-size: 10px; font-weight: 700; color: var(--text-muted); letter-spacing: .06em; text-transform: uppercase; }
.energy-btns    { display: flex; gap: 3px; }
.energy-btn {
  width: 26px; height: 26px; border-radius: var(--radius-md);
  border: 1px solid var(--border-color); background: var(--bg-card);
  cursor: pointer; color: var(--border-color);
  display: flex; align-items: center; justify-content: center;
  transition: all var(--transition-fast);
}
.energy-btn:hover { border-color: var(--color-warning); color: var(--color-warning); }
.energy-btn.active { background: rgba(245,158,11,0.12); border-color: var(--color-warning); color: var(--color-warning); }

/* 7일 히트맵 */
.week-heatmap  { display: flex; flex-direction: column; gap: 5px; align-items: flex-end; }
.week-label    { font-size: 10px; font-weight: 700; color: var(--text-muted); letter-spacing: .06em; text-transform: uppercase; }
.heatmap-cols  { display: flex; align-items: flex-end; gap: 3px; height: 28px; }
.heatmap-col   { display: flex; flex-direction: column; align-items: center; justify-content: flex-end; height: 100%; width: 18px; gap: 2px; }
.heatmap-bar   { width: 10px; border-radius: 3px; transition: height .3s; min-height: 4px; }
.heatmap-label { font-size: 8px; color: var(--text-muted); }
.heat-0 { background: var(--border-color); }
.heat-1 { background: rgba(48,127,226,0.3); }
.heat-2 { background: rgba(48,127,226,0.65); }
.heat-3 { background: var(--color-kpi); }

/* ── KPI 스트립 ── */
.kpi-strip {
  display: flex; align-items: center; gap: 8px;
  padding: 8px 20px; background: var(--bg-card);
  border-bottom: 1px solid var(--border-color);
  overflow-x: auto; flex-shrink: 0;
}
.kpi-strip-label { font-size: 10px; font-weight: 700; color: var(--text-muted); letter-spacing: .06em; text-transform: uppercase; white-space: nowrap; }
.kpi-chip {
  display: flex; align-items: center; gap: 5px;
  background: var(--bg-hover); border: 1px solid var(--border-color);
  border-radius: var(--radius-full); padding: 3px 10px;
  font-size: 11px; white-space: nowrap; cursor: pointer;
  text-decoration: none; color: inherit; transition: border-color var(--transition-fast);
}
.kpi-chip:hover { border-color: var(--color-project); }
.kpi-dot  { width: 6px; height: 6px; border-radius: 50%; flex-shrink: 0; }
.kpi-name { color: var(--text-secondary); }
.kpi-rate { font-weight: 700; font-size: 10px; }
.rate-high { color: var(--color-success); }
.rate-mid  { color: var(--color-warning); }
.rate-low  { color: var(--color-danger); }

/* ── 이월 배너 ── */
.carry-banner {
  display: flex; align-items: center; gap: 10px; padding: 8px 20px;
  background: rgba(245,158,11,0.07); border-bottom: 1px solid rgba(245,158,11,0.22);
  font-size: 12px; color: #92400E; flex-shrink: 0;
}
.carry-banner strong { color: #C2410C; }
.carry-all-btn {
  margin-left: auto; font-size: 11px; font-weight: 600;
  padding: 3px 10px; border-radius: var(--radius-md);
  background: #F97316; color: white; border: none;
  cursor: pointer; transition: background var(--transition-fast); font-family: inherit;
}
.carry-all-btn:hover { background: #EA580C; }
.carry-dismiss-btn { background: none; border: none; cursor: pointer; color: #F97316; display: flex; align-items: center; padding: 2px; }

/* ── 3컬럼 ── */
.scrum-columns {
  display: grid; grid-template-columns: 1fr 1fr 1fr;
  gap: 12px; padding: 12px 14px;
  overflow-y: auto; flex: 1; align-items: start;
}

/* ── 컬럼 공통 ── */
.scrum-col { display: flex; flex-direction: column; gap: 7px; min-height: 80px; }
.col-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 8px 11px; border-radius: var(--radius-md); font-size: 13px; font-weight: 600;
}
.col-header-left { display: flex; align-items: center; gap: 7px; }
.col-icon   { flex-shrink: 0; }
.col-title  { font-weight: 600; }
.col-badge  { font-size: 10px; font-weight: 600; padding: 2px 7px; border-radius: var(--radius-sm); letter-spacing: .03em; }
.col-count  { font-size: 11px; font-weight: 500; }

.col-yesterday .col-header { background: var(--bg-hover); border: 1px solid var(--border-color); color: var(--text-secondary); }
.col-yesterday .col-badge  { background: var(--border-color); color: var(--text-secondary); }
.col-today .col-header     { background: rgba(48,127,226,0.07); border: 1px solid rgba(48,127,226,0.2); color: #1D6FCC; }
.col-today .col-badge.today-badge { background: var(--color-project); color: #fff; }
.col-tomorrow .col-header  { background: rgba(139,92,246,0.07); border: 1px solid rgba(139,92,246,0.2); color: #6D28D9; }
.col-tomorrow .col-badge   { background: rgba(139,92,246,0.12); color: #7C3AED; }

.col-empty { font-size: 12px; color: var(--text-muted); text-align: center; padding: 16px 0; }

/* ── 태스크 목록 ── */
.task-list { display: flex; flex-direction: column; gap: 5px; }
.drag-zone { min-height: 36px; border-radius: var(--radius-md); transition: background var(--transition-fast), outline var(--transition-fast); }
.drag-zone.drop-target { outline: 2px dashed var(--color-project); background: rgba(48,127,226,0.04); }

/* ── 태스크 카드 ── */
.task-card {
  background: var(--bg-card); border: 1px solid var(--border-color);
  border-left: 3px solid var(--border-color);
  border-radius: var(--radius-md); padding: 8px 10px;
  transition: box-shadow var(--transition-base), border-color var(--transition-base), opacity var(--transition-base);
  cursor: default; box-shadow: var(--shadow-xs);
}
.task-card:hover { border-color: rgba(48,127,226,0.3); box-shadow: var(--shadow-sm); }
.task-card.done           { border-left-color: var(--color-kpi); }
.task-card.carried-over   { border-left-color: var(--color-warning); }
.task-card.done           { opacity: .5; }
.task-card.done .task-title { text-decoration: line-through; color: var(--text-muted); }
.task-card.carried-over   { border-color: rgba(245,158,11,0.35); background: rgba(245,158,11,0.04); }
.task-card.dragging       { opacity: .4; border-style: dashed; border-color: var(--color-project); }
.task-card.expanded       { border-color: rgba(48,127,226,0.4); box-shadow: 0 2px 8px rgba(48,127,226,.1); }
.task-card-tomorrow       { border-color: rgba(139,92,246,0.2); }
.task-card-tomorrow:hover { border-color: rgba(139,92,246,0.4); }

.task-top { display: flex; align-items: flex-start; gap: 6px; }

.drag-handle { color: var(--border-color); cursor: grab; flex-shrink: 0; margin-top: 2px; display: flex; align-items: center; }
.drag-handle:hover  { color: var(--text-secondary); }
.drag-handle:active { cursor: grabbing; }

.task-check {
  width: 16px; height: 16px; border-radius: 4px;
  border: 2px solid var(--border-color); flex-shrink: 0; margin-top: 1px;
  cursor: pointer; display: flex; align-items: center; justify-content: center;
  transition: all var(--transition-fast); color: white;
}
.task-check:hover  { border-color: var(--color-project); }
.task-check.checked { background: var(--color-kpi); border-color: var(--color-kpi); }
.task-check-tomorrow { border-color: rgba(139,92,246,0.4); cursor: default; }

.task-title { flex: 1; font-size: 13px; font-weight: 500; color: var(--text-primary); line-height: 1.4; cursor: text; word-break: break-word; }
.task-edit-input {
  flex: 1; font-size: 13px; font-weight: 500;
  border: none; outline: 2px solid var(--color-project); border-radius: var(--radius-sm);
  padding: 1px 4px; color: var(--text-primary); background: rgba(48,127,226,0.06); font-family: inherit;
}

.task-actions { display: flex; gap: 2px; flex-shrink: 0; opacity: 0; transition: opacity var(--transition-fast); }
.task-card:hover .task-actions { opacity: 1; }
.task-action-btn {
  width: 22px; height: 22px; background: none; border: none;
  cursor: pointer; color: var(--text-muted); border-radius: var(--radius-sm);
  display: flex; align-items: center; justify-content: center;
  transition: all var(--transition-fast);
}
.task-action-btn:hover { background: rgba(48,127,226,0.08); color: var(--color-project); }
.task-delete {
  width: 22px; height: 22px; background: none; border: none;
  cursor: pointer; color: var(--text-muted); border-radius: var(--radius-sm);
  display: flex; align-items: center; justify-content: center;
  transition: all var(--transition-fast); opacity: 0;
}
.task-card:hover .task-delete { opacity: 1; }
.task-delete:hover { background: rgba(239,68,68,0.1); color: var(--color-danger); }

.icon-rotate { transform: rotate(180deg); transition: transform .2s; }

/* 확장 패널 */
.task-expand {
  margin: 6px 0 5px; border-top: 1px solid var(--border-color);
  padding-top: 7px; display: flex; flex-direction: column; gap: 6px;
}
.task-note-input {
  width: 100%; font-size: 12px; color: var(--text-primary); font-family: inherit;
  border: 1px solid var(--border-color); border-radius: var(--radius-md); padding: 6px 8px;
  resize: vertical; outline: none; background: var(--bg-hover); line-height: 1.5;
  box-sizing: border-box; min-height: 52px; transition: border-color var(--transition-fast);
}
.task-note-input:focus { border-color: var(--color-project); background: var(--bg-card); }
.task-note-input::placeholder { color: var(--text-muted); }
.expand-footer { display: flex; align-items: center; gap: 6px; font-size: 11px; color: var(--text-secondary); }
.time-est-input {
  width: 50px; font-size: 11px; border: 1px solid var(--border-color);
  border-radius: var(--radius-sm); padding: 2px 5px; outline: none;
  font-family: inherit; color: var(--text-primary); background: var(--bg-card);
  text-align: center; transition: border-color var(--transition-fast);
}
.time-est-input:focus { border-color: var(--color-project); }
.time-est-input.small { width: 40px; }

.task-footer { display: flex; align-items: center; justify-content: space-between; margin-top: 6px; }
.task-tags   { display: flex; gap: 4px; flex-wrap: wrap; }
.tag { font-size: 10px; padding: 2px 6px; border-radius: var(--radius-sm); font-weight: 500; border: 1px solid; }
.tag-kpi   { background: rgba(48,127,226,0.1);  color: #1D6FCC; border-color: rgba(48,127,226,0.25); }
.tag-p1    { background: rgba(239,68,68,0.1);   color: #DC2626; border-color: rgba(239,68,68,0.25); }
.tag-p2    { background: rgba(249,115,22,0.1);  color: #EA580C; border-color: rgba(249,115,22,0.25); }
.tag-p3    { background: var(--bg-hover); color: var(--text-secondary); border-color: var(--border-color); }
.tag-carry { background: rgba(245,158,11,0.12); color: #D97706; border-color: rgba(245,158,11,0.3); }
.tag-priority { cursor: pointer; user-select: none; }
.tag-priority:hover { filter: brightness(.92); }
.tag-none  { background: var(--bg-hover); color: var(--text-muted); border-style: dashed; border-color: var(--border-color); }
.tag-note  { background: rgba(16,185,129,0.1); color: #0D9C6E; border-color: rgba(16,185,129,0.25); display: flex; align-items: center; gap: 2px; }
.task-time { font-size: 10px; color: var(--text-muted); display: flex; align-items: center; gap: 2px; }

/* 이월 관련 */
.yesterday-actions { flex-shrink: 0; }
.carry-btn {
  font-size: 11px; color: #F97316; background: none;
  border: 1px solid rgba(249,115,22,0.3); border-radius: var(--radius-sm); padding: 2px 7px;
  cursor: pointer; transition: all var(--transition-fast); white-space: nowrap; font-family: inherit;
}
.carry-btn:hover { background: rgba(249,115,22,0.08); }
.carried-label { font-size: 10px; color: var(--color-kpi); display: flex; align-items: center; gap: 3px; }

/* ── 블로커 ── */
.blocker-area { border-radius: var(--radius-md); padding: 9px 11px; border-left: 3px solid; }
.blocker-area.blocker-low  { background: rgba(38,201,129,0.05);  border: 1px solid rgba(38,201,129,0.2);  border-left: 3px solid var(--color-kpi); }
.blocker-area.blocker-med  { background: rgba(245,158,11,0.05);  border: 1px solid rgba(245,158,11,0.2);  border-left: 3px solid var(--color-warning); }
.blocker-area.blocker-high { background: rgba(239,68,68,0.05);   border: 1px solid rgba(239,68,68,0.15);  border-left: 3px solid var(--color-danger); }

.blocker-header { display: flex; align-items: center; gap: 5px; font-size: 11px; font-weight: 600; color: var(--color-danger); margin-bottom: 6px; }
.blocker-severity { display: flex; gap: 3px; margin-left: auto; }
.severity-btn {
  font-size: 9px; font-weight: 600; padding: 1px 5px; border-radius: var(--radius-xs);
  border: 1px solid var(--border-color); background: var(--bg-card); cursor: pointer;
  color: var(--text-muted); transition: all var(--transition-fast); letter-spacing: .03em; font-family: inherit;
}
.severity-btn.active.sev-low  { background: rgba(239,68,68,0.08);  color: #FCA5A5; border-color: rgba(239,68,68,0.2); }
.severity-btn.active.sev-med  { background: rgba(249,115,22,0.08); color: #F97316; border-color: rgba(249,115,22,0.25); }
.severity-btn.active.sev-high { background: rgba(239,68,68,0.12);  color: #EF4444; border-color: rgba(239,68,68,0.3); }
.blocker-input {
  width: 100%; font-size: 12px; color: var(--text-secondary); background: transparent;
  border: none; outline: none; resize: vertical; line-height: 1.5;
  font-family: inherit; box-sizing: border-box; min-height: 48px;
}
.blocker-input::placeholder { color: rgba(239,68,68,0.4); }

/* ── 진행률 ── */
.progress-card { background: var(--bg-card); border: 1px solid var(--border-color); border-radius: var(--radius-md); padding: 9px 12px; box-shadow: var(--shadow-xs); }
.today-progress { border-color: rgba(48,127,226,0.3); background: rgba(48,127,226,0.04); }
.progress-header { display: flex; justify-content: space-between; font-size: 11px; color: var(--text-secondary); margin-bottom: 6px; }
.progress-value  { font-weight: 700; font-size: 12px; color: var(--text-primary); }
.progress-bar    { height: 7px; background: var(--bg-hover); border-radius: var(--radius-full); overflow: hidden; }
.progress-fill   { height: 100%; border-radius: var(--radius-full); transition: width .6s ease-out; }
.today-fill      { background: var(--color-project); }
.progress-summary { display: flex; gap: 6px; font-size: 10px; color: var(--text-muted); margin-top: 5px; }

/* ── 태스크 추가 ── */
.add-task-btn {
  display: flex; align-items: center; gap: 6px;
  padding: 8px 10px; border: 1px dashed var(--border-color);
  border-radius: var(--radius-md); font-size: 12px; color: var(--text-muted);
  cursor: pointer; transition: all var(--transition-fast); background: transparent; font-family: inherit;
}
.add-task-btn:hover { border-color: var(--color-project); color: var(--color-project); background: rgba(48,127,226,0.04); }
.shortcut-hint {
  margin-left: auto; font-size: 9px; color: var(--text-muted);
  border: 1px solid var(--border-color); border-radius: var(--radius-xs);
  padding: 1px 4px; font-family: monospace;
}

.add-task-form {
  background: var(--bg-card); border: 1px solid var(--color-project);
  border-radius: var(--radius-md); padding: 9px 11px;
  display: flex; flex-direction: column; gap: 7px;
  box-shadow: 0 0 0 3px rgba(48,127,226,0.08);
}
.add-input {
  border: none; outline: none; font-size: 13px;
  color: var(--text-primary); width: 100%; font-family: inherit; background: transparent;
}
.add-form-opts { display: flex; align-items: center; gap: 5px; flex-wrap: wrap; }
.priority-btn {
  font-size: 10px; padding: 2px 7px; border-radius: var(--radius-sm);
  border: 1px solid var(--border-color); background: var(--bg-card);
  cursor: pointer; font-weight: 500; color: var(--text-secondary); transition: all var(--transition-fast); font-family: inherit;
}
.priority-btn.active.priority-btn-p1 { background: rgba(239,68,68,0.1);  color: #DC2626; border-color: rgba(239,68,68,0.25); }
.priority-btn.active.priority-btn-p2 { background: rgba(249,115,22,0.1); color: #EA580C; border-color: rgba(249,115,22,0.25); }
.priority-btn.active.priority-btn-p3 { background: var(--bg-hover); color: var(--text-secondary); border-color: var(--border-color); }
.kpi-check-label { font-size: 10px; color: var(--text-secondary); display: flex; align-items: center; gap: 4px; cursor: pointer; margin-left: auto; }
.time-est-inline { display: flex; align-items: center; gap: 4px; color: var(--text-muted); }
.time-est-icon   { color: var(--text-muted); flex-shrink: 0; }

.add-form-actions { display: flex; gap: 6px; justify-content: flex-end; padding-top: 4px; border-top: 1px solid var(--border-color); }
.add-submit-btn {
  font-size: 12px; font-weight: 600; padding: 4px 14px;
  border-radius: var(--radius-md); border: none; background: var(--color-project);
  color: white; cursor: pointer; transition: background var(--transition-fast); font-family: inherit;
}
.add-submit-btn:hover { background: #1D6FCC; }
.add-cancel-btn {
  font-size: 12px; padding: 4px 10px;
  border-radius: var(--radius-md); border: 1px solid var(--border-color);
  background: var(--bg-card); color: var(--text-secondary); cursor: pointer; transition: all var(--transition-fast); font-family: inherit;
}
.add-cancel-btn:hover { background: var(--bg-hover); }

/* ── 내일 KPI 예보 ── */
.forecast-card  { background: rgba(139,92,246,0.06); border: 1px solid rgba(139,92,246,0.2); border-radius: var(--radius-md); padding: 10px 12px; }
.forecast-title { font-size: 10px; font-weight: 700; color: #7C3AED; margin-bottom: 8px; letter-spacing: .05em; text-transform: uppercase; }
.forecast-row   { display: flex; align-items: center; gap: 7px; margin-bottom: 5px; }
.forecast-name  { font-size: 10px; color: var(--text-secondary); width: 70px; flex-shrink: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.forecast-bar-wrap { flex: 1; display: flex; align-items: center; gap: 5px; }
.forecast-bar  { flex: 1; height: 4px; background: rgba(139,92,246,0.15); border-radius: 2px; overflow: hidden; }
.forecast-fill { height: 100%; background: linear-gradient(90deg, #8B5CF6, #A78BFA); border-radius: 2px; transition: width .4s; }
.forecast-rate { font-size: 10px; font-weight: 600; color: #7C3AED; width: 28px; text-align: right; }

/* ── 주간 달성 요약 ── */
.weekly-summary-card  { background: var(--bg-card); border: 1px solid var(--border-color); border-radius: var(--radius-md); padding: 10px 12px; box-shadow: var(--shadow-xs); }
.weekly-summary-title { font-size: 10px; font-weight: 700; color: var(--text-secondary); margin-bottom: 8px; display: flex; align-items: center; gap: 5px; letter-spacing: .05em; text-transform: uppercase; }
.weekly-bars          { display: flex; align-items: flex-end; gap: 4px; height: 40px; }
.weekly-day-col       { flex: 1; display: flex; flex-direction: column; align-items: center; gap: 3px; height: 100%; }
.weekly-bar-track     { flex: 1; width: 100%; display: flex; align-items: flex-end; background: var(--bg-hover); border-radius: var(--radius-sm); overflow: hidden; }
.weekly-bar-fill      { width: 100%; border-radius: var(--radius-sm); transition: height .4s; }
.weekly-bar-fill.heat-0 { background: var(--border-color); }
.weekly-bar-fill.heat-1 { background: rgba(48,127,226,0.35); }
.weekly-bar-fill.heat-2 { background: var(--color-project); }
.weekly-bar-fill.heat-3 { background: #1D6FCC; }
.weekly-day-label     { font-size: 9px; color: var(--text-muted); white-space: nowrap; }
.weekly-day-label.today { color: var(--color-project); font-weight: 700; }

/* ── 일일 KPI 체크 바 ── */
.daily-kpi-bar { background: var(--bg-card); border-bottom: 1px solid var(--border-color); padding: 8px 20px; flex-shrink: 0; }
.dkpi-header   { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
.dkpi-header-icon { color: var(--color-kpi); flex-shrink: 0; }
.dkpi-title    { font-size: 11px; font-weight: 700; color: var(--color-kpi); letter-spacing: .05em; text-transform: uppercase; }
.dkpi-progress { font-size: 11px; font-weight: 700; color: var(--text-primary); margin-left: 2px; }
.dkpi-mini-bar { flex: 1; height: 4px; background: var(--border-color); border-radius: 2px; overflow: hidden; max-width: 120px; }
.dkpi-mini-fill { height: 100%; background: var(--color-kpi); border-radius: 2px; transition: width .4s ease; }
.dkpi-list { display: flex; flex-wrap: wrap; gap: 8px; }
.dkpi-item {
  display: flex; align-items: center; gap: 6px;
  background: var(--bg-hover); border: 1px solid var(--border-color);
  border-radius: var(--radius-md); padding: 5px 10px; transition: border-color var(--transition-fast);
}
.dkpi-item:has(.dkpi-bool-btn.done) { border-color: rgba(16,185,129,0.35); background: rgba(16,185,129,0.05); }
.dkpi-bool-btn {
  width: 20px; height: 20px; border-radius: var(--radius-sm);
  border: 2px solid var(--border-color); background: var(--bg-card);
  cursor: pointer; display: flex; align-items: center; justify-content: center;
  color: transparent; flex-shrink: 0; transition: all var(--transition-fast);
}
.dkpi-bool-btn.done { background: var(--color-kpi); border-color: var(--color-kpi); color: white; }
.dkpi-bool-btn:hover:not(:disabled):not(.done) { border-color: var(--color-kpi); }
.dkpi-bool-btn:disabled { opacity: 0.5; cursor: default; }
.dkpi-num-input {
  width: 56px; padding: 3px 6px; font-size: 12px;
  border: 1px solid var(--border-color); border-radius: var(--radius-md);
  text-align: right; outline: none; font-family: inherit;
  color: var(--text-primary); background: var(--bg-card); transition: border-color var(--transition-fast);
}
.dkpi-num-input:focus { border-color: var(--color-project); }
.dkpi-name { font-size: 12px; color: var(--text-secondary); font-weight: 500; white-space: nowrap; }
.dkpi-unit { font-size: 11px; color: var(--text-muted); }
.dkpi-rate-bar  { width: 40px; height: 4px; background: var(--border-color); border-radius: 2px; overflow: hidden; flex-shrink: 0; }
.dkpi-rate-fill { height: 100%; border-radius: 2px; transition: width .3s; }
.dkpi-rate-pct  { font-size: 10px; font-weight: 700; color: var(--text-secondary); }

/* ── 다크모드 ── */
html.dark .scrum-page { color: var(--text-primary); background: var(--bg-main); }
html.dark .scrum-header { background: var(--bg-card); border-color: var(--border-color); }
html.dark .focus-goal-wrap { background: var(--bg-hover); border-color: var(--border-color); }
html.dark .energy-btn { background: var(--bg-card); border-color: var(--border-color); }
html.dark .kpi-strip  { background: var(--bg-card); border-color: var(--border-color); }
html.dark .kpi-chip   { background: var(--bg-hover); border-color: var(--border-color); }
html.dark .task-card  { background: var(--bg-card); border-color: var(--border-color); }
html.dark .task-card.carried-over { background: rgba(245,158,11,0.06); border-color: rgba(245,158,11,0.2); }
html.dark .task-note-input { background: var(--bg-hover); border-color: var(--border-color); }
html.dark .task-note-input:focus { border-color: var(--color-project); background: var(--bg-card); }
html.dark .progress-card { background: var(--bg-card); border-color: var(--border-color); }
html.dark .today-progress { background: rgba(48,127,226,0.06); border-color: rgba(48,127,226,0.25); }
html.dark .col-yesterday .col-header { background: var(--bg-hover); border-color: var(--border-color); }
html.dark .col-today .col-header     { background: rgba(48,127,226,0.1); border-color: rgba(48,127,226,0.3); color: #93C5FD; }
html.dark .col-tomorrow .col-header  { background: rgba(139,92,246,0.1); border-color: rgba(139,92,246,0.3); color: #C4B5FD; }
html.dark .add-task-form  { background: var(--bg-card); }
html.dark .add-cancel-btn { background: var(--bg-hover); border-color: var(--border-color); }
html.dark .add-cancel-btn:hover { background: var(--bg-active); }
html.dark .time-est-input { background: var(--bg-card); border-color: var(--border-color); }
html.dark .severity-btn   { background: var(--bg-card); border-color: var(--border-color); }
html.dark .weekly-summary-card { background: var(--bg-card); border-color: var(--border-color); }
html.dark .dkpi-item { background: var(--bg-hover); border-color: var(--border-color); }
html.dark .priority-btn { background: var(--bg-card); border-color: var(--border-color); }
</style>
