//package com.daepamarket.daepa_market_backend.delivery;
//
//
//import jakarta.servlet.http.HttpServletRequest;
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//
//@RestController
//@RequestMapping("/api/delivery")
//@RequiredArgsConstructor
//
//public class deliveryController {
//    private final deliveryService deliveryService;
//
//    @GetMapping("/list")
//    public ResponseEntity<?> getSent(HttpServletRequest request) {
//        return deliveryService.getSentList(request);
//    }
//
//}
