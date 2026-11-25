import { ArrowLeft, Download, Edit3, FileText, Share2, Trash2, Type } from 'lucide-react';
import type { AppLocale } from '../lib/i18n';
import { STRINGS } from '../lib/i18n';
import type { AuraScanDocument } from '../lib/storage';

interface DocumentViewProps {
  locale: AppLocale;
  document: AuraScanDocument | undefined;
  onBack: () => void;
  onSharePdf: (doc: AuraScanDocument) => void;
  onSavePdf: (doc: AuraScanDocument) => void;
  onRename: (doc: AuraScanDocument) => void;
  onDelete: (doc: AuraScanDocument) => void;
  onOpenOcr: (doc: AuraScanDocument) => void;
}

export function DocumentView({
  locale,
  document,
  onBack,
  onSharePdf,
  onSavePdf,
  onRename,
  onDelete,
  onOpenOcr,
}: DocumentViewProps) {
  const t = STRINGS[locale];

  return (
    <div className="flex min-h-screen flex-col bg-[#F6FAFB] text-slate-900">
      <header className="sticky top-0 z-20 border-b border-slate-200 bg-white">
        <div className="mx-auto flex max-w-3xl items-center justify-between px-4 py-3">
          <div className="flex min-w-0 items-center gap-2">
            <button
              type="button"
              onClick={onBack}
              aria-label={t.back}
              className="flex min-h-12 min-w-12 items-center justify-center rounded-xl text-slate-700 hover:bg-slate-100"
            >
              <ArrowLeft className="h-5 w-5" aria-hidden="true" />
            </button>
            <h1 className="truncate text-lg font-semibold text-slate-900">
              {document ? document.name : t.docNotFound}
            </h1>
          </div>
          {document && (
            <div className="flex items-center gap-1">
              <button
                type="button"
                onClick={() => onRename(document)}
                aria-label={t.rename}
                className="flex min-h-12 min-w-12 items-center justify-center rounded-xl text-slate-700 hover:bg-slate-100"
              >
                <Edit3 className="h-5 w-5" aria-hidden="true" />
              </button>
              <button
                type="button"
                onClick={() => onDelete(document)}
                aria-label={t.delete}
                className="flex min-h-12 min-w-12 items-center justify-center rounded-xl text-red-600 hover:bg-red-50"
              >
                <Trash2 className="h-5 w-5" aria-hidden="true" />
              </button>
            </div>
          )}
        </div>
      </header>

      {!document ? (
        <main className="mx-auto flex w-full max-w-3xl flex-1 items-center justify-center p-6">
          <div className="rounded-2xl bg-slate-100 px-6 py-10 text-center">
            <FileText className="mx-auto h-10 w-10 text-teal-700" aria-hidden="true" />
            <p className="mt-3 text-base font-semibold text-slate-900">{t.docNotFound}</p>
          </div>
        </main>
      ) : (
        <>
          <div className="border-b border-slate-200 bg-white/70">
            <div className="mx-auto flex max-w-3xl flex-wrap gap-2 px-4 py-2.5">
              <button
                type="button"
                onClick={() => onOpenOcr(document)}
                className="flex min-h-11 items-center gap-1.5 rounded-xl bg-teal-700 px-3.5 py-2 text-xs font-semibold text-white hover:bg-teal-800"
              >
                <Type className="h-4 w-4" aria-hidden="true" />
                <span>{t.toolOcr}</span>
              </button>
              <button
                type="button"
                onClick={() => onSharePdf(document)}
                className="flex min-h-11 items-center gap-1.5 rounded-xl bg-[#E8F4F6] px-3.5 py-2 text-xs font-semibold text-teal-900 hover:bg-[#DCEEEF]"
              >
                <Share2 className="h-4 w-4" aria-hidden="true" />
                <span>{t.sharePdf}</span>
              </button>
              <button
                type="button"
                onClick={() => onSavePdf(document)}
                className="flex min-h-11 items-center gap-1.5 rounded-xl border border-slate-300 bg-white px-3.5 py-2 text-xs font-semibold text-slate-700 hover:bg-slate-50"
              >
                <Download className="h-4 w-4" aria-hidden="true" />
                <span>{t.savePdfToDevice}</span>
              </button>
              <button
                type="button"
                onClick={() => onRename(document)}
                className="flex min-h-11 items-center gap-1.5 rounded-xl border border-slate-300 bg-white px-3.5 py-2 text-xs font-semibold text-slate-700 hover:bg-slate-50"
              >
                <Edit3 className="h-4 w-4" aria-hidden="true" />
                <span>{t.rename}</span>
              </button>
              <button
                type="button"
                onClick={() => onDelete(document)}
                className="flex min-h-11 items-center gap-1.5 rounded-xl border border-red-200 bg-white px-3.5 py-2 text-xs font-semibold text-red-600 hover:bg-red-50"
              >
                <Trash2 className="h-4 w-4" aria-hidden="true" />
                <span>{t.delete}</span>
              </button>
            </div>
          </div>

          <main className="mx-auto w-full max-w-3xl flex-1 space-y-4 px-4 py-5">
            {document.pageDataUrls.map((url, idx) => (
              <div
                key={`${document.id}-page-${idx}`}
                className="rounded-2xl bg-white p-3.5 shadow-2xs ring-1 ring-slate-200"
              >
                <div className="mb-2 text-xs font-semibold text-teal-700">
                  {t.pageOf(idx + 1, document.pageDataUrls.length)}
                </div>
                <img
                  src={url}
                  alt={`Document page ${idx + 1}`}
                  className="w-full rounded-xl bg-slate-50 object-contain"
                />
              </div>
            ))}
          </main>
        </>
      )}
    </div>
  );
}
