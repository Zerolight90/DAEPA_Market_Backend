package com.daepamarket.daepa_market_backend.admin.analytics;

import com.daepamarket.daepa_market_backend.admin.deal.AdminDealRepository;
import com.daepamarket.daepa_market_backend.domain.getout.GetoutRepository;
import com.daepamarket.daepa_market_backend.domain.naga.NagaRepository;
import com.daepamarket.daepa_market_backend.domain.oneonone.OneOnOneRepository;
import com.daepamarket.daepa_market_backend.domain.product.ProductEntity;
import com.daepamarket.daepa_market_backend.domain.product.ProductRepository;
import com.daepamarket.daepa_market_backend.domain.stop.StopRepository;
import com.daepamarket.daepa_market_backend.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final AdminDealRepository dealRepository;
    private final UserRepository userRepository;
    private final OneOnOneRepository oneOnOneRepository;
    private final NagaRepository nagaRepository;
    private final StopRepository stopRepository;
    private final GetoutRepository getoutRepository;
    private final ProductRepository productRepository;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MM/dd");

    public List<DailyTransactionDTO> getWeeklyTransactionTrend() {
        // 현재 날짜 기준으로 이번 주 일요일부터 토요일까지 계산
        LocalDate today = LocalDate.now();
        DayOfWeek dayOfWeek = today.getDayOfWeek();
        // Java DayOfWeek: MONDAY=1, SUNDAY=7
        // 일요일이 0이 되도록 조정
        int daysFromSunday = dayOfWeek == DayOfWeek.SUNDAY ? 0 : dayOfWeek.getValue() % 7;

        LocalDate weekStart = today.minusDays(daysFromSunday); // 이번 주 일요일
        LocalDate weekEnd = weekStart.plusDays(7); // 다음 주 일요일 (토요일까지 포함)

        // Timestamp로 변환
        Timestamp startTimestamp = Timestamp.valueOf(weekStart.atStartOfDay());
        Timestamp endTimestamp = Timestamp.valueOf(weekEnd.atStartOfDay());

        // 일간 거래 건수 조회
        List<Object[]> results = dealRepository.findDailyDealCounts(startTimestamp, endTimestamp);

        // 결과를 Map으로 변환 (날짜 -> 데이터)
        Map<LocalDate, DailyData> dealDataMap = new HashMap<>();
        for (Object[] row : results) {
            LocalDate dealDate = ((java.sql.Date) row[0]).toLocalDate();
            Long count = ((Number) row[1]).longValue();
            Long totalAmount = row[2] != null ? ((Number) row[2]).longValue() : 0L;
            Long sellerCount = row[3] != null ? ((Number) row[3]).longValue() : 0L;
            dealDataMap.put(dealDate, new DailyData(count.intValue(), totalAmount, sellerCount.intValue()));
        }

        // 일요일부터 토요일까지 7일치 데이터 생성
        List<DailyTransactionDTO> weeklyData = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate currentDate = weekStart.plusDays(i);
            String dateStr = currentDate.format(DATE_FORMATTER);
            DailyData data = dealDataMap.getOrDefault(currentDate, new DailyData(0, 0L, 0));
            weeklyData.add(new DailyTransactionDTO(dateStr, data.count, data.totalAmount, data.sellerCount));
        }

        return weeklyData;
    }

    // 내부 클래스: 일일 데이터
    private static class DailyData {
        int count;
        Long totalAmount;
        int sellerCount;

        DailyData(int count, Long totalAmount, int sellerCount) {
            this.count = count;
            this.totalAmount = totalAmount;
            this.sellerCount = sellerCount;
        }
    }

    @Transactional(readOnly = true)
    public DashboardStatsDTO getDashboardStats() {
        // 1. 전체 회원 수
        Long totalUsers = userRepository.count();

        // 2. 이번 달 거래건 및 거래액
        LocalDate now = LocalDate.now();
        LocalDate monthStart = now.withDayOfMonth(1);
        LocalDate monthEnd = now.plusMonths(1).withDayOfMonth(1);

        Timestamp monthStartTimestamp = Timestamp.valueOf(monthStart.atStartOfDay());
        Timestamp monthEndTimestamp = Timestamp.valueOf(monthEnd.atStartOfDay());

        List<Object[]> monthlyDeals = dealRepository.findDailyDealCounts(monthStartTimestamp, monthEndTimestamp);

        Long monthlyTransactions = monthlyDeals.stream()
                .mapToLong(row -> ((Number) row[1]).longValue())
                .sum();

        Long monthlyRevenue = monthlyDeals.stream()
                .mapToLong(row -> row[2] != null ? ((Number) row[2]).longValue() : 0L)
                .sum();

        // 3. 처리 안된 신고/문의 수
        // 문의: ooRe가 null이거나 빈 문자열인 것
        long pendingInquiries = oneOnOneRepository.findAll().stream()
                .filter(oo -> oo.getOoRe() == null || oo.getOoRe().isBlank())
                .count();

        // 신고: 처리 안된 신고 (status가 "pending"인 것)
        long pendingReports = nagaRepository.findAll().stream()
                .filter(naga -> {
                    var reportedUser = userRepository.findById(naga.getBIdx2()).orElse(null);
                    if (reportedUser == null) {
                        return true; // 사용자가 없으면 처리 안된 것으로 간주
                    }

                    Integer userStatus = reportedUser.getUStatus();
                    boolean hasGetout = getoutRepository.existsByUserUIdx(naga.getBIdx2());
                    boolean hasStop = stopRepository.existsByUserUIdx(naga.getBIdx2());

                    // 상태 값 정의:
                    // 0: 정지, 1: 활성, 2: 사용자 탈퇴, 3: 관리자 탈퇴, 9: 보류
                    // NagaReportService와 동일한 로직 사용
                    
                    // 탈퇴 상태(3)이고 탈퇴 기록이 있으면 처리됨 (관리자 탈퇴 처리)
                    if (userStatus != null && userStatus == 3 && hasGetout) {
                        return false; // 처리됨
                    }
                    // 정지 상태(0)이고 정지 기록이 있으면 처리됨
                    if (userStatus != null && userStatus == 0 && hasStop) {
                        return false; // 처리됨
                    }
                    // 활성 상태(1)이지만 이전에 조치(정지 또는 탈퇴)가 있었으면 처리됨 (정지/탈퇴 후 활성화)
                    if (userStatus != null && userStatus == 1 && (hasStop || hasGetout)) {
                        return false; // 처리됨
                    }
                    // 그 외는 처리 안됨 (pending)
                    return true; // 처리 안됨
                })
                .count();

        return new DashboardStatsDTO(
                totalUsers,
                monthlyTransactions,
                monthlyRevenue,
                pendingInquiries + pendingReports
        );
    }

    @Transactional(readOnly = true)
    public List<RecentProductDTO> getRecentProducts(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        List<ProductEntity> products = productRepository.findRecentProducts(pageable);

        return products.stream().map(product -> {
            String categoryName = product.getCtLow() != null
                    ? (product.getCtLow().getMiddle() != null
                    ? (product.getCtLow().getMiddle().getUpper() != null
                    ? product.getCtLow().getMiddle().getUpper().getUpperCt()
                    : "기타")
                    : "기타")
                    : "기타";

            String sellerName = product.getSeller() != null
                    ? (product.getSeller().getUname() != null ? product.getSeller().getUname() : "알 수 없음")
                    : "알 수 없음";

            return new RecentProductDTO(
                    product.getPdIdx(),
                    product.getPdTitle() != null ? product.getPdTitle() : "제목 없음",
                    sellerName,
                    product.getPdPrice() != null ? product.getPdPrice() : 0L,
                    categoryName,
                    product.getPdCreate() != null ? product.getPdCreate() : LocalDateTime.now()
            );
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CategoryRatioDTO> getCategoryRatios() {
        // 모든 상품을 가져와서 상위 카테고리별로 그룹화
        List<ProductEntity> products = productRepository.findAll().stream()
                .filter(p -> !p.isPdDel()) // 삭제되지 않은 상품만
                .collect(Collectors.toList());

        // 상위 카테고리별로 그룹화하여 카운트
        Map<String, Long> categoryCountMap = products.stream()
                .map(product -> {
                    if (product.getCtLow() != null
                            && product.getCtLow().getMiddle() != null
                            && product.getCtLow().getMiddle().getUpper() != null
                            && product.getCtLow().getMiddle().getUpper().getUpperCt() != null) {
                        return product.getCtLow().getMiddle().getUpper().getUpperCt();
                    }
                    return "기타";
                })
                .collect(Collectors.groupingBy(
                        category -> category,
                        Collectors.counting()
                ));

        // CategoryRatioDTO 리스트로 변환
        return categoryCountMap.entrySet().stream()
                .map(entry -> new CategoryRatioDTO(entry.getKey(), entry.getValue()))
                .sorted((a, b) -> Long.compare(b.getCount(), a.getCount())) // 카운트 내림차순 정렬
                .collect(Collectors.toList());
    }
}
