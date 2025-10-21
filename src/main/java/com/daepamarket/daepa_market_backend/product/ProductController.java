// com.daepamarket.daepa_market_backend.product.ProductController
package com.daepamarket.daepa_market_backend.product;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
            BindingResult br, // ⬅️ 추가
            @RequestPart(value="images", required=false) List<MultipartFile> images // ⬅️ 이름 "images" 확인!
    ) {
        if (br.hasErrors()) {
            // 어떤 필드가 왜 실패했는지 클라이언트에 보여줌
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
    public ResponseEntity<?> listByNames(
            @RequestParam(required = false) String big,
            @RequestParam(required = false) String mid,
            @RequestParam(required = false) String sub,
            @RequestParam(defaultValue = "recent") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "40") int size
    ) {
        return ResponseEntity.ok(productService.getProductsByNames(big, mid, sub, sort, page, size));
    }
}
