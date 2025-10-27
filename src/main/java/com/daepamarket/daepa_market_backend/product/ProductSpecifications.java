package com.daepamarket.daepa_market_backend.product;

import com.daepamarket.daepa_market_backend.domain.Category.CtLowEntity;
import com.daepamarket.daepa_market_backend.domain.Category.CtMiddleEntity;
import com.daepamarket.daepa_market_backend.domain.Category.CtUpperEntity;
import com.daepamarket.daepa_market_backend.domain.product.ProductEntity;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

public class ProductSpecifications {

    private ProductSpecifications() {}

    public static Specification<ProductEntity> keywordLike(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) return null;
            String pat = "%" + keyword.trim() + "%";
            return cb.or(
                    cb.like(root.get("pdTitle"), pat),
                    cb.like(root.get("pdContent"), pat)
            );
        };
    }

    public static Specification<ProductEntity> statusEq(Integer status) {
        return (root, query, cb) -> (status == null) ? null
                : cb.equal(root.get("pdStatus"), status);
    }

    public static Specification<ProductEntity> locationLike(String location) {
        return (root, query, cb) -> {
            if (location == null || location.isBlank()) return null;
            return cb.like(root.get("pdLocation"), "%" + location.trim() + "%");
        };
    }

    public static Specification<ProductEntity> priceGe(Long minPrice) {
        return (root, query, cb) -> (minPrice == null) ? null
                : cb.ge(root.get("pdPrice"), minPrice);
    }

    public static Specification<ProductEntity> priceLe(Long maxPrice) {
        return (root, query, cb) -> (maxPrice == null) ? null
                : cb.le(root.get("pdPrice"), maxPrice);
    }

    /** 카테고리: low/middle/upper 순서로 가장 구체적인 조건을 우선 적용 */
    public static Specification<ProductEntity> inCategory(Long upperId, Long middleId, Long lowId) {
        return (root, query, cb) -> {
            // Product.low
            Join<ProductEntity, CtLowEntity> low = root.join("low", JoinType.LEFT);
            if (lowId != null) {
                return cb.equal(low.get("lowIdx"), lowId);
            }
            // low.middle
            Join<CtLowEntity, CtMiddleEntity> middle = low.join("middle", JoinType.LEFT);
            if (middleId != null) {
                return cb.equal(middle.get("middleIdx"), middleId);
            }
            // middle.upper
            Join<CtMiddleEntity, CtUpperEntity> upper = middle.join("upper", JoinType.LEFT);
            if (upperId != null) {
                return cb.equal(upper.get("upperIdx"), upperId);
            }
            return null;
        };
    }
}
