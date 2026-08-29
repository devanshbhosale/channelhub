package com.devanshbhosale.channelhub.dto;

/**
 * isDemo is null on register (dropped from JSON) and true/false on login.
 */
public record TokenRes(String token, UserRes user) {

    public record UserRes(String email, String fullName, String role, Boolean isDemo) {
    }
}
