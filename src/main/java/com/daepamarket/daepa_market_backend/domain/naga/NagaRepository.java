package com.daepamarket.daepa_market_backend.domain.naga;

import com.daepamarket.daepa_market_backend.admin.user.ReportHistoryDTO;
import com.daepamarket.daepa_market_backend.domain.user.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NagaRepository extends JpaRepository<NagaEntity, Long> {

    @Query("""
        SELECT new com.daepamarket.daepa_market_backend.admin.user.ReportHistoryDTO(
            n.ngIdx,
            u.unickname,
            n.ngContent,
            n.ngDate,
            n.ngStatus
        )
        FROM NagaEntity n
        JOIN UserEntity u ON n.sIdx = u.uIdx   
        WHERE n.bIdx2 = :userId                
        ORDER BY n.ngDate DESC
    """)
    List<ReportHistoryDTO> findReportsByUserId(@Param("userId") Long userId);
}
