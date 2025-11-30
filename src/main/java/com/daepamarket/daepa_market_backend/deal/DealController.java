package com.daepamarket.daepa_market_backend.deal;


import com.daepamarket.daepa_market_backend.domain.deal.DealBuyHistoryDTO;
import com.daepamarket.daepa_market_backend.domain.deal.DealSellHistoryDTO;
import com.daepamarket.daepa_market_backend.domain.user.UserEntity;
import com.daepamarket.daepa_market_backend.domain.user.UserRepository;
import com.daepamarket.daepa_market_backend.jwt.JwtProvider;
import com.daepamarket.daepa_market_backend.pay.PayService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/deal")
public class DealController {

    private final DealService dealService;
    private final PayService payService; // ??PayService ì£¼ì…
    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;

    /**
     * ??[? ê·œ] êµ¬ë§¤ ?•ì • API
     * @param dealId ?•ì •??ê±°ë˜ ID
     * @param request ?¬ìš©???¸ì¦???„í•œ HttpServletRequest
     */
    @PostMapping("/{dealId}/confirm")
    public ResponseEntity<?> confirmPurchase(
            @PathVariable Long dealId,
            HttpServletRequest request) {
        try {
            // 1. ? í°?ì„œ ?¬ìš©??ID ì¶”ì¶œ (ê¸°ì¡´ ë¡œì§ ?œìš©)
            String auth = request.getHeader("Authorization");
            if (auth == null || !auth.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "?¸ì¦ ? í°???†ìŠµ?ˆë‹¤."));
            }
            String token = auth.substring(7);
            if (jwtProvider.isExpired(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "? í°??ë§Œë£Œ?˜ì—ˆ?µë‹ˆ??"));
            }
            Long userId = Long.valueOf(jwtProvider.getUid(token));

            // 2. ?œë¹„??ë¡œì§ ?¸ì¶œ
            payService.finalizePurchase(dealId, userId);
            dealService.buyerMannerUp(userId);

            return ResponseEntity.ok(Map.of("message", "êµ¬ë§¤ê°€ ?±ê³µ?ìœ¼ë¡??•ì •?˜ì—ˆ?µë‹ˆ?? ?ë§¤?ì—ê²??•ì‚°???„ë£Œ?©ë‹ˆ??"));

        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            // ?œë²„ ?¤ë¥˜ ë¡œê¹… (?¤ì œ ?´ì˜??ì¤‘ìš”)
            // log.error("êµ¬ë§¤ ?•ì • ì²˜ë¦¬ ì¤??¤ë¥˜ ë°œìƒ: dealId={}, userId={}", dIdx, userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "êµ¬ë§¤ ?•ì • ì²˜ë¦¬ ì¤??œë²„ ?¤ë¥˜ê°€ ë°œìƒ?ˆìŠµ?ˆë‹¤."));
        }
    }

    // ?ˆì „ê²°ì œ ?•ì‚° ?´ì—­ (?´ê? ??ê²?
    @GetMapping("/safe")
    public ResponseEntity<?> getMySettlements(HttpServletRequest request) {
        return dealService.getMySettlements(request);
    }

    @GetMapping("/mySell")
    public ResponseEntity<?> getMySell(HttpServletRequest request) {
        try {
            // 1) Authorization ?¤ë” êº¼ë‚´ê¸?
            String auth = request.getHeader("Authorization");
            if (auth == null || !auth.startsWith("Bearer ")) {
                return ResponseEntity.status(401).body("? í°???†ìŠµ?ˆë‹¤.");
            }

            // 2) Bearer ?˜ë¼?´ê¸°
            String token = auth.substring(7);

            // 3) ? í° ë§Œë£Œ ?•ì¸
            if (jwtProvider.isExpired(token)) {
                return ResponseEntity.status(401).body("? íš¨?˜ì? ?Šì? ? í°?…ë‹ˆ??");
            }

            // 4) ? í°?ì„œ userId ë½‘ê¸°
            Long userId = Long.valueOf(jwtProvider.getUid(token));

            // 5) DB??ì§„ì§œ ì¡´ì¬?˜ëŠ”ì§€ ì²´í¬
            UserEntity user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                return ResponseEntity.status(404).body("?¬ìš©?ë? ì°¾ì„ ???†ìŠµ?ˆë‹¤.");
            }

            // 6) ?ë§¤??idx = ? í° ì£¼ì¸??ê±°ë˜ë§?ì¡°íšŒ
            List<DealSellHistoryDTO> list = dealService.getMySellHistory(userId);

            return ResponseEntity.ok(list);

        } catch (Exception e) {
            return ResponseEntity.status(500).body("?œë²„ ?¤ë¥˜ê°€ ë°œìƒ?ˆìŠµ?ˆë‹¤.");
        }
    }

    @GetMapping("/myBuy")
    public ResponseEntity<?> getMyBuys(HttpServletRequest request) {
        try {
            String auth = request.getHeader("Authorization");
            if (auth == null || !auth.startsWith("Bearer ")) {
                return ResponseEntity.status(401).body("? í°???†ìŠµ?ˆë‹¤.");
            }

            String token = auth.substring(7);
            if (jwtProvider.isExpired(token)) {
                return ResponseEntity.status(401).body("? íš¨?˜ì? ?Šì? ? í°?…ë‹ˆ??");
            }

            Long uIdx = Long.valueOf(jwtProvider.getUid(token));

            List<DealBuyHistoryDTO> list = dealService.getMyBuyHistory(uIdx);

            return ResponseEntity.ok(list);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("?œë²„ ?¤ë¥˜: " + e.getMessage());
        }
    }


    //êµ¬ë§¤?´ì—­?ì„œ êµ¬ë§¤?•ì • ë²„íŠ¼
    @PatchMapping("/{dealId}/buy-confirm")
    public ResponseEntity<?> confirmBuy(
            @PathVariable Long dealId,
            HttpServletRequest request
    ) {
        try {
            String auth = request.getHeader("Authorization");
            if (auth == null || !auth.startsWith("Bearer ")) {
                return ResponseEntity.status(401).body("? í°???†ìŠµ?ˆë‹¤.");
            }

            String token = auth.substring(7);
            if (jwtProvider.isExpired(token)) {
                return ResponseEntity.status(401).body("? íš¨?˜ì? ?Šì? ? í°?…ë‹ˆ??");
            }

            Long uIdx = Long.valueOf(jwtProvider.getUid(token)); // ë¡œê·¸?¸í•œ ?¬ëŒ(êµ¬ë§¤??

            // ?œë¹„?¤ì— ?„ì„
            dealService.confirmBuy(dealId, uIdx);

            return ResponseEntity.ok("êµ¬ë§¤?•ì • ?„ë£Œ");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("?œë²„ ?¤ë¥˜: " + e.getMessage());
        }
    }

    @GetMapping("/safe/count")
    public ResponseEntity<Map<String, Long>> getSettlementCount(
            @RequestParam(name = "uIdx") Long uIdx
    ) {
        long count = dealService.getSettlementCount(uIdx); // ?ëŠ” getSettlementCountLastYear
        return ResponseEntity.ok(Map.of("count", count));
    }


}

