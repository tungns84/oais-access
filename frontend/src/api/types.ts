export type DocumentKind = "PDF" | "OFFICE" | "IMAGE" | "VIDEO";

export interface DipManifest {
  id: string;
  title: string;
  kind: DocumentKind;
  mimeType: string;
  pageCount: number;
  hlsUrl: string | null;
  antiDownloadMode: 1 | 2 | 3;
  requiresPageToken: boolean;
}

export interface PageTokenResponse {
  token: string;
  expiresInSeconds: number;
}
