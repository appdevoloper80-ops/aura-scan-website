export type AgeGateStatus = 'UNSET' | 'ADULT_18_PLUS' | 'UNDER_18';

export interface AuraScanDocument {
  id: string;
  name: string;
  createdAt: number;
  pageCount: number;
  pageDataUrls: string[];
  pdfBlob: Blob;
}

const AGE_KEY = 'aurascan_age_gate_status';
const DB_NAME = 'aurascan_db';
const DB_VERSION = 1;
const STORE_NAME = 'documents';

export function getAgeGateStatus(): AgeGateStatus {
  try {
    const val = localStorage.getItem(AGE_KEY);
    if (val === 'ADULT_18_PLUS' || val === 'UNDER_18') return val;
  } catch {
    // ignore
  }
  return 'UNSET';
}

export function setAgeGateStatus(status: AgeGateStatus): void {
  try {
    localStorage.setItem(AGE_KEY, status);
  } catch {
    // ignore
  }
}

function openDb(): Promise<IDBDatabase> {
  return new Promise((resolve, reject) => {
    const request = indexedDB.open(DB_NAME, DB_VERSION);
    request.onupgradeneeded = () => {
      const db = request.result;
      if (!db.objectStoreNames.contains(STORE_NAME)) {
        db.createObjectStore(STORE_NAME, { keyPath: 'id' });
      }
    };
    request.onsuccess = () => resolve(request.result);
    request.onerror = () => reject(request.error ?? new Error('IndexedDB failed'));
  });
}

export async function getAllDocuments(): Promise<AuraScanDocument[]> {
  const db = await openDb();
  return new Promise((resolve, reject) => {
    const tx = db.transaction(STORE_NAME, 'readonly');
    const store = tx.objectStore(STORE_NAME);
    const req = store.getAll();
    req.onsuccess = () => {
      const docs = (req.result as AuraScanDocument[]) || [];
      docs.sort((a, b) => b.createdAt - a.createdAt);
      resolve(docs);
    };
    req.onerror = () => reject(req.error ?? new Error('Failed to load documents'));
  });
}

export async function saveDocument(doc: AuraScanDocument): Promise<void> {
  const db = await openDb();
  return new Promise((resolve, reject) => {
    const tx = db.transaction(STORE_NAME, 'readwrite');
    tx.objectStore(STORE_NAME).put(doc);
    tx.oncomplete = () => resolve();
    tx.onerror = () => reject(tx.error ?? new Error('Failed to save document'));
  });
}

export async function renameStoredDocument(id: string, newName: string): Promise<void> {
  const db = await openDb();
  return new Promise((resolve, reject) => {
    const tx = db.transaction(STORE_NAME, 'readwrite');
    const store = tx.objectStore(STORE_NAME);
    const getReq = store.get(id);
    getReq.onsuccess = () => {
      const existing = getReq.result as AuraScanDocument | undefined;
      if (existing) {
        store.put({ ...existing, name: newName.trim() });
      }
    };
    tx.oncomplete = () => resolve();
    tx.onerror = () => reject(tx.error ?? new Error('Failed to rename document'));
  });
}

export async function deleteStoredDocument(id: string): Promise<void> {
  const db = await openDb();
  return new Promise((resolve, reject) => {
    const tx = db.transaction(STORE_NAME, 'readwrite');
    tx.objectStore(STORE_NAME).delete(id);
    tx.oncomplete = () => resolve();
    tx.onerror = () => reject(tx.error ?? new Error('Failed to delete document'));
  });
}

export function sanitizeFileName(name: string, ext: string): string {
  const clean = name
    .trim()
    .replace(/[^a-zA-Z0-9._\- ]/g, '_')
    .trim() || 'aurascan_export';
  return clean.toLowerCase().endsWith(`.${ext}`) ? clean : `${clean}.${ext}`;
}

export function downloadBlob(blob: Blob, filename: string): void {
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  a.remove();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}

export async function shareOrDownloadFile(
  blob: Blob,
  filename: string,
  mimeType: string,
  title: string
): Promise<'shared' | 'downloaded'> {
  const file = new File([blob], filename, { type: mimeType });
  if (typeof navigator !== 'undefined' && navigator.canShare?.({ files: [file] })) {
    try {
      await navigator.share({ title, files: [file] });
      return 'shared';
    } catch (err) {
      if ((err as Error)?.name === 'AbortError') {
        return 'shared';
      }
    }
  }
  downloadBlob(blob, filename);
  return 'downloaded';
}

export function formatTimestampName(prefix: 'Scan' | 'PDF', ts = Date.now()): string {
  const d = new Date(ts);
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${prefix} ${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
}
