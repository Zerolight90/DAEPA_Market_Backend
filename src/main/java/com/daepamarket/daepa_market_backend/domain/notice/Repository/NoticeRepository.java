package com.daepamarket.daepa_market_backend.domain.notice.Repository;

import com.daepamarket.daepa_market_backend.domain.notice.NoticeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NoticeRepository extends JpaRepository<NoticeEntity, Long> {
}
