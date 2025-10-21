package com.daepamarket.daepa_market_backend.category;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/category")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")
public class CategoryController {
    private final CategoryService categoryService;

    //상위
    @GetMapping("/uppers")
    public List<CtUpperDto> uppers() {
        return categoryService.getUpperList();
    }

    // 상위 -> 중위
    @GetMapping("/uppers/{upperId}/middles")
    public List<CtMiddleDto> middles(@PathVariable("upperId") Long upperId) {
        return categoryService.getMiddleList(upperId);
    }

    //중위 -> 하위
    @GetMapping("/middles/{middleId}/lows")
    public List<CtLowDto> lows(@PathVariable("middleId") Long middleId) {
        return categoryService.getLowList(middleId);
    }



    @GetMapping("/api/categories/upper")
    public ResponseEntity<List<CtUpperDto>> getUpperCategories() {
        List<CtUpperDto> categories = categoryService.getUpperList();
        
        // Entity를 DTO로 변환하여 프론트엔드에 필요한 데이터만 전달
        List<CtUpperDto> dtos = categories.stream()
                .map(c -> new CtUpperDto(c.getUpperIdx(), c.getUpperCt()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }
}
