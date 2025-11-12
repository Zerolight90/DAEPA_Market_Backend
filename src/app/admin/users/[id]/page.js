"use client";

import { useState, useEffect, use } from "react";
import {
  ArrowLeft,
  Edit,
  User,
  Calendar,
  MapPin,
  Phone,
  Star,
  ShoppingBag,
  MessageSquare,
  Shield,
  AlertCircle
} from "lucide-react";
import Link from "next/link";
import styles from "../../admin.module.css";

export default function UserDetailPage({ params }) {
  const { id } = use(params);
  const [user, setUser] = useState(null);
  const [activeTab, setActiveTab] = useState("sell");
  const [reviews, setReviews] = useState({ sell: [], buy: [] });
  const [isLoading, setIsLoading] = useState(true);

  const getStatusBadge = (statusNum) => {
    switch (statusNum) {
      case 1:
        return <span className={styles.statusSuccess}>활성</span>;
      case 2:
        return <span className={styles.statusGray}>탈퇴</span>;
      case 3:
        return <span className={styles.statusError}>정지</span>;
      case 9:
        return <span className={styles.statusWarning}>보류</span>;
      default:
        return <span className={styles.statusWarning}>보류</span>;
    }
  };

  const renderStars = (ratingValue) => {
    const numeric = Number(ratingValue);
    const rating = Number.isFinite(numeric) ? Math.round(Math.max(0, Math.min(5, numeric))) : 0;
    return Array.from({ length: 5 }, (_, index) => (
      <Star
        key={index}
        size={14}
        className={index < rating ? styles.starFilled : styles.starEmpty}
      />
    ));
  };

  const getReviewerName = (review, type) => {
    if (!review) return "-";
    if (review.reviewerName) return review.reviewerName;
    if (type === "sell") return review.buyerName ?? review.customerName ?? review.writer ?? "-";
    return review.sellerName ?? review.writer ?? "-";
  };

  const getReviewDate = (review) => {
    if (!review) return "-";
    const value = review.date ?? review.createdAt ?? review.reviewDate ?? review.updatedAt;
    return value ? new Date(value).toLocaleDateString("ko-KR") : "-";
  };

  const getReviewComment = (review) => {
    if (!review) return "-";
    return review.content ?? review.comment ?? review.text ?? "-";
  };

  const getReviewRating = (review) => {
    if (!review) return 0;
    const raw = review.rating ?? review.score ?? review.star ?? review.points;
    return Number(raw ?? 0);
  };

  const handleMannerTempChange = async (change) => {
    if (!user) return;

    const newTemp = (user.umanner ?? 0) + change;
    if (newTemp < 0 || newTemp > 100) {
      alert("매너온도는 0도에서 100도 사이여야 합니다.");
      return;
    }

    try {
      const res = await fetch(`${process.env.NEXT_PUBLIC_API_BASE}/api/admin/users/${id}/manner`, {
        method: "PATCH",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ umanner: newTemp })
      });

      if (!res.ok) throw new Error("매너온도 변경 실패");

      setUser((prev) => ({ ...prev, umanner: newTemp }));
      alert("매너온도가 변경되었습니다.");
    } catch (err) {
      console.error(err);
      alert("매너온도 변경 중 오류가 발생했습니다.");
    }
  };

  useEffect(() => {
    const fetchUserData = async () => {
      setIsLoading(true);
      try {
        const res = await fetch(`${process.env.NEXT_PUBLIC_API_BASE}/api/admin/users/${id}`);
        if (!res.ok) throw new Error("사용자 정보를 불러오지 못했습니다.");
        const data = await res.json();
        setUser(data);

        const reviewRes = await fetch(`${process.env.NEXT_PUBLIC_API_BASE}/api/admin/users/${id}/reviews/sell`);
        if (reviewRes.ok) {
          const reviewData = await reviewRes.json();
          const sellReviews = Array.isArray(reviewData) ? reviewData : reviewData?.sellReviews ?? [];
          const buyReviews = Array.isArray(reviewData?.buyReviews) ? reviewData.buyReviews : [];
          setReviews({ sell: sellReviews, buy: buyReviews });
        }
      } catch (err) {
        console.error("데이터 조회 실패:", err);
      } finally {
        setIsLoading(false);
      }
    };

    fetchUserData();
  }, [id]);

  if (isLoading) {
    return (
      <div
        style={{
          display: "flex",
          justifyContent: "center",
          alignItems: "center",
          height: "100vh",
          fontSize: "1.25rem",
          color: "#64748b"
        }}
      >
        로딩 중...
      </div>
    );
  }

  if (!user) {
    return (
      <div
        style={{
          display: "flex",
          justifyContent: "center",
          alignItems: "center",
          height: "100vh",
          fontSize: "1.25rem",
          color: "#64748b"
        }}
      >
        사용자 정보를 찾을 수 없습니다.
      </div>
    );
  }

  return (
    <div className={styles.pageContainer}>
      <div className={styles.pageHeader}>
        <div style={{ display: "flex", alignItems: "center", gap: "1rem", marginBottom: "1rem" }}>
          <Link
            href="/admin/users"
            style={{
              display: "flex",
              alignItems: "center",
              gap: "0.5rem",
              color: "#64748b",
              textDecoration: "none",
              fontSize: "0.875rem"
            }}
          >
            <ArrowLeft size={16} />
            목록으로 돌아가기
          </Link>
        </div>
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start" }}>
          <div>
            <h1 className={styles.pageTitle}>{user.uname}</h1>
            <p className={styles.pageSubtitle}>사용자 상세 정보 및 활동 내역</p>
          </div>
          <Link href={`/admin/users/${id}/edit`} style={{ textDecoration: "none" }}>
            <button
              style={{
                display: "flex",
                alignItems: "center",
                gap: "0.5rem",
                padding: "0.75rem 1.5rem",
                background: "#f0f9ff",
                color: "#2563eb",
                border: "1px solid #bfdbfe",
                borderRadius: "0.5rem",
                fontSize: "0.875rem",
                fontWeight: "600",
                cursor: "pointer",
                transition: "all 0.2s"
              }}
            >
              <Edit size={16} />
              정보 수정
            </button>
          </Link>
        </div>
      </div>

      <div style={{ display: "grid", gridTemplateColumns: "1fr 2fr", gap: "2rem" }}>
        <div style={{ display: "flex", flexDirection: "column", gap: "2rem" }}>
          <div className={styles.tableContainer}>
            <div style={{ padding: "2rem" }}>
              <div style={{ display: "flex", alignItems: "center", gap: "1.5rem", marginBottom: "2rem" }}>
                <div
                  style={{
                    width: "80px",
                    height: "80px",
                    borderRadius: "50%",
                    background: "#e2e8f0",
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "center"
                  }}
                >
                  <User size={40} color="#64748b" />
                </div>
                <div>
                  <h2 style={{ margin: 0, fontSize: "1.5rem", fontWeight: 700, color: "#1e293b" }}>{user.uname}</h2>
                  <p style={{ margin: "0.25rem 0 0 0", color: "#64748b" }}>{user.uid}</p>
                </div>
              </div>

              <div style={{ display: "grid", gridTemplateColumns: "repeat(2, minmax(0, 1fr))", gap: "1.25rem" }}>
                <div style={{ display: "flex", alignItems: "center", gap: "1rem" }}>
                  <Shield size={18} color="#64748b" />
                  <div>
                    <div style={{ fontSize: "0.875rem", color: "#64748b", marginBottom: "0.25rem" }}>상태</div>
                    <div style={{ fontWeight: 600, color: "#1e293b" }}>{getStatusBadge(user.ustatus)}</div>
                  </div>
                </div>
                <div style={{ display: "flex", alignItems: "center", gap: "1rem" }}>
                  <MessageSquare size={18} color="#64748b" />
                  <div>
                    <div style={{ fontSize: "0.875rem", color: "#64748b", marginBottom: "0.25rem" }}>닉네임</div>
                    <div style={{ fontWeight: 600, color: "#1e293b" }}>{user.unickname || "-"}</div>
                  </div>
                </div>
                <div style={{ display: "flex", alignItems: "center", gap: "1rem" }}>
                  <Calendar size={18} color="#64748b" />
                  <div>
                    <div style={{ fontSize: "0.875rem", color: "#64748b", marginBottom: "0.25rem" }}>가입일</div>
                    <div style={{ fontWeight: 600, color: "#1e293b" }}>{new Date(user.udate).toLocaleDateString("ko-KR")}</div>
                  </div>
                </div>
                <div style={{ display: "flex", alignItems: "center", gap: "1rem" }}>
                  <AlertCircle size={18} color="#64748b" />
                  <div>
                    <div style={{ fontSize: "0.875rem", color: "#64748b", marginBottom: "0.25rem" }}>신고 횟수</div>
                    <div style={{ fontWeight: 600, color: "#1e293b" }}>{(user.reportCount ?? (user.reportHistory ? user.reportHistory.length : 0))}회</div>
                  </div>
                </div>
                <div style={{ display: "flex", alignItems: "center", gap: "1rem" }}>
                  <Phone size={18} color="#64748b" />
                  <div>
                    <div style={{ fontSize: "0.875rem", color: "#64748b", marginBottom: "0.25rem" }}>전화번호</div>
                    <div style={{ fontWeight: 600, color: "#1e293b" }}>{user.uphone}</div>
                  </div>
                </div>
                <div style={{ display: "flex", alignItems: "center", gap: "1rem" }}>
                  <MapPin size={18} color="#64748b" />
                  <div>
                    <div style={{ fontSize: "0.875rem", color: "#64748b", marginBottom: "0.25rem" }}>주소</div>
                    <div style={{ fontWeight: 600, color: "#1e293b" }}>{user.ulocation || "-"}</div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div className={styles.tableContainer}>
            <div style={{ padding: "2rem" }}>
              <h3
                style={{
                  margin: "0 0 1.5rem 0",
                  fontSize: "1.125rem",
                  fontWeight: 600,
                  color: "#1e293b"
                }}
              >
                매너온도
              </h3>
              <div style={{ display: "flex", alignItems: "center", gap: "1rem", marginBottom: "1rem" }}>
                <div
                  style={{
                    width: "100%",
                    height: "12px",
                    background: "#e2e8f0",
                    borderRadius: "6px",
                    overflow: "hidden"
                  }}
                >
                  <div
                    style={{
                      width: `${user.umanner}%`,
                      height: "100%",
                      background: "linear-gradient(90deg, #34d399, #22c55e)",
                      borderRadius: "6px",
                      transition: "width 0.5s ease"
                    }}
                  ></div>
                </div>
                <span
                  style={{
                    fontSize: "1.25rem",
                    fontWeight: 700,
                    color: "#16a34a",
                    minWidth: "60px",
                    textAlign: "right"
                  }}
                >
                  {user.umanner}°C
                </span>
              </div>
              <div style={{ display: "flex", justifyContent: "flex-end", gap: "0.5rem" }}>
                <button
                  onClick={() => handleMannerTempChange(-1)}
                  style={{ padding: "0.25rem 0.5rem", border: "1px solid #d1d5db", borderRadius: "0.25rem" }}
                >
                  -1
                </button>
                <button
                  onClick={() => handleMannerTempChange(1)}
                  style={{ padding: "0.25rem 0.5rem", border: "1px solid #d1d5db", borderRadius: "0.25rem" }}
                >
                  +1
                </button>
              </div>
            </div>
          </div>
        </div>

        <div className={styles.tableContainer}>
          <div style={{ borderBottom: "1px solid #e5e7eb", display: "flex" }}>
            <button
              onClick={() => setActiveTab("sell")}
              style={{
                flex: 1,
                padding: "1rem",
                background: activeTab === "sell" ? "white" : "#f9fafb",
                border: "none",
                borderBottom: activeTab === "sell" ? "2px solid #3b82f6" : "2px solid transparent",
                color: activeTab === "sell" ? "#3b82f6" : "#64748b",
                fontWeight: 600,
                cursor: "pointer",
                transition: "all 0.2s"
              }}
            >
              판매 후기 ({reviews.sell.length})
            </button>
            <button
              onClick={() => setActiveTab("buy")}
              style={{
                flex: 1,
                padding: "1rem",
                background: activeTab === "buy" ? "white" : "#f9fafb",
                border: "none",
                borderBottom: activeTab === "buy" ? "2px solid #3b82f6" : "2px solid transparent",
                color: activeTab === "buy" ? "#3b82f6" : "#64748b",
                fontWeight: 600,
                cursor: "pointer",
                transition: "all 0.2s"
              }}
            >
              구매 후기 ({reviews.buy.length})
            </button>
          </div>

          <div style={{ padding: "2rem", maxHeight: "600px", overflowY: "auto" }}>
            {reviews[activeTab].length === 0 ? (
              <div style={{ textAlign: "center", color: "#64748b", padding: "3rem 0" }}>
                {activeTab === "sell" ? "받은 판매 후기가 없습니다." : "받은 구매 후기가 없습니다."}
              </div>
            ) : (
              <div style={{ display: "flex", flexDirection: "column", gap: "1.5rem" }}>
                {reviews[activeTab].map((review, index) => (
                  <div
                    key={index}
                    style={{
                      paddingBottom: "1.5rem",
                      borderBottom:
                        index < reviews[activeTab].length - 1 ? "1px solid #f1f5f9" : "none"
                    }}
                  >
                    <div
                      style={{
                        display: "flex",
                        justifyContent: "space-between",
                        alignItems: "center",
                        marginBottom: "0.75rem"
                      }}
                    >
                      <div style={{ display: "flex", alignItems: "center", gap: "0.75rem" }}>
                        <div
                          style={{
                            width: "40px",
                            height: "40px",
                            borderRadius: "50%",
                            background: "#f1f5f9",
                            display: "flex",
                            alignItems: "center",
                            justifyContent: "center"
                          }}
                        >
                          <User size={20} color="#94a3b8" />
                        </div>
                        <div>
                          <div style={{ fontWeight: 600, color: "#1e293b" }}>{getReviewerName(review, activeTab)}</div>
                          <div style={{ fontSize: "0.875rem", color: "#64748b" }}>{getReviewDate(review)}</div>
                        </div>
                      </div>
                      <div style={{ display: "flex", gap: "0.25rem" }}>{renderStars(getReviewRating(review))}</div>
                    </div>
                    <p style={{ margin: 0, color: "#374151", lineHeight: "1.6" }}>{getReviewComment(review)}</p>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
