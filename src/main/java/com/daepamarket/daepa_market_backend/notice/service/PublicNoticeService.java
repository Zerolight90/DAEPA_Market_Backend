package com.daepamarket.daepa_market_backend.notice.service;

import com.daepamarket.daepa_market_backend.domain.notice.NoticeEntity;
import com.daepamarket.daepa_market_backend.domain.notice.NoticeRepository;
import com.daepamarket.daepa_market_backend.notice.dto.PublicNoticeDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class PublicNoticeService {

    private final NoticeRepository noticeRepository;

    private String getCategoryName(Byte nCategory) {
        if (nCategory == null) {
            return "기타"; // Default or unknown category
        }
        switch (nCategory) {
            case 0: return "공지사항";
            case 1: return "이벤트";
            case 2: return "업데이트";
            case 3: return "점검";
            default: return "기타";
        }
    }

    private PublicNoticeDTO toDTO(NoticeEntity e) {
        return PublicNoticeDTO.builder()
                .nIdx(e.getNIdx())

                .nSubject(e.getNSubject())
                .nContent(e.getNContent())
                .nImg(e.getNImg())
                .nDate(e.getNDate())
                .adminNick(e.getAdmin().getAdNick())
                .nCategory(e.getNCategory()) // Changed to set nCategory directly
                .nFix(e.getNFix())
                .build();
    }

    public Page<PublicNoticeDTO> findNotices(Pageable pageable) {
        Page<NoticeEntity> noticePage = noticeRepository.findAllWithAdmin(pageable);
        return noticePage.map(this::toDTO);
    }
}
