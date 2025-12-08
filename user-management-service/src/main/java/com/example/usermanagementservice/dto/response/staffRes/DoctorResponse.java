package com.example.usermanagementservice.dto.response.staffRes;

import lombok.*;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DoctorResponse extends StaffMemberResponse {
    private String specialization;
}