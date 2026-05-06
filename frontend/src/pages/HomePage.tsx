import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import { api } from "../api/client";

const KIND_ICON: Record<string, string> = {
  PDF: "📕",
  OFFICE: "📘",
  IMAGE: "🖼️",
  VIDEO: "🎬",
};

export default function HomePage() {
  const qc = useQueryClient();
  const [toast, setToast] = useState<string | null>(null);

  const { data, isLoading, error } = useQuery({
    queryKey: ["documents"],
    queryFn: api.listDocuments,
  });

  const rescan = useMutation({
    mutationFn: api.rescan,
    onSuccess: (res) => {
      qc.invalidateQueries({ queryKey: ["documents"] });
      qc.invalidateQueries({ queryKey: ["manifest"] });
      setToast(`✓ Rescan: ${res.indexedCount} AIP(s) in ${res.elapsedMs}ms`);
      setTimeout(() => setToast(null), 4000);
    },
    onError: (e) => {
      setToast(`✗ Rescan failed: ${String(e)}`);
      setTimeout(() => setToast(null), 4000);
    },
  });

  if (isLoading) return <div className="card">Đang tải danh sách tài liệu…</div>;
  if (error) return <div className="card">Lỗi khi tải danh sách: {String(error)}</div>;

  return (
    <div>
      <div style={{ display: "flex", alignItems: "center", gap: 12, marginBottom: 4 }}>
        <h1 style={{ margin: 0 }}>Tài liệu khả dụng</h1>
        <button
          onClick={() => rescan.mutate()}
          disabled={rescan.isPending}
          className="btn ghost"
          title="Re-scan archival-storage/samples/ folder"
          style={{ marginLeft: "auto" }}
        >
          {rescan.isPending ? "Đang scan…" : "↻ Rescan"}
        </button>
      </div>
      <p style={{ color: "var(--muted)" }}>
        OAIS Access · DIP được generate khi bạn click vào tài liệu.
      </p>

      {toast && (
        <div
          style={{
            padding: 10,
            background: toast.startsWith("✓") ? "rgba(63, 185, 80, 0.15)" : "rgba(248, 81, 73, 0.15)",
            border: "1px solid var(--border)",
            borderRadius: 6,
            marginBottom: 12,
            fontSize: 14,
          }}
        >
          {toast}
        </div>
      )}

      {!data || data.length === 0 ? (
        <div className="card">
          <h2>Chưa có tài liệu</h2>
          <p>
            Đặt file mẫu vào <code>archival-storage/samples/</code> (PDF, DOCX, PNG, MP4) rồi bấm{" "}
            <strong>Rescan</strong> hoặc restart backend.
          </p>
        </div>
      ) : (
        <div
          style={{
            display: "grid",
            gridTemplateColumns: "repeat(auto-fill, minmax(260px, 1fr))",
            gap: 16,
            marginTop: 16,
          }}
        >
          {data.map((doc) => (
            <Link
              key={doc.id}
              to={`/view/${doc.id}`}
              className="card"
              style={{ display: "block", color: "inherit", textDecoration: "none" }}
            >
              <div style={{ fontSize: 36 }}>{KIND_ICON[doc.kind] ?? "📄"}</div>
              <div style={{ fontWeight: 600, marginTop: 8 }}>{doc.title}</div>
              <div style={{ marginTop: 8, display: "flex", gap: 8, alignItems: "center" }}>
                <span className={`badge kind-${doc.kind}`}>{doc.kind}</span>
                {doc.pageCount > 0 && doc.kind !== "VIDEO" && (
                  <span className="badge">{doc.pageCount} trang</span>
                )}
              </div>
              <div style={{ marginTop: 8, color: "var(--muted)", fontSize: 12 }}>
                {doc.mimeType}
              </div>
            </Link>
          ))}
        </div>
      )}
    </div>
  );
}
