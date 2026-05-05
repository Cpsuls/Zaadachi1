package org.example.zaadachi.service;

import lombok.extern.slf4j.Slf4j;
import org.example.zaadachi.Dto.TaskCreateDto;
import org.example.zaadachi.Dto.TaskResponseDto;
import org.example.zaadachi.Dto.TaskUpdateDto;
import org.example.zaadachi.entity.Task;
import org.example.zaadachi.entity.User;
import org.example.zaadachi.enums.TaskStatus;
import lombok.RequiredArgsConstructor;
import org.example.zaadachi.enums.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.example.zaadachi.repository.TaskRepository;
import org.example.zaadachi.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskService {
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public Task createTask(TaskCreateDto dto) {
        User author = getCurrentUser();
        Task task = new Task();
        task.setDescription(dto.getDescription());
        task.setPriority(dto.getPriority());
        task.setType(dto.getType());
        task.setAuthor(author);
        task.setStatus(TaskStatus.ACTIVE);
        LocalDateTime now = LocalDateTime.now();
        switch (dto.getPriority()) {
            case CRITICAL -> task.setDeadline(now.plusHours(4));
            case HIGH   -> task.setDeadline(now.plusDays(1));
            case MEDIUM -> task.setDeadline(now.plusDays(3));
            case LOW    -> task.setDeadline(now.plusDays(7));
        }

        return taskRepository.save(task);
    }

    public Task updateTask(Long taskId, TaskUpdateDto dto) {
        User current = getCurrentUser();
        Task task = taskRepository.findById(taskId).orElseThrow();

        // Обычный юзер может менять только свои задачи
        boolean isOwner = task.getAuthor().getId().equals(current.getId());
        boolean isAssignee = task.getAssignee() != null && task.getAssignee().getId().equals(current.getId());

        if (!isOwner && !isAssignee) throw new AccessDeniedException("No access to this task");

        if (dto.getDescription() != null) task.setDescription(dto.getDescription());
        if (dto.getStatus() != null) task.setStatus(dto.getStatus());
        if (dto.getDeadline() != null) task.setDeadline(dto.getDeadline());

        // Исполнитель может менять исполнителя (в т.ч. ставить себя)
        if (dto.getAssigneeUsername() != null && current.getRole().equals("ROLE_EXECUTOR")) {
            User assignee = userRepository.findByUsername(dto.getAssigneeUsername())
                    .orElseThrow(() -> new RuntimeException("Assignee not found"));
            task.setAssignee(assignee);
        }
        return taskRepository.save(task);
    }

    public List<TaskResponseDto> getMyTasks() { return toDTO(taskRepository.findByAuthorId(getCurrentUser().getId())); }
    public List<TaskResponseDto> getMyActiveTasks() { return toDTO(taskRepository.findByAuthorIdAndStatus(getCurrentUser().getId(), TaskStatus.ACTIVE)); }
    public List<TaskResponseDto> getUnassignedActiveTasks() { return toDTO(taskRepository.findByStatusAndAssigneeIsNull(TaskStatus.ACTIVE)); }
    public List<TaskResponseDto> getMyCompletedTasks() { return toDTO(taskRepository.findByAssigneeIdAndStatus(getCurrentUser().getId(), TaskStatus.COMPLETED)); }

    //  "взять задачу на себя"
    public Task assignToMyself(Long taskId) {
        User executor = getCurrentUser();
        if (!executor.getRole().equals("ROLE_EXECUTOR")) throw new AccessDeniedException("Only executors can take tasks");

        Task task = taskRepository.findById(taskId).orElseThrow();
        if (task.getAssignee() != null) throw new IllegalStateException("Task already assigned");
        if (!task.getStatus().equals(TaskStatus.ACTIVE)) throw new IllegalStateException("Only active tasks can be taken");

        task.setAssignee(executor);
        task.setStatus(TaskStatus.IN_PROGRESS);
        return taskRepository.save(task);
    }

    private List<TaskResponseDto> toDTO(List<Task> tasks) {
        return tasks.stream().map(t -> new TaskResponseDto(
                t.getId(), t.getDescription(), t.getStatus(), t.getPriority(), t.getType(),
                t.getDeadline(), t.getAuthor().getUsername(),
                t.getAssignee() != null ? t.getAssignee().getUsername() : null
        )).toList();
    }

    public Task adminAssignTask(Long taskId, String assigneeUsername) {
        User assignee = userRepository.findByUsername(assigneeUsername)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + assigneeUsername));

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

        task.setAssignee(assignee);
        if (task.getStatus() == TaskStatus.ACTIVE) {
            task.setStatus(TaskStatus.IN_PROGRESS);
        }
        return taskRepository.save(task);
    }


    public void adminDeleteTask(Long taskId) {
        if (!taskRepository.existsById(taskId)) {
            throw new IllegalArgumentException("Task not found: " + taskId);
        }
        taskRepository.deleteById(taskId);
    }


    public Page<TaskResponseDto> getAllTasksPageable(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return taskRepository.findAll(pageable).map(t -> new TaskResponseDto(
                t.getId(), t.getDescription(), t.getStatus(), t.getPriority(),
                t.getType(), t.getDeadline(),
                t.getAuthor().getUsername(),
                t.getAssignee() != null ? t.getAssignee().getUsername() : null
        ));
    }

        public void autoDistributeTasks() {
            // 1. Находим все свободные задачи
            List<Task> unassignedTasks = taskRepository.findByStatusAndAssigneeIsNull(TaskStatus.ACTIVE);
            if (unassignedTasks.isEmpty()) return;

            // 2. Находим всех курьеров
            List<User> executors = userRepository.findByRole(UserRole.ROLE_EXECUTOR);
            if (executors.isEmpty()) throw new RuntimeException("No executors available");

            // 3. Считаем нагрузку на каждого (количество задач в работе)
            List<TaskStatus> activeStatuses = Arrays.asList(TaskStatus.ACTIVE, TaskStatus.IN_PROGRESS);
            Map<User, Integer> executorLoad = new HashMap<>();

            for (User executor : executors) {
                long count = taskRepository.countByAssigneeIdAndStatusIn(executor.getId(), activeStatuses);
                executorLoad.put(executor, (int) count);
            }

            List<User> sortedExecutors = executorLoad.entrySet().stream()
                    .sorted(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .toList();

            log.info("Распределение " + unassignedTasks.size() + " задач на " +
                    sortedExecutors.size() + " курьеров");

            int executorIndex = 0;
            for (Task task : unassignedTasks) {
                User chosenExecutor = sortedExecutors.get(executorIndex);

                task.setAssignee(chosenExecutor);
                task.setStatus(TaskStatus.IN_PROGRESS);
                taskRepository.save(task);

                executorIndex++;
                if (executorIndex >= sortedExecutors.size()) {
                    executorIndex = 0;
                }
            }
        }
    }

