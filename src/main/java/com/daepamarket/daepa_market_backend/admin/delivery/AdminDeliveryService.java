package com.daepamarket.daepa_market_backend.admin.delivery;

import com.daepamarket.daepa_market_backend.domain.delivery.DeliveryEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminDeliveryService {

    private final AdminDeliveryRepository deliveryRepository;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public List<DeliveryDTO> getAllDeliveries() {
        return deliveryRepository.findAllWithDetails().stream()
                .map(row -> {
                    Long dvIdx = ((Number) row[0]).longValue();
                    Long dIdx = ((Number) row[1]).longValue();
                    String productName = (String) row[2];
                    String sellerName = (String) row[3];
                    String buyerName = (String) row[4];
                    String address = (String) row[5];
                    String addressDetail = row[6] != null ? (String) row[6] : "";
                    Integer dvStatus = row[7] != null ? ((Number) row[7]).intValue() : 0;
                    String tradeType = (String) row[8];
                    String dealDate = "";
                    if (row[9] != null) {
                        if (row[9] instanceof java.sql.Timestamp) {
                            dealDate = ((java.sql.Timestamp) row[9]).toLocalDateTime().toLocalDate().format(DATE_FORMATTER);
                        } else if (row[9] instanceof java.time.LocalDate) {
                            dealDate = ((java.time.LocalDate) row[9]).format(DATE_FORMATTER);
                        }
                    }

                    return new DeliveryDTO(dvIdx, dIdx, productName, sellerName, buyerName, 
                            address, addressDetail, dvStatus, tradeType, dealDate);
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public void updateDeliveryStatus(Long dvIdx, Integer status) {
        DeliveryEntity delivery = deliveryRepository.findById(dvIdx)
                .orElseThrow(() -> new RuntimeException("배송 정보를 찾을 수 없습니다."));
        delivery.updateStatus(status);
        deliveryRepository.save(delivery);
    }
}

