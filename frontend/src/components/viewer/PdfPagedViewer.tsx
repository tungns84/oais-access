import { useEffect, useMemo, useRef, useState } from "react";
import type { DipManifest } from "../../api/types";
import { api, fetchPageImage } from "../../api/client";

interface Props {
  manifest: DipManifest;
}

interface TokenCacheEntry {
  token: string;
  expiresAt: number;
}

export default function PdfPagedViewer({ manifest }: Props) {
  const [page, setPage] = useState(1);
  const [zoom, setZoom] = useState(1);
  const [tokenCache, setTokenCache] = useState<Record<number, TokenCacheEntry>>({});
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const requiresToken = manifest.requiresPageToken;

  const ensureToken = useMemo(() => {
    return async (n: number): Promise<string | undefined> => {
      if (!requiresToken) return undefined;
      const cached = tokenCache[n];
      if (cached && cached.expiresAt > Date.now() + 5_000) return cached.token;
      const res = await api.getPageToken(manifest.id, n);
      const expiresAt = Date.now() + res.expiresInSeconds * 1000;
      setTokenCache((prev) => ({ ...prev, [n]: { token: res.token, expiresAt } }));
      return res.token;
    };
  }, [manifest.id, requiresToken, tokenCache]);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      const tok = await ensureToken(page);
      if (cancelled) return;
      const blob = await fetchPageImage(manifest.id, page, tok);
      if (cancelled) return;
      const bitmap = await createImageBitmap(blob);
      if (cancelled) {
        bitmap.close();
        return;
      }
      const canvas = canvasRef.current;
      if (!canvas) {
        bitmap.close();
        return;
      }
      canvas.width = bitmap.width;
      canvas.height = bitmap.height;
      canvas.getContext("2d")?.drawImage(bitmap, 0, 0);
      bitmap.close();
    })();
    return () => {
      cancelled = true;
    };
  }, [page, manifest.id, ensureToken]);

  const last = manifest.pageCount;

  return (
    <div className="card" style={{ padding: 16 }}>
      <div style={{ display: "flex", gap: 8, alignItems: "center", marginBottom: 12 }}>
        <button className="btn ghost" onClick={() => setPage(1)} disabled={page === 1}>
          ⏮
        </button>
        <button
          className="btn ghost"
          onClick={() => setPage((p) => Math.max(1, p - 1))}
          disabled={page === 1}
        >
          ←
        </button>
        <span>
          Trang{" "}
          <input
            type="number"
            min={1}
            max={last}
            value={page}
            onChange={(e) => {
              const v = Number(e.target.value);
              if (v >= 1 && v <= last) setPage(v);
            }}
            style={{
              width: 60,
              padding: 4,
              background: "var(--bg)",
              color: "var(--text)",
              border: "1px solid var(--border)",
              borderRadius: 4,
            }}
          />
          {" / "}
          {last}
        </span>
        <button
          className="btn ghost"
          onClick={() => setPage((p) => Math.min(last, p + 1))}
          disabled={page === last}
        >
          →
        </button>
        <button className="btn ghost" onClick={() => setPage(last)} disabled={page === last}>
          ⏭
        </button>
        <div style={{ flex: 1 }} />
        <button className="btn ghost" onClick={() => setZoom((z) => Math.max(0.5, z - 0.25))}>
          −
        </button>
        <span style={{ minWidth: 40, textAlign: "center" }}>{Math.round(zoom * 100)}%</span>
        <button className="btn ghost" onClick={() => setZoom((z) => Math.min(3, z + 0.25))}>
          +
        </button>
      </div>
      <div
        style={{
          display: "flex",
          justifyContent: "center",
          background: "#000",
          padding: 16,
          borderRadius: 6,
          overflow: "auto",
          maxHeight: "75vh",
        }}
      >
        <canvas
          ref={canvasRef}
          onContextMenu={(e) => e.preventDefault()}
          style={{
            transform: `scale(${zoom})`,
            transformOrigin: "top center",
            maxWidth: "100%",
            height: "auto",
            userSelect: "none",
          }}
        />
      </div>
    </div>
  );
}
