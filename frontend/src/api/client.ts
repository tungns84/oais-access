import { getViewerId } from "./viewerId";
import { getMode } from "./mode";
import type { DipManifest, PageTokenResponse } from "./types";

function commonHeaders(): HeadersInit {
  return {
    Accept: "application/json",
    "X-Viewer-Id": getViewerId(),
    "X-Anti-Download-Mode": String(getMode()),
  };
}

async function get<T>(path: string): Promise<T> {
  const res = await fetch(path, { headers: commonHeaders() });
  if (!res.ok) throw new Error(`${res.status} ${res.statusText}`);
  return (await res.json()) as T;
}

async function post<T>(path: string): Promise<T> {
  const res = await fetch(path, { method: "POST", headers: commonHeaders() });
  if (!res.ok) throw new Error(`${res.status} ${res.statusText}`);
  return (await res.json()) as T;
}

export interface RescanResult {
  indexedCount: number;
  elapsedMs: number;
  scannedAt: string;
}

export const api = {
  listDocuments: () => get<DipManifest[]>("/api/documents"),
  getManifest: (id: string) => get<DipManifest>(`/api/documents/${id}/manifest`),
  getPageToken: (id: string, page: number) =>
    get<PageTokenResponse>(`/api/dip/${id}/page-token/${page}`),
  rescan: () => post<RescanResult>("/api/admin/rescan"),
};

export function pageImageUrl(id: string, pageNumber: number, token?: string): string {
  const params = new URLSearchParams({ m: String(getMode()) });
  if (token) params.set("t", token);
  return `/api/dip/${id}/page/${pageNumber}?${params.toString()}`;
}

export async function fetchPageImage(
  id: string,
  pageNumber: number,
  token?: string,
): Promise<Blob> {
  const url = pageImageUrl(id, pageNumber, token);
  const res = await fetch(url, { headers: commonHeaders() });
  if (!res.ok) throw new Error(`${res.status} ${res.statusText}`);
  return res.blob();
}

export function hlsAssetUrl(id: string, fileName: string, token?: string): string {
  const params = new URLSearchParams({ m: String(getMode()) });
  if (token) params.set("t", token);
  return `/api/dip/${id}/hls/${fileName}?${params.toString()}`;
}
