package com.daepamarket.daepa_market_backend.admin.notice.Service;

import com.daepamarket.daepa_market_backend.S3Service;
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
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NoticeService {

    private final NoticeRepository noticeRepository;
    private final AdminRepository adminRepository;
    private final S3Service s3Service;

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
                .nFix(e.getNFix())
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
    public NoticeResponseDTO createNotice(NoticeRequestDTO req, MultipartFile file) {
        AdminEntity admin = adminRepository.findById(req.getAdIdx())
                .orElseThrow(() -> new RuntimeException("관리자 정보가 존재하지 않습니다."));

        // 이미지 파일이 있는 경우 S3에 업로드하고 URL을 받아옴
        String imageUrl = null;
        if (file != null && !file.isEmpty()) {
            try {
                imageUrl = s3Service.uploadFile(file, "notice");
            } catch (IOException e) {
                throw new RuntimeException("S3 파일 업로드에 실패했습니다.", e);
            }
        }

        NoticeEntity notice = NoticeEntity.builder()
                .admin(admin)
                .nSubject(req.getNSubject())
                .nContent(req.getNContent())
                .nCategory(req.getNCategory())
                .nImg(imageUrl) // S3에서 받은 URL로 설정
                .nIp(req.getNIp())
                .nDate(LocalDate.now())
                .nFix(req.getNFix() != null ? req.getNFix() : (byte)0) // nFix 값 설정, 기본값을 byte로 명시적 캐스팅
                .build();

        return toDTO(noticeRepository.save(notice));
    }

    /** 수정 — LazyException 방지용 @Transactional */
    @Transactional
    public NoticeResponseDTO update(Long id, NoticeUpdateDTO req, MultipartFile file) {
        // 1. 기존 공지사항 엔티티를 조회
        NoticeEntity origin = noticeRepository.findByIdWithAdmin(id)
                .orElseThrow(() -> new RuntimeException("공지사항 없음"));

        String oldImageUrl = origin.getNImg();
        String newImageUrl = oldImageUrl; // 기본적으로 기존 이미지 URL을 유지

        try {
            // 2. 이미지 파일 처리 로직
            // 2-1. 새 파일이 업로드된 경우 (이미지 교체 또는 추가)
            if (file != null && !file.isEmpty()) {
                newImageUrl = s3Service.uploadFile(file, "notice");
                // 기존 이미지가 있었다면 S3에서 삭제
                if (oldImageUrl != null) {
                    s3Service.deleteFile(oldImageUrl);
                }
            }
            // 2-2. 새 파일은 없지만, DTO의 이미지 URL이 null로 온 경우 (기존 이미지 삭제 요청)
            else if (req.getNImg() == null && oldImageUrl != null) {
                s3Service.deleteFile(oldImageUrl);
                newImageUrl = null; // DB에 저장될 URL을 null로 설정
            }
        } catch (IOException e) {
            throw new RuntimeException("S3 파일 처리 중 오류가 발생했습니다.", e);
        }

        // 3. 엔티티 정보 업데이트
        origin.setNImg(newImageUrl); // 최종 이미지 URL로 업데이트
        if (req.getNCategory() != null) {origin.setNCategory(req.getNCategory());}
        if (req.getNFix() != null) {origin.setNFix(req.getNFix());} // nFix 값 업데이트
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
