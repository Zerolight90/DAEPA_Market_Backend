package com.daepamarket.daepa_market_backend.product;

import com.daepamarket.daepa_market_backend.domain.product.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/sellers")
public class SellerController {

    private final ProductRepository productRepository;

    /**
     * 판매자 페이지에서 쓰는 목록
     * - 삭제된 상품(p.pdDel = true) 제외
     * - 판매완료 후 3일 지난 상품 제외
     */
    @GetMapping("/{sellerId}/products")
    public List<ProductCardDTO> listProductsBySeller(
            @PathVariable Long sellerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "pdIdx"));
        LocalDateTime cutoff = LocalDateTime.now().minusDays(3);

        return productRepository
                .findAlivePageBySellerId(sellerId, cutoff, pageable)   // ✅ 여기만 변경
                .map(ProductCardDTO::from)
                .getContent();
    }
}
