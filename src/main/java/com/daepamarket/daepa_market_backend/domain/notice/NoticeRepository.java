package com.daepamarket.daepa_market_backend.domain.notice;

import com.daepamarket.daepa_market_backend.domain.notice.NoticeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface NoticeRepository extends JpaRepository<NoticeEntity, Long> {

    @Query("SELECT n FROM NoticeEntity n JOIN FETCH n.admin ORDER BY n.nDate DESC")
    List<NoticeEntity> findAllWithAdmin();

    @Query("SELECT n FROM NoticeEntity n JOIN FETCH n.admin ORDER BY n.nFix DESC, n.nIdx DESC")
    Page<NoticeEntity> findAllWithAdmin(Pageable pageable);

    @Query("SELECT n FROM NoticeEntity n JOIN FETCH n.admin WHERE n.nIdx = :id")
    Optional<NoticeEntity> findByIdWithAdmin(@Param("id") Long id);
}
