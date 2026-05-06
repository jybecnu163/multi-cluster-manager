package com.cloudplatform.manager.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.util.List;

@Data
public class UserRequest {
    @NotBlank
    private String name;
    @NotBlank @Email
    private String email;
    @NotBlank @Size(min = 8)
    @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-zA-Z]).+$", message = "密码必须包含字母和数字")
    private String password;
    @NotNull
    private List<Long> departmentIds;
    @NotNull
    private Long primaryDepartmentId;
}