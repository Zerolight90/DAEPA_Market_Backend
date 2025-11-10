package com.daepamarket.daepa_market_backend.oneonone;

import com.daepamarket.daepa_market_backend.S3Service;
import com.daepamarket.daepa_market_backend.domain.oneonone.OneOnOneEntity;
import com.daepamarket.daepa_market_backend.domain.user.UserEntity;
import com.daepamarket.daepa_market_backend.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserOneOnOneService {

    private final UserOneOnOneRepository oneOnOneRepository;
    private final UserRepository userRepository;
    private final S3Service s3Service;

    @Transactional
    public Long create(Long userId, UserOneOnOneCreateDTO dto, MultipartFile photo) {

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다. id=" + userId));

        String photoUrl = null;
        if (photo != null && !photo.isEmpty()) {
            try {
                // 폴더 이름은 편한 걸로 바꿔도 됨
                photoUrl = s3Service.uploadFile(photo, "inquiries");
            } catch (IOException e) {
                throw new RuntimeException("문의 이미지 업로드 실패", e);
            }
        }

        OneOnOneEntity entity = OneOnOneEntity.builder()
                .user(user)
                .ooStatus(dto.getStatus())
                .ooTitle(dto.getTitle())
                .ooContent(dto.getContent())
                .ooPhoto(photoUrl)
                .ooDate(LocalDate.now())
                .ooRe(null)        // 답변 전이니까 null
                .build();

        oneOnOneRepository.save(entity);
        return entity.getOoIdx();
    }

    @Transactional(readOnly = true)
    public List<OneOnOneEntity> getMyInquiries(Long userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다. id=" + userId));
        return oneOnOneRepository.findByUserOrderByOoDateDesc(user);
    }
}
