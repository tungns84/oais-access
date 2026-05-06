import { useEffect, useRef, useState } from "react";
import Hls from "hls.js";
import type { DipManifest } from "../../api/types";
import { api, hlsAssetUrl } from "../../api/client";
import { getViewerId } from "../../api/viewerId";

interface Props {
  manifest: DipManifest;
}

export default function VideoViewer({ manifest }: Props) {
  const videoRef = useRef<HTMLVideoElement>(null);
  const hlsRef = useRef<Hls | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const video = videoRef.current;
    if (!video) return;

    let cancelled = false;
    (async () => {
      let token: string | undefined;
      if (manifest.requiresPageToken) {
        try {
          const res = await api.getPageToken(manifest.id, 0);
          token = res.token;
        } catch (e) {
          setError("Không xin được token: " + String(e));
          return;
        }
      }
      if (cancelled) return;

      const masterUrl = hlsAssetUrl(manifest.id, "master.m3u8", token);

      // Build segment URLs by intercepting hls.js loader to append the same token + viewer header.
      if (Hls.isSupported()) {
        const hls = new Hls({
          xhrSetup: (xhr) => {
            xhr.setRequestHeader("X-Viewer-Id", getViewerId());
          },
        });
        hlsRef.current = hls;

        if (token) {
          // Rewrite each fragment URL to carry the same session token.
          const sessionToken = token;
          // eslint-disable-next-line @typescript-eslint/no-explicit-any
          const OrigLoader = hls.config.loader as any;
          // eslint-disable-next-line @typescript-eslint/no-explicit-any
          class TokenizingLoader extends OrigLoader {
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
            load(context: any, config: any, callbacks: any) {
              const m = String(context.url).match(/\/api\/dip\/[^/]+\/hls\/([^?]+)/);
              if (m) {
                context.url = hlsAssetUrl(manifest.id, m[1], sessionToken);
              }
              super.load(context, config, callbacks);
            }
          }
          // eslint-disable-next-line @typescript-eslint/no-explicit-any
          hls.config.loader = TokenizingLoader as any;
        }

        hls.loadSource(masterUrl);
        hls.attachMedia(video);
      } else if (video.canPlayType("application/vnd.apple.mpegurl")) {
        video.src = masterUrl;
      } else {
        setError("Trình duyệt không hỗ trợ HLS.");
      }
    })();

    return () => {
      cancelled = true;
      hlsRef.current?.destroy();
      hlsRef.current = null;
    };
  }, [manifest.id, manifest.requiresPageToken]);

  return (
    <div className="card" style={{ padding: 16, background: "#000" }}>
      {error && <div style={{ color: "var(--danger)" }}>{error}</div>}
      <video
        ref={videoRef}
        controls
        controlsList="nodownload noremoteplayback noplaybackrate"
        disablePictureInPicture
        onContextMenu={(e) => e.preventDefault()}
        style={{ width: "100%", maxHeight: "75vh", background: "#000" }}
      />
    </div>
  );
}
