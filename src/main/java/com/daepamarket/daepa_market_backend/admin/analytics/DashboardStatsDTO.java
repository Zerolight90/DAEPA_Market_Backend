package com.daepamarket.daepa_market_backend.admin.analytics;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsDTO {
    private Long totalUsers;           // 전체 회원 수
    private Long monthlyTransactions;  // 이번 달 거래건
    private Long monthlyRevenue;       // 이번 달 거래액
    private Long pendingReports;       // 처리 안된 신고/문의 수
}

