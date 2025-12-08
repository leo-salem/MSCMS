package com.example.usermanagementservice.dto.response.staffRes;

import lombok.*;

// Physiotherapist Response
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PhysiotherapistResponse extends StaffMemberResponse {
    private Integer yearsExperience;
}