package com.aoop.renthubbd.dto;

import com.aoop.renthubbd.model.AuditAction;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuditFilter {
    private String admin;
    private AuditAction action;
    private String keyword;
    private int limit = 300;

    public boolean isEmpty() {
        return (admin == null || admin.isBlank())
                && action == null
                && (keyword == null || keyword.isBlank());
    }
}