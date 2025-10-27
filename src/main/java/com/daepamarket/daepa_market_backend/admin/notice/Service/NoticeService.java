package com.daepamarket.daepa_market_backend.admin.notice.Service;

import com.daepamarket.daepa_market_backend.domain.admin.AdminEntity;
import com.daepamarket.daepa_market_backend.domain.admin.AdminRepository;
import com.daepamarket.daepa_market_backend.admin.notice.DTO.NoticeRequestDTO;
import com.daepamarket.daepa_market_backend.admin.notice.DTO.NoticeResponseDTO;
import com.daepamarket.daepa_market_backend.domain.notice.NoticeEntity;
import com.daepamarket.daepa_market_backend.domain.notice.NoticeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NoticeService {

    private final NoticeRepository noticeRepository;
    private final AdminRepository adminRepository;

    /** 공통 변환 메서드: Entity → ResponseDTO */
    private NoticeResponseDTO toDTO(NoticeEntity e) {
        return NoticeResponseDTO.builder()
                .nIdx(e.getNIdx())
                .nSubject(e.getNSubject())
                .nContent(e.getNContent())
                .nImg(e.getNImg())
                .nDate(e.getNDate().toString())
                .nIp(e.getNIp())
                .nCategory(e.getNCategory())
                .build();
    }


    /** 전체 목록 DTO 반환 */
    public List<NoticeResponseDTO> findAllDTO() {
        return noticeRepository.findAll()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /** 상세 조회 DTO 반환 */
    public NoticeResponseDTO findByIdDTO(Long id) {
        return toDTO(findById(id)); // 아래 findById 재사용
    }

    /** 내부 공용: Entity 조회 */
    private NoticeEntity findById(Long id) {
        return noticeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("공지사항이 존재하지 않습니다."));
    }

    /** 공지 작성 (DTO 받아서 Entity 저장 후 DTO로 반환) */
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

    /** 수정 — 나중에 RequestDTO로 변경 예정 */
    public NoticeResponseDTO update(Long id, NoticeEntity req) {
        NoticeEntity origin = findById(id);

        origin.setNCategory(req.getNCategory());
        origin.setNSubject(req.getNSubject());
        origin.setNContent(req.getNContent());
        origin.setNImg(req.getNImg());
        origin.setNIp(req.getNIp());

        return toDTO(noticeRepository.save(origin));
    }

    /** 삭제 */
    public void delete(Long id) {
        noticeRepository.deleteById(id);
    }
}
