package com.daepamarket.daepa_market_backend.product;

import com.daepamarket.daepa_market_backend.domain.product.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/sellers")
public class SellerController {

    private final ProductRepository productRepository;

    @GetMapping("/{sellerId}/products")
    public List<ProductCardDTO> listProductsBySeller(
            @PathVariable Long sellerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "pdIdx"));
        return productRepository.findPageBySellerId(sellerId, pageable)
                .map(ProductCardDTO::from)
                .getContent();
    }
}