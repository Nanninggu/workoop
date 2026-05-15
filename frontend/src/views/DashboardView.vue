<template>
  <div class="page-container">

    <!-- 스켈레톤 로딩 -->
    <template v-if="store.loading && !dashboard">
      <div class="stats-grid">
        <div v-for="i in 4" :key="i" class="stat-card">
          <div class="sk-icon"></div>
          <div class="sk-info">
            <div class="sk-val"></div>
            <div class="sk-label"></div>
          </div>
        </div>
      </div>
      <div class="period-summary-row">
        <div v-for="i in 3" :key="i" class="period-card">
          <div class="sk-text sk-w60 mb-2"></div>
          <div class="sk-val-lg mb-2"></div>
          <div class="sk-bar"></div>
        </div>
      </div>
      <div class="section-block">
        <div class="sk-text sk-w40 mb-4"></div>
        <div class="kpi-grid">
          <div v-for="i in 6" :key="i" class="kpi-summary-card" style="min-height:140px">
            <div class="sk-text sk-w60 mb-2"></div>
            <div class="sk-text sk-w80 mb-3"></div>
            <div class="sk-bar mb-2"></div>
            <div class="sk-text sk-w40"></div>
          </div>
        </div>
      </div>
    </template>

    <template v-else-if="dashboard">

      <!-- 새로고침 바 -->
      <div class="refresh-bar">
        <span class="last-updated-txt" v-if="lastUpdated">
          <Clock :size="13" /> {{ lastUpdatedLabel }}에 업데이트됨
        </span>
        <el-button size="small" :loading="refreshing" @click="refresh" class="refresh-btn">
          <RefreshCw :size="13" class="mr-1" /> 새로고침
        </el-button>
      </div>

      <!-- ① 상단 통계 카드 4종 -->
      <div class="stats-grid">
        <div class="stat-card">
          <div class="stat-icon" style="background:#EFF6FF; color:#3B82F6">
            <BarChart3 :size="22" />
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ dashboard.activeKpis }}</div>
            <div class="stat-label">활성 KPI</div>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon" style="background:#F0FDF4; color:#10B981">
            <CheckCircle2 :size="22" />
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ dashboard.recordedInPeriod }} / {{ dashboard.activeKpis }}</div>
            <div class="stat-label">기간 내 기록 완료</div>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon" style="background:#FFF7ED; color:#F59E0B">
            <Target :size="22" />
          </div>
          <div class="stat-info">
            <div class="stat-value-row">
              <span class="stat-value">{{ dashboard.overallAchievementRate }}%</span>
              <span
                v-if="!trendComparison.neutral"
                class="trend-badge"
                :class="trendComparison.improved ? 'trend-up' : 'trend-down'"
              >
                <TrendingUp v-if="trendComparison.improved" :size="12" />
                <TrendingDown v-else :size="12" />
                {{ Math.abs(trendComparison.diff) }}%
              </span>
            </div>
            <div class="stat-label">평균 달성률 <span class="trend-period">전주 대비</span></div>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon" style="background:#FDF4FF; color:#A855F7">
            <AlertCircle :size="22" />
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ dashboard.activeKpis - dashboard.recordedInPeriod }}</div>
            <div class="stat-label">미입력 KPI</div>
          </div>
        </div>
      </div>

      <!-- 🎖 연속 달성 경보 (CAT8: GAM-2) -->
      <div v-if="streakWarnings.length > 0" class="streak-warning-bar">
        <div class="sw-icon">⚠️</div>
        <div class="sw-content">
          <strong>연속 달성 위기!</strong>
          <span v-for="(w, i) in streakWarnings" :key="i" class="sw-kpi">
            {{ w.name }} ({{ w.streak }}일 연속)
          </span>
          <span class="sw-note">오늘 기록을 입력하면 스트릭을 유지할 수 있습니다.</span>
        </div>
        <router-link to="/records" class="sw-link">기록 입력 →</router-link>
      </div>

      <!-- 🏆 배지 알림 (CAT8: GAM-1) -->
      <div v-if="newBadges.length > 0" class="badge-notification">
        <div v-for="badge in newBadges" :key="badge.id" class="badge-item">
          <span class="badge-emoji">{{ badge.emoji }}</span>
          <div>
            <div class="badge-name">{{ badge.name }}</div>
            <div class="badge-desc">{{ badge.desc }}</div>
          </div>
        </div>
      </div>

      <!-- ② 오늘의 스크럼 현황 위젯 -->
      <div class="scrum-widget" v-if="todayScrum || todayCheckIn">
        <div class="scrum-widget-header">
          <div class="scrum-widget-title">
            <Zap :size="15" class="scrum-widget-icon" />
            오늘의 스크럼
          </div>
          <router-link to="/scrum" class="scrum-widget-link">열기 →</router-link>
        </div>
        <div class="scrum-widget-body">
          <!-- 에너지 + 집중목표 -->
          <div class="scrum-meta-row" v-if="todayCheckIn">
            <div class="scrum-energy-pips" v-if="todayCheckIn.energy">
              <span
                v-for="n in 5" :key="n"
                class="energy-pip"
                :class="{ active: todayCheckIn.energy >= n }"
              ></span>
            </div>
            <span class="scrum-focus" v-if="todayCheckIn.focus">
              {{ todayCheckIn.focus }}
            </span>
          </div>
          <!-- 태스크 진행률 -->
          <div class="scrum-task-row" v-if="todayScrum">
            <div class="scrum-task-counts">
              <span class="scrum-done-count">{{ scrumDoneCount }}</span>
              <span class="scrum-sep">/</span>
              <span class="scrum-total-count">{{ scrumTotalCount }}</span>
              <span class="scrum-count-label">완료</span>
            </div>
            <div class="scrum-task-bar">
              <div
                class="scrum-task-fill"
                :style="{ width: scrumRate + '%', background: scrumRate >= 80 ? '#10b981' : scrumRate >= 50 ? '#f59e0b' : '#3b82f6' }"
              ></div>
            </div>
            <span class="scrum-rate-pct">{{ scrumRate }}%</span>
          </div>
          <!-- 7일 미니 히스토리 -->
          <div class="scrum-history-row">
            <div
              v-for="day in scrumWeekHistory"
              :key="day.date"
              class="scrum-hist-col"
              :title="`${day.label}: ${day.rate}%`"
            >
              <div class="scrum-hist-track">
                <div
                  class="scrum-hist-fill"
                  :style="{
                    height: Math.max(day.rate, 5) + '%',
                    background: day.rate >= 80 ? '#10b981' : day.rate >= 50 ? '#3b82f6' : day.rate > 0 ? '#93c5fd' : '#e2e8f0'
                  }"
                ></div>
              </div>
              <span class="scrum-hist-label" :class="{ today: day.isToday }">{{ day.dayLabel }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- ③ STAR 노트 요약 위젯 -->
      <div class="star-widget" v-if="recentStarStories.length || true">
        <div class="star-widget-header">
          <div class="star-widget-title">
            <Star :size="15" class="star-widget-icon" />
            STAR 노트
          </div>
          <div class="star-widget-right">
            <span class="star-widget-count">{{ allStarStories.length }}개 스토리</span>
            <router-link to="/star" class="star-widget-link">전체 보기 →</router-link>
          </div>
        </div>

        <!-- LP 커버리지 미니바 -->
        <div class="star-lp-mini" v-if="allStarStories.length">
          <div
            v-for="lp in STAR_LP_OPTIONS"
            :key="lp.value"
            class="star-lp-dot"
            :class="{ covered: starCoveredLps.has(lp.value) }"
            :style="starCoveredLps.has(lp.value) ? { background: lp.color } : {}"
            :title="lp.label + ' · ' + lp.ko"
          ></div>
          <span class="star-lp-mini-label">{{ starCoveredLps.size }}/14 LP 커버</span>
        </div>

        <!-- 최근 스토리 목록 -->
        <div class="star-story-list" v-if="recentStarStories.length">
          <router-link
            v-for="story in recentStarStories"
            :key="story.id"
            to="/star"
            class="star-story-item"
          >
            <span
              v-if="story.lpTag"
              class="star-story-lp"
              :style="{ background: starLpColor(story.lpTag) + '22', color: starLpColor(story.lpTag), borderColor: starLpColor(story.lpTag) }"
            >{{ starLpShort(story.lpTag) }}</span>
            <span class="star-story-title">{{ story.title || '(제목 없음)' }}</span>
            <span class="star-story-date">{{ story.updatedAt }}</span>
          </router-link>
        </div>

        <!-- 빈 상태: STAR 방법론 설명 -->
        <div class="star-widget-empty" v-else>
          <div class="star-method-grid">
            <div class="star-method-item">
              <span class="star-method-letter" style="background:#3B82F6">S</span>
              <div class="star-method-body">
                <div class="star-method-name">Situation <span class="star-method-ko">상황</span></div>
                <div class="star-method-desc">배경 상황을 간략히 설명하여 면접관이 맥락을 이해하도록 함</div>
              </div>
            </div>
            <div class="star-method-item">
              <span class="star-method-letter" style="background:#F59E0B">T</span>
              <div class="star-method-body">
                <div class="star-method-name">Task <span class="star-method-ko">과제</span></div>
                <div class="star-method-desc">해결해야 했던 문제나 달성하고자 했던 목표를 명확히 기술</div>
              </div>
            </div>
            <div class="star-method-item">
              <span class="star-method-letter" style="background:#10B981">A</span>
              <div class="star-method-body">
                <div class="star-method-name">Action <span class="star-method-ko">행동</span></div>
                <div class="star-method-desc">본인이 직접 취한 구체적 행동 기술 — '우리'보다 <strong>'나'</strong>의 행동 강조</div>
              </div>
            </div>
            <div class="star-method-item">
              <span class="star-method-letter" style="background:#EC4899">R</span>
              <div class="star-method-body">
                <div class="star-method-name">Result <span class="star-method-ko">결과</span></div>
                <div class="star-method-desc">구체적인 <strong>수치화된 성과</strong> 및 배운 점(Learnings) 기술</div>
              </div>
            </div>
          </div>
          <div class="star-widget-cta">
            <span class="star-widget-cta-text">AWS 리더십 원칙 기반으로 나만의 경험을 정리해보세요</span>
            <router-link to="/star" class="star-widget-create-link">첫 스토리 작성 →</router-link>
          </div>
        </div>
      </div>

      <!-- ④ 주기별 현황 요약 (DAILY / WEEKLY / MONTHLY) -->
      <!-- 팀 현황 배너 -->
      <div v-if="orgStore.currentOrg" class="team-banner">
        <div class="team-banner-left">
          <span class="team-icon">🏢</span>
          <div>
            <div class="team-name">{{ orgStore.currentOrg.name }}</div>
            <div class="team-sub">팀 KPI를 관리하고 함께 성장하세요</div>
          </div>
        </div>
        <router-link to="/kpis" class="team-banner-btn">팀 KPI 보기 →</router-link>
      </div>
      <div v-else class="team-banner team-banner--empty">
        <span class="team-icon">👥</span>
        <div>
          <div class="team-name">아직 조직이 없어요</div>
          <div class="team-sub">조직을 만들고 팀원을 초대해 보세요</div>
        </div>
        <router-link to="/onboarding" class="team-banner-btn">조직 만들기 →</router-link>
      </div>

      <!-- 번아웃 배너 -->
      <BurnoutBanner />

      <div class="period-summary-row" v-if="dashboard.periodSummary">
        <div class="period-card daily">
          <div class="period-label">오늘 (매일)</div>
          <div class="period-counts">
            <span class="period-done">{{ dashboard.periodSummary.dailyRecorded }}</span>
            <span class="period-sep">/</span>
            <span class="period-total">{{ dashboard.periodSummary.dailyTotal }}</span>
          </div>
          <el-progress
            :percentage="pct(dashboard.periodSummary.dailyRecorded, dashboard.periodSummary.dailyTotal)"
            color="#3B82F6" :show-text="false" :stroke-width="6"
          />
        </div>
        <div class="period-card weekly">
          <div class="period-label">이번 주 (매주)</div>
          <div class="period-counts">
            <span class="period-done">{{ dashboard.periodSummary.weeklyRecorded }}</span>
            <span class="period-sep">/</span>
            <span class="period-total">{{ dashboard.periodSummary.weeklyTotal }}</span>
          </div>
          <el-progress
            :percentage="pct(dashboard.periodSummary.weeklyRecorded, dashboard.periodSummary.weeklyTotal)"
            color="#10B981" :show-text="false" :stroke-width="6"
          />
        </div>
        <div class="period-card monthly">
          <div class="period-label">이번 달 (매월)</div>
          <div class="period-counts">
            <span class="period-done">{{ dashboard.periodSummary.monthlyRecorded }}</span>
            <span class="period-sep">/</span>
            <span class="period-total">{{ dashboard.periodSummary.monthlyTotal }}</span>
          </div>
          <el-progress
            :percentage="pct(dashboard.periodSummary.monthlyRecorded, dashboard.periodSummary.monthlyTotal)"
            color="#F59E0B" :show-text="false" :stroke-width="6"
          />
        </div>
      </div>

      <!-- ③ 이번 주 하이라이트 (D번) -->
      <div class="highlights-row" v-if="weekHighlights">
        <!-- 최고 달성 KPI -->
        <div class="highlight-card top">
          <div class="highlight-icon"><Trophy :size="16" /></div>
          <div class="highlight-body">
            <div class="highlight-label">최고 달성</div>
            <div class="highlight-name">{{ weekHighlights.topKpi?.kpiName }}</div>
            <div class="highlight-value">{{ weekHighlights.topKpi?.achievementRate }}%</div>
          </div>
        </div>
        <!-- 최장 연속 달성 -->
        <div class="highlight-card streak" v-if="weekHighlights.bestStreak">
          <div class="highlight-icon"><Flame :size="16" /></div>
          <div class="highlight-body">
            <div class="highlight-label">최장 연속</div>
            <div class="highlight-name">{{ weekHighlights.bestStreak.kpiName }}</div>
            <div class="highlight-value">{{ weekHighlights.bestStreak.streak }}일 연속</div>
          </div>
        </div>
        <!-- 완료율 -->
        <div class="highlight-card completion">
          <div class="highlight-icon"><CheckCircle2 :size="16" /></div>
          <div class="highlight-body">
            <div class="highlight-label">기간 내 완료율</div>
            <div class="highlight-name">{{ weekHighlights.total - weekHighlights.missed.length }} / {{ weekHighlights.total }} KPI 완료</div>
            <div class="highlight-value">{{ Math.round((weekHighlights.total - weekHighlights.missed.length) / weekHighlights.total * 100) }}%</div>
          </div>
        </div>
        <!-- 미완료 KPI 경고 -->
        <div class="highlight-card missed" v-if="weekHighlights.missed.length > 0">
          <div class="highlight-icon"><AlertTriangle :size="16" /></div>
          <div class="highlight-body">
            <div class="highlight-label">미완료 KPI</div>
            <div class="highlight-missed-list">
              <span v-for="k in weekHighlights.missed.slice(0, 3)" :key="k.kpiId" class="missed-chip">
                {{ k.kpiName }}
              </span>
              <span v-if="weekHighlights.missed.length > 3" class="missed-chip more">
                +{{ weekHighlights.missed.length - 3 }}개
              </span>
            </div>
          </div>
        </div>
      </div>

      <!-- ④ 14일 성과 추이 차트 -->
      <div class="section-block" v-if="dashboard.trendPoints?.length">
        <div class="section-header">
          <h2 class="section-title">최근 14일 성과 추이</h2>
          <span class="section-sub">입력된 기록의 평균 달성률</span>
        </div>
        <v-chart :option="trendChartOption" style="height:220px; width:100%" autoresize />
      </div>

      <!-- ④ KPI 현황 카드 (카테고리별 그룹핑) -->
      <div class="section-block">
        <div class="kpi-section-header">
          <h2 class="section-title">KPI 현황</h2>
          <div class="kpi-section-controls">
            <!-- 정렬 옵션 (C번) -->
            <div class="sort-group">
              <button
                v-for="opt in sortOptions" :key="opt.value"
                class="sort-opt-btn"
                :class="{ active: kpiSortOrder === opt.value }"
                @click="kpiSortOrder = opt.value"
              >{{ opt.label }}</button>
            </div>
            <el-button size="small" @click="exportCsv">
              <Download :size="14" class="mr-1" /> CSV
            </el-button>
            <router-link to="/records">
              <el-button type="primary" size="small">
                <Plus :size="14" class="mr-1" /> 실적 입력
              </el-button>
            </router-link>
          </div>
        </div>

        <!-- 카테고리별 그룹 -->
        <div class="category-kpi-groups">
          <div
            v-for="group in groupedKpiSummaries"
            :key="group.categoryName"
            class="category-kpi-group"
          >
            <!-- 카테고리 헤더 -->
            <div class="category-kpi-header">
              <span class="cat-group-dot" :style="{ backgroundColor: group.categoryColor }"></span>
              <span class="cat-group-name">{{ group.categoryName }}</span>
              <span class="cat-group-count">{{ group.kpis.length }}개</span>
              <span class="cat-group-done" :style="{ color: group.categoryColor }">
                {{ group.kpis.filter(k => k.recordedInPeriod).length }} / {{ group.kpis.length }} 완료
              </span>
            </div>

            <div class="kpi-grid">
              <div
                v-for="kpi in group.kpis"
                :key="kpi.kpiId"
                class="kpi-summary-card"
                :class="{
                  recorded: kpi.recordedInPeriod,
                  perfect: kpi.achievementRate >= 100 && kpi.kpiType !== 'BOOLEAN',
                  'bool-done': kpi.kpiType === 'BOOLEAN' && kpi.latestBooleanValue
                }"
              >
                <div class="kpi-card-header">
                  <div class="kpi-badges">
                    <span class="period-chip">{{ kpi.periodLabel }}</span>
                    <span v-if="kpi.recordedInPeriod" class="recorded-badge">
                      <Check :size="11" /> 완료
                    </span>
                  </div>
                </div>

                <!-- KPI 이름 → 상세 페이지 링크 -->
                <div class="kpi-card-name">
                  <router-link :to="`/kpis/${kpi.kpiId}`" class="kpi-name-link">
                    {{ kpi.kpiName }}
                  </router-link>
                </div>

                <!-- Streak 배지 -->
                <div v-if="kpi.streak > 0" class="streak-badge">
                  <Flame :size="13" /> {{ kpi.streak }}일 연속
                </div>

                <!-- BOOLEAN -->
                <div v-if="kpi.kpiType === 'BOOLEAN'" class="kpi-boolean">
                  <div class="boolean-indicator" :class="kpi.latestBooleanValue ? 'done' : 'pending'">
                    <component :is="kpi.latestBooleanValue ? Check : Minus" :size="16" />
                    {{ kpi.latestBooleanValue ? '달성' : '미달성' }}
                  </div>
                </div>

                <!-- NUMERIC / PERCENTAGE -->
                <template v-else>
                  <div class="kpi-values">
                    <span class="actual-value">
                      {{ kpi.latestValue != null ? Number(kpi.latestValue).toLocaleString() : '-' }}
                    </span>
                    <span class="target-value">/ {{ Number(kpi.targetValue).toLocaleString() }} {{ kpi.unit }}</span>
                  </div>
                  <el-progress
                    :percentage="Math.min(kpi.achievementRate, 100)"
                    :color="progressColor(kpi.achievementRate)"
                    :show-text="false" :stroke-width="7" class="mt-2"
                  />
                  <div class="achievement-text">
                    {{ kpi.achievementRate }}% 달성
                    <span v-if="kpi.achievementRate >= 100" class="perfect-badge">
                      <Star :size="11" /> 목표 달성!
                    </span>
                  </div>
                </template>

                <!-- 빠른 입력 버튼 -->
                <button class="quick-input-btn" @click="openQuickInput(kpi)">
                  <Edit3 :size="13" /> 빠른 입력
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- ⑤ 카테고리별 달성률 -->
      <div class="section-block">
        <h2 class="section-title mb-4">카테고리별 달성률</h2>
        <div class="category-summary-grid">
          <div
            v-for="cat in dashboard.categorySummaries.filter(c => c.totalKpis > 0)"
            :key="cat.categoryId"
            class="category-card"
          >
            <div class="category-header">
              <div class="cat-dot" :style="{ backgroundColor: cat.categoryColor }"></div>
              <span class="cat-name">{{ cat.categoryName }}</span>
              <span class="cat-count">{{ cat.totalKpis }}개</span>
              <span class="cat-rate" :style="{ color: cat.categoryColor }">
                {{ cat.avgAchievementRate }}%
              </span>
            </div>
            <el-progress
              :percentage="cat.avgAchievementRate"
              :color="cat.categoryColor"
              :stroke-width="10"
            />
          </div>
        </div>
      </div>

    </template> <!-- end v-else-if="dashboard" -->

    <!-- 빠른 입력 다이얼로그 -->
    <el-dialog
      v-model="quickInputVisible"
      :title="quickKpi ? `빠른 입력 — ${quickKpi.kpiName}` : ''"
      width="420px"
      destroy-on-close
    >
      <template v-if="quickKpi">
        <div class="quick-dialog-meta">
          <span class="q-cat" :style="{ background: quickKpi.categoryColor+'22', color: quickKpi.categoryColor }">
            {{ quickKpi.categoryName }}
          </span>
          <span class="q-chip">{{ quickKpi.periodLabel }}</span>
          <span v-if="quickKpi.targetValue" class="q-chip">
            목표: {{ Number(quickKpi.targetValue).toLocaleString() }} {{ quickKpi.unit }}
          </span>
        </div>

        <!-- 날짜 선택 -->
        <div class="quick-date-row">
          <span class="quick-date-label">날짜</span>
          <el-date-picker
            v-model="quickDate"
            type="date"
            format="YYYY년 MM월 DD일"
            value-format="YYYY-MM-DD"
            :clearable="false"
            :disabled-date="(d) => d > new Date()"
            style="flex:1"
            @change="onQuickDateChange"
          />
        </div>

        <!-- BOOLEAN -->
        <div v-if="quickKpi.kpiType === 'BOOLEAN'" class="quick-input-field">
          <el-switch
            v-model="quickForm.booleanValue"
            active-text="달성" inactive-text="미달성" active-color="#10B981"
            size="large"
          />
        </div>

        <!-- NUMERIC / PERCENTAGE -->
        <div v-else class="quick-input-field">
          <el-input-number
            v-model="quickForm.actualValue"
            :min="0"
            :precision="quickKpi.kpiType === 'PERCENTAGE' ? 1 : 0"
            :max="quickKpi.kpiType === 'PERCENTAGE' ? 100 : undefined"
            controls-position="right"
            style="width:100%"
            size="large"
          />
          <span v-if="quickKpi.unit" class="q-unit">{{ quickKpi.unit }}</span>
          <!-- 달성률 미리보기 -->
          <div v-if="quickKpi.targetValue && quickForm.actualValue != null" class="quick-rate-preview">
            <el-progress
              :percentage="Math.min(quickForm.actualValue / quickKpi.targetValue * 100, 100)"
              :color="progressColor(quickForm.actualValue / quickKpi.targetValue * 100)"
              :show-text="false" :stroke-width="8"
            />
            <span>{{ Math.min(Math.round(quickForm.actualValue / quickKpi.targetValue * 100), 100) }}% 달성</span>
          </div>
        </div>

        <el-input
          v-model="quickForm.note"
          placeholder="메모 (선택)"
          class="mt-3"
          clearable
        />
      </template>

      <template #footer>
        <el-button @click="quickInputVisible = false">취소</el-button>
        <el-button type="primary" @click="saveQuickInput" :loading="quickSaving">저장</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useKpiStore } from '@/store/kpiStore'
import { useOrgStore } from '@/store/orgStore'
import { useAuthStore } from '@/store/authStore'
import { useBurnoutStore } from '@/store/burnoutStore'
import { recordApi, exportApi } from '@/api/kpiApi'
import BurnoutBanner from '@/components/BurnoutBanner.vue'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'

import { use } from 'echarts/core'
import { LineChart, BarChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import VChart from 'vue-echarts'

import {
  BarChart3, CheckCircle2, Target, AlertCircle,
  Check, Minus, Plus, Flame, Edit3, Download,
  TrendingUp, TrendingDown, Trophy, AlertTriangle, Star,
  RefreshCw, Clock, Zap
} from 'lucide-vue-next'

const store       = useKpiStore()
const orgStore    = useOrgStore()
const authStore   = useAuthStore()
const burnout     = useBurnoutStore()

const STAR_LP_OPTIONS = [
  { value: 'customer_obsession',  label: 'Customer Obsession',              ko: '고객 집착',     color: '#3B82F6' },
  { value: 'ownership',           label: 'Ownership',                       ko: '주인의식',      color: '#10B981' },
  { value: 'invent_simplify',     label: 'Invent and Simplify',             ko: '발명과 단순화',  color: '#8B5CF6' },
  { value: 'are_right',           label: 'Are Right, A Lot',                ko: '높은 판단력',   color: '#F59E0B' },
  { value: 'learn_curious',       label: 'Learn and Be Curious',            ko: '학습과 호기심',  color: '#06B6D4' },
  { value: 'hire_develop',        label: 'Hire and Develop the Best',       ko: '최고 인재',     color: '#EC4899' },
  { value: 'highest_standards',   label: 'Insist on the Highest Standards', ko: '최고 기준',     color: '#EF4444' },
  { value: 'think_big',           label: 'Think Big',                       ko: '큰 그림',      color: '#6366F1' },
  { value: 'bias_action',         label: 'Bias for Action',                 ko: '행동 편향',     color: '#F97316' },
  { value: 'frugality',           label: 'Frugality',                       ko: '절약',         color: '#84CC16' },
  { value: 'earn_trust',          label: 'Earn Trust',                      ko: '신뢰 구축',     color: '#14B8A6' },
  { value: 'dive_deep',           label: 'Dive Deep',                       ko: '깊이 파고들기',  color: '#A855F7' },
  { value: 'backbone',            label: 'Have Backbone',                   ko: '원칙 고수',     color: '#DC2626' },
  { value: 'deliver_results',     label: 'Deliver Results',                 ko: '성과 달성',     color: '#059669' },
]

use([LineChart, BarChart, GridComponent, TooltipComponent, LegendComponent, CanvasRenderer])

const dashboard = computed(() => store.dashboard)

// 카테고리별 그룹핑 + 정렬 (7번, C번)
const kpiSortOrder = ref('default') // 'default' | 'rate_desc' | 'rate_asc' | 'incomplete_first'

function sortKpis(kpis) {
  switch (kpiSortOrder.value) {
    case 'rate_desc':        return [...kpis].sort((a, b) => b.achievementRate - a.achievementRate)
    case 'rate_asc':         return [...kpis].sort((a, b) => a.achievementRate - b.achievementRate)
    case 'incomplete_first': return [...kpis].sort((a, b) => (a.recordedInPeriod ? 1 : 0) - (b.recordedInPeriod ? 1 : 0))
    default:                 return kpis
  }
}

const groupedKpiSummaries = computed(() => {
  if (!dashboard.value?.kpiSummaries) return []
  const groups = {}
  dashboard.value.kpiSummaries.forEach(kpi => {
    const key = kpi.categoryName
    if (!groups[key]) {
      groups[key] = { categoryName: kpi.categoryName, categoryColor: kpi.categoryColor, kpis: [] }
    }
    groups[key].kpis.push(kpi)
  })
  return Object.values(groups).map(g => ({ ...g, kpis: sortKpis(g.kpis) }))
})

// 전주 대비 트렌드: 실제 캘린더 주(월~일) 기준 이번 주 vs 지난 주 평균 달성률 비교
const trendComparison = computed(() => {
  const points = dashboard.value?.trendPoints || []
  if (points.length === 0) return { diff: 0, neutral: true }

  // 이번 주 월요일 계산 (dayjs 기본 week: 일요일 시작 → day()===0이면 일요일)
  const today = dayjs()
  const dayOfWeek = today.day() // 0=일, 1=월, ..., 6=토
  const daysFromMonday = dayOfWeek === 0 ? 6 : dayOfWeek - 1
  const thisMonday = today.subtract(daysFromMonday, 'day').format('YYYY-MM-DD')
  const lastMonday = dayjs(thisMonday).subtract(7, 'day').format('YYYY-MM-DD')
  const lastSunday  = dayjs(thisMonday).subtract(1, 'day').format('YYYY-MM-DD')

  const withData = (arr) => arr.filter(p => p.recordedCount > 0)
  const avg = (arr) => {
    const valid = withData(arr)
    return valid.length === 0 ? 0 : valid.reduce((s, p) => s + p.avgAchievementRate, 0) / valid.length
  }

  const thisWeekPts = points.filter(p => p.date >= thisMonday)
  const lastWeekPts = points.filter(p => p.date >= lastMonday && p.date <= lastSunday)

  // 두 기간 모두 데이터 없으면 neutral
  if (withData(thisWeekPts).length === 0 && withData(lastWeekPts).length === 0) {
    return { diff: 0, neutral: true }
  }

  const diff = Math.round(avg(thisWeekPts) - avg(lastWeekPts))
  return { diff, improved: diff > 0, declined: diff < 0, neutral: diff === 0 }
})

// 이번 주 하이라이트 (D번)
const weekHighlights = computed(() => {
  const kpis = dashboard.value?.kpiSummaries || []
  if (kpis.length === 0) return null
  const topKpi = [...kpis].sort((a, b) => b.achievementRate - a.achievementRate)[0]
  const bestStreak = [...kpis].filter(k => k.streak > 0).sort((a, b) => b.streak - a.streak)[0]
  const missed = kpis.filter(k => !k.recordedInPeriod)
  const perfect = kpis.filter(k => k.achievementRate >= 100)
  return { topKpi, bestStreak, missed, perfect, total: kpis.length }
})

// ── CAT8: 게이미피케이션 ──
// 연속 달성 위기 경보: streak > 0인데 오늘 미기록인 DAILY KPI
const streakWarnings = computed(() => {
  const kpis = dashboard.value?.kpiSummaries || []
  return kpis.filter(k =>
    k.frequency === 'DAILY' && k.streak > 0 && !k.recordedToday
  ).map(k => ({ name: k.kpiName, streak: k.streak }))
})

// 배지 시스템 (localStorage 기반)
const BADGE_KEY = 'workoop-badges'
const newBadges = ref([])

const badgeDefinitions = [
  { id: 'first_kpi',   emoji: '🥇', name: '첫 KPI 달성', desc: '첫 번째 KPI 기록을 완료했습니다!',        check: (d) => d.totalKpis >= 1 },
  { id: 'streak_7',    emoji: '🔥', name: '7일 연속 달성', desc: '7일 연속으로 KPI를 달성했습니다!',       check: (d) => (d.kpiSummaries || []).some(k => k.streak >= 7) },
  { id: 'streak_30',   emoji: '💎', name: '30일 연속 달성', desc: '30일 연속 달성! 놀라운 꾸준함입니다!',  check: (d) => (d.kpiSummaries || []).some(k => k.streak >= 30) },
  { id: 'perfect_week',emoji: '⭐', name: '완벽한 한 주',   desc: '이번 주 모든 KPI를 100% 달성했습니다!', check: (d) => d.overallAchievementRate >= 100 },
  { id: 'active_10',   emoji: '🚀', name: '10개 KPI 운영', desc: '10개 이상의 KPI를 관리하고 있습니다!',   check: (d) => d.activeKpis >= 10 },
]

function checkBadges() {
  const d = dashboard.value
  if (!d) return
  const earned = JSON.parse(localStorage.getItem(BADGE_KEY) || '[]')
  const freshBadges = []
  badgeDefinitions.forEach(def => {
    if (!earned.includes(def.id) && def.check(d)) {
      earned.push(def.id)
      freshBadges.push(def)
    }
  })
  if (freshBadges.length > 0) {
    localStorage.setItem(BADGE_KEY, JSON.stringify(earned))
    newBadges.value = freshBadges
    setTimeout(() => { newBadges.value = [] }, 8000)
  }
}

// 빠른 입력 상태
const quickInputVisible = ref(false)
const quickKpi = ref(null)
const quickDate = ref(dayjs().format('YYYY-MM-DD'))
const quickForm = ref({ actualValue: null, booleanValue: false, note: '' })
const quickSaving = ref(false)
const quickDateLoading = ref(false)

// 마지막 업데이트 시간 (ux2)
const lastUpdated  = ref(null)
const refreshing   = ref(false)

const lastUpdatedLabel = computed(() => {
  if (!lastUpdated.value) return ''
  const diff = Math.floor((Date.now() - lastUpdated.value) / 1000)
  if (diff < 10)   return '방금'
  if (diff < 60)   return `${diff}초 전`
  if (diff < 3600) return `${Math.floor(diff / 60)}분 전`
  return dayjs(lastUpdated.value).format('HH:mm')
})

async function refresh() {
  refreshing.value = true
  try {
    await store.fetchDashboard()
    lastUpdated.value = Date.now()
  } finally {
    refreshing.value = false
  }
}

// ── STAR 위젯 ──
const allStarStories = computed(() => {
  try { return JSON.parse(localStorage.getItem('workoop-star-stories')) || [] } catch { return [] }
})
const recentStarStories = computed(() =>
  [...allStarStories.value]
    .sort((a, b) => b.updatedAt.localeCompare(a.updatedAt))
    .slice(0, 3)
)
const starCoveredLps = computed(() => new Set(allStarStories.value.map(s => s.lpTag).filter(Boolean)))

function starLpColor(val) {
  return STAR_LP_OPTIONS.find(l => l.value === val)?.color || '#94A3B8'
}
function starLpShort(val) {
  const lp = STAR_LP_OPTIONS.find(l => l.value === val)
  return lp ? lp.ko : ''
}

// ── 스크럼 위젯 ──
const TODAY = dayjs().format('YYYY-MM-DD')

const todayScrum = computed(() => {
  try { return JSON.parse(localStorage.getItem(`workoop-scrum-${TODAY}`)) || null } catch { return null }
})
const todayCheckIn = computed(() => {
  try { return JSON.parse(localStorage.getItem(`workoop-checkin-${TODAY}`)) || null } catch { return null }
})

const scrumDoneCount  = computed(() => (todayScrum.value?.tasks || []).filter(t => t.done).length)
const scrumTotalCount = computed(() => (todayScrum.value?.tasks || []).length)
const scrumRate = computed(() =>
  scrumTotalCount.value === 0 ? 0 : Math.round(scrumDoneCount.value / scrumTotalCount.value * 100)
)

const scrumWeekHistory = computed(() =>
  Array.from({ length: 7 }, (_, i) => {
    const d = dayjs().subtract(6 - i, 'day')
    const date = d.format('YYYY-MM-DD')
    let tasks = []
    try { tasks = JSON.parse(localStorage.getItem(`workoop-scrum-${date}`))?.tasks || [] } catch { /* */ }
    const rate = tasks.length === 0 ? 0 : Math.round(tasks.filter(t => t.done).length / tasks.length * 100)
    return { date, label: d.format('M/D'), dayLabel: d.format('dd'), rate, isToday: date === TODAY }
  })
)

onMounted(async () => {
  refreshing.value = true
  try {
    await store.fetchDashboard()
    lastUpdated.value = Date.now()
    checkBadges()
    // 번아웃 분석 (비동기 — 대시보드 로딩 차단 안 함)
    burnout.analyzeMyBurnout()
    if (orgStore.currentOrg) {
      burnout.analyzeTeamBurnout(orgStore.currentOrg.id)
    }
  } finally {
    refreshing.value = false
  }
})

// 추이 차트 옵션
const trendChartOption = computed(() => {
  const points = dashboard.value?.trendPoints || []
  const dates  = points.map(p => dayjs(p.date).format('MM/DD'))
  const rates  = points.map(p => p.avgAchievementRate)
  return {
    tooltip: {
      trigger: 'axis',
      formatter: (params) => {
        const d = points[params[0].dataIndex]
        return `${dayjs(d.date).format('MM월 DD일')}<br/>
                평균 달성률: <b>${d.avgAchievementRate}%</b><br/>
                기록 건수: ${d.recordedCount}건`
      }
    },
    grid: { left: 40, right: 20, top: 16, bottom: 36 },
    xAxis: {
      type: 'category', data: dates,
      axisLabel: { fontSize: 11, color: '#94A3B8' },
      axisLine: { lineStyle: { color: '#E2E8F0' } }
    },
    yAxis: {
      type: 'value', min: 0, max: 100,
      axisLabel: { formatter: '{value}%', fontSize: 11, color: '#94A3B8' },
      splitLine: { lineStyle: { color: '#F1F5F9' } }
    },
    series: [{
      name: '평균 달성률',
      type: 'line',
      data: rates,
      smooth: true,
      lineStyle: { color: '#3B82F6', width: 3 },
      itemStyle: { color: '#3B82F6' },
      areaStyle: {
        color: {
          type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [
            { offset: 0, color: 'rgba(59,130,246,0.25)' },
            { offset: 1, color: 'rgba(59,130,246,0.02)' }
          ]
        }
      },
      symbol: 'circle',
      symbolSize: (val) => val > 0 ? 7 : 4
    }]
  }
})

function openQuickInput(kpi) {
  quickKpi.value = kpi
  quickDate.value = dayjs().format('YYYY-MM-DD')
  // 오늘 날짜 기준으로 기존 값 세팅
  quickForm.value = {
    actualValue: kpi.latestValue != null ? Number(kpi.latestValue) : null,
    booleanValue: kpi.latestBooleanValue ?? false,
    note: ''
  }
  quickInputVisible.value = true
}

// 날짜 변경 시 해당 날짜 기존 기록 조회 (5번)
async function onQuickDateChange(newDate) {
  if (!quickKpi.value) return
  quickDateLoading.value = true
  try {
    const res = await recordApi.getByDate(newDate)
    const record = (res.data || []).find(r => r.kpiId === quickKpi.value.kpiId)
    quickForm.value = {
      actualValue: record?.actualValue != null ? Number(record.actualValue) : null,
      booleanValue: record?.booleanValue ?? false,
      note: record?.note ?? ''
    }
  } catch {
    quickForm.value = { actualValue: null, booleanValue: false, note: '' }
  } finally {
    quickDateLoading.value = false
  }
}

async function saveQuickInput() {
  quickSaving.value = true
  try {
    await recordApi.upsert({
      kpiId: quickKpi.value.kpiId,
      actualValue: quickKpi.value.kpiType !== 'BOOLEAN' ? quickForm.value.actualValue : null,
      booleanValue: quickKpi.value.kpiType === 'BOOLEAN' ? quickForm.value.booleanValue : null,
      recordedDate: quickDate.value,
      note: quickForm.value.note || null
    })
    const dateLabel = quickDate.value === dayjs().format('YYYY-MM-DD') ? '오늘' : quickDate.value
    ElMessage.success(`${dateLabel} 실적이 저장되었습니다.`)
    quickInputVisible.value = false
    await store.fetchDashboard()
  } finally {
    quickSaving.value = false
  }
}

function exportCsv() {
  exportApi.csv(
    dayjs().subtract(1, 'month').format('YYYY-MM-DD'),
    dayjs().format('YYYY-MM-DD')
  )
  ElMessage.success('CSV 파일을 다운로드합니다.')
}

const sortOptions = [
  { label: '기본순', value: 'default' },
  { label: '달성↑', value: 'rate_desc' },
  { label: '달성↓', value: 'rate_asc' },
  { label: '미완료먼저', value: 'incomplete_first' }
]

const pct = (done, total) => total === 0 ? 0 : Math.round(done / total * 100)
const progressColor = (r) => r >= 100 ? '#F59E0B' : r >= 80 ? '#10B981' : r >= 50 ? '#F59E0B' : '#EF4444'
</script>

<style scoped>
.page-container { padding: 24px 28px; }

.stat-card { display: flex; align-items: center; gap: 16px; }

/* ── 팀 배너 ── */
.team-banner {
  display: flex; align-items: center; justify-content: space-between;
  padding: 14px 18px; margin-bottom: 16px;
  background: rgba(48,127,226,0.05);
  border: 1px solid rgba(48,127,226,0.18);
  border-radius: var(--radius-lg);
  gap: 12px;
}
.team-banner--empty { background: var(--bg-hover); border-color: var(--border-color); }
.team-banner-left { display: flex; align-items: center; gap: 12px; }
.team-icon { font-size: 1.4rem; }
.team-name { font-size: 14px; font-weight: 700; color: var(--text-primary); }
.team-sub  { font-size: 12px; color: var(--text-secondary); margin-top: 2px; }
.team-banner-btn {
  padding: 6px 14px; background: var(--color-project); color: white;
  border-radius: var(--radius-md); font-size: 13px; font-weight: 600;
  text-decoration: none; white-space: nowrap; transition: background var(--transition-base);
}
.team-banner-btn:hover { background: #1D6FCC; }

/* ── 새로고침 바 ── */
.refresh-bar {
  display: flex; align-items: center; justify-content: flex-end;
  gap: 10px; margin-bottom: 12px;
}
.last-updated-txt {
  display: flex; align-items: center; gap: 5px;
  font-size: 12px; color: var(--text-muted);
}
.refresh-btn { color: var(--text-secondary); }

/* ── 상단 통계 ── */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  margin-bottom: 14px;
}
@media (max-width: 1024px) { .stats-grid { grid-template-columns: repeat(2,1fr); } }

.stat-card {
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  padding: 16px 18px;
  display: flex; align-items: center; gap: 14px;
  border: 1px solid var(--border-color);
  box-shadow: var(--shadow-xs);
  transition: border-color var(--transition-base), box-shadow var(--transition-base);
}
.stat-card:hover { border-color: var(--gray-300); box-shadow: var(--shadow-sm); }
.stat-icon {
  width: 38px; height: 38px; border-radius: var(--radius-md);
  display: flex; align-items: center; justify-content: center; flex-shrink: 0;
}
.stat-value {
  font-size: 22px; font-weight: 700; color: var(--text-primary); line-height: 1;
  font-family: 'JetBrains Mono', 'SF Mono', monospace;
  font-variant-numeric: tabular-nums;
}
.stat-label { font-size: 12px; color: var(--text-secondary); margin-top: 4px; font-weight: 500; }

/* ── 주기별 요약 ── */
.period-summary-row {
  display: grid; grid-template-columns: repeat(3,1fr);
  gap: 12px; margin-bottom: 14px;
}
.period-card {
  background: var(--bg-card); border-radius: var(--radius-lg);
  padding: 14px 16px; border: 1px solid var(--border-color);
  box-shadow: var(--shadow-xs); transition: border-color var(--transition-base);
}
.period-card.daily   { border-top: 3px solid var(--color-project); }
.period-card.weekly  { border-top: 3px solid var(--color-kpi); }
.period-card.monthly { border-top: 3px solid var(--color-warning); }
.period-label { font-size: 12px; color: var(--text-secondary); font-weight: 600; margin-bottom: 8px; }
.period-counts { display: flex; align-items: baseline; gap: 3px; margin-bottom: 10px; }
.period-done  { font-size: 26px; font-weight: 800; color: var(--text-primary); font-family: 'JetBrains Mono', monospace; }
.period-sep   { font-size: 16px; color: var(--gray-300); }
.period-total { font-size: 16px; color: var(--text-muted); }

/* ── 섹션 공통 ── */
.section-block {
  background: var(--bg-card); border-radius: var(--radius-lg);
  padding: 18px 20px; margin-bottom: 14px;
  border: 1px solid var(--border-color); box-shadow: var(--shadow-xs);
  transition: border-color var(--transition-base);
}
.section-header { display: flex; align-items: baseline; gap: 10px; margin-bottom: 14px; }
.section-title  { font-size: 15px; font-weight: 600; color: var(--text-primary); letter-spacing: -0.01em; }
.section-sub    { font-size: 12px; color: var(--text-muted); }

/* ── KPI 카드 그리드 ── */
.kpi-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(220px, 1fr)); gap: 10px; }
.kpi-summary-card {
  border: 1px solid var(--border-color); border-radius: var(--radius-lg);
  padding: 12px 14px; background: var(--bg-card);
  transition: border-color var(--transition-base), box-shadow var(--transition-base);
  position: relative; box-shadow: var(--shadow-xs);
}
.kpi-summary-card.recorded { border-color: rgba(38,201,129,0.35); background: rgba(38,201,129,0.03); }
.kpi-summary-card:hover { border-color: var(--gray-300); box-shadow: var(--shadow-sm); }

.kpi-card-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 7px; }
.category-badge { font-size: 10px; font-weight: 600; padding: 2px 7px; border-radius: var(--radius-sm); }
.kpi-badges { display: flex; align-items: center; gap: 4px; }
.period-chip {
  font-size: 10px; color: var(--text-muted);
  background: var(--bg-hover); padding: 2px 6px; border-radius: var(--radius-sm);
}
.recorded-badge {
  display: flex; align-items: center; gap: 3px;
  font-size: 10px; font-weight: 600;
  color: #0D9C6E; background: rgba(16,185,129,0.12); padding: 2px 7px; border-radius: var(--radius-sm);
}
.kpi-card-name { font-size: 13px; font-weight: 600; color: var(--text-primary); margin-bottom: 6px; line-height: 1.3; }

.streak-badge {
  display: inline-flex; align-items: center; gap: 3px;
  font-size: 10px; font-weight: 700;
  color: #D97706; background: rgba(245,158,11,0.12); padding: 2px 7px; border-radius: var(--radius-sm);
  margin-bottom: 7px;
}

.kpi-values { display: flex; align-items: baseline; gap: 4px; }
.actual-value { font-size: 20px; font-weight: 700; color: var(--text-primary); font-family: 'JetBrains Mono', monospace; font-variant-numeric: tabular-nums; }
.target-value { font-size: 12px; color: var(--text-muted); }
.achievement-text { font-size: 11px; color: var(--text-secondary); margin-top: 4px; text-align: right; }

.boolean-indicator {
  display: flex; align-items: center; gap: 6px;
  font-size: 13px; font-weight: 600; padding: 7px 12px; border-radius: var(--radius-md);
}
.boolean-indicator.done    { background: rgba(16,185,129,0.1); color: #0D9C6E; }
.boolean-indicator.pending { background: var(--bg-hover); color: var(--text-muted); }

.quick-input-btn {
  display: flex; align-items: center; gap: 4px;
  width: 100%; margin-top: 10px; padding: 6px 0;
  font-size: 12px; font-weight: 500; color: var(--color-project);
  background: none; border: none; border-top: 1px solid var(--border-color);
  cursor: pointer; justify-content: center; transition: color var(--transition-fast);
  font-family: inherit;
}
.quick-input-btn:hover { color: #1D6FCC; }

/* ── 카테고리 요약 ── */
.category-summary-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(260px, 1fr)); gap: 12px; }
.category-card {
  padding: 14px; border: 1px solid var(--border-color);
  border-radius: var(--radius-lg); background: var(--bg-card);
  box-shadow: var(--shadow-xs); transition: border-color var(--transition-base);
}
.category-header { display: flex; align-items: center; gap: 8px; margin-bottom: 10px; }
.cat-dot   { width: 9px; height: 9px; border-radius: 50%; flex-shrink: 0; }
.cat-name  { font-weight: 600; font-size: 13px; color: var(--text-primary); flex: 1; }
.cat-count { font-size: 12px; color: var(--text-muted); }
.cat-rate  { font-size: 13px; font-weight: 700; }

/* ── 빠른 입력 다이얼로그 ── */
.quick-dialog-meta { display: flex; gap: 6px; margin-bottom: 12px; flex-wrap: wrap; }
.q-cat  { font-size: 12px; font-weight: 600; padding: 3px 9px; border-radius: var(--radius-sm); }
.q-chip { font-size: 12px; color: var(--text-secondary); background: var(--bg-hover); padding: 3px 9px; border-radius: var(--radius-sm); border: 1px solid var(--border-color); }
.quick-date-row {
  display: flex; align-items: center; gap: 10px;
  background: var(--bg-hover); border: 1px solid var(--border-color);
  border-radius: var(--radius-md); padding: 10px 14px; margin-bottom: 14px;
}
.quick-date-label { font-size: 13px; font-weight: 600; color: var(--text-secondary); white-space: nowrap; }
.quick-input-field { display: flex; flex-direction: column; gap: 10px; }
.q-unit { font-size: 13px; color: var(--text-secondary); }
.quick-rate-preview { display: flex; align-items: center; gap: 10px; font-size: 12px; color: var(--text-secondary); font-weight: 600; }

/* ── 트렌드 화살표 ── */
.stat-value-row { display: flex; align-items: center; gap: 8px; }
.trend-badge {
  display: inline-flex; align-items: center; gap: 2px;
  font-size: 11px; font-weight: 700; padding: 2px 7px; border-radius: var(--radius-sm);
}
.trend-badge.trend-up   { background: rgba(16,185,129,0.12); color: #0D9C6E; }
.trend-badge.trend-down { background: rgba(239,68,68,0.12);  color: #DC2626; }
.trend-period { font-size: 11px; color: var(--text-muted); margin-left: 2px; }

/* ── 이번 주 하이라이트 ── */
.highlights-row {
  display: grid; grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 10px; margin-bottom: 14px;
}
.highlight-card {
  border-radius: var(--radius-lg); padding: 13px 15px;
  display: flex; align-items: flex-start; gap: 12px;
  border: 1px solid transparent; box-shadow: var(--shadow-xs);
}
.highlight-card.top        { background: rgba(245,158,11,0.06); border-color: rgba(245,158,11,0.2); }
.highlight-card.streak     { background: rgba(249,115,22,0.06); border-color: rgba(249,115,22,0.2); }
.highlight-card.completion { background: rgba(16,185,129,0.06); border-color: rgba(16,185,129,0.2); }
.highlight-card.missed     { background: rgba(239,68,68,0.06);  border-color: rgba(239,68,68,0.2); }

.highlight-icon {
  width: 30px; height: 30px; border-radius: var(--radius-md);
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0; margin-top: 1px;
}
.highlight-card.top        .highlight-icon { background: rgba(245,158,11,0.15); color: #D97706; }
.highlight-card.streak     .highlight-icon { background: rgba(249,115,22,0.15); color: #EA580C; }
.highlight-card.completion .highlight-icon { background: rgba(16,185,129,0.15); color: #059669; }
.highlight-card.missed     .highlight-icon { background: rgba(239,68,68,0.15);  color: #DC2626; }

.highlight-label { font-size: 11px; color: var(--text-secondary); font-weight: 600; margin-bottom: 3px; text-transform: uppercase; letter-spacing: 0.04em; }
.highlight-name  { font-size: 13px; font-weight: 600; color: var(--text-primary); margin-bottom: 4px; line-height: 1.3; }
.highlight-value { font-size: 15px; font-weight: 700; color: var(--text-primary); }
.highlight-card.top    .highlight-value { color: #D97706; }
.highlight-card.streak .highlight-value { color: #EA580C; }
.highlight-card.completion .highlight-value { color: #059669; }

.highlight-missed-list { display: flex; flex-wrap: wrap; gap: 4px; margin-top: 4px; }
.missed-chip { font-size: 10px; color: #DC2626; background: rgba(239,68,68,0.1); padding: 2px 7px; border-radius: var(--radius-sm); font-weight: 600; }
.missed-chip.more { background: var(--bg-hover); color: var(--text-secondary); }

/* ── KPI 정렬 옵션 ── */
.kpi-section-header {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 14px; flex-wrap: wrap; gap: 10px;
}
.kpi-section-controls { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.sort-group { display: flex; border: 1px solid var(--border-color); border-radius: var(--radius-md); overflow: hidden; }
.sort-opt-btn {
  padding: 5px 10px; border: none; background: var(--bg-card);
  font-size: 12px; font-weight: 500; color: var(--text-secondary); cursor: pointer;
  border-right: 1px solid var(--border-color); transition: all var(--transition-fast);
  font-family: inherit;
}
.sort-opt-btn:last-child { border-right: none; }
.sort-opt-btn:hover { background: var(--bg-hover); color: var(--text-primary); }
.sort-opt-btn.active { background: rgba(48,127,226,0.08); color: var(--color-project); font-weight: 700; }

/* ── 100% 달성 카드 ── */
.kpi-summary-card.perfect {
  border-color: rgba(245,158,11,0.35) !important;
  background: rgba(245,158,11,0.04) !important;
}
.kpi-summary-card.bool-done {
  border-color: rgba(16,185,129,0.35) !important;
  background: rgba(16,185,129,0.04) !important;
}
.perfect-badge {
  display: inline-flex; align-items: center; gap: 3px;
  font-size: 10px; font-weight: 700; color: #D97706;
  background: rgba(245,158,11,0.12); padding: 1px 6px; border-radius: var(--radius-sm);
  margin-left: 4px;
}

/* ── 카테고리 KPI 그룹 ── */
.category-kpi-groups { display: flex; flex-direction: column; gap: 18px; }
.category-kpi-header {
  display: flex; align-items: center; gap: 8px;
  margin-bottom: 10px; padding-bottom: 10px;
  border-bottom: 1px solid var(--border-color);
}
.cat-group-dot  { width: 9px; height: 9px; border-radius: 50%; flex-shrink: 0; }
.cat-group-name { font-size: 14px; font-weight: 700; color: var(--text-primary); }
.cat-group-count {
  font-size: 11px; color: var(--text-muted);
  background: var(--bg-hover); padding: 2px 8px; border-radius: var(--radius-sm);
  border: 1px solid var(--border-color);
}
.cat-group-done { font-size: 12px; font-weight: 600; margin-left: auto; }

/* ── KPI 이름 링크 ── */
.kpi-name-link { color: var(--text-primary); text-decoration: none; font-weight: 600; transition: color var(--transition-fast); }
.kpi-name-link:hover { color: var(--color-project); text-decoration: underline; }

/* ── 게이미피케이션 ── */
.streak-warning-bar {
  display: flex; align-items: center; gap: 12px;
  background: rgba(245,158,11,0.08); border: 1px solid rgba(245,158,11,0.25);
  border-radius: var(--radius-lg); padding: 12px 16px; flex-wrap: wrap;
  margin-bottom: 14px;
}
.sw-icon { font-size: 1.3rem; flex-shrink: 0; }
.sw-content { flex: 1; font-size: 13px; color: #92400E; display: flex; gap: 8px; flex-wrap: wrap; align-items: center; }
.sw-content strong { font-weight: 800; }
.sw-kpi { background: rgba(245,158,11,0.15); border: 1px solid rgba(245,158,11,0.3); border-radius: var(--radius-sm); padding: 1px 8px; font-weight: 600; font-size: 12px; color: #D97706; }
.sw-note { font-size: 12px; color: #78350F; }
.sw-link { font-size: 13px; font-weight: 700; color: #D97706; text-decoration: none; white-space: nowrap; }
.sw-link:hover { text-decoration: underline; }

.badge-notification { display: flex; gap: 10px; flex-wrap: wrap; }
.badge-item {
  display: flex; align-items: center; gap: 10px;
  background: linear-gradient(135deg, #1A62B5, var(--color-project));
  border-radius: var(--radius-lg); padding: 12px 16px;
  animation: badge-in 0.4s ease;
}
@keyframes badge-in { from { opacity: 0; transform: translateY(-12px) scale(0.9); } to { opacity: 1; transform: none; } }
.badge-emoji { font-size: 2rem; flex-shrink: 0; }
.badge-name  { font-size: 14px; font-weight: 800; color: white; }
.badge-desc  { font-size: 12px; color: rgba(255,255,255,0.75); margin-top: 2px; }

/* ── STAR 위젯 ── */
.star-widget {
  background: var(--bg-card); border: 1px solid var(--border-color);
  border-radius: var(--radius-lg); padding: 14px 18px; margin-bottom: 14px;
  box-shadow: var(--shadow-xs);
}
.star-widget-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 10px; }
.star-widget-title  { display: flex; align-items: center; gap: 6px; font-size: 14px; font-weight: 600; color: var(--text-primary); }
.star-widget-icon   { color: var(--color-warning); }
.star-widget-right  { display: flex; align-items: center; gap: 12px; }
.star-widget-count  { font-size: 11px; color: var(--text-muted); }
.star-widget-link   { font-size: 12px; font-weight: 600; color: var(--color-project); text-decoration: none; transition: color var(--transition-fast); }
.star-widget-link:hover { color: #1D6FCC; }

.star-lp-mini { display: flex; align-items: center; gap: 5px; margin-bottom: 10px; flex-wrap: wrap; }
.star-lp-dot  { width: 15px; height: 15px; border-radius: 3px; background: var(--bg-hover); cursor: default; transition: background .2s; border: 1px solid var(--border-color); }
.star-lp-dot.covered { border-color: transparent; }
.star-lp-mini-label { font-size: 10px; color: var(--text-muted); margin-left: 4px; }

.star-story-list { display: flex; flex-direction: column; gap: 5px; }
.star-story-item {
  display: flex; align-items: center; gap: 8px;
  padding: 8px 10px; border-radius: var(--radius-md);
  background: var(--bg-hover); border: 1px solid var(--border-color);
  text-decoration: none; transition: border-color var(--transition-fast);
}
.star-story-item:hover { border-color: var(--gray-300); }
.star-story-lp {
  font-size: 10px; font-weight: 700;
  padding: 2px 7px; border-radius: var(--radius-sm); border: 1px solid;
  white-space: nowrap; flex-shrink: 0;
}
.star-story-title { flex: 1; font-size: 13px; font-weight: 600; color: var(--text-primary); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.star-story-date  { font-size: 10px; color: var(--text-muted); flex-shrink: 0; }

.star-widget-empty { display: flex; flex-direction: column; gap: 14px; }
.star-method-grid  { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; }
.star-method-item {
  display: flex; align-items: flex-start; gap: 10px;
  padding: 10px 12px; background: var(--bg-hover);
  border: 1px solid var(--border-color); border-radius: var(--radius-md);
}
.star-method-letter {
  width: 24px; height: 24px; border-radius: var(--radius-md);
  display: flex; align-items: center; justify-content: center;
  font-size: 13px; font-weight: 900; color: white; flex-shrink: 0;
}
.star-method-body { display: flex; flex-direction: column; gap: 2px; }
.star-method-name { font-size: 12px; font-weight: 700; color: var(--text-primary); display: flex; align-items: center; gap: 6px; }
.star-method-ko   { font-size: 10px; color: var(--text-secondary); font-weight: 500; }
.star-method-desc { font-size: 11px; color: var(--text-secondary); line-height: 1.45; }
.star-method-desc strong { color: var(--text-primary); }

.star-widget-cta {
  display: flex; align-items: center; justify-content: space-between;
  padding: 8px 12px; background: rgba(245,158,11,0.07);
  border: 1px solid rgba(245,158,11,0.2); border-radius: var(--radius-md);
}
.star-widget-cta-text { font-size: 12px; color: #92400E; }
.star-widget-create-link { font-size: 12px; font-weight: 700; color: #D97706; text-decoration: none; transition: color var(--transition-fast); white-space: nowrap; }
.star-widget-create-link:hover { color: #B45309; }

/* ── 스크럼 위젯 ── */
.scrum-widget {
  background: var(--bg-card); border: 1px solid var(--border-color);
  border-radius: var(--radius-lg); padding: 14px 18px; margin-bottom: 14px;
  box-shadow: var(--shadow-xs);
}
.scrum-widget-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; }
.scrum-widget-title  { display: flex; align-items: center; gap: 6px; font-size: 14px; font-weight: 600; color: var(--text-primary); }
.scrum-widget-icon   { color: var(--color-kpi); }
.scrum-widget-link   { font-size: 12px; font-weight: 600; color: var(--color-project); text-decoration: none; transition: color var(--transition-fast); }
.scrum-widget-link:hover { color: #1D6FCC; }

.scrum-widget-body { display: flex; flex-direction: column; gap: 10px; }
.scrum-meta-row { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.scrum-energy-pips { display: flex; gap: 4px; }
.energy-pip { width: 9px; height: 9px; border-radius: 50%; background: var(--border-color); transition: background var(--transition-fast); }
.energy-pip.active { background: var(--color-warning); }
.scrum-focus { font-size: 13px; color: var(--text-secondary); font-style: italic; flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

.scrum-task-row { display: flex; align-items: center; gap: 10px; }
.scrum-task-counts { display: flex; align-items: baseline; gap: 3px; flex-shrink: 0; }
.scrum-done-count  { font-size: 18px; font-weight: 800; color: var(--text-primary); font-family: 'JetBrains Mono', monospace; }
.scrum-sep         { font-size: 14px; color: var(--gray-300); }
.scrum-total-count { font-size: 14px; color: var(--text-muted); }
.scrum-count-label { font-size: 11px; color: var(--text-muted); margin-left: 2px; }
.scrum-task-bar    { flex: 1; height: 5px; background: var(--bg-hover); border-radius: var(--radius-full); overflow: hidden; }
.scrum-task-fill   { height: 100%; border-radius: var(--radius-full); transition: width .4s ease; }
.scrum-rate-pct    { font-size: 12px; font-weight: 700; color: var(--text-primary); flex-shrink: 0; }

.scrum-history-row { display: flex; gap: 6px; align-items: flex-end; }
.scrum-hist-col    { flex: 1; display: flex; flex-direction: column; align-items: center; gap: 3px; }
.scrum-hist-track  { width: 100%; height: 26px; background: var(--bg-hover); border-radius: var(--radius-sm); overflow: hidden; display: flex; align-items: flex-end; }
.scrum-hist-fill   { width: 100%; border-radius: var(--radius-sm); transition: height .4s ease; }
.scrum-hist-label  { font-size: 9px; color: var(--text-muted); }
.scrum-hist-label.today { color: var(--color-project); font-weight: 700; }
</style>
