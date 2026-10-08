package com.kubee.auth.support.dto;

import com.kubee.auth.support.entity.SupportCategory;
import com.kubee.auth.support.entity.SupportPriority;
import com.kubee.auth.support.entity.SupportStatus;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpdateRequestDto {
    private SupportStatus status;
    private SupportPriority priority;
    private SupportCategory category;
    private String assignedUuid;
}
