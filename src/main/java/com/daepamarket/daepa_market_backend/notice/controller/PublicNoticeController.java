package com.daepamarket.daepa_market_backend.notice.controller;

import com.daepamarket.daepa_market_backend.notice.dto.PublicNoticeDTO;
import com.daepamarket.daepa_market_backend.notice.service.PublicNoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/public/notices")
@CrossOrigin(origins = {"http://localhost:3000", "http://127.0.0.1:3000", "http://3.34.181.73"})
public class PublicNoticeController {

    private final PublicNoticeService publicNoticeService;

    @GetMapping
    public Page<PublicNoticeDTO> getNotices(@PageableDefault(size = 10, sort = "nIdx", direction = Sort.Direction.DESC) Pageable pageable) {
        return publicNoticeService.findNotices(pageable);
    }
}
