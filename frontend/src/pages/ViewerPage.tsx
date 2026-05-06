import { useQuery } from "@tanstack/react-query";
import { Link, useParams } from "react-router-dom";
import { api } from "../api/client";
import PdfPagedViewer from "../components/viewer/PdfPagedViewer";
import ImageViewer from "../components/viewer/ImageViewer";
import VideoViewer from "../components/viewer/VideoViewer";
import NoContextOverlay from "../components/overlay/NoContextOverlay";
import WatermarkOverlay from "../components/overlay/WatermarkOverlay";
import BlurOnBlurOverlay from "../components/overlay/BlurOnBlurOverlay";

export default function ViewerPage() {
  const { id } = useParams<{ id: string }>();
  const { data: manifest, isLoading, error } = useQuery({
    queryKey: ["manifest", id],
    queryFn: () => api.getManifest(id!),
    enabled: !!id,
  });

  if (isLoading) return <div className="card">Loading manifest…</div>;
  if (error) return (
    <div className="card">
      <p>Không thể tải manifest: {String(error)}</p>
      <Link to="/">← Về trang chủ</Link>
    </div>
  );
  if (!manifest) return null;

  const watermarkActive = manifest.antiDownloadMode >= 2;
  const strongMode = manifest.antiDownloadMode === 3;

  return (
    <div>
      <Link to="/">← Tài liệu</Link>
      <h1 style={{ marginBottom: 4 }}>{manifest.title}</h1>
      <div style={{ display: "flex", gap: 8, marginBottom: 16 }}>
        <span className={`badge kind-${manifest.kind}`}>{manifest.kind}</span>
        <span className="badge">Mode {manifest.antiDownloadMode}</span>
        {manifest.pageCount > 0 && manifest.kind !== "VIDEO" && (
          <span className="badge">{manifest.pageCount} trang</span>
        )}
      </div>

      <div className="viewer-shell">
        <NoContextOverlay />
        {watermarkActive && <WatermarkOverlay />}
        {strongMode && <BlurOnBlurOverlay />}

        {manifest.kind === "PDF" || manifest.kind === "OFFICE" ? (
          <PdfPagedViewer manifest={manifest} />
        ) : manifest.kind === "IMAGE" ? (
          <ImageViewer manifest={manifest} />
        ) : (
          <VideoViewer manifest={manifest} />
        )}
      </div>
    </div>
  );
}
