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

            // 신고 처리 상태 확인 - 해당 신고에 대한 조치가 있는지 확인
            Long reportedUserId = r.getBIdx2();
            var reportedUser = userRepository.findById(reportedUserId).orElse(null);
            
            if (reportedUser != null) {
                Integer userStatus = reportedUser.getUStatus();
                
                // 해당 사용자에 대한 조치 확인 (최신 조치 우선)
                boolean hasGetout = getoutRepository.existsByUserUIdx(reportedUserId);
                boolean hasStop = stopRepository.existsByUserUIdx(reportedUserId);
                
                // 탈퇴 상태이고 탈퇴 기록이 있으면 탈퇴 처리됨
                if (userStatus == 3 && hasGetout) {
                    dto.setStatus("banned");
                    dto.setActionType("ban");
                } 
                // 정지 상태이고 정지 기록이 있으면 정지 처리됨
                else if (userStatus == 0 && hasStop) {
                    dto.setStatus("suspended");
                    dto.setActionType("suspend");
                } 
                // 활성 상태이지만 이전에 조치가 있었으면 활성화 처리됨
                else if (userStatus == 1 && (hasStop || hasGetout)) {
                    dto.setStatus("activated");
                    dto.setActionType("activate");
                } 
                // 조치 없음
                else {
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

    /** 계정 정지 */
    @Transactional
    public void suspendUser(Long reportId, StopDTO request) {
        NagaEntity report = nagaRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("신고 데이터 없음"));

        Long userId = report.getBIdx2();

        var user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("유저 없음"));

        // 탈퇴된 사용자는 정지할 수 없음 (먼저 활성화 필요)
        // 하지만 번복이 가능하므로, 탈퇴 상태에서는 정지 불가
        // 사용자가 탈퇴를 되돌리고 싶다면 활성화 후 정지해야 함
        if (user.getUStatus() == 3) {
            throw new RuntimeException("탈퇴된 사용자는 정지할 수 없습니다. 먼저 활성화한 후 정지할 수 있습니다.");
        }

        long plusDays = convertDuration(request.getDuration());

        // 정지 기록 추가 (기존 기록이 있어도 새 기록 추가)
        StopEntity stop = StopEntity.builder()
                .user(user)
                .stopDate(LocalDate.parse(request.getSuspendDate(), ISO_DATE)) // 입력일
                .stopContent(request.getReason())
                .stopSince(LocalDate.now().plusDays(plusDays)) // 해제 예정일
                .build();

        stopRepository.save(stop);

        // 상태를 정지로 변경
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

        // 모든 상태에서 활성화 가능 (정지, 탈퇴 모두 되돌릴 수 있음)
        // 상태만 활성화로 변경하고 기존 기록은 유지
        user.setUStatus(1); // 활성
        userRepository.save(user);
        
        // 정지/탈퇴 기록은 남겨두고 상태만 활성화로 변경
        // (기록은 유지하여 이전 조치 내역을 추적할 수 있도록 함)
    }

    /** 계정 탈퇴 */
    @Transactional
    public void banUser(Long reportId, GetOutDTO request) {
        NagaEntity report = nagaRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("신고 데이터 없음"));

        Long userId = report.getBIdx2();
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("유저 없음"));

        // 이미 탈퇴된 사용자는 다시 탈퇴할 수 없음
        if (user.getUStatus() == 3) {
            throw new RuntimeException("이미 탈퇴된 사용자입니다.");
        }

        // 탈퇴 기록 추가 (기존 기록이 있어도 새 기록 추가 가능)
        GetoutEntity out = GetoutEntity.builder()
                .user(user)
                .goStatus(request.getReason() != null && !request.getReason().trim().isEmpty() 
                        ? request.getReason() : "1") // 탈퇴 사유
                .goOutdata(LocalDate.now()) // 엔티티 필드명 정확히!
                .build();

        getoutRepository.save(out);

        // 상태를 탈퇴로 변경
        // 정지 기록은 남겨두고 탈퇴 상태로 변경
        // (기록은 유지하여 이전 조치 내역을 추적할 수 있도록 함)
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
