import { useState } from 'react';
import {
  ArrowDown,
  ArrowLeft,
  ArrowUp,
  FilePlus2,
  ImagePlus,
  Loader2,
  X,
} from 'lucide-react';
import { APP_CONSTANTS } from '../lib/constants';
import { fileToNormalizedDataUrl } from '../lib/canvasUtils';
import type { AppLocale } from '../lib/i18n';
import { STRINGS } from '../lib/i18n';
import { buildPdfFromDataUrls, type PdfMargin, type PdfPageSize } from '../lib/pdfBuilder';
import { formatTimestampName, type AuraScanDocument } from '../lib/storage';

interface PickedImage {
  id: string;
  dataUrl: string;
}

interface ImagesToPdfViewProps {
  locale: AppLocale;
  onBack: () => void;
  onPdfCreated: (doc: AuraScanDocument) => Promise<void>;
  onError: (msg: string) => void;
}

export function ImagesToPdfView({
  locale,
  onBack,
  onPdfCreated,
  onError,
}: ImagesToPdfViewProps) {
  const t = STRINGS[locale];
  const [images, setImages] = useState<PickedImage[]>([]);
  const [pageSize, setPageSize] = useState<PdfPageSize>('A4');
  const [margin, setMargin] = useState<PdfMargin>('SMALL');
  const [isBuilding, setIsBuilding] = useState(false);

  const handleFilesSelected = async (files: FileList | null) => {
    if (!files || files.length === 0) return;
    const remaining = APP_CONSTANTS.MAX_SCAN_PAGES - images.length;
    if (remaining <= 0) {
      onError(t.scannerPagesLimit(APP_CONSTANTS.MAX_SCAN_PAGES));
      return;
    }
    const chosen = Array.from(files).slice(0, remaining);
    try {
      const loaded: PickedImage[] = [];
      for (const file of chosen) {
        const { dataUrl } = await fileToNormalizedDataUrl(file, 1600);
        loaded.push({ id: crypto.randomUUID(), dataUrl });
      }
      setImages((prev) => [...prev, ...loaded]);
    } catch (err) {
      onError((err as Error).message || 'Failed to read images');
    }
  };

  const moveItem = (index: number, delta: number) => {
    const target = index + delta;
    if (target < 0 || target >= images.length) return;
    setImages((prev) => {
      const copy = [...prev];
      const [item] = copy.splice(index, 1);
      copy.splice(target, 0, item);
      return copy;
    });
  };

  const handleCreatePdf = async () => {
    if (images.length === 0 || isBuilding) return;
    setIsBuilding(true);
    try {
      const pageUrls = images.map((img) => img.dataUrl);
      const pdfBlob = await buildPdfFromDataUrls(pageUrls, pageSize, margin);
      const now = Date.now();
      const doc: AuraScanDocument = {
        id: crypto.randomUUID(),
        name: formatTimestampName('PDF', now),
        createdAt: now,
        pageCount: pageUrls.length,
        pageDataUrls: pageUrls,
        pdfBlob,
      };
      await onPdfCreated(doc);
    } catch (err) {
      onError((err as Error).message || 'Could not create PDF');
      setIsBuilding(false);
    }
  };

  return (
    <div className="flex min-h-screen flex-col bg-[#F6FAFB] text-slate-900">
      <header className="sticky top-0 z-20 border-b border-slate-200 bg-white">
        <div className="mx-auto flex max-w-3xl items-center gap-2 px-4 py-3">
          <button
            type="button"
            onClick={onBack}
            aria-label={t.back}
            className="flex min-h-12 min-w-12 items-center justify-center rounded-xl text-slate-700 hover:bg-slate-100"
          >
            <ArrowLeft className="h-5 w-5" aria-hidden="true" />
          </button>
          <h1 className="text-lg font-semibold text-slate-900">{t.imagesToPdfTitle}</h1>
        </div>
      </header>

      <main className="mx-auto w-full max-w-3xl flex-1 space-y-5 px-4 py-5 pb-24">
        <label className="flex min-h-12 w-full cursor-pointer items-center justify-center gap-2 rounded-2xl bg-[#E8F4F6] px-5 py-3 text-sm font-semibold text-teal-900 ring-1 ring-teal-900/10 hover:bg-[#DCEEEF]">
          <ImagePlus className="h-4 w-4" aria-hidden="true" />
          <span>{images.length === 0 ? t.pickImages : t.addMore}</span>
          <input
            type="file"
            accept="image/*"
            multiple
            className="sr-only"
            onChange={(e) => {
              handleFilesSelected(e.target.files);
              e.target.value = '';
            }}
          />
        </label>

        {images.length === 0 ? (
          <div className="rounded-2xl bg-slate-100/80 px-6 py-10 text-center ring-1 ring-slate-200">
            <ImagePlus className="mx-auto h-10 w-10 text-teal-700" aria-hidden="true" />
            <p className="mt-3 text-base font-semibold text-slate-900">{t.imagesEmptyTitle}</p>
            <p className="mx-auto mt-1 max-w-sm text-sm text-slate-600">{t.imagesEmptySubtitle}</p>
          </div>
        ) : (
          <>
            <div className="space-y-4 rounded-2xl bg-white p-4 ring-1 ring-slate-200">
              <div className="space-y-2">
                <div className="text-xs font-semibold text-slate-700">{t.pageSizeLabel}</div>
                <div className="flex flex-wrap gap-2">
                  {(
                    [
                      ['A4', t.pageSizeA4],
                      ['FIT_TO_IMAGE', t.pageSizeFit],
                    ] as [PdfPageSize, string][]
                  ).map(([val, label]) => (
                    <button
                      key={val}
                      type="button"
                      onClick={() => setPageSize(val)}
                      className={`min-h-11 rounded-xl px-4 py-2 text-xs font-semibold ${
                        pageSize === val
                          ? 'bg-teal-700 text-white'
                          : 'bg-slate-50 text-slate-700 ring-1 ring-slate-300'
                      }`}
                    >
                      {label}
                    </button>
                  ))}
                </div>
              </div>

              <div className="space-y-2">
                <div className="text-xs font-semibold text-slate-700">{t.marginLabel}</div>
                <div className="flex flex-wrap gap-2">
                  {(
                    [
                      ['NONE', t.marginNone],
                      ['SMALL', t.marginSmall],
                      ['MEDIUM', t.marginMedium],
                    ] as [PdfMargin, string][]
                  ).map(([val, label]) => (
                    <button
                      key={val}
                      type="button"
                      onClick={() => setMargin(val)}
                      className={`min-h-11 rounded-xl px-4 py-2 text-xs font-semibold ${
                        margin === val
                          ? 'bg-teal-700 text-white'
                          : 'bg-slate-50 text-slate-700 ring-1 ring-slate-300'
                      }`}
                    >
                      {label}
                    </button>
                  ))}
                </div>
              </div>
            </div>

            <ul className="space-y-2.5">
              {images.map((item, idx) => (
                <li
                  key={item.id}
                  className="flex items-center gap-3 rounded-2xl bg-white p-3 ring-1 ring-slate-200"
                >
                  <img
                    src={item.dataUrl}
                    alt={`Selected image ${idx + 1}`}
                    className="h-16 w-16 shrink-0 rounded-xl bg-slate-100 object-cover"
                  />
                  <span className="flex-1 text-sm font-semibold text-slate-800">
                    {t.pageOf(idx + 1, images.length)}
                  </span>
                  <button
                    type="button"
                    aria-label={`Move image ${idx + 1} up`}
                    disabled={idx === 0}
                    onClick={() => moveItem(idx, -1)}
                    className="flex min-h-12 min-w-12 items-center justify-center rounded-xl text-slate-700 hover:bg-slate-100 disabled:opacity-35"
                  >
                    <ArrowUp className="h-5 w-5" aria-hidden="true" />
                  </button>
                  <button
                    type="button"
                    aria-label={`Move image ${idx + 1} down`}
                    disabled={idx === images.length - 1}
                    onClick={() => moveItem(idx, 1)}
                    className="flex min-h-12 min-w-12 items-center justify-center rounded-xl text-slate-700 hover:bg-slate-100 disabled:opacity-35"
                  >
                    <ArrowDown className="h-5 w-5" aria-hidden="true" />
                  </button>
                  <button
                    type="button"
                    aria-label={`Remove image ${idx + 1}`}
                    onClick={() =>
                      setImages((prev) => prev.filter((entry) => entry.id !== item.id))
                    }
                    className="flex min-h-12 min-w-12 items-center justify-center rounded-xl text-red-600 hover:bg-red-50"
                  >
                    <X className="h-5 w-5" aria-hidden="true" />
                  </button>
                </li>
              ))}
            </ul>
          </>
        )}
      </main>

      {images.length > 0 && (
        <div className="sticky bottom-0 z-20 border-t border-slate-200 bg-white/95 p-4 backdrop-blur-xs">
          <div className="mx-auto max-w-3xl">
            <button
              type="button"
              onClick={handleCreatePdf}
              disabled={isBuilding}
              className="flex min-h-12 w-full items-center justify-center gap-2 rounded-2xl bg-teal-700 px-5 py-3 text-sm font-semibold text-white hover:bg-teal-800 disabled:opacity-60"
            >
              {isBuilding ? (
                <Loader2 className="h-4 w-4 animate-spin" aria-hidden="true" />
              ) : (
                <FilePlus2 className="h-4 w-4" aria-hidden="true" />
              )}
              <span>{isBuilding ? t.creatingPdf : t.createPdf}</span>
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
