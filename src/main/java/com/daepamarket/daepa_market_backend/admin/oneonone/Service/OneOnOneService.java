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

    private static final int STATUS_OFFSET = 100;

    private final OneOnOneRepository repository;

    public List<OneOnOneEntity> getList() {
        return repository.findAllWithUser();
    }

    public OneOnOneEntity getById(Long id) {
        return repository.findByIdWithUser(id)
                .orElseThrow(() -> new RuntimeException("문의 내역을 찾을 수 없습니다."));
    }

    private int extractCategory(Integer storedStatus) {
        if (storedStatus == null) {
            return 0;
        }
        return storedStatus >= STATUS_OFFSET ? storedStatus - STATUS_OFFSET : storedStatus;
    }

    private int applyStatus(Integer storedStatus, boolean completed) {
        int category = extractCategory(storedStatus);
        if (category < 0) {
            category = 0;
        }
        return completed ? category + STATUS_OFFSET : category;
    }

    @Transactional
    public void addReply(Long id, String replyText) {
        OneOnOneEntity inquiry = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("문의 내역을 찾을 수 없습니다."));

        if (replyText == null || replyText.isBlank()) {
            inquiry.setOoRe(null);
            inquiry.setOoStatus(applyStatus(inquiry.getOoStatus(), false));
        } else {
            inquiry.setOoRe(replyText);
            inquiry.setOoStatus(applyStatus(inquiry.getOoStatus(), true));
        }

        repository.save(inquiry);
    }

    @Transactional
    public void updateStatus(Long id, String status) {
        OneOnOneEntity inquiry = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("문의 내역을 찾을 수 없습니다."));

        if (status == null) {
            throw new IllegalArgumentException("상태 값이 비어 있습니다.");
        }

        String normalized = status.trim().toLowerCase();
        switch (normalized) {
            case "pending" -> inquiry.setOoStatus(applyStatus(inquiry.getOoStatus(), false));
            case "completed" -> inquiry.setOoStatus(applyStatus(inquiry.getOoStatus(), true));
            default -> throw new IllegalArgumentException("지원하지 않는 상태 값입니다: " + status);
        }

        repository.save(inquiry);
    }

    @Transactional
    public void delete(Long id) {
        OneOnOneEntity inquiry = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("문의 내역을 찾을 수 없습니다."));
        repository.delete(inquiry);
    }
}
