package com.example.usermanagementservice.dto.response.staffRes;

import lombok.*;

// Assistant Coach Response
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssistantCoachResponse extends StaffMemberResponse {
    private String specialty;
}
