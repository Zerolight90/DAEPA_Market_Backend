  // 막대 그래프 컴포넌트
  function BarChart({ data }) {
    const [hoveredIndex, setHoveredIndex] = useState(null);
    
    if (!data || data.length === 0) {
      return (
        <div style={{ height: "300px", display: "flex", alignItems: "center", justifyContent: "center", color: "#64748b" }}>
          데이터가 없습니다.
        </div>
      );
    }

    const maxValue = Math.max(...data.map(d => d.value || 0), 1);
    const greenColors = [
      "#10b981", // emerald-500
      "#22c55e", // green-500
      "#16a34a", // green-600
      "#15803d", // green-700
      "#4ade80", // green-400
      "#34d399", // emerald-400
      "#6ee7b7"  // emerald-300
    ];

    const yAxisStep = 3;
    const minYAxisMax = yAxisStep * 5;
    const yAxisMax = Math.max(Math.ceil(maxValue / yAxisStep) * yAxisStep, minYAxisMax);
    const yAxisLabels = Array.from(
      { length: Math.floor(yAxisMax / yAxisStep) + 1 },
      (_, i) => i * yAxisStep
    );

    return (
      <div style={{ 
        height: "300px",
        padding: "1.5rem 1rem 2rem 3rem",
        position: "relative"
      }}>
        {/* Y축 라벨 */}
        <div style={{
          position: "absolute",
          left: "0.5rem",
          top: "1.5rem",
          bottom: "2rem",
          display: "flex",
          flexDirection: "column",
          justifyContent: "space-between",
          fontSize: "0.75rem",
          color: "#94a3b8",
          width: "30px"
        }}>
          {yAxisLabels
            .slice()
            .reverse()
            .map((label) => (
              <span key={label}>{label}</span>
            ))}
        </div>

        {/* 막대 그래프 */}
        <div style={{ 
          height: "100%",
          display: "flex", 
          alignItems: "flex-end", 
          justifyContent: "space-between",
          gap: "0.75rem",
          position: "relative",
          paddingBottom: "1.5rem"
        }}>
          {data.map((item, index) => {
            const height = yAxisMax > 0 ? (item.value / yAxisMax) * 100 : 0;
            const barColor = greenColors[index % greenColors.length];
            
            return (
              <div 
                key={index} 
                style={{ 
                  flex: 1, 
                  position: "relative", 
                  height: "100%",
                  display: "flex",
                  flexDirection: "column",
                  alignItems: "center",
                  justifyContent: "flex-end"
                }}
                onMouseEnter={() => setHoveredIndex(index)}
                onMouseLeave={() => setHoveredIndex(null)}
              >
                {/* 막대 */}
                <div
                  style={{
                    width: "100%",
                    height: `${height}%`,
                    backgroundColor: barColor,
                    borderRadius: "0.375rem 0.375rem 0 0",
                    minHeight: item.value > 0 ? "4px" : "0",
                    transition: "all 0.2s",
                    cursor: "pointer",
                    boxShadow: hoveredIndex === index ? "0 4px 12px rgba(0,0,0,0.15)" : "0 2px 4px rgba(0,0,0,0.1)",
                    transform: hoveredIndex === index ? "scaleY(1.05)" : "scaleY(1)",
                    transformOrigin: "bottom"
                  }}
                />
                
                {/* 날짜 라벨 (X축) */}
                <div style={{
                  position: "absolute",
                  bottom: "-1.5rem",
                  fontSize: "0.75rem",
                  color: "#64748b",
                  whiteSpace: "nowrap",
                  fontWeight: hoveredIndex === index ? 600 : 400
                }}>
                  {item.date}
                </div>

                {/* 거래 건수 라벨 */}
                {item.value > 0 && (
                  <div style={{
                    position: "absolute",
                    bottom: `calc(${height}% + 8px)`,
                    fontSize: "0.75rem",
                    color: "#1e293b",
                    fontWeight: 600,
                    backgroundColor: "#fff",
                    padding: "0.125rem 0.375rem",
                    borderRadius: "0.25rem",
                    whiteSpace: "nowrap",
                    boxShadow: "0 1px 2px rgba(0,0,0,0.1)",
                    opacity: hoveredIndex === index ? 1 : 0.8
                  }}>
                    {item.value}건
                  </div>
                )}

                {/* 툴팁 */}
                {hoveredIndex === index && (
                  <div style={{
                    position: "absolute",
                    bottom: `${height}%`,
                    left: "50%",
                    transform: "translateX(-50%)",
                    marginBottom: "0.5rem",
                    backgroundColor: "#1e293b",
                    color: "#fff",
                    padding: "0.75rem 1rem",
                    borderRadius: "0.5rem",
                    fontSize: "0.875rem",
                    whiteSpace: "nowrap",
                    zIndex: 10,
                    boxShadow: "0 4px 12px rgba(0,0,0,0.2)",
                    pointerEvents: "none"
                  }}>
                    <div style={{ marginBottom: "0.25rem", fontWeight: 600 }}>
                      {item.date}
                    </div>
                    <div style={{ marginBottom: "0.25rem" }}>
                      거래 건수: <strong>{item.value || 0}건</strong>
                    </div>
                    <div style={{ marginBottom: "0.25rem" }}>
                      총 금액: <strong>₩{formatNumber(item.totalAmount || 0)}</strong>
                    </div>
                    <div>
                      판매자 수: <strong>{item.sellerCount || 0}명</strong>
                    </div>
                    {/* 화살표 */}
                    <div style={{
                      position: "absolute",
                      bottom: "-6px",
                      left: "50%",
                      transform: "translateX(-50%)",
                      width: 0,
                      height: 0,
                      borderLeft: "6px solid transparent",
                      borderRight: "6px solid transparent",
                      borderTop: "6px solid #1e293b"
                    }} />
                  </div>
                )}
              </div>
            );
          })}
        </div>
      </div>
    );
  }
