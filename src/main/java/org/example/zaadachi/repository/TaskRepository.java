package org.example.zaadachi.repository;

import org.example.zaadachi.entity.Task;
import org.example.zaadachi.enums.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByAuthorId(Long authorId);
    List<Task> findByAuthorIdAndStatus(Long authorId, TaskStatus status);
    List<Task> findByStatusAndAssigneeIsNull(TaskStatus status);
    List<Task> findByAssigneeIdAndStatus(Long assigneeId, TaskStatus status);
    List<Task> findByAssigneeId(Long assigneeId);

    long countByAssigneeIdAndStatusIn(Long id, List<TaskStatus> activeStatuses);
}
