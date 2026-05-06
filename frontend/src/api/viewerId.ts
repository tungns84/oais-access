const KEY = "oais.viewerId";

export function getViewerId(): string {
  let id = sessionStorage.getItem(KEY);
  if (!id) {
    id = (crypto as Crypto & { randomUUID: () => string }).randomUUID();
    sessionStorage.setItem(KEY, id);
  }
  return id;
}
