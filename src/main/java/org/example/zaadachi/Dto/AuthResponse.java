package org.example.zaadachi.Dto;

import org.example.zaadachi.enums.UserRole;
import lombok.Data;

@Data
public class AuthResponse {
    private String message;
    private String username;
    private UserRole role;
}

