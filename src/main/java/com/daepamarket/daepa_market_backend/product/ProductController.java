package com.daepamarket.daepa_market_backend.product;

import com.daepamarket.daepa_market_backend.domain.deal.DealRepository;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final ProductRepository productRepository;
    private final DealRepository dealRepository;
    private final JwtProvider jwtProvider;
    private final CookieUtil cookieUtil;

    @PostMapping(value = "/create-multipart", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createMultipart(
            HttpServletRequest request,
            @Valid @RequestPart("dto") ProductCreateDTO dto,
            BindingResult br,
            @RequestPart(value = "images", required = false) List<MultipartFile> images
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

        Long userId = Long.valueOf(jwtProvider.getUid(token));
        Long id = productService.registerMultipart(userId, dto, images);
        return ResponseEntity.ok(id);
    }

    @PostMapping(value = "/{id}/edit-multipart", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateMultipart(
            @PathVariable("id") Long id,
            HttpServletRequest request,
            @Valid @RequestPart("dto") ProductCreateDTO dto,
            BindingResult br,
            @RequestPart(value = "images", required = false) List<MultipartFile> images
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
        Long userId = Long.valueOf(jwtProvider.getUid(token));

        productService.updateMultipart(id, userId, dto, images);
        return ResponseEntity.ok().build();
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

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<Page<ProductListDTO>> listByIds(
            @RequestParam(required = false) Long upperId,
            @RequestParam(name = "mid", required = false) Long middleId,
            @RequestParam(name = "low", required = false) Long lowId,
            @RequestParam(required = false) Long min,
            @RequestParam(required = false) Long max,
            @RequestParam(required = false) String dDeal,
            @RequestParam(required = false, defaultValue = "false") boolean excludeSold,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "recent") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<ProductListDTO> mapped = productService
                .getProductsByIds(
                        upperId,
                        middleId,
                        lowId,
                        min,
                        max,
                        dDeal,
                        excludeSold,
                        keyword,
                        sort,
                        page,
                        size
                )
                .map(this::toListDTO);
        return ResponseEntity.ok(mapped);
    }

    @GetMapping("/by-name")
    @Transactional(readOnly = true)
    public ResponseEntity<Page<ProductListDTO>> listByNames(
            @RequestParam(required = false) String big,
            @RequestParam(required = false) String mid,
            @RequestParam(required = false) String sub,
            @RequestParam(required = false) Long min,
            @RequestParam(required = false) Long max,
            @RequestParam(required = false) String dDeal,
            @RequestParam(required = false, defaultValue = "false") boolean excludeSold,
            @RequestParam(defaultValue = "recent") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<ProductListDTO> mapped = productService
                .getProductsByNames(
                        big,
                        mid,
                        sub,
                        min,
                        max,
                        dDeal,
                        excludeSold,
                        sort,
                        page,
                        size
                )
                .map(this::toListDTO);
        return ResponseEntity.ok(mapped);
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<ProductDetailDTO> getProduct(@PathVariable("id") Long id) {
        ProductDetailDTO dto = productService.getProductDetail(id);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/{id}/related")
    @Transactional(readOnly = true)
    public ResponseEntity<List<ProductListDTO>> getRelated(
            @PathVariable("id") Long id,
            @RequestParam(name = "limit", defaultValue = "10") int limit
    ) {
        List<ProductEntity> related = productService.getRelatedProducts(id, limit);
        List<ProductListDTO> dtoList = related.stream()
                .map(this::toListDTO)
                .toList();
        return ResponseEntity.ok(dtoList);
    }

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

    @PutMapping("/{id}")
    public ResponseEntity<?> updateProduct(
            @PathVariable("id") Long id,
            HttpServletRequest request,
            @Valid @RequestBody ProductCreateDTO dto
    ) {
        String token = resolveAccessToken(request);
        if (token == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        Long userId = Long.valueOf(jwtProvider.getUid(token));

        productService.updateProduct(id, userId, dto);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/delete")
    public ResponseEntity<?> deleteProduct(
            @PathVariable("id") Long id,
            HttpServletRequest request
    ) {
        String token = resolveAccessToken(request);
        if (token == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        Long userId = Long.valueOf(jwtProvider.getUid(token));

        productService.softDeleteProduct(id, userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/bump")
    public ResponseEntity<?> bumpProduct(
            @PathVariable("id") Long id,
            HttpServletRequest request
    ) {
        String token = resolveAccessToken(request);
        if (token == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        Long userId = Long.valueOf(jwtProvider.getUid(token));

        productService.bumpProduct(id, userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<?> completeProduct(
            @PathVariable("id") Long id,
            HttpServletRequest request
    ) {
        String token = resolveAccessToken(request);
        if (token == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        Long userId = Long.valueOf(jwtProvider.getUid(token));

        productService.completeProduct(id, userId);
        return ResponseEntity.ok().build();
    }

    private ProductListDTO toListDTO(ProductEntity p) {
        String thumb = p.getPdThumb();
        if (thumb == null && p.getImages() != null && !p.getImages().isEmpty()) {
            thumb = p.getImages().get(0).getImageUrl();
        }

        Long dsell = 0L;
        Long dstatus = 0L;
        var dealOpt = dealRepository.findByProduct_PdIdx(p.getPdIdx());
        if (dealOpt.isPresent()) {
            var d = dealOpt.get();
            dsell = (d.getDSell() != null) ? d.getDSell() : 0L;
            dstatus = (d.getDStatus() != null) ? d.getDStatus() : 0L;
        }

        return ProductListDTO.builder()
                .pdIdx(p.getPdIdx())
                .pdTitle(p.getPdTitle())
                .pdPrice(p.getPdPrice())
                .pdThumb(thumb)
                .pdLocation(p.getPdLocation())
                .pdCreate(p.getPdCreate() != null ? p.getPdCreate().toString() : null)
                .dsell(dsell)
                .dstatus(dstatus)
                .build();
    }
}
