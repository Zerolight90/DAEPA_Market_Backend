package com.daepamarket.daepa_market_backend.admin.user;

import com.daepamarket.daepa_market_backend.admin.deal.AdminDealRepository;
import com.daepamarket.daepa_market_backend.admin.review.AllReviewDTO;
import com.daepamarket.daepa_market_backend.domain.deal.DealEntity;
import com.daepamarket.daepa_market_backend.domain.location.LocationEntity;
import com.daepamarket.daepa_market_backend.domain.location.LocationRepository;
import com.daepamarket.daepa_market_backend.domain.naga.NagaRepository;
import com.daepamarket.daepa_market_backend.domain.review.ReviewRepository;
import com.daepamarket.daepa_market_backend.domain.user.UserEntity;
import com.daepamarket.daepa_market_backend.domain.user.UserRepository;
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
    private final LocationRepository locationRepository;
    private final ReviewRepository reviewRepository;  // ✅ 통합 리뷰

    public UserDetailDTO getUserDetail(Long uIdx) {
        UserEntity user = userRepository.findById(uIdx)
                .orElseThrow(() -> new RuntimeException("유저를 찾을 수 없습니다."));

        // 구매/판매 거래 조회
        List<DealEntity> buys = dealRepository.findByBuyer_uIdx(uIdx);
        List<DealEntity> sells = dealRepository.findBySeller_uIdx(uIdx);

        List<TradeHistoryDTO> history = new ArrayList<>();
        buys.forEach(d -> history.add(TradeHistoryDTO.fromEntity(d, "BUY")));
        sells.forEach(d -> history.add(TradeHistoryDTO.fromEntity(d, "SELL")));

        history.sort((a, b) -> {
            if (a.getDate() == null) return 1;
            if (b.getDate() == null) return -1;
            return b.getDate().compareTo(a.getDate());
        });

        UserDetailDTO dto = UserDetailDTO.fromEntity(user);
        dto.setTradeHistory(history);

        // 주소
        List<LocationEntity> locations = locationRepository.findByUserId(uIdx);
        if (locations != null && !locations.isEmpty()) {
            LocationEntity defaultLocation = locations.stream()
                    .filter(LocationEntity::isLocDefault)
                    .findFirst()
                    .orElse(locations.get(0));

            if (defaultLocation != null) {
                String address = defaultLocation.getLocAddress() != null ? defaultLocation.getLocAddress().trim() : "";
                String detail = defaultLocation.getLocDetail() != null ? defaultLocation.getLocDetail().trim() : "";
                if (!address.isEmpty() || !detail.isEmpty()) {
                    dto.setULocation(address + (detail.isEmpty() ? "" : " " + detail));
                }
            }
        }

        List<ReportHistoryDTO> reports = nagaRepository.findReportsByUserId(uIdx);
        dto.setReportHistory(reports);
        dto.setReportCount(reports != null ? reports.size() : 0);

        List<AllReviewDTO> reviews = reviewRepository.findReviewRowsByTargetUser(uIdx);
        dto.setReviews(reviews);

        return dto;
    }

    @Transactional
    public void updateManner(Long uIdx, Double manner) {
        UserEntity user = userRepository.findById(uIdx)
                .orElseThrow(() -> new RuntimeException("유저를 찾을 수 없습니다."));
        user.setUManner(manner);
        userRepository.save(user);
    }

    /**
     * 이 유저가 관련된(구매자/판매자) 리뷰들만
     */
    // AdminUserService 안
    public List<com.daepamarket.daepa_market_backend.admin.review.AllReviewDTO> getUserReviews(Long userId) {
        return reviewRepository.findReviewRowsByTargetUser(userId);
    }


    @Transactional
    public void updateUser(Long uIdx, UserUpdateDTO dto) {
        UserEntity user = userRepository.findById(uIdx)
                .orElseThrow(() -> new RuntimeException("유저를 찾을 수 없습니다."));

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
        if (dto.getUmanner() != null) {
            user.setUManner(dto.getUmanner());
        }

        userRepository.save(user);

        // 주소 반영
        if (dto.getLoc_address() != null || dto.getLoc_detail() != null) {
            List<LocationEntity> locations = locationRepository.findByUserId(uIdx);
            LocationEntity defaultLoc = null;
            if (locations != null && !locations.isEmpty()) {
                defaultLoc = locations.stream()
                        .filter(LocationEntity::isLocDefault)
                        .findFirst()
                        .orElse(locations.get(0));
            }

            String address = dto.getLoc_address() != null ? dto.getLoc_address().trim() : null;
            String detail = dto.getLoc_detail() != null ? dto.getLoc_detail().trim() : null;

            if (defaultLoc != null) {
                if (address != null) defaultLoc.setLocAddress(address);
                if (detail != null) defaultLoc.setLocDetail(detail);
                locationRepository.save(defaultLoc);
            } else if (address != null && !address.isEmpty()) {
                LocationEntity created = LocationEntity.builder()
                        .user(user)
                        .locAddress(address)
                        .locDetail(detail != null ? detail : "")
                        .locDefault(true)
                        .build();
                locationRepository.save(created);
            }
        }
    }
}

