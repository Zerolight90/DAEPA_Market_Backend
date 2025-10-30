package com.daepamarket.daepa_market_backend.admin.oneonone.Service;

import com.daepamarket.daepa_market_backend.domain.oneonone.OneOnOneEntity;
import com.daepamarket.daepa_market_backend.domain.oneonone.OneOnOneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OneOnOneService {

    private final OneOnOneRepository repository;

    public List<OneOnOneEntity> getList(){
        return repository.findAllWithUser();
    }
}
