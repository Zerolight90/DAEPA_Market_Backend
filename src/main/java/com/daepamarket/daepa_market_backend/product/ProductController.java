// com.daepamarket.daepa_market_backend.product.ProductController
package com.daepamarket.daepa_market_backend.product;

import com.daepamarket.daepa_market_backend.domain.product.ProductEntity;
import com.daepamarket.daepa_market_backend.domain.product.ProductRepository;
import com.daepamarket.daepa_market_backend.domain.productimage.ProductImageEntity;

// [ADDED] 로그인 검증을 위한 의존성 추가
import com.daepamarket.daepa_market_backend.jwt.CookieUtil;
import com.daepamarket.daepa_market_backend.jwt.JwtProvider;

import jakarta.servlet.http.Cookie;                // [ADDED]
import jakarta.servlet.http.HttpServletRequest;   // [ADDED]
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final ProductRepository productRepository;

    // [ADDED] JWT/쿠키 유틸 주입
    private final JwtProvider jwtProvider;
    private final CookieUtil cookieUtil;

    /** ✅ 멀티파트(JSON + files)로 등록 (로그인 필요) */
    @PostMapping(value="/create-multipart", consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createMultipart(
            HttpServletRequest request,                                  // [CHANGED] (원래: @RequestHeader("X-USER-ID") Long userIdx)
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

        // ===== 로그인 검증 추가 시작 =====
        // [ADDED] ACCESS_TOKEN 쿠키 → 없으면 Authorization: Bearer 헤더에서 토큰 추출
        String token = resolveAccessToken(request);
        if (token == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        // [ADDED] 토큰 만료/유효성 검사
        if (jwtProvider.isExpired(token)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "토큰 만료");
        }

        // [ADDED] 토큰 subject(유저 ID) 꺼내기
        Long userId;
        try {
            userId = Long.valueOf(jwtProvider.getUid(token));  // subject = uIdx (문자열)
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰");
        }
        // ===== 로그인 검증 추가 끝 =====

        // [CHANGED] userIdx 대신 추출한 userId 사용
        Long id = productService.registerMultipart(userId, dto, images);
        return ResponseEntity.ok(id);
    }

    /** [NEW METHOD] ACCESS_TOKEN 쿠키 → Authorization: Bearer 순으로 토큰 추출 */
    private String resolveAccessToken(HttpServletRequest request) { // [NEW METHOD]
        // 1) 쿠키 우선
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie c : cookies) {
                if (CookieUtil.ACCESS.equals(c.getName())) {
                    String v = c.getValue();
                    if (v != null && !v.isBlank()) return v;
                }
            }
        }
        // 2) Authorization: Bearer
        String auth = request.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            return auth.substring(7);
        }
        return null;
    }

    /** ✅ ID 필터 (upperId/middleId/lowId) + 정렬 + 페이지네이션 */
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

    /** (옵션) 이름 필터 버전: /api/products/by-name?big=&mid=&sub= */
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

    /** ✅ 상세 조회 */
    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<ProductDetailDTO> getProduct(@PathVariable("id") Long id) {
        ProductEntity p = productRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "상품 없음"));

        ProductDetailDTO dto = ProductDetailDTO.builder()
                .pdIdx(p.getPdIdx())
                .pdTitle(p.getPdTitle())
                .pdPrice(p.getPdPrice())
                .pdLocation(p.getPdLocation())
                .pdCreate(p.getPdCreate() != null ? p.getPdCreate().toString() : null)
                .pdContent(p.getPdContent())
                .pdThumb(p.getPdThumb())
                .images(p.getImages().stream().map(ProductImageEntity::getImageUrl).toList())
                .sellerName(p.getSeller().getUname())
                .sellerId(p.getSeller().getUIdx())
                .build();

        return ResponseEntity.ok(dto);
    }

    /** Entity -> 리스트 DTO 매핑 */
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
}
