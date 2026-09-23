package com.ubb.practicas.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserImportRecordDto {
    private int rowNumber;
    private String fullName;
    private String email;
    private String password;
    private String roleName;
}
