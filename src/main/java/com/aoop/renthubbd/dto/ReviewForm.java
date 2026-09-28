package com.aoop.renthubbd.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReviewForm {
    private Integer rating;
    private String title;
    private String comment;
}