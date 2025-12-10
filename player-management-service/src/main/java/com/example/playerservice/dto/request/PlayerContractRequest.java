package com.example.playerservice.dto.request;

import com.example.playerservice.dto.validation.Create;
import com.example.playerservice.dto.validation.Update;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

public record PlayerContractRequest(
        @NotNull(groups = Create.class)
        @Positive(groups = {Create.class, Update.class})
        Long playerId,

        @NotNull(groups = Create.class)
        LocalDate startDate,

        @NotNull(groups = Create.class)
        LocalDate endDate,

        @Min(value = 0, groups = {Create.class, Update.class})
        @NotNull(groups = Create.class)
        Long salary,

        @Min(value = 0, groups = {Create.class, Update.class})
        @NotNull(groups = Create.class)
        Long releaseClause
) {
}

