import { useEffect, useRef } from "react";
import type { DipManifest } from "../../api/types";
import { api, fetchPageImage } from "../../api/client";

interface Props {
  manifest: DipManifest;
}

export default function ImageViewer({ manifest }: Props) {
  const canvasRef = useRef<HTMLCanvasElement>(null);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      let token: string | undefined;
      if (manifest.requiresPageToken) {
        const t = await api.getPageToken(manifest.id, 1);
        token = t.token;
      }
      const blob = await fetchPageImage(manifest.id, 1, token);
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
  }, [manifest.id, manifest.requiresPageToken]);

  return (
    <div
      className="card"
      style={{
        display: "flex",
        justifyContent: "center",
        background: "#000",
        padding: 16,
      }}
    >
      <canvas
        ref={canvasRef}
        onContextMenu={(e) => e.preventDefault()}
        style={{
          maxWidth: "100%",
          maxHeight: "75vh",
          height: "auto",
          userSelect: "none",
        }}
      />
    </div>
  );
}
