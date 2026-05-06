const KEY = "oais.antiDownloadMode";

export type AntiDownloadMode = 1 | 2 | 3;

export function getMode(): AntiDownloadMode {
  const v = Number(localStorage.getItem(KEY) ?? "1");
  return v === 2 || v === 3 ? v : 1;
}

export function setMode(mode: AntiDownloadMode): void {
  localStorage.setItem(KEY, String(mode));
}
