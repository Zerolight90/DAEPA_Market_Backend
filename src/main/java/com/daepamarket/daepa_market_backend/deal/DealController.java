package com.daepamarket.daepa_market_backend.deal;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/deal")
@RequiredArgsConstructor
public class DealController {

    private final DealService dealService;

    @GetMapping("/mySell")
    public ResponseEntity<?> getMySafeDeal(HttpServletRequest request) {
        return dealService.getMySafeDeal(request);
    }

}
