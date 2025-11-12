package com.daepamarket.daepa_market_backend.admin.oneonone.Service;

import com.daepamarket.daepa_market_backend.domain.oneonone.OneOnOneEntity;
import com.daepamarket.daepa_market_backend.domain.oneonone.OneOnOneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OneOnOneService {

    private final OneOnOneRepository repository;

    public List<OneOnOneEntity> getList() {
        return repository.findAllWithUser();
    }

    public OneOnOneEntity getById(Long id) {
        return repository.findByIdWithUser(id)
                .orElseThrow(() -> new RuntimeException("문의 내역을 찾을 수 없습니다."));
    }

    // 답변 저장 기능만 (상태 변경 없음)
    @Transactional
    public void addReply(Long id, String replyText) {
        OneOnOneEntity inquiry = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("문의 내역을 찾을 수 없습니다."));
        inquiry.setOoRe(replyText);
        repository.save(inquiry);
    }
}
