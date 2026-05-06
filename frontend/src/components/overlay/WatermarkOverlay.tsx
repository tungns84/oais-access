import { useEffect, useState } from "react";
import { getViewerId } from "../../api/viewerId";

function formatTimestamp(d: Date): string {
  const pad = (n: number) => String(n).padStart(2, "0");
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
}

export default function WatermarkOverlay() {
  const [now, setNow] = useState(formatTimestamp(new Date()));
  const viewerId = getViewerId().slice(0, 8);

  useEffect(() => {
    const id = setInterval(() => setNow(formatTimestamp(new Date())), 5_000);
    return () => clearInterval(id);
  }, []);

  const text = `${viewerId} • ${now}`;
  const cells = Array.from({ length: 60 });

  return (
    <div
      aria-hidden
      style={{
        position: "fixed",
        inset: 0,
        pointerEvents: "none",
        zIndex: 50,
        overflow: "hidden",
      }}
    >
      <div
        style={{
          position: "absolute",
          top: "-50%",
          left: "-50%",
          width: "200%",
          height: "200%",
          transform: "rotate(-30deg)",
          display: "grid",
          gridTemplateColumns: "repeat(6, 1fr)",
          gridAutoRows: "100px",
          alignItems: "center",
          justifyItems: "center",
          color: "rgba(248, 81, 73, 0.18)",
          fontSize: 14,
          fontWeight: 600,
          letterSpacing: 1,
          fontFamily: "monospace",
        }}
      >
        {cells.map((_, i) => (
          <span key={i}>{text}</span>
        ))}
      </div>
    </div>
  );
}
