package com.aoop.renthubbd.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminPasswordForm {
    private String currentPassword;
    private String newPassword;
    private String confirmPassword;
}