package com.aoop.renthubbd.dto;

import com.aoop.renthubbd.model.AdminRole;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminRegisterForm {
    private String username;
    private String password;
    private String confirmPassword;
    private AdminRole role;
}