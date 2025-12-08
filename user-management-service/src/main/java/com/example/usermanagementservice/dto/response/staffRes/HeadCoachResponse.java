package com.example.usermanagementservice.dto.response.staffRes;

import lombok.*;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HeadCoachResponse extends StaffMemberResponse {
    private Integer yearsOfExperience;
    private String coachingLicenseLevel;
    private List<String> preManagedTeams;
}
