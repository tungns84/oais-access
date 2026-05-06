# POC OAIS Access — Document Viewing System (No Direct Download)

POC theo `prd.md`: hệ thống cho phép **xem** tài liệu trực tuyến mà **không** cung cấp chức năng tải về, lấy cảm hứng từ functional entity **Access** trong mô hình OAIS (ISO 14721).

**Stack**: ReactJS (Vite + TypeScript) + Spring Boot 3 (Java 21) + PDFBox + LibreOffice headless + ffmpeg.

---

## OAIS Access Mapping

| OAIS Concept | POC Implementation |
|---|---|
| **AIP** (Archival Info Package) | File trong `archival-storage/samples/` + `AipMetadata` |
| **DIP** (Dissemination Info Package) | Page-PNG / HLS segment trong `archival-storage/derived-cache/` |
| **Coordinate Access Activities** | `DocumentController`, `DipController` |
| **Generate DIP** | `DipGeneratorService` + `Renderer` strategy (PDF/Office/Image/Video) |
| **Deliver Response** | Streaming + cache-control + page token (mode 3) + no-attachment headers |
| **Access Aids** | Manifest endpoint mô tả pages, kind, viewer hint |
| **Audit** | `AccessAuditService` ghi JSON line vào `logs/access-audit.log` |

---

## Cấu trúc

```
poc-oais-access/
├── prd.md                       # PRD gốc
├── README.md                    # File này
├── docker-compose.yml           # backend + frontend container (LibreOffice/ffmpeg pre-installed)
├── backend/                     # Spring Boot 3 + Java 21
│   ├── build.gradle.kts
│   └── src/main/java/com/poc/oais/access/
│       ├── controller/          # REST endpoints
│       ├── service/             # Storage, DIP gen, token, watermark, audit
│       │   └── renderer/        # PDF/Office/Image/Video renderers
│       ├── config/              # CORS, properties
│       └── model/               # AipMetadata, DipManifest
├── frontend/                    # Vite + React 18 + TS
│   └── src/
│       ├── pages/               # Home, Viewer, About
│       ├── components/
│       │   ├── viewer/          # PdfPagedViewer, ImageViewer, VideoViewer
│       │   └── overlay/         # NoContext, Watermark, BlurOnBlur
│       └── api/                 # client, types, viewerId
└── archival-storage/
    ├── samples/                 # đặt file mẫu vào đây
    └── derived-cache/           # (auto, gitignored)
```

---

## Yêu cầu môi trường

| Tool | Mục đích | Cài đặt |
|---|---|---|
| Java 21 | Backend runtime | https://adoptium.net |
| Gradle 8.5+ | Build backend (nếu không dùng wrapper) | `winget install Gradle.Gradle` |
| Node.js 20+ | Frontend dev | https://nodejs.org |
| LibreOffice | Office render (DOCX/XLSX/PPTX) | `winget install TheDocumentFoundation.LibreOffice` / `apt install libreoffice` |
| ffmpeg | Video HLS segmenting | `winget install Gyan.FFmpeg` / `apt install ffmpeg` |

LibreOffice/ffmpeg là **optional** — nếu thiếu, các loại tài liệu khác (PDF, Image) vẫn hoạt động. Chỉ DOCX/XLSX/PPTX/MP4 là phụ thuộc.

---

## Chạy POC

### Bước 1 — Chuẩn bị tài liệu mẫu

Đặt 4 file mẫu vào `archival-storage/samples/`:

```
archival-storage/samples/
├── sample.pdf    # PDF bất kỳ
├── sample.docx   # Word
├── sample.png    # Image
└── sample.mp4    # H.264 MP4 (dạng web)
```

`ArchivalStorageService` sẽ scan thư mục này lúc Spring Boot startup. Restart backend nếu thêm/xoá file.

### Bước 2 — Chạy backend (Spring Boot)

```powershell
cd backend
gradle bootRun
# hoặc nếu đã sinh wrapper: .\gradlew.bat bootRun
```

Lần đầu, sinh wrapper jar nếu cần:
```powershell
gradle wrapper
```

Backend chạy trên `http://localhost:8090` (default; override qua env `SERVER_PORT=8090`). Verify: `curl http://localhost:8090/api/health`.

### Bước 3 — Chạy frontend (Vite)

```powershell
cd frontend
npm install
npm run dev
```

Frontend chạy trên `http://localhost:5173`. Vite proxy `/api` → backend.

### Chạy bằng Docker (optional)

```powershell
docker compose up
```

Image base có sẵn LibreOffice + ffmpeg.

### Chạy unit test

```powershell
cd backend
gradle test
```

Test coverage:
- `AccessApplicationTests` — smoke test health + documents endpoint.
- `TokenServiceTest` — HMAC issue/verify, mismatch/expiry/tampering rejection.

---

## Anti-Download Modes

POC hỗ trợ 3 chế độ chống tải, switch runtime qua **ModeSelector** ở header (hoặc env `OAIS_ANTI_DOWNLOAD_MODE` cho default backend).

| Mode | Hành vi |
|---|---|
| **1 — Basic** | Disable right-click/copy/drag/selection; remove download button; backend gửi `Cache-Control: no-store`, `Content-Disposition: inline; filename=""`, no-referrer |
| **2 — Watermark** | Mode 1 + watermark `{viewerId}-{timestamp}` chéo trên mỗi page (server-side `Graphics2D` cho ảnh, CSS overlay cho video) |
| **3 — Strong** | Mode 2 + HMAC page-token TTL 30s bound `viewerId`; viewer **blur** khi mất focus / mở DevTools / tab inactive |

Mode hiện tại được truyền qua header `X-Anti-Download-Mode` (cho fetch JSON) hoặc query param `m=1|2|3` (cho asset URL `<img>`/`<video>`). Backend `ModeResolver` đọc cả hai, fallback về config.

---

## Demo Flow End-to-End

1. **Smoke**: Mở `http://localhost:5173` → home page liệt kê 4 cards (PDF, DOCX, PNG, MP4).
2. **Viewer**: Click từng card → render đúng (PdfPagedViewer/ImageViewer/VideoViewer). Navigate page (PDF/DOCX), seek (video).
3. **Mode 1**: Right-click trên ảnh page → menu bị chặn. DevTools → Network tab thấy URL `/api/dip/.../page/N`, response header `Content-Disposition: inline; filename=""` (không gợi ý filename khi save).
4. **Mode 2**: Đổi sang Mode 2 → mỗi page có watermark `{8 chars viewerId} • {yyyy-MM-dd HH:mm:ss}` chéo đỏ nhạt. Save page-as-PNG vẫn dính watermark.
5. **Mode 3**:
   - Network tab: thấy request `/api/dip/.../page-token/N` rồi `/api/dip/.../page/N?t=...&m=3`.
   - Copy URL từ Network sang **incognito tab** → 403 (token bound viewerId).
   - Mở DevTools (F12) trên viewer → viewer **blur 20px**.
   - Alt+Tab sang app khác → viewer **blur**.
   - Đợi 30s không thao tác → token expire, request mới sẽ refresh token tự động.
6. **Audit**: `Get-Content backend/logs/access-audit.log -Tail 20` → mỗi page request có một dòng JSON `{ts, aipId, viewerId, event, details}`.
7. **OAIS mapping**: Click "OAIS Mapping" trên header → trang `/about` giải thích từng functional entity.

---

## Bổ sung tài liệu test

POC scan thư mục `archival-storage/samples/` lúc backend startup. Có 2 cách add file:

### Cách 1 — Hot-reload qua nút Rescan (Recommended)

1. Đặt file vào `archival-storage/samples/` (depth tối đa 2 level từ root).
2. Trên Home page, bấm nút **↻ Rescan** ở góc phải trên.
3. Toast hiển thị `✓ Rescan: N AIP(s) in Xms` — list refresh tự động.

Hoặc dùng curl:

```powershell
curl -X POST http://localhost:8090/api/admin/rescan
# {"indexedCount":3,"elapsedMs":12,"scannedAt":"2026-05-06T..."}
```

### Cách 2 — Restart backend (legacy)

Restart `mvn spring-boot:run` — `@PostConstruct` scan tự động.

### Quy tắc filename

| Rule | Lý do |
|---|---|
| Extension hỗ trợ: `.pdf` `.png` `.jpg` `.jpeg` `.docx` `.xlsx` `.pptx` `.mp4` | Khớp `DocumentKind.fromExtension()` |
| Không bắt đầu bằng `.` | Skip dot-files |
| Không trùng tên `README.txt` | Hardcode skip |
| Filename khác → ID khác (SHA-1 hash) | Đổi tên = doc mới + cache mới |
| Cùng filename → cache reused | Tiết kiệm render khi re-test cùng file |

### Nguồn sample khuyến nghị

| File | Mục đích test | Source |
|---|---|---|
| Mozilla `tracemonkey.pdf` (1MB, 14 pages) — đã có | Multi-page PDF baseline | https://raw.githubusercontent.com/mozilla/pdf.js/master/test/pdfs/tracemonkey.pdf |
| arXiv paper PDF (vài MB, 30+ pages) | Performance medium | https://arxiv.org/pdf/2501.12948.pdf (ví dụ) |
| Wikipedia ảnh JPG | JPG passthrough | https://upload.wikimedia.org/wikipedia/commons/thumb/1/15/Cat_August_2010-4.jpg/640px-Cat_August_2010-4.jpg |
| Self-generated PNG | Đã có (`sample.png`) | Script PowerShell trong session demo |

### Reset cache để re-test

Khi muốn test lại render từ đầu (vd: đổi DPI):

```powershell
Remove-Item -Recurse -Force "D:\projects\claudeCode\poc-oais-access\archival-storage\derived-cache\*"
# Bấm Rescan trên UI
```

---

## API Reference

```
GET  /api/health                                  → {status, antiDownloadMode, archivalRoot}
GET  /api/documents                               → List<DipManifest>
GET  /api/documents/{id}/manifest                 → DipManifest
GET  /api/documents/{id}                          → AipMetadata
POST /api/admin/rescan                            → re-scan samples folder, return {indexedCount, elapsedMs, scannedAt}
GET  /api/dip/{id}/page-token/{n}                 → {token, expiresInSeconds}    (mode 3)
GET  /api/dip/{id}/page/{n}?t={token}&m={mode}    → image/png (PDF/OFFICE/IMAGE)
GET  /api/dip/{id}/hls/{file}?t={token}&m={mode}  → application/vnd.apple.mpegurl | video/mp2t
```

Headers chung:
- Request: `X-Viewer-Id` (UUID per session, frontend tự sinh), `X-Anti-Download-Mode` (1|2|3).
- Response: `Cache-Control: no-store`, `X-Content-Type-Options: nosniff`, `Referrer-Policy: no-referrer`, `Cross-Origin-Resource-Policy: same-origin`, `X-Anti-Download-Mode`.

---

## Cấu hình

`backend/src/main/resources/application.yml`:

```yaml
oais:
  storage:
    archival-root: ../archival-storage/samples
    derived-cache: ../archival-storage/derived-cache
  anti-download:
    mode: 1                          # default; override per-request via X-Anti-Download-Mode
    page-token-ttl-seconds: 30
  watermark:
    template: "{viewerId}-{timestamp}"
    enabled-for-modes: [2, 3]
  rendering:
    pdf-dpi: 150
    office-convert-timeout-seconds: 60
    ffmpeg-timeout-seconds: 300
    libreoffice-binary: soffice
    ffmpeg-binary: ffmpeg
  audit:
    log-path: ./logs/access-audit.log
  cors:
    allowed-origins: http://localhost:5173
```

Env override: `OAIS_STORAGE_ARCHIVAL_ROOT`, `OAIS_ANTI_DOWNLOAD_MODE`, `LIBREOFFICE_BINARY`, `FFMPEG_BINARY`.

---

## Threat Model & POC Limits

| Threat | Mitigation | POC Limit |
|---|---|---|
| User dùng "Save As" trên trang | `Content-Disposition: inline; filename=""`, image-based render thay vì serve file gốc | Vẫn save được PNG đơn lẻ — watermark che giảm giá trị |
| User mở Network tab, copy URL | Mode 3: token bound viewerId + 30s expiry | Trong cùng session URL vẫn valid trong 30s |
| OS screenshot | `BlurOnBlurOverlay` blur khi mất focus | Không chống được professional capture (camera, OBS) |
| User dùng DevTools xoá overlay | DevTools heuristic blur (outerHeight - innerHeight > 160) | User có quyền browser luôn thắng |
| User share viewerId | Server log + có thể rate limit theo viewerId | Không có auth thật để attribute thực |
| Path traversal qua AIP id | ID là SHA-1 hash, lookup qua index map (không concat path) | Đã chặn |
| HLS segment filename injection | Whitelist regex `^(master\.m3u8\|seg-\d+\.ts)$` | Đã chặn |

> **POC này KHÔNG phải DRM thật**. Người có quyền truy cập màn hình luôn có thể chụp screenshot bằng OS / camera ngoài. Defense-in-depth giảm casual leak, không thay thế access control thật trong production.

---

## Out of Scope

- Authentication thật (chỉ pseudo `viewerId` UUID per session).
- Upload UI — đặt file thủ công vào `archival-storage/samples/`.
- Search / list filtering nâng cao.
- Render performance không tối ưu cho file lớn (>50MB hoặc >100 pages).
- Giải pháp DRM thật (Widevine/PlayReady) cho video.
- Persistence của HMAC key (sinh ngẫu nhiên mỗi lần restart, invalidating tokens).
