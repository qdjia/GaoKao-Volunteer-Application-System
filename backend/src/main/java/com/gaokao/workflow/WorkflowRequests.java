package com.gaokao.workflow;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class WorkflowRequests {
    private WorkflowRequests() {}
    public record Preference(@Positive long planId, boolean acceptAdjustment,
                             @NotNull @Size(max=6) List<@NotNull @Positive Long> majorIds) {}
    public record Draft(@Min(0) int revision, @NotNull @Size(max=45) List<@NotNull @Valid Preference> preferences) {}
    public record Submit(@Min(1) int revision, @NotNull UUID requestId) {}
    public record Window(@Min(0) int revision, @NotNull Instant startsAt, @NotNull Instant endsAt,
                         @NotNull @DecimalMin("0") @DecimalMax("750") @Digits(integer=3,fraction=2) BigDecimal physicsLine,
                         @NotNull @DecimalMin("0") @DecimalMax("750") @Digits(integer=3,fraction=2) BigDecimal historyLine) {}
    public record Plan(@Min(1) int plannedCount, @NotNull @DecimalMin("1.00") @DecimalMax("1.05") @Digits(integer=1,fraction=4) BigDecimal filingRatio) {}
    public record Notice(@NotBlank @Size(max=32) String version) {}
    public record Reset(@NotBlank String confirmation) {}
}
