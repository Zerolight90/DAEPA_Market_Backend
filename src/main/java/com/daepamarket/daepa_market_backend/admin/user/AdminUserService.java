package com.daepamarket.daepa_market_backend.admin.user;

import com.daepamarket.daepa_market_backend.admin.review.SaleReviewDTO;
import com.daepamarket.daepa_market_backend.admin.review.SaleReviewRepository;
import com.daepamarket.daepa_market_backend.domain.location.LocationEntity;
import com.daepamarket.daepa_market_backend.domain.location.LocationRepository;
import com.daepamarket.daepa_market_backend.domain.naga.NagaRepository;
import com.daepamarket.daepa_market_backend.domain.user.UserRepository;
import com.daepamarket.daepa_market_backend.domain.user.UserEntity;
import com.daepamarket.daepa_market_backend.domain.deal.DealEntity;
import com.daepamarket.daepa_market_backend.admin.deal.AdminDealRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;
    private final AdminDealRepository dealRepository;
    private final NagaRepository nagaRepository;
    private final SaleReviewRepository saleReviewRepository;
    private final LocationRepository locationRepository;


    public UserDetailDTO getUserDetail(Long uIdx) {
        UserEntity user = userRepository.findById(uIdx)
                .orElseThrow(() -> new RuntimeException("유저를 찾을 수 없습니다."));

        // 구매/판매 거래 조회
        List<DealEntity> buys = dealRepository.findByBuyer_uIdx(uIdx);
        List<DealEntity> sells = dealRepository.findBySeller_uIdx(uIdx);

        // 하나의 거래 리스트로 합치기
        List<TradeHistoryDTO> history = new ArrayList<>();
        buys.forEach(d -> history.add(TradeHistoryDTO.fromEntity(d, "BUY")));
        sells.forEach(d -> history.add(TradeHistoryDTO.fromEntity(d, "SELL")));

        // 최신순 정렬
        history.sort((a, b) -> {
            if (a.getDate() == null) return 1;
            if (b.getDate() == null) return -1;
            return b.getDate().compareTo(a.getDate());
        });

        // 유저 DTO 매핑 + 거래내역 넣기
        UserDetailDTO dto = UserDetailDTO.fromEntity(user);
        dto.setTradeHistory(history);

        // 주소 정보 조회 (상세 주소 포함)
        List<LocationEntity> locations = locationRepository.findByUserId(uIdx);
        if (locations != null && !locations.isEmpty()) {
            // 기본 주소가 있으면 사용, 없으면 첫 번째 주소 사용
            LocationEntity defaultLocation = locations.stream()
                    .filter(LocationEntity::isLocDefault)
                    .findFirst()
                    .orElse(locations.get(0));
            
            if (defaultLocation != null) {
                String address = defaultLocation.getLocAddress() != null ? defaultLocation.getLocAddress().trim() : "";
                String detail = defaultLocation.getLocDetail() != null ? defaultLocation.getLocDetail().trim() : "";
                
                // 주소와 상세 주소를 합쳐서 설정
                if (!address.isEmpty() || !detail.isEmpty()) {
                    dto.setULocation(address + (detail.isEmpty() ? "" : " " + detail));
                }
            }
        }

        // 신고 내역 조회
        List<ReportHistoryDTO> reports = nagaRepository.findReportsByUserId(uIdx);
        dto.setReportHistory(reports);

        return dto;
    }

    @Transactional
    public void updateManner(Long uIdx, Double manner) {
        UserEntity user = userRepository.findById(uIdx)
                .orElseThrow(() -> new RuntimeException("유저를 찾을 수 없습니다."));
        user.setUManner(manner);
        userRepository.save(user);
    }

    // 판매 후기 조회
    public List<SaleReviewDTO> getUserSaleReviews(Long userId) {
        return saleReviewRepository.findSaleReviewsBySeller(userId);
    }

    @Transactional
    public void updateUser(Long uIdx, UserUpdateDTO dto) {
        UserEntity user = userRepository.findById(uIdx)
                .orElseThrow(() -> new RuntimeException("유저를 찾을 수 없습니다."));

        // 사용자 정보 업데이트
        if (dto.getUname() != null && !dto.getUname().trim().isEmpty()) {
            user.setUname(dto.getUname().trim());
        }
        if (dto.getUnickname() != null) {
            user.setUnickname(dto.getUnickname().trim());
        }
        if (dto.getUphone() != null && !dto.getUphone().trim().isEmpty()) {
            user.setUphone(dto.getUphone().trim());
        }
        if (dto.getUbirth() != null && !dto.getUbirth().trim().isEmpty()) {
            user.setUBirth(dto.getUbirth().trim());
        }
        if (dto.getUgender() != null && !dto.getUgender().trim().isEmpty()) {
            user.setUGender(dto.getUgender().trim());
        }
        if (dto.getUstatus() != null) {
            user.setUStatus(dto.getUstatus());
        }
        if (dto.getUwarn() != null) {
            user.setUWarn(dto.getUwarn());
        }

        userRepository.save(user);

        // 주소 정보 업데이트
        if (dto.getLoc_address() != null || dto.getLoc_detail() != null) {
            List<LocationEntity> locations = locationRepository.findByUserId(uIdx);
            LocationEntity defaultLocation = null;

            if (locations != null && !locations.isEmpty()) {
                defaultLocation = locations.stream()
                        .filter(LocationEntity::isLocDefault)
                        .findFirst()
                        .orElse(locations.get(0));
            }

            String address = dto.getLoc_address() != null ? dto.getLoc_address().trim() : null;
            String detail = dto.getLoc_detail() != null ? dto.getLoc_detail().trim() : null;

            if (defaultLocation != null) {
                // 기존 주소 업데이트
                if (address != null) {
                    defaultLocation.setLocAddress(address);
                }
                if (detail != null) {
                    defaultLocation.setLocDetail(detail);
                }
                locationRepository.save(defaultLocation);
            } else if (address != null && !address.isEmpty()) {
                // 새 주소 생성 (주소가 비어있지 않을 때만)
                LocationEntity newLocation = LocationEntity.builder()
                        .user(user)
                        .locAddress(address)
                        .locDetail(detail != null ? detail : "")
                        .locDefault(true)
                        .build();
                locationRepository.save(newLocation);
            }
        }
    }

}