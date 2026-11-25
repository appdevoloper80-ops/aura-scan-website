import { useState } from 'react';
import {
  FileText,
  Images,
  MoreVertical,
  ScanLine,
  Settings,
  SlidersHorizontal,
  Type,
} from 'lucide-react';
import { APP_CONSTANTS } from '../lib/constants';
import type { AppLocale } from '../lib/i18n';
import { STRINGS } from '../lib/i18n';
import type { AuraScanDocument } from '../lib/storage';

interface HomeViewProps {
  locale: AppLocale;
  onToggleLocale: () => void;
  documents: AuraScanDocument[];
  onStartScan: () => void;
  onOpenImagesToPdf: () => void;
  onOpenResizer: () => void;
  onOpenOcr: () => void;
  onOpenSettings: () => void;
  onOpenDocument: (docId: string) => void;
  onRenameDocument: (doc: AuraScanDocument) => void;
  onShareDocument: (doc: AuraScanDocument) => void;
  onSaveDocumentToDevice: (doc: AuraScanDocument) => void;
  onDeleteDocument: (doc: AuraScanDocument) => void;
}

export function HomeView({
  locale,
  onToggleLocale,
  documents,
  onStartScan,
  onOpenImagesToPdf,
  onOpenResizer,
  onOpenOcr,
  onOpenSettings,
  onOpenDocument,
  onRenameDocument,
  onShareDocument,
  onSaveDocumentToDevice,
  onDeleteDocument,
}: HomeViewProps) {
  const t = STRINGS[locale];
  const [openMenuDocId, setOpenMenuDocId] = useState<string | null>(null);

  return (
    <div className="flex min-h-screen flex-col bg-[#F6FAFB] text-slate-900">
      <header className="sticky top-0 z-20 border-b border-slate-200/80 bg-white/95 backdrop-blur-xs">
        <div className="mx-auto flex max-w-3xl items-center justify-between px-4 py-3">
          <div className="flex items-center gap-2.5">
            <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-teal-700 text-white">
              <ScanLine className="h-5 w-5" aria-hidden="true" />
            </div>
            <div>
              <h1 className="text-lg font-semibold leading-tight text-slate-900">{t.appName}</h1>
              <p className="text-xs text-slate-500">{t.appSubtitle}</p>
            </div>
          </div>
          <div className="flex items-center gap-1.5">
            <button
              type="button"
              onClick={onToggleLocale}
              className="min-h-12 rounded-xl px-3 text-xs font-semibold text-teal-800 hover:bg-teal-50"
              aria-label={locale === 'en' ? 'Switch language to Hindi' : 'Switch language to English'}
            >
              {locale === 'en' ? 'हिंदी' : 'EN'}
            </button>
            <button
              type="button"
              onClick={onOpenSettings}
              aria-label={t.settings}
              className="flex min-h-12 min-w-12 items-center justify-center rounded-xl text-slate-700 hover:bg-slate-100"
            >
              <Settings className="h-5 w-5" aria-hidden="true" />
            </button>
          </div>
        </div>
      </header>

      <main className="mx-auto w-full max-w-3xl flex-1 space-y-6 px-4 py-5">
        {/* Large primary Scan CTA */}
        <button
          type="button"
          onClick={onStartScan}
          className="flex min-h-[76px] w-full items-center gap-4 rounded-2xl bg-teal-700 px-5 py-4 text-left text-white shadow-sm transition hover:bg-teal-800 focus:outline-none focus:ring-2 focus:ring-teal-600 focus:ring-offset-2"
        >
          <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-xl bg-white/15">
            <ScanLine className="h-6 w-6" aria-hidden="true" />
          </div>
          <div className="min-w-0 flex-1">
            <div className="text-base font-semibold">{t.scanDocument}</div>
            <div className="text-xs text-teal-50/90">{t.scanSubtitle}</div>
          </div>
        </button>

        {/* Tools row with 3 cards */}
        <section aria-labelledby="tools-heading" className="space-y-2.5">
          <h2 id="tools-heading" className="text-sm font-semibold text-slate-800">
            {t.toolsSection}
          </h2>
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-3">
            <button
              type="button"
              onClick={onOpenImagesToPdf}
              className="flex min-h-[104px] flex-col items-start justify-between rounded-2xl bg-[#E8F4F6] p-4 text-left ring-1 ring-teal-900/10 transition hover:bg-[#DCEEEF]"
            >
              <span className="flex h-10 w-10 items-center justify-center rounded-xl bg-teal-700/10 text-teal-800">
                <Images className="h-5 w-5" aria-hidden="true" />
              </span>
              <span className="mt-3 text-sm font-semibold text-slate-900">{t.toolImagesToPdf}</span>
            </button>

            <button
              type="button"
              onClick={onOpenResizer}
              className="flex min-h-[104px] flex-col items-start justify-between rounded-2xl bg-[#E8F4F6] p-4 text-left ring-1 ring-teal-900/10 transition hover:bg-[#DCEEEF]"
            >
              <span className="flex h-10 w-10 items-center justify-center rounded-xl bg-teal-700/10 text-teal-800">
                <SlidersHorizontal className="h-5 w-5" aria-hidden="true" />
              </span>
              <span className="mt-3 text-sm font-semibold text-slate-900">{t.toolResizer}</span>
            </button>

            <button
              type="button"
              onClick={onOpenOcr}
              className="flex min-h-[104px] flex-col items-start justify-between rounded-2xl bg-[#E8F4F6] p-4 text-left ring-1 ring-teal-900/10 transition hover:bg-[#DCEEEF]"
            >
              <span className="flex h-10 w-10 items-center justify-center rounded-xl bg-teal-700/10 text-teal-800">
                <Type className="h-5 w-5" aria-hidden="true" />
              </span>
              <span className="mt-3 text-sm font-semibold text-slate-900">{t.toolOcr}</span>
            </button>
          </div>
        </section>

        {/* Recent documents */}
        <section aria-labelledby="recent-docs-heading" className="space-y-3">
          <h2 id="recent-docs-heading" className="text-sm font-semibold text-slate-800">
            {t.recentDocuments}
          </h2>

          {documents.length === 0 ? (
            <div className="rounded-2xl bg-slate-100/80 px-6 py-10 text-center ring-1 ring-slate-200/80">
              <FileText className="mx-auto h-10 w-10 text-teal-700" aria-hidden="true" />
              <p className="mt-3 text-base font-semibold text-slate-900">{t.emptyDocsTitle}</p>
              <p className="mx-auto mt-1 max-w-sm text-sm text-slate-600">{t.emptyDocsSubtitle}</p>
            </div>
          ) : (
            <ul className="space-y-2.5">
              {documents.map((doc) => {
                const thumb = doc.pageDataUrls[0];
                const pageLabel =
                  doc.pageCount === 1 ? t.pageSingular : t.pagePlural(doc.pageCount);
                const menuOpen = openMenuDocId === doc.id;
                return (
                  <li
                    key={doc.id}
                    className="relative flex items-center gap-3 rounded-2xl bg-white p-3 shadow-2xs ring-1 ring-slate-200/80"
                  >
                    <button
                      type="button"
                      onClick={() => onOpenDocument(doc.id)}
                      className="flex min-w-0 flex-1 items-center gap-3 text-left"
                    >
                      <div className="h-[72px] w-[54px] shrink-0 overflow-hidden rounded-xl bg-slate-100 ring-1 ring-slate-200">
                        {thumb ? (
                          <img
                            src={thumb}
                            alt={`Preview of ${doc.name}`}
                            className="h-full w-full object-cover"
                          />
                        ) : (
                          <div className="flex h-full w-full items-center justify-center text-teal-700">
                            <FileText className="h-6 w-6" aria-hidden="true" />
                          </div>
                        )}
                      </div>
                      <div className="min-w-0 flex-1">
                        <div className="truncate text-sm font-semibold text-slate-900">
                          {doc.name}
                        </div>
                        <div className="mt-0.5 text-xs text-slate-500">
                          {new Date(doc.createdAt).toLocaleString()}
                        </div>
                        <div className="mt-1 text-xs font-medium text-teal-700">{pageLabel}</div>
                      </div>
                    </button>

                    <div className="relative">
                      <button
                        type="button"
                        aria-label={`More options for ${doc.name}`}
                        aria-expanded={menuOpen}
                        onClick={() => setOpenMenuDocId(menuOpen ? null : doc.id)}
                        className="flex min-h-12 min-w-12 items-center justify-center rounded-xl text-slate-600 hover:bg-slate-100"
                      >
                        <MoreVertical className="h-5 w-5" aria-hidden="true" />
                      </button>

                      {menuOpen && (
                        <div
                          role="menu"
                          className="absolute right-0 top-12 z-30 w-44 rounded-2xl bg-white py-1.5 shadow-lg ring-1 ring-slate-200"
                        >
                          <button
                            type="button"
                            role="menuitem"
                            onClick={() => {
                              setOpenMenuDocId(null);
                              onRenameDocument(doc);
                            }}
                            className="block w-full px-4 py-2.5 text-left text-sm text-slate-800 hover:bg-slate-50"
                          >
                            {t.rename}
                          </button>
                          <button
                            type="button"
                            role="menuitem"
                            onClick={() => {
                              setOpenMenuDocId(null);
                              onShareDocument(doc);
                            }}
                            className="block w-full px-4 py-2.5 text-left text-sm text-slate-800 hover:bg-slate-50"
                          >
                            {t.share}
                          </button>
                          <button
                            type="button"
                            role="menuitem"
                            onClick={() => {
                              setOpenMenuDocId(null);
                              onSaveDocumentToDevice(doc);
                            }}
                            className="block w-full px-4 py-2.5 text-left text-sm text-slate-800 hover:bg-slate-50"
                          >
                            {t.saveToDevice}
                          </button>
                          <button
                            type="button"
                            role="menuitem"
                            onClick={() => {
                              setOpenMenuDocId(null);
                              onDeleteDocument(doc);
                            }}
                            className="block w-full px-4 py-2.5 text-left text-sm font-medium text-red-600 hover:bg-red-50"
                          >
                            {t.delete}
                          </button>
                        </div>
                      )}
                    </div>
                  </li>
                );
              })}
            </ul>
          )}
        </section>
      </main>

      {/* Reserved fixed-height adaptive banner ad slot (empty for now) */}
      <footer
        aria-hidden="true"
        style={{ height: `${APP_CONSTANTS.AD_BANNER_HEIGHT_PX}px` }}
        className="w-full shrink-0 border-t border-slate-200/60 bg-white"
      />
    </div>
  );
}
