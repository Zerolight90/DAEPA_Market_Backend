package com.daepamarket.daepa_market_backend.admin.banner;

import com.daepamarket.daepa_market_backend.S3Service;
import com.daepamarket.daepa_market_backend.admin.banner.dto.BannerOrderUpdateDTO;
import com.daepamarket.daepa_market_backend.admin.banner.dto.BannerRequestDTO;
import com.daepamarket.daepa_market_backend.admin.banner.dto.BannerResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminBannerService {

    private final S3Service s3Service;
    private final BannerStorageService bannerStorageService;

    @Transactional(readOnly = true)
    public List<BannerResponseDTO> getAllBanners() {
        return bannerStorageService.loadBanners().stream()
                .sorted(Comparator.comparing(BannerStorageService.BannerRecord::getDisplayOrder, Comparator.nullsLast(Integer::compareTo)))
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BannerResponseDTO> getActiveBanners() {
        return bannerStorageService.loadBanners().stream()
                .filter(record -> Boolean.TRUE.equals(record.getActive()))
                .sorted(Comparator.comparing(BannerStorageService.BannerRecord::getDisplayOrder, Comparator.nullsLast(Integer::compareTo)))
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BannerResponseDTO getBanner(Long id) {
        return bannerStorageService.loadBanners().stream()
                .filter(record -> Objects.equals(record.getId(), id))
                .findFirst()
                .map(this::toDto)
                .orElseThrow(() -> new IllegalArgumentException("배너를 찾을 수 없습니다."));
    }

    public BannerResponseDTO createBanner(BannerRequestDTO request, MultipartFile imageFile) {
        List<BannerStorageService.BannerRecord> banners = loadBanners();

        BannerStorageService.BannerRecord banner = new BannerStorageService.BannerRecord();
        banner.setId(generateNextId(banners));
        banner.setCreatedAt(LocalDateTime.now());
        banner.setUpdatedAt(LocalDateTime.now());

        applyMetadata(banner, request);
        applyImage(banner, request, imageFile, true);
        applyDisplayOrderForCreate(banner, request.getDisplayOrder(), banners);

        banners.add(banner);
        persistBanners(banners);

        return toDto(banner);
    }

    public BannerResponseDTO updateBanner(Long id, BannerRequestDTO request, MultipartFile imageFile) {
        List<BannerStorageService.BannerRecord> banners = loadBanners();
        BannerStorageService.BannerRecord banner = findBanner(banners, id);

        applyMetadata(banner, request);
        applyImage(banner, request, imageFile, false);
        applyDisplayOrderForUpdate(banner, request.getDisplayOrder(), banners);

        persistBanners(banners);
        return toDto(banner);
    }

    public void deleteBanner(Long id) {
        List<BannerStorageService.BannerRecord> banners = loadBanners();
        BannerStorageService.BannerRecord banner = findBanner(banners, id);

        // S3에서 이미지 삭제
        if (banner.getImageUrl() != null && !banner.getImageUrl().isBlank()) {
            try {
                // S3 URL인 경우에만 삭제
                if (banner.getImageUrl().contains("s3.ap-northeast-2.amazonaws.com")) {
                    s3Service.deleteFile(banner.getImageUrl());
                }
            } catch (Exception e) {
                // S3 삭제 실패해도 배너 삭제는 진행
                System.err.println("S3 이미지 삭제 중 오류 발생: " + e.getMessage());
            }
        }

        boolean removed = banners.removeIf(record -> Objects.equals(record.getId(), id));
        if (!removed) {
            throw new IllegalArgumentException("배너를 찾을 수 없습니다.");
        }
        persistBanners(banners);
    }

    public BannerResponseDTO toggleActive(Long id) {
        List<BannerStorageService.BannerRecord> banners = loadBanners();
        BannerStorageService.BannerRecord banner = findBanner(banners, id);
        banner.setActive(!Boolean.TRUE.equals(banner.getActive()));
        banner.setUpdatedAt(LocalDateTime.now());
        persistBanners(banners);
        return toDto(banner);
    }

    public void updateOrder(List<BannerOrderUpdateDTO> orders) {
        if (orders == null || orders.isEmpty()) {
            return;
        }

        Map<Long, Integer> orderMap = orders.stream()
                .collect(Collectors.toMap(BannerOrderUpdateDTO::getId, BannerOrderUpdateDTO::getDisplayOrder));

        List<BannerStorageService.BannerRecord> banners = loadBanners();
        for (BannerStorageService.BannerRecord banner : banners) {
            Integer newOrder = orderMap.get(banner.getId());
            if (newOrder != null) {
                banner.setDisplayOrder(newOrder);
            }
        }

        persistBanners(banners);
    }

    private List<BannerStorageService.BannerRecord> loadBanners() {
        return new ArrayList<>(bannerStorageService.loadBanners());
    }

    private void persistBanners(List<BannerStorageService.BannerRecord> banners) {
        normalizeOrder(banners);
        bannerStorageService.saveBanners(banners);
    }

    private void applyMetadata(BannerStorageService.BannerRecord banner, BannerRequestDTO request) {
        String title = trimToNull(request.getTitle());
        banner.setTitle(title != null ? title : "");
        banner.setSubtitle(trimToNull(request.getSubtitle()));
        banner.setActive(request.getActive() == null || request.getActive());
        banner.setUpdatedAt(LocalDateTime.now());
    }

    private void applyImage(BannerStorageService.BannerRecord banner, BannerRequestDTO request, MultipartFile imageFile, boolean isCreate) {
        String resolvedImage = null;

        if (imageFile != null && !imageFile.isEmpty()) {
            try {
                // S3에 업로드
                resolvedImage = s3Service.uploadFile(imageFile, "banners");
            } catch (IOException e) {
                throw new RuntimeException("이미지 업로드에 실패했습니다.", e);
            }
        } else if (request.getImageUrl() != null && !request.getImageUrl().isBlank()) {
            resolvedImage = request.getImageUrl().trim();
        } else if (isCreate && (banner.getImageUrl() == null || banner.getImageUrl().isBlank())) {
            throw new IllegalArgumentException("배너 이미지는 필수입니다.");
        }

        // 기존 이미지가 있고 새 이미지로 변경하는 경우, 기존 S3 이미지 삭제
        if (resolvedImage != null && banner.getImageUrl() != null 
                && !banner.getImageUrl().equals(resolvedImage)
                && banner.getImageUrl().contains("s3.ap-northeast-2.amazonaws.com")) {
            try {
                s3Service.deleteFile(banner.getImageUrl());
            } catch (Exception e) {
                // S3 삭제 실패해도 새 이미지 저장은 진행
                System.err.println("기존 S3 이미지 삭제 중 오류 발생: " + e.getMessage());
            }
        }

        if (resolvedImage != null) {
            banner.setImageUrl(resolvedImage);
        }
    }

    private void applyDisplayOrderForCreate(BannerStorageService.BannerRecord banner, Integer requestedOrder, List<BannerStorageService.BannerRecord> banners) {
        int maxOrder = banners.size() + 1;
        int targetOrder = (requestedOrder == null || requestedOrder < 1) ? maxOrder : Math.min(requestedOrder, maxOrder);

        for (BannerStorageService.BannerRecord candidate : banners) {
            Integer order = candidate.getDisplayOrder();
            if (order != null && order >= targetOrder) {
                candidate.setDisplayOrder(order + 1);
            }
        }
        banner.setDisplayOrder(targetOrder);
    }

    private void applyDisplayOrderForUpdate(BannerStorageService.BannerRecord banner, Integer requestedOrder, List<BannerStorageService.BannerRecord> banners) {
        if (requestedOrder == null) {
            return;
        }

        int maxOrder = banners.size();
        int currentOrder = Optional.ofNullable(banner.getDisplayOrder()).orElse(maxOrder);
        int targetOrder = Math.min(Math.max(requestedOrder, 1), maxOrder);

        if (targetOrder == currentOrder) {
            return;
        }

        for (BannerStorageService.BannerRecord candidate : banners) {
            if (candidate.getId().equals(banner.getId())) {
                continue;
            }
            Integer order = candidate.getDisplayOrder();
            if (order == null) {
                continue;
            }

            if (targetOrder < currentOrder) {
                if (order >= targetOrder && order < currentOrder) {
                    candidate.setDisplayOrder(order + 1);
                }
            } else {
                if (order <= targetOrder && order > currentOrder) {
                    candidate.setDisplayOrder(order - 1);
                }
            }
        }
        banner.setDisplayOrder(targetOrder);
    }

    private void normalizeOrder(List<BannerStorageService.BannerRecord> banners) {
        banners.sort(Comparator.comparing(BannerStorageService.BannerRecord::getDisplayOrder, Comparator.nullsLast(Integer::compareTo)));
        int order = 1;
        for (BannerStorageService.BannerRecord banner : banners) {
            banner.setDisplayOrder(order++);
        }
    }

    private BannerStorageService.BannerRecord findBanner(List<BannerStorageService.BannerRecord> banners, Long id) {
        return banners.stream()
                .filter(candidate -> Objects.equals(candidate.getId(), id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("배너를 찾을 수 없습니다."));
    }

    private BannerResponseDTO toDto(BannerStorageService.BannerRecord record) {
        return BannerResponseDTO.builder()
                .id(record.getId())
                .title(record.getTitle())
                .subtitle(record.getSubtitle())
                .imageUrl(record.getImageUrl())
                .displayOrder(record.getDisplayOrder())
                .active(Boolean.TRUE.equals(record.getActive()))
                .createdAt(record.getCreatedAt())
                .updatedAt(record.getUpdatedAt())
                .build();
    }

    private long generateNextId(List<BannerStorageService.BannerRecord> banners) {
        return banners.stream()
                .map(BannerStorageService.BannerRecord::getId)
                .filter(Objects::nonNull)
                .mapToLong(Long::longValue)
                .max()
                .orElse(0L) + 1;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
