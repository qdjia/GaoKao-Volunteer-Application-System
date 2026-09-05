package com.gaokao.dto;

import jakarta.validation.constraints.NotBlank;

public record AccountStatusRequest(@NotBlank(message = "账号状态不能为空") String status) {
}
