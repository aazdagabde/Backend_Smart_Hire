package com.smarthire.api.controller;

import com.smarthire.api.dto.DashboardStatsDTO;
import com.smarthire.api.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/stats")
    public ResponseEntity<DashboardStatsDTO> getDashboardStats(Principal principal) {
        // 'principal.getName()' retourne l'email de l'utilisateur connecté (grâce au JWT)
        DashboardStatsDTO stats = dashboardService.getStatsForUser(principal.getName());
        return ResponseEntity.ok(stats);
    }
}