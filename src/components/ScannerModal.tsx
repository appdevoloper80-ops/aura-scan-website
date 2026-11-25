import { useEffect, useRef, useState } from 'react';
import { ArrowLeft, Camera, Check, ImagePlus, Loader2, Trash2 } from 'lucide-react';
import { APP_CONSTANTS } from '../lib/constants';
import {
  applyScanFilterToDataUrl,
  fileToNormalizedDataUrl,
  type ScanFilter,
} from '../lib/canvasUtils';
import type { AppLocale } from '../lib/i18n';
import { STRINGS } from '../lib/i18n';
import { buildPdfFromDataUrls } from '../lib/pdfBuilder';
import { formatTimestampName, type AuraScanDocument } from '../lib/storage';

interface ScannedPageDraft {
  id: string;
  originalDataUrl: string;
  filteredDataUrl: string;
}

interface ScannerModalProps {
  locale: AppLocale;
  onCancel: () => void;
  onSaveDocument: (doc: AuraScanDocument) => Promise<void>;
  onError: (message: string) => void;
}

export function ScannerModal({
  locale,
  onCancel,
  onSaveDocument,
  onError,
}: ScannerModalProps) {
  const t = STRINGS[locale];
  const videoRef = useRef<HTMLVideoElement | null>(null);
  const streamRef = useRef<MediaStream | null>(null);

  const [cameraActive, setCameraActive] = useState(false);
  const [cameraMessage, setCameraMessage] = useState<string | null>(null);
  const [filter, setFilter] = useState<ScanFilter>('clean');
  const [pages, setPages] = useState<ScannedPageDraft[]>([]);
  const [isSaving, setIsSaving] = useState(false);

  const stopCamera = () => {
    if (streamRef.current) {
      streamRef.current.getTracks().forEach((track) => track.stop());
      streamRef.current = null;
    }
    setCameraActive(false);
  };

  useEffect(() => {
    return () => {
      if (streamRef.current) {
        streamRef.current.getTracks().forEach((track) => track.stop());
      }
    };
  }, []);

  const startCamera = async () => {
    setCameraMessage(null);
    if (!navigator.mediaDevices?.getUserMedia) {
      setCameraMessage(t.scannerCameraUnavailable);
      return;
    }
    try {
      const stream = await navigator.mediaDevices.getUserMedia({
        video: { facingMode: 'environment' },
        audio: false,
      });
      streamRef.current = stream;
      setCameraActive(true);
      requestAnimationFrame(() => {
        if (videoRef.current) {
          videoRef.current.srcObject = stream;
        }
      });
    } catch {
      setCameraMessage(t.scannerCameraUnavailable);
    }
  };

  const captureFromCamera = async () => {
    const video = videoRef.current;
    if (!video || pages.length >= APP_CONSTANTS.MAX_SCAN_PAGES) return;
    const canvas = document.createElement('canvas');
    canvas.width = video.videoWidth || 1280;
    canvas.height = video.videoHeight || 720;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;
    ctx.drawImage(video, 0, 0, canvas.width, canvas.height);
    const original = canvas.toDataURL('image/jpeg', 0.92);
    const filtered = await applyScanFilterToDataUrl(original, filter);
    setPages((prev) => [
      ...prev,
      { id: crypto.randomUUID(), originalDataUrl: original, filteredDataUrl: filtered },
    ]);
  };

  const handleImportPhotos = async (files: FileList | null) => {
    if (!files || files.length === 0) return;
    const remaining = APP_CONSTANTS.MAX_SCAN_PAGES - pages.length;
    if (remaining <= 0) {
      onError(t.scannerPagesLimit(APP_CONSTANTS.MAX_SCAN_PAGES));
      return;
    }
    const chosen = Array.from(files).slice(0, remaining);
    try {
      const imported: ScannedPageDraft[] = [];
      for (const file of chosen) {
        const { dataUrl } = await fileToNormalizedDataUrl(file, 1600);
        const filtered = await applyScanFilterToDataUrl(dataUrl, filter);
        imported.push({
          id: crypto.randomUUID(),
          originalDataUrl: dataUrl,
          filteredDataUrl: filtered,
        });
      }
      setPages((prev) => [...prev, ...imported]);
    } catch (err) {
      onError((err as Error).message || 'Could not load selected image');
    }
  };

  const handleFilterChange = async (nextFilter: ScanFilter) => {
    setFilter(nextFilter);
    const updated = await Promise.all(
      pages.map(async (p) => ({
        ...p,
        filteredDataUrl: await applyScanFilterToDataUrl(p.originalDataUrl, nextFilter),
      }))
    );
    setPages(updated);
  };

  const handleFinishScan = async () => {
    if (pages.length === 0 || isSaving) return;
    setIsSaving(true);
    try {
      stopCamera();
      const pageUrls = pages.map((p) => p.filteredDataUrl);
      const pdfBlob = await buildPdfFromDataUrls(pageUrls, 'A4', 'SMALL');
      const now = Date.now();
      const doc: AuraScanDocument = {
        id: crypto.randomUUID(),
        name: formatTimestampName('Scan', now),
        createdAt: now,
        pageCount: pageUrls.length,
        pageDataUrls: pageUrls,
        pdfBlob,
      };
      await onSaveDocument(doc);
    } catch (err) {
      onError((err as Error).message || 'Failed to save scan');
      setIsSaving(false);
    }
  };

  return (
    <div className="flex min-h-screen flex-col bg-[#F6FAFB] text-slate-900">
      <header className="sticky top-0 z-20 border-b border-slate-200 bg-white">
        <div className="mx-auto flex max-w-3xl items-center justify-between px-4 py-3">
          <div className="flex items-center gap-2">
            <button
              type="button"
              onClick={() => {
                stopCamera();
                onCancel();
              }}
              aria-label={t.back}
              className="flex min-h-12 min-w-12 items-center justify-center rounded-xl text-slate-700 hover:bg-slate-100"
            >
              <ArrowLeft className="h-5 w-5" aria-hidden="true" />
            </button>
            <h1 className="text-lg font-semibold text-slate-900">{t.scannerTitle}</h1>
          </div>
          {pages.length > 0 && (
            <button
              type="button"
              onClick={handleFinishScan}
              disabled={isSaving}
              className="flex min-h-12 items-center gap-2 rounded-xl bg-teal-700 px-4 py-2 text-sm font-semibold text-white hover:bg-teal-800 disabled:opacity-60"
            >
              {isSaving ? (
                <Loader2 className="h-4 w-4 animate-spin" aria-hidden="true" />
              ) : (
                <Check className="h-4 w-4" aria-hidden="true" />
              )}
              <span>{t.scannerSavePdf}</span>
            </button>
          )}
        </div>
      </header>

      <main className="mx-auto w-full max-w-3xl flex-1 space-y-5 px-4 py-5">
        <div className="flex flex-wrap gap-2.5">
          {!cameraActive ? (
            <button
              type="button"
              onClick={startCamera}
              className="flex min-h-12 items-center gap-2 rounded-xl bg-teal-700 px-4 py-2.5 text-sm font-semibold text-white hover:bg-teal-800"
            >
              <Camera className="h-4 w-4" aria-hidden="true" />
              <span>{t.scannerCameraStart}</span>
            </button>
          ) : (
            <button
              type="button"
              onClick={captureFromCamera}
              disabled={pages.length >= APP_CONSTANTS.MAX_SCAN_PAGES}
              className="flex min-h-12 items-center gap-2 rounded-xl bg-teal-700 px-4 py-2.5 text-sm font-semibold text-white hover:bg-teal-800 disabled:opacity-50"
            >
              <Camera className="h-4 w-4" aria-hidden="true" />
              <span>{t.scannerCapturePage}</span>
            </button>
          )}

          <label className="flex min-h-12 cursor-pointer items-center gap-2 rounded-xl bg-[#E8F4F6] px-4 py-2.5 text-sm font-semibold text-teal-900 ring-1 ring-teal-900/10 hover:bg-[#DCEEEF]">
            <ImagePlus className="h-4 w-4" aria-hidden="true" />
            <span>{t.scannerImportGallery}</span>
            <input
              type="file"
              accept="image/*"
              multiple
              className="sr-only"
              onChange={(e) => {
                handleImportPhotos(e.target.files);
                e.target.value = '';
              }}
            />
          </label>
        </div>

        {cameraMessage && (
          <div className="rounded-xl bg-amber-50 px-4 py-3 text-xs font-medium text-amber-900 ring-1 ring-amber-200">
            {cameraMessage}
          </div>
        )}

        {cameraActive && (
          <div className="overflow-hidden rounded-2xl bg-slate-950 ring-1 ring-slate-300">
            <video
              ref={videoRef}
              autoPlay
              playsInline
              muted
              className="max-h-80 w-full object-contain"
            />
          </div>
        )}

        {/* Filter chips */}
        <div className="space-y-2">
          <div className="text-xs font-semibold text-slate-600">{t.scannerFilterLabel}</div>
          <div className="flex flex-wrap gap-2">
            {(
              [
                ['original', t.filterOriginal],
                ['clean', t.filterClean],
                ['grayscale', t.filterGrayscale],
              ] as [ScanFilter, string][]
            ).map(([key, label]) => (
              <button
                key={key}
                type="button"
                onClick={() => handleFilterChange(key)}
                className={`min-h-11 rounded-xl px-3.5 py-2 text-xs font-semibold transition ${
                  filter === key
                    ? 'bg-teal-700 text-white'
                    : 'bg-white text-slate-700 ring-1 ring-slate-300 hover:bg-slate-50'
                }`}
              >
                {label}
              </button>
            ))}
          </div>
        </div>

        {pages.length === 0 ? (
          <div className="rounded-2xl bg-slate-100/80 px-6 py-12 text-center ring-1 ring-slate-200">
            <Camera className="mx-auto h-10 w-10 text-teal-700" aria-hidden="true" />
            <p className="mt-3 text-sm text-slate-600">{t.scannerEmpty}</p>
          </div>
        ) : (
          <div className="grid gap-4 sm:grid-cols-2">
            {pages.map((page, index) => (
              <div
                key={page.id}
                className="overflow-hidden rounded-2xl bg-white p-3 shadow-2xs ring-1 ring-slate-200"
              >
                <div className="mb-2 flex items-center justify-between">
                  <span className="text-xs font-semibold text-teal-700">
                    {t.pageOf(index + 1, pages.length)}
                  </span>
                  <button
                    type="button"
                    aria-label={`Remove page ${index + 1}`}
                    onClick={() =>
                      setPages((prev) => prev.filter((item) => item.id !== page.id))
                    }
                    className="flex min-h-10 min-w-10 items-center justify-center rounded-lg text-red-600 hover:bg-red-50"
                  >
                    <Trash2 className="h-4 w-4" aria-hidden="true" />
                  </button>
                </div>
                <img
                  src={page.filteredDataUrl}
                  alt={`Scanned page ${index + 1}`}
                  className="max-h-72 w-full rounded-xl bg-slate-50 object-contain"
                />
              </div>
            ))}
          </div>
        )}
      </main>
    </div>
  );
}
