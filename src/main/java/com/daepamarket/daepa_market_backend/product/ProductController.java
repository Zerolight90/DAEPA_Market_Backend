// com.daepamarket.daepa_market_backend.product.ProductController
package com.daepamarket.daepa_market_backend.product;

import com.daepamarket.daepa_market_backend.domain.product.ProductEntity;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.daepamarket.daepa_market_backend.product.ProductListDTO;
import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    // (참고) JSON body만 받는 기존 엔드포인트
    // @PostMapping("/create")
    // public ResponseEntity<Long> create(
    //         @RequestHeader("X-USER-ID") Long userIdx,
    //         @Valid @RequestBody ProductCreateDTO dto
    // ) {
    //     Long id = productService.register(userIdx, dto);
    //     return ResponseEntity.ok(id);
    // }

    /** ✅ 멀티파트(JSON + files)로 등록 */
    @PostMapping(value="/create-multipart", consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createMultipart(
            @RequestHeader("X-USER-ID") Long userIdx,
            @Valid @RequestPart("dto") ProductCreateDTO dto,
            BindingResult br,
            @RequestPart(value="images", required=false) List<MultipartFile> images
    ) {
        System.out.println("[controller] images part size = " + (images == null ? -1 : images.size()));
        if (br.hasErrors()) {
            var errors = br.getFieldErrors().stream()
                    .map(e -> e.getField() + " : " + e.getDefaultMessage())
                    .toList();
            return ResponseEntity.badRequest().body(errors);
        }
        Long id = productService.registerMultipart(userIdx, dto, images);
        return ResponseEntity.ok(id);
    }



    // ID 필터
    @GetMapping
    public ResponseEntity<?> listByIds(
            @RequestParam(required = false) Long upperId,
            @RequestParam(required = false) Long middleId,
            @RequestParam(required = false) Long lowId,
            @RequestParam(defaultValue = "recent") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "40") int size
    ) {
        return ResponseEntity.ok(productService.getProductsByIds(upperId, middleId, lowId, sort, page, size));
    }

    // (옵션) 이름 필터 버전: /api/products/by-name?big=전자제품&mid=노트북/PC&sub=맥

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
    // ProductController.java
    private ProductListDTO toListDTO(ProductEntity p) {
        // 썸네일은 pdThumb가 우선, 없으면 첫 번째 이미지
        String thumb = p.getPdThumb();
        if (thumb == null && p.getImages() != null && !p.getImages().isEmpty()) {
            thumb = p.getImages().get(0).getImageUrl();
        }

        return ProductListDTO.builder()
                .id(p.getPdIdx())
                .title(p.getPdTitle())
                .price(p.getPdPrice())
                .location(p.getPdLocation())
                .thumbnail(thumb)
                .createdAt(p.getPdCreate() != null ? p.getPdCreate().toString() : null)
                .build();
    }
}
