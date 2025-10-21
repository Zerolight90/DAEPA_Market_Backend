package com.daepamarket.daepa_market_backend.category;

import com.daepamarket.daepa_market_backend.domain.Category.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CtUpperRepository upperRepo;
    private final CtMiddleRepository middleRepo;
    private final CtLowRepository lowRepo;

    // 상위 카테고리 조회
    // public List<CtUpperEntity> getUpperList() {
    //     return upperRepo.findAll();
    // }
    // ✅ Entity가 아닌 DTO를 반환하도록 수정
    public List<CtUpperDto> getUpperList() {
        // 1. DB에서 Entity 조회
        List<CtUpperEntity> entities = upperRepo.findAll();
        // 2. Entity 목록을 DTO 목록으로 변환 (이 과정에서 Lazy Loading 오류가 발생하지 않음)
        return entities.stream()
            .map(entity -> new CtUpperDto(entity.getUpperIdx(), entity.getUpperCt()))
            .collect(Collectors.toList());
    }
    

    // 중위 카테고리 조회
    public List<CtMiddleDto> getMiddleList(Long upperIdx) {
        List<CtMiddleEntity> entities = middleRepo.findByUpper_UpperIdx(upperIdx);

        return entities.stream()
            .map(entity -> new CtMiddleDto(entity.getUpper().getUpperIdx(), entity.getMiddleCt(), entity.getMiddleIdx()))
            .collect(Collectors.toList());
    }

    public List<CtLowDto> getLowList(Long middleIdx) {
        // (선택) 존재 검증만 수행
        middleRepo.findById(middleIdx)
                .orElseThrow(() -> new IllegalArgumentException("중위 카테고리 없음"));
        
        List<CtLowEntity> entities = lowRepo.findByMiddle_MiddleIdx(middleIdx);
                
        return entities.stream()
            .map(entity -> new CtLowDto(entity.getMiddle().getMiddleIdx(), entity.getLowCt(), entity.getLowIdx()))
            .collect(Collectors.toList());
    }

    // 생성 (관리자 전용)
    public CtUpperEntity addUpper(String name) {
        return upperRepo.save(CtUpperEntity.builder().upperCt(name).build());
    }

    public CtMiddleEntity addMiddle(String name, Long upperIdx) {
        CtUpperEntity upper = upperRepo.findById(upperIdx)
                .orElseThrow(() -> new RuntimeException("상위 카테고리 없음"));
        return middleRepo.save(CtMiddleEntity.builder()
                .middleCt(name)
                .upper(upper)
                .build());
    }

    public CtLowEntity addLow(String name, Long middleIdx) {
        CtMiddleEntity middle = middleRepo.findById(middleIdx)
                .orElseThrow(() -> new RuntimeException("중위 카테고리 없음"));
        return lowRepo.save(CtLowEntity.builder()
                .lowCt(name)
                .middle(middle)
                .build());
    }
}
