package com.ptkt.service;

import com.ptkt.dto.WsEvent;
import com.ptkt.mapper.KpiRecordMapper;
import com.ptkt.mapper.ProjectMapper;
import com.ptkt.mapper.TaskMapper;
import com.ptkt.model.KpiRecord;
import com.ptkt.model.Task;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaskService {

    private final TaskMapper taskMapper;
    private final KpiRecordMapper kpiRecordMapper;
    private final ProjectMapper projectMapper;
    private final NotificationService notifService;
    private final RealtimeService realtimeService;

    public List<Task> findByProjectId(Long projectId) {
        return taskMapper.findByProjectId(projectId);
    }

    public List<Task> findMyTasks(Long userId) {
        return taskMapper.findByAssigneeId(userId);
    }

    public Task findById(Long id) {
        return taskMapper.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("태스크를 찾을 수 없습니다."));
    }

    @Transactional
    public Task create(Task task) {
        if (task.getStatus() == null)    task.setStatus("BACKLOG");
        if (task.getPriority() == null)  task.setPriority("P2");
        if (task.getSortOrder() == null) task.setSortOrder(0);
        taskMapper.insert(task);
        Task created = findById(task.getId());

        if (created.getAssigneeId() != null) {
            notifService.send(
                created.getAssigneeId(),
                "TASK_ASSIGNED",
                "새 태스크가 할당되었습니다",
                created.getTitle(),
                "/projects/" + created.getProjectId() + "/board"
            );
        }

        broadcastTask("TASK_CREATED", created, null);
        return created;
    }

    @Transactional
    public Task update(Long id, Task task) {
        findById(id);
        task.setId(id);
        taskMapper.update(task);
        Task updated = findById(id);
        broadcastTask("TASK_UPDATED", updated, null);
        return updated;
    }

    @Transactional
    public Task changeStatus(Long id, String newStatus, Long actorId) {
        Task task = findById(id);
        String prevStatus = task.getStatus();
        taskMapper.updateStatus(id, newStatus);

        if ("DONE".equals(newStatus) && !"DONE".equals(prevStatus)) {
            if (task.getKpiId() != null && task.getKpiContribution() != null) {
                applyKpiContribution(task, actorId);
                notifService.send(
                    actorId,
                    "KPI_ACHIEVED",
                    "KPI 실적이 반영되었습니다",
                    task.getTitle() + " 완료 → " + task.getKpiName()
                        + " +" + task.getKpiContribution() + (task.getKpiUnit() != null ? task.getKpiUnit() : ""),
                    "/kpis"
                );
            }
            notifService.send(
                actorId,
                "TASK_DONE",
                "태스크를 완료했습니다 ✅",
                task.getTitle(),
                "/projects/" + task.getProjectId() + "/board"
            );
        }

        Task updated = findById(id);
        broadcastTask("TASK_STATUS_CHANGED", updated, actorId);
        return updated;
    }

    @Transactional
    public void delete(Long id) {
        Task task = findById(id);
        taskMapper.deleteById(id);
        broadcastTask("TASK_DELETED", task, null);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private void broadcastTask(String type, Task task, Long actorId) {
        Long orgId = projectMapper.findById(task.getProjectId())
                .map(p -> p.getOrgId())
                .orElse(null);
        realtimeService.broadcastToOrg(orgId, "tasks", WsEvent.builder()
                .type(type)
                .payload(task)
                .orgId(orgId)
                .actorId(actorId)
                .build());
    }

    private void applyKpiContribution(Task task, Long userId) {
        LocalDate today = LocalDate.now();
        var existing = kpiRecordMapper.findLatestByKpiIdBetween(task.getKpiId(), today, today);

        if (existing.isPresent()) {
            KpiRecord rec = existing.get();
            java.math.BigDecimal newVal = (rec.getActualValue() == null
                    ? task.getKpiContribution()
                    : rec.getActualValue().add(task.getKpiContribution()));
            rec.setActualValue(newVal);
            rec.setNote("칸반 태스크 자동 반영");
            kpiRecordMapper.update(rec);
        } else {
            KpiRecord rec = KpiRecord.builder()
                    .kpiId(task.getKpiId())
                    .actualValue(task.getKpiContribution())
                    .recordedDate(today)
                    .ownerId(userId)
                    .note("칸반 태스크 자동 반영: " + task.getTitle())
                    .build();
            kpiRecordMapper.insert(rec);
        }
    }
}
