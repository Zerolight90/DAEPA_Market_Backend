// src/main/java/com/daepamarket/daepa_market_backend/naga/NagaService.java
package com.daepamarket.daepa_market_backend.naga;

import com.daepamarket.daepa_market_backend.domain.naga.NagaEntity;
import com.daepamarket.daepa_market_backend.domain.naga.NagaRepository;
import com.daepamarket.daepa_market_backend.domain.product.ProductEntity;
import com.daepamarket.daepa_market_backend.domain.product.ProductRepository;
import com.daepamarket.daepa_market_backend.domain.user.UserEntity;
import com.daepamarket.daepa_market_backend.naga.dto.NagaReportRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class NagaService {

    private final NagaRepository nagaRepository;
    private final ProductRepository productRepository;

    // NagaService.java
    @Transactional
    public Long report(UserEntity reporter, NagaReportRequest req) {
        Long reporterId = reporter.getUIdx(); // 신고한 사람 (me)

        ProductEntity product = productRepository.findById(req.getProductId())
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));
        Long sellerId = product.getSeller().getUIdx(); // 신고받은 사람 (seller)

        if (sellerId.equals(reporterId)) {
            throw new IllegalStateException("본인이 등록한 상품은 신고할 수 없습니다.");
        }

        String content = req.getNgContent() == null ? "" : req.getNgContent().trim();
        if (content.length() > 400) content = content.substring(0, 400);

        // 🔁 여기서 '방향'을 스키마에 맞게 설정!
        NagaEntity entity = NagaEntity.builder()
                .bIdx2(sellerId)                  // 신고받은 사람(판매자)  ← b_idx2
                .sIdx(reporterId)                 // 신고한 사람(나)       ← s_idx
                .ngStatus(req.getNgStatus())      // 1~4
                .ngContent(content)
                .ngDate(LocalDate.now())
                .build();

        return nagaRepository.save(entity).getNgIdx();
    }

}
