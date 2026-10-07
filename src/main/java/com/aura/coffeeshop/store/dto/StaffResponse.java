package com.aura.coffeeshop.store.dto;

import com.aura.coffeeshop.store.StaffRole;
import com.aura.coffeeshop.store.entity.Staff;

/** @param roleLabelKm display label for the role, e.g. "អ្នកគ្រប់គ្រងហាង" */
public record StaffResponse(String id, String name, StaffRole role, String roleLabel, String roleLabelKm) {

    public static StaffResponse from(Staff staff) {
        return new StaffResponse(staff.getId(), staff.getName(), staff.getRole(),
                staff.getRole().getLabel(), staff.getRole().getLabelKm());
    }
}
