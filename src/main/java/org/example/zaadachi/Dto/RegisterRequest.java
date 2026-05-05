package org.example.zaadachi.Dto;

import org.example.zaadachi.enums.UserRole;
import lombok.Data;
import lombok.Getter;

@Data
@Getter
public class RegisterRequest {
    private String username;
    private String password;
    private UserRole role;
}


