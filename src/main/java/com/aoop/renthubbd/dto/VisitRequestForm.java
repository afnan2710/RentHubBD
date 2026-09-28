package com.aoop.renthubbd.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Getter
@Setter
public class VisitRequestForm {

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate requestedDate;

    private String requestedTime;
    private String message;
}