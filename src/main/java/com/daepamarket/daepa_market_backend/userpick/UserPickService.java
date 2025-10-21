package com.daepamarket.daepa_market_backend.userpick;

import java.util.List;

import com.daepamarket.daepa_market_backend.domain.userpick.UserPickEntity;
import com.daepamarket.daepa_market_backend.domain.userpick.UserPickRepository;
import org.springframework.stereotype.Service;

import com.daepamarket.daepa_market_backend.domain.Category.CtLowEntity;
import com.daepamarket.daepa_market_backend.domain.Category.CtLowRepository;
import com.daepamarket.daepa_market_backend.domain.Category.CtMiddleEntity;
import com.daepamarket.daepa_market_backend.domain.Category.CtMiddleRepository;
import com.daepamarket.daepa_market_backend.domain.Category.CtUpperEntity;
import com.daepamarket.daepa_market_backend.domain.Category.CtUpperRepository;
import com.daepamarket.daepa_market_backend.domain.user.UserEntity;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserPickService {

    private final UserPickRepository userPickRepository;

    private final CtUpperRepository ctUpperRepository;
    private final CtMiddleRepository ctMiddleRepository;
    private final CtLowRepository ctLowRepository;

    // 특정 사용자의 모든 관심 상품 목록 조회
    public List<UserPickEntity> findPicksByUser(UserEntity user) {
        return userPickRepository.findByUser(user);
    }

    // 관심 상품 삭제
    public void deletePick(Long upIdx) {
        // 먼저 해당 ID의 데이터가 존재하는지 확인 (선택사항이지만 더 안전)
        if (!userPickRepository.existsById(upIdx)) {
            throw new RuntimeException("해당 관심 상품을 찾을 수 없습니다. ID: " + upIdx);
        }
        userPickRepository.deleteById(upIdx);
    }

    // 관심 상품 생성 및 저장 메소드 추가
    public UserPickEntity createPick(UserPickCreateRequestDto requestDto, UserEntity user) {
        CtUpperEntity ctUpperEntity = ctUpperRepository.findByUpperCt(requestDto.getUpperCategory())
            .orElseThrow(() -> new RuntimeException("상위 카테고리를 찾을 수 없음: " + requestDto.getUpperCategory()));

        CtMiddleEntity ctMiddleEntity = ctMiddleRepository.findByMiddleCt(requestDto.getMiddleCategory())
            .orElseThrow(() -> new RuntimeException("중간 카테고리를 찾을 수 없음: " + requestDto.getMiddleCategory()));

        CtLowEntity ctLowEntity = ctLowRepository.findByLowCt(requestDto.getLowCategory())
            .orElseThrow(() -> new RuntimeException("하위 카테고리를 찾을 수 없음: " + requestDto.getLowCategory()));

        UserPickEntity newPick = UserPickEntity.builder()
                .user(user)
                // DTO에서 받은 데이터로 엔티티 필드 설정
                // 실제 UserPickEntity 필드명에 맞게 set 메소드 호출 필요
                // 예: .setCtUpper(requestDto.getUpperCategory())
                .ctUpper(ctUpperEntity)
                .ctMiddle(ctMiddleEntity)
                .ctLow(ctLowEntity)
                .minPrice(requestDto.getMinPrice())
                .maxPrice(requestDto.getMaxPrice())
                .build();
        
        return userPickRepository.save(newPick);
    }

}
