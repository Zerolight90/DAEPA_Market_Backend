package com.daepamarket.daepa_market_backend.domain.notice;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface NoticeRepository extends JpaRepository<NoticeEntity, Long> {

    @Query("SELECT n FROM NoticeEntity n JOIN FETCH n.admin ORDER BY n.nDate DESC")
    List<NoticeEntity> findAllWithAdmin();

    @Query("SELECT n FROM NoticeEntity n JOIN FETCH n.admin WHERE n.nIdx = :id")
    Optional<NoticeEntity> findByIdWithAdmin(@Param("id") Long id);
}
