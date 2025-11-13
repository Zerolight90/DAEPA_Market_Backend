package com.daepamarket.daepa_market_backend.admin.banner;

import com.daepamarket.daepa_market_backend.admin.banner.dto.BannerOrderUpdateDTO;
import com.daepamarket.daepa_market_backend.admin.banner.dto.BannerRequestDTO;
import com.daepamarket.daepa_market_backend.admin.banner.dto.BannerResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/admin/banners")
@RequiredArgsConstructor
public class AdminBannerController {

    private final AdminBannerService adminBannerService;

    @GetMapping
    public ResponseEntity<List<BannerResponseDTO>> getBanners(
            @RequestParam(required = false, defaultValue = "false") boolean activeOnly
    ) {
        if (activeOnly) {
            return ResponseEntity.ok(adminBannerService.getActiveBanners());
        }
        return ResponseEntity.ok(adminBannerService.getAllBanners());
    }

    @GetMapping("/active")
    public ResponseEntity<List<BannerResponseDTO>> getActiveBanners() {
        return ResponseEntity.ok(adminBannerService.getActiveBanners());
    }

    @GetMapping("/{id}")
    public ResponseEntity<BannerResponseDTO> getBanner(@PathVariable Long id) {
        return ResponseEntity.ok(adminBannerService.getBanner(id));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BannerResponseDTO> createBanner(
            @ModelAttribute BannerRequestDTO request,
            @RequestPart(value = "imageFile", required = false) MultipartFile imageFile
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminBannerService.createBanner(request, imageFile));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BannerResponseDTO> updateBanner(
            @PathVariable Long id,
            @ModelAttribute BannerRequestDTO request,
            @RequestPart(value = "imageFile", required = false) MultipartFile imageFile
    ) {
        return ResponseEntity.ok(adminBannerService.updateBanner(id, request, imageFile));
    }

    @PatchMapping("/{id}/toggle")
    public ResponseEntity<BannerResponseDTO> toggleBanner(@PathVariable Long id) {
        return ResponseEntity.ok(adminBannerService.toggleActive(id));
    }

    @PatchMapping("/order")
    public ResponseEntity<Void> updateOrder(@RequestBody List<BannerOrderUpdateDTO> orders) {
        adminBannerService.updateOrder(orders);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBanner(@PathVariable Long id) {
        adminBannerService.deleteBanner(id);
        return ResponseEntity.noContent().build();
    }
}

