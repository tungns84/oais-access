export default function AboutPage() {
  return (
    <div className="card">
      <h1>OAIS Access — POC Mapping</h1>
      <p>
        POC này demo functional entity <strong>Access</strong> trong mô hình OAIS (ISO 14721) —
        phần giao tiếp với người dùng cuối để xem tài liệu lưu trữ <em>không cho phép tải về</em>.
      </p>
      <h2>Mapping</h2>
      <table style={{ width: "100%", borderCollapse: "collapse" }}>
        <thead>
          <tr style={{ borderBottom: "1px solid var(--border)", textAlign: "left" }}>
            <th style={{ padding: 8 }}>OAIS Concept</th>
            <th style={{ padding: 8 }}>POC Implementation</th>
          </tr>
        </thead>
        <tbody>
          {[
            ["AIP", "File trong archival-storage/samples/ + AipMetadata"],
            ["DIP", "Page-PNG / HLS segment trong derived-cache/"],
            ["Coordinate Access Activities", "DocumentController, DipController"],
            ["Generate DIP", "DipGeneratorService + renderer per kind"],
            ["Deliver Response", "Streaming + cache-control + page token (mode 3)"],
            ["Access Aids", "Manifest endpoint mô tả pages, kind"],
            ["Audit", "AccessAuditService append JSON line"],
          ].map(([k, v]) => (
            <tr key={k} style={{ borderBottom: "1px solid var(--border)" }}>
              <td style={{ padding: 8, fontWeight: 600 }}>{k}</td>
              <td style={{ padding: 8 }}>{v}</td>
            </tr>
          ))}
        </tbody>
      </table>

      <h2>Anti-Download Modes</h2>
      <ul>
        <li>
          <strong>Mode 1 — Basic</strong>: Disable right-click/copy/drag, no download button,
          <code>Cache-Control: no-store</code>, image-based render.
        </li>
        <li>
          <strong>Mode 2 — Watermark</strong>: Mode 1 + watermark <code>{"{viewerId}-{timestamp}"}</code>{" "}
          chéo trên mỗi page/frame.
        </li>
        <li>
          <strong>Mode 3 — Strong</strong>: Mode 2 + HMAC page-token TTL 30s bound viewerId,
          blur khi mất focus / mở DevTools.
        </li>
      </ul>

      <h2>Giới hạn (Out of Scope)</h2>
      <p style={{ color: "var(--warning)" }}>
        POC <strong>không phải DRM thật</strong>. Người có quyền truy cập màn hình luôn có thể
        screenshot bằng OS / camera. Defense-in-depth giảm casual leak, không thay thế access
        control thật trong production.
      </p>
    </div>
  );
}
