package org.example.zaadachi.Dto;

import org.example.zaadachi.enums.TaskStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

import java.time.LocalDateTime;
@Data
@AllArgsConstructor
@Getter
public class TaskUpdateDto {
    private String description;
   private TaskStatus status;
   private LocalDateTime deadline;
    private String assigneeUsername;
}
