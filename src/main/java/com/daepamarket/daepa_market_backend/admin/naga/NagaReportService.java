package com.daepamarket.daepa_market_backend.admin.naga;

import com.daepamarket.daepa_market_backend.domain.getout.GetoutEntity;
import com.daepamarket.daepa_market_backend.domain.getout.GetoutRepository;
import com.daepamarket.daepa_market_backend.domain.naga.NagaEntity;
import com.daepamarket.daepa_market_backend.domain.naga.NagaRepository;
import com.daepamarket.daepa_market_backend.domain.stop.StopEntity;
import com.daepamarket.daepa_market_backend.domain.stop.StopRepository;
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
        var rows = nagaRepository.findAllOrderByNgDateDesc();

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
                                    status == 3 ? "spam"  : "other";

            dto.setType(type);
            dto.setContent(r.getNgContent());
            if (r.getNgDate() != null) {
                dto.setCreatedAt(r.getNgDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            }

            Long reportedUserId = r.getBIdx2();
            var reportedUser = userRepository.findById(reportedUserId).orElse(null);
            if (reportedUser != null) {
                Integer userStatus = reportedUser.getUStatus();
                boolean hasGetout = getoutRepository.existsByUserUIdx(reportedUserId);
                boolean hasStop   = stopRepository.existsByUserUIdx(reportedUserId);

                if (userStatus == 3 && hasGetout) {
                    dto.setStatus("banned");
                    dto.setActionType("ban");
                } else if (userStatus == 0 && hasStop) {
                    dto.setStatus("suspended");
                    dto.setActionType("suspend");
                } else if (userStatus == 1 && (hasStop || hasGetout)) {
                    dto.setStatus("activated");
                    dto.setActionType("activate");
                } else {
                    dto.setStatus("pending");
                    dto.setActionType(null);
                }
            } else {
                dto.setStatus("pending");
                dto.setActionType(null);
            }

            return dto;
        }).collect(Collectors.toList());
    }

    /** 계정 정지: 피신고자(b_idx2) u_manner -= 10 (최소 0), u_status=0(정지) */
    @Transactional
    public void suspendUser(Long reportId, StopDTO request) {
        NagaEntity report = nagaRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("신고 데이터 없음"));

        Long userId = report.getBIdx2(); // ✅ 피신고자
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("유저 없음"));

        if (user.getUStatus() == 3) {
            throw new RuntimeException("탈퇴된 사용자는 정지할 수 없습니다. 먼저 활성화하십시오.");
        }

        long plusDays = convertDuration(request.getDuration());

        StopEntity stop = StopEntity.builder()
                .user(user)
                .stopDate(LocalDate.parse(request.getSuspendDate(), ISO_DATE))
                .stopContent(request.getReason())
                .stopSince(LocalDate.now().plusDays(plusDays))
                .build();
        stopRepository.save(stop);

        double now = user.getUManner() == null ? 0d : user.getUManner();
        user.setUManner(Math.max(0d, now - 10d)); // ✅ –10(최소 0)

        user.setUStatus(0); // 정지
        userRepository.save(user);
    }

    /** 계정 활성화: 상태만 1(활성)로 복구, 점수 변화 없음 */
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

    /** 계정 탈퇴: 피신고자 u_manner=0, u_status=3(탈퇴) */
    @Transactional
    public void banUser(Long reportId, GetOutDTO request) {
        NagaEntity report = nagaRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("신고 데이터 없음"));

        Long userId = report.getBIdx2(); // ✅ 피신고자
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("유저 없음"));

        if (user.getUStatus() == 3) {
            throw new RuntimeException("이미 탈퇴된 사용자입니다.");
        }

        GetoutEntity out = GetoutEntity.builder()
                .user(user)
                .goStatus(request.getReason() != null && !request.getReason().trim().isEmpty()
                        ? request.getReason() : "1")
                .goOutdata(LocalDate.now())
                .build();
        getoutRepository.save(out);

        user.setUManner(0d); // ✅ 점수 0
        user.setUStatus(3);  // 탈퇴
        userRepository.save(user);
    }

    /** 신선도 하락: 피신고자 u_manner -= 5 (최소 0) */
    @Transactional
    public void decreaseManner(Long reportId) {
        NagaEntity report = nagaRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("신고 데이터 없음"));

        Long userId = report.getBIdx2(); // ✅ 피신고자
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("유저 없음"));

        double now = user.getUManner() == null ? 0d : user.getUManner();
        user.setUManner(Math.max(0d, now - 5d)); // ✅ –5(최소 0)
        userRepository.save(user);
    }

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
