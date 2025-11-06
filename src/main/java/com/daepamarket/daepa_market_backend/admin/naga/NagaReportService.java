package com.daepamarket.daepa_market_backend.admin.naga;

import com.daepamarket.daepa_market_backend.domain.naga.NagaEntity;
import com.daepamarket.daepa_market_backend.domain.naga.NagaRepository;
import com.daepamarket.daepa_market_backend.domain.stop.StopEntity;
import com.daepamarket.daepa_market_backend.domain.stop.StopRepository;
import com.daepamarket.daepa_market_backend.domain.getout.GetoutEntity;
import com.daepamarket.daepa_market_backend.domain.getout.GetoutRepository;
import com.daepamarket.daepa_market_backend.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NagaReportService {

    private final NagaRepository nagaRepository;
    private final UserRepository userRepository;
    private final StopRepository stopRepository;
    private final GetoutRepository getoutRepository;

    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Transactional(readOnly = true)
    public List<NagaReportDTO> getAllReports() {
        List<NagaEntity> rows = nagaRepository.findAllOrderByNgDateDesc();

        return rows.stream().map(r -> {
            NagaReportDTO dto = new NagaReportDTO();
            dto.setId(r.getNgIdx());

            String reporter = userRepository.findNicknameById(r.getSIdx());
            String reported = userRepository.findNicknameById(r.getBIdx2());

            dto.setReporterName(reporter != null ? reporter : ("유저#" + r.getSIdx()));
            dto.setReportedName(reported != null ? reported : ("유저#" + r.getBIdx2()));

            Integer status = r.getNgStatus();
            String type =
                    status == 1 ? "fraud" :
                            status == 2 ? "abuse" :
                                    status == 3 ? "spam" : "other";

            dto.setType(type);
            dto.setContent(r.getNgContent());

            if (r.getNgDate() != null) {
                dto.setCreatedAt(r.getNgDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            }

            return dto;
        }).collect(Collectors.toList());
    }

    /** 계정 정지 */
    @Transactional
    public void suspendUser(Long reportId, StopDTO request) {
        NagaEntity report = nagaRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("신고 데이터 없음"));

        Long userId = report.getBIdx2();

        var user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("유저 없음"));

        long plusDays = convertDuration(request.getDuration());

        StopEntity stop = StopEntity.builder()
                .user(user)
                .stopDate(LocalDate.parse(request.getSuspendDate(), ISO_DATE)) // 입력일
                .stopContent(request.getReason())
                .stopSince(LocalDate.now().plusDays(plusDays)) // 해제 예정일
                .build();

        stopRepository.save(stop);

        user.setUStatus(0); // 정지
        userRepository.save(user);
    }

    /** 계정 활성화 */
    @Transactional
    public void activateUser(Long reportId) {
        NagaEntity report = nagaRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("신고 데이터 없음"));

        Long userId = report.getBIdx2();

        var user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("유저 없음"));

        user.setUStatus(1); // 활성
        userRepository.save(user);
    }

    /** 계정 탈퇴 */
    @Transactional
    public void banUser(Long reportId, GetOutDTO request) {
        NagaEntity report = nagaRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("신고 데이터 없음"));

        Long userId = report.getBIdx2();
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("유저 없음"));

        GetoutEntity out = GetoutEntity.builder()
                .user(user)
                .goStatus("1")               // 탈퇴 사유 코드
                .goOutdata(LocalDate.now()) // 엔티티 필드명 정확히!
                .build();

        getoutRepository.save(out);

        user.setUStatus(3); // 탈퇴
        userRepository.save(user);
    }

    /** 정지 기간 */
    private long convertDuration(String duration) {
        return switch (duration) {
            case "1일" -> 1;
            case "3일" -> 3;
            case "7일" -> 7;
            case "30일" -> 30;
            case "90일" -> 90;
            case "무기한" -> 3650; // 10년
            default -> 0;
        };
    }
}
