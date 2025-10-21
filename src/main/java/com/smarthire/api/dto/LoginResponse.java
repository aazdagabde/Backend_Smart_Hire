package com.smarthire.api.dto;

public record LoginResponse(String jwt, Long id, String email, String firstName, String lastName,
                            java.util.List<String> roles) {
}
