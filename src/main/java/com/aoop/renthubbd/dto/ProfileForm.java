package com.aoop.renthubbd.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

@Getter
@Setter
public class ProfileForm {

    private String firstName;
    private String lastName;
    private String countryCode;
    private String phoneNumber;
    private String address;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dob;

    private MultipartFile profilePhoto;
}