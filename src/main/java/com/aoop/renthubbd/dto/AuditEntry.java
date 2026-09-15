package com.aoop.renthubbd.dto;

import com.aoop.renthubbd.model.AuditAction;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class AuditEntry {
    private LocalDateTime timestamp;
    private String actor;
    private AuditAction action;
    private String detail;

    public AuditEntry() {}

    public AuditEntry(LocalDateTime timestamp, String actor, AuditAction action, String detail) {
        this.timestamp = timestamp;
        this.actor = actor;
        this.action = action;
        this.detail = detail;
    }

    public String getActionLabel() {
        return action != null ? action.getLabel() : "Unknown";
    }
}