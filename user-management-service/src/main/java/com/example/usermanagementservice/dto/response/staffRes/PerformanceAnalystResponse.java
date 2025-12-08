package com.example.usermanagementservice.dto.response.staffRes;

import lombok.*;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PerformanceAnalystResponse extends StaffMemberResponse {
    private Integer yearsExperience;
    private String toolsUsed;
}