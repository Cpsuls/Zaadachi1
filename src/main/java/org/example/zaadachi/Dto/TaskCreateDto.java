package org.example.zaadachi.Dto;

import org.example.zaadachi.enums.TaskPriority;
import org.example.zaadachi.enums.TaskType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

import java.time.LocalDateTime;
@Data
@AllArgsConstructor
@Getter
public class TaskCreateDto {
    private String description;
    private TaskPriority priority;
    private TaskType type;
//    private LocalDateTime deadline;
}