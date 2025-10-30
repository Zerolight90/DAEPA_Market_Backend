package com.daepamarket.daepa_market_backend.admin.oneonone.Controller;

import com.daepamarket.daepa_market_backend.admin.oneonone.DTO.OneOnOneResponseDTO;
import com.daepamarket.daepa_market_backend.admin.oneonone.Service.OneOnOneService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/contact")
public class OneOnOneController {

    private final OneOnOneService service;
    private final RestClient.Builder builder;

    @GetMapping
    public List<OneOnOneResponseDTO> getList(){
        return service.getList().stream().map(i -> OneOnOneResponseDTO.builder()
                .id(i.getOoIdx())
                .name(i.getUser().getUname())
                .title(i.getOoTitle())
                .content(i.getOoContent())
                .photo(i.getOoPhoto())
                .category(i.getOoStatus())
                .date(i.getOoDate())
                .build()
        ).toList();
    }
}
