import { useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { getMode, setMode, type AntiDownloadMode } from "../api/mode";

export default function ModeSelector() {
  const [mode, setLocalMode] = useState<AntiDownloadMode>(getMode());
  const qc = useQueryClient();

  function change(next: AntiDownloadMode) {
    setMode(next);
    setLocalMode(next);
    qc.invalidateQueries();
  }

  return (
    <div style={{ display: "flex", gap: 8, alignItems: "center", fontSize: 13 }}>
      <span style={{ color: "var(--muted)" }}>Anti-DL:</span>
      {[1, 2, 3].map((m) => (
        <button
          key={m}
          onClick={() => change(m as AntiDownloadMode)}
          className={`btn ${mode === m ? "" : "ghost"}`}
          style={{ padding: "4px 10px", fontSize: 12 }}
        >
          Mode {m}
        </button>
      ))}
    </div>
  );
}
