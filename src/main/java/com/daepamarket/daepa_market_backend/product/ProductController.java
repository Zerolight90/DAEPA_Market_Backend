// src/main/java/com/daepamarket/daepa_market_backend/product/ProductController.java
package com.daepamarket.daepa_market_backend.product;

import com.daepamarket.daepa_market_backend.domain.product.ProductEntity;
import com.daepamarket.daepa_market_backend.domain.product.ProductRepository;
import com.daepamarket.daepa_market_backend.jwt.CookieUtil;
import com.daepamarket.daepa_market_backend.jwt.JwtProvider;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final ProductRepository productRepository;
    private final JwtProvider jwtProvider;
    private final CookieUtil cookieUtil;

    // ==========================
    // 등록
    // ==========================
    @PostMapping(value="/create-multipart", consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createMultipart(
            HttpServletRequest request,
            @Valid @RequestPart("dto") ProductCreateDTO dto,
            BindingResult br,
            @RequestPart(value="images", required=false) List<MultipartFile> images
    ) {
        if (br.hasErrors()) {
            var errors = br.getFieldErrors().stream()
                    .map(e -> e.getField() + " : " + e.getDefaultMessage())
                    .toList();
            return ResponseEntity.badRequest().body(errors);
        }

        String token = resolveAccessToken(request);
        if (token == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        if (jwtProvider.isExpired(token)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "토큰 만료");
        }

        Long userId;
        try {
            userId = Long.valueOf(jwtProvider.getUid(token));
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰");
        }

        Long id = productService.registerMultipart(userId, dto, images);
        return ResponseEntity.ok(id);
    }

    private String resolveAccessToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie c : cookies) {
                if (CookieUtil.ACCESS.equals(c.getName())) {
                    String v = c.getValue();
                    if (v != null && !v.isBlank()) return v;
                }
            }
        }
        String auth = request.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            return auth.substring(7);
        }
        return null;
    }

    // ==========================
    // 목록 조회
    // ==========================
    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<Page<ProductListDTO>> listByIds(
            @RequestParam(required = false) Long upperId,
            @RequestParam(required = false) Long middleId,
            @RequestParam(required = false) Long lowId,
            @RequestParam(defaultValue = "recent") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<ProductListDTO> mapped = productService
                .getProductsByIds(upperId, middleId, lowId, sort, page, size)
                .map(this::toListDTO);
        return ResponseEntity.ok(mapped);
    }

    @GetMapping("/by-name")
    @Transactional(readOnly = true)
    public ResponseEntity<Page<ProductListDTO>> listByNames(
            @RequestParam(required = false) String big,
            @RequestParam(required = false) String mid,
            @RequestParam(required = false) String sub,
            @RequestParam(defaultValue = "recent") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<ProductListDTO> mapped = productService
                .getProductsByNames(big, mid, sub, sort, page, size)
                .map(this::toListDTO);
        return ResponseEntity.ok(mapped);
    }

    // ==========================
    // ✅ 단건 상세 조회
    // ==========================
    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<ProductDetailDTO> getProduct(@PathVariable("id") Long id) {
        ProductDetailDTO dto = productService.getProductDetail(id);
        return ResponseEntity.ok(dto);
    }

    // ==========================
    // Entity -> 리스트 DTO
    // ==========================
    private ProductListDTO toListDTO(ProductEntity p) {
        String thumb = p.getPdThumb();
        if (thumb == null && p.getImages() != null && !p.getImages().isEmpty()) {
            thumb = p.getImages().get(0).getImageUrl();
        }
        return ProductListDTO.builder()
                .pdIdx(p.getPdIdx())
                .pdTitle(p.getPdTitle())
                .pdPrice(p.getPdPrice())
                .pdThumb(thumb)
                .pdLocation(p.getPdLocation())
                .pdCreate(p.getPdCreate() != null ? p.getPdCreate().toString() : null)
                .build();
    }

    // 내 상품 조회
    @GetMapping("/mypage")
    public List<productMyPageDTO> myProduct(
            HttpServletRequest request,
            @RequestParam(required = false) Integer status
    ) {
        log.info("/api/products/mypage called, status={}", status);

        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "토큰이 없습니다.");
        }

        String accessToken = auth.substring(7);

        Long uIdx = Long.valueOf(jwtProvider.getUid(accessToken));
        log.info("token -> uIdx = {}", uIdx);

        return productService.getMyProductByUIdx(uIdx, status);
    }


}
