package org.example.zaadachi.Dto;

import org.example.zaadachi.enums.TaskPriority;
import org.example.zaadachi.enums.TaskStatus;
import org.example.zaadachi.enums.TaskType;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
@Data
@AllArgsConstructor
public class TaskResponseDto {
    private Long id;
    private String description;
    private TaskStatus status;
    private TaskPriority priority;
    private TaskType type;
    private LocalDateTime deadline;
    private String authorUSername;
    private String assigneeUSername;
}
