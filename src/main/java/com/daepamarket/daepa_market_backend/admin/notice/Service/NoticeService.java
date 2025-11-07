package com.daepamarket.daepa_market_backend.admin.notice.Service;

import com.daepamarket.daepa_market_backend.admin.notice.DTO.NoticeUpdateDTO;
import com.daepamarket.daepa_market_backend.domain.admin.AdminEntity;
import com.daepamarket.daepa_market_backend.domain.admin.AdminRepository;
import com.daepamarket.daepa_market_backend.admin.notice.DTO.NoticeRequestDTO;
import com.daepamarket.daepa_market_backend.admin.notice.DTO.NoticeResponseDTO;
import com.daepamarket.daepa_market_backend.domain.notice.NoticeEntity;
import com.daepamarket.daepa_market_backend.domain.notice.NoticeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NoticeService {

    private final NoticeRepository noticeRepository;
    private final AdminRepository adminRepository;

    /** Entity → DTO 변환 */
    private NoticeResponseDTO toDTO(NoticeEntity e) {
        return NoticeResponseDTO.builder()
                .nIdx(e.getNIdx())
                .nSubject(e.getNSubject())
                .nContent(e.getNContent())
                .nImg(e.getNImg())
                .nDate(e.getNDate().toString())
                .nIp(e.getNIp())

                .nCategory(e.getNCategory())
                .adminNick(e.getAdmin().getAdNick())
                .build();
    }

    /** 전체조회 */
    public List<NoticeResponseDTO> findAllDTO() {
        return noticeRepository.findAllWithAdmin()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /** 상세조회 */
    public NoticeResponseDTO findByIdDTO(Long id) {
        return noticeRepository.findByIdWithAdmin(id)
                .map(this::toDTO)
                .orElseThrow(() -> new RuntimeException("공지사항이 존재하지 않습니다."));
    }

    /** 등록 */
    public NoticeResponseDTO createNotice(NoticeRequestDTO req) {
        AdminEntity admin = adminRepository.findById(req.getAdIdx())
                .orElseThrow(() -> new RuntimeException("관리자 정보가 존재하지 않습니다."));

        NoticeEntity notice = NoticeEntity.builder()
                .admin(admin)
                .nSubject(req.getNSubject())
                .nContent(req.getNContent())
                .nCategory(req.getNCategory())
                .nImg(req.getNImg())
                .nIp(req.getNIp())
                .nDate(LocalDate.now())
                .build();

        return toDTO(noticeRepository.save(notice));
    }

    /** 수정 — LazyException 방지용 @Transactional */
    @Transactional
    public NoticeResponseDTO update(Long id, NoticeUpdateDTO req) {
        NoticeEntity origin = noticeRepository.findByIdWithAdmin(id)
                .orElseThrow(() -> new RuntimeException("공지사항 없음"));

        if (req.getNCategory() != null) {origin.setNCategory(req.getNCategory());}
        origin.setNSubject(req.getNSubject());
        origin.setNContent(req.getNContent());
        // 수정 시 등록 날짜를 현재 날짜로 업데이트
        origin.setNDate(LocalDate.now());

        return toDTO(noticeRepository.save(origin));
    }

    /** 삭제 */
    public void delete(Long id) {
        noticeRepository.deleteById(id);
    }
}
