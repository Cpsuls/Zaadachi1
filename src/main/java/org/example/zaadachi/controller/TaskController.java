package org.example.zaadachi.controller;

import org.example.zaadachi.Dto.TaskCreateDto;
import org.example.zaadachi.Dto.TaskResponseDto;
import org.example.zaadachi.Dto.TaskTakeDto;
import org.example.zaadachi.Dto.TaskUpdateDto;
import org.example.zaadachi.entity.Task;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.example.zaadachi.service.TaskService;

import java.util.List;
// controller/TaskController.java
@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
@Slf4j
public class TaskController {

    private final TaskService taskService;
    @PostMapping
    @PreAuthorize("hasAnyRole('ROLE_USER', 'ROLE_EXECUTOR', 'ROLE_ADMIN')")
    public ResponseEntity<TaskResponseDto> createTask( @RequestBody TaskCreateDto dto) {
        return ResponseEntity.ok(toDTO(taskService.createTask(dto)));
    }


    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROLE_USER', 'ROLE_EXECUTOR', 'ROLE_ADMIN')")
    public ResponseEntity<TaskResponseDto> updateTask(@PathVariable Long id, @RequestBody TaskUpdateDto dto) {
        return ResponseEntity.ok(toDTO(taskService.updateTask(id, dto)));
    }

    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('ROLE_USER', 'ROLE_EXECUTOR', 'ROLE_ADMIN')")
    public ResponseEntity<List<TaskResponseDto>> getMyTasks() {
        return ResponseEntity.ok(taskService.getMyTasks());
    }

    @GetMapping("/my/active")
    @PreAuthorize("hasAnyRole('ROLE_USER', 'ROLE_EXECUTOR', 'ROLE_ADMIN')")
    public ResponseEntity<List<TaskResponseDto>> getMyActiveTasks() {
        return ResponseEntity.ok(taskService.getMyActiveTasks());
    }

    @GetMapping("/executor/unassigned-active")
    @PreAuthorize("hasAnyRole('ROLE_EXECUTOR', 'ROLE_ADMIN')")
    public ResponseEntity<List<TaskResponseDto>> getUnassignedActiveTasks() {
        return ResponseEntity.ok(taskService.getUnassignedActiveTasks());
    }

    @GetMapping("/executor/my-completed")
    @PreAuthorize("hasAnyRole('ROLE_EXECUTOR', 'ROLE_ADMIN')")
    public ResponseEntity<List<TaskResponseDto>> getMyCompletedTasks() {
        return ResponseEntity.ok(taskService.getMyCompletedTasks());
    }

//    @PostMapping("/{id}/take")
//    @PreAuthorize("hasAnyRole('ROLE_EXECUTOR', 'ROLE_ADMIN')")
//    public ResponseEntity<TaskResponseDto> takeTask(@RequestBody TaskTakeDto taskTakeDto) {
//        return ResponseEntity.ok(toDTO(taskService.assignToMyself(taskTakeDto.getTaskId())));
//    }я не ебу почему не работает короче блять через админа будет назначение и потом через put execturoа
//    обновление статуса

    @PutMapping("/{id}/assign")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<TaskResponseDto> adminAssignTask(
            @PathVariable Long id,
            @RequestParam String assigneeUsername) {
        return ResponseEntity.ok(toDTO(taskService.adminAssignTask(id, assigneeUsername)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Void> adminDeleteTask(@PathVariable Long id) {
        taskService.adminDeleteTask(id);
        return ResponseEntity.noContent().build();
    }


    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Page<TaskResponseDto>> getAllTasksPageable(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(taskService.getAllTasksPageable(page, size));
    }

    private TaskResponseDto toDTO(Task t) {
        return new TaskResponseDto(
                t.getId(), t.getDescription(), t.getStatus(), t.getPriority(),
                t.getType(), t.getDeadline(),
                t.getAuthor().getUsername(),
                t.getAssignee() != null ? t.getAssignee().getUsername() : null
        );
    }
}
