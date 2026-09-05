package com.gaokao.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.ToString;

@Data
public class LoginRequest {
    @NotBlank(message = "账号不能为空")
    @Size(max = 50, message = "账号长度不能超过50位")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(max = 72, message = "密码长度不能超过72位")
    @ToString.Exclude
    private String password;

    @Override
    public String toString() {
        return "LoginRequest[username=***, password=***]";
    }
}
