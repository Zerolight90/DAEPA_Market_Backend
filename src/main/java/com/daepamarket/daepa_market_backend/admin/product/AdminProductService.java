package com.daepamarket.daepa_market_backend.admin.product;

import com.daepamarket.daepa_market_backend.admin.product.dto.AdminProductSummaryDTO;
import com.daepamarket.daepa_market_backend.domain.product.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.format.DateTimeFormatter;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class AdminProductService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public Page<AdminProductSummaryDTO> getProducts(int page, int size, String status) {
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1));
        String normalizedStatus = normalizeStatus(status);
        return productRepository.findAdminProducts(normalizedStatus, pageable)
                .map(this::convertToDto);
    }

    @Transactional
    public void softDeleteProduct(Long productId) {
        var product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));

        if (!product.isPdDel()) {
            product.setPdDel(true);
            productRepository.save(product);
        }
    }

    private AdminProductSummaryDTO convertToDto(AdminProductProjection projection) {
        String category = Stream.of(projection.getUpperCt(), projection.getMiddleCt(), projection.getLowCt())
                .filter(StringUtils::hasText)
                .reduce((a, b) -> a + " / " + b)
                .orElse(null);

        String createdAt = projection.getPdCreate() != null
                ? projection.getPdCreate().format(DATE_TIME_FORMATTER)
                : null;

        boolean reported = projection.getReportCount() != null && projection.getReportCount() > 0;
        boolean sold = (projection.getDealStatus() != null && projection.getDealStatus() > 0)
                || (projection.getDealSell() != null && projection.getDealSell() > 0);
        String saleStatus = sold ? "판매완료" : "판매중";

        return new AdminProductSummaryDTO(
                projection.getPdIdx(),
                projection.getPdTitle(),
                projection.getPdPrice(),
                projection.getPdThumb(),
                category,
                createdAt,
                projection.getSellerId(),
                projection.getSellerName(),
                saleStatus,
                reported
        );
    }

    private String normalizeStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return null;
        }
        String value = status.trim().toUpperCase();
        return switch (value) {
            case "ALL", "전체" -> null;
            case "ON-SALE", "ON_SALE", "판매중" -> "ON_SALE";
            case "SOLD-OUT", "SOLD_OUT", "판매완료" -> "SOLD_OUT";
            case "REPORTED", "신고" -> "REPORTED";
            default -> null;
        };
    }
}

