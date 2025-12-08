package com.example.usermanagementservice.dto.response.staffRes;

import lombok.*;

// Specific Coach Response
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpecificCoachResponse extends StaffMemberResponse {
    private String skillType;
}