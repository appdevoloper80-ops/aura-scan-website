import { useState } from 'react';
import {
  ArrowLeft,
  Copy,
  Download,
  ImagePlus,
  Loader2,
  LockOpen,
  Play,
  Share2,
  Type,
} from 'lucide-react';
import { APP_CONSTANTS, isProUnlocked } from '../lib/constants';
import { fileToNormalizedDataUrl } from '../lib/canvasUtils';
import type { AppLocale } from '../lib/i18n';
import { STRINGS } from '../lib/i18n';
import { runBrowserOcr, type OcrLanguage } from '../lib/ocrEngine';
import {
  downloadBlob,
  sanitizeFileName,
  shareOrDownloadFile,
  type AuraScanDocument,
} from '../lib/storage';
import { ProUpsellModal } from './Dialogs';

interface OcrViewProps {
  locale: AppLocale;
  document?: AuraScanDocument;
  onBack: () => void;
  onNotify: (msg: string) => void;
}

export function OcrView({ locale, document, onBack, onNotify }: OcrViewProps) {
  const t = STRINGS[locale];
  const isStandalone = !document;

  const [singleImageUrl, setSingleImageUrl] = useState<string | null>(null);
  const [language, setLanguage] = useState<OcrLanguage>('LATIN');
  const [isProcessing, setIsProcessing] = useState(false);
  const [progress, setProgress] = useState({ current: 0, total: 0 });
  const [hasRun, setHasRun] = useState(false);
  const [extractedText, setExtractedText] = useState('');
  const [showUnlockCard, setShowUnlockCard] = useState(false);
  const [showProModal, setShowProModal] = useState(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  const availablePages = document
    ? document.pageDataUrls
    : singleImageUrl
    ? [singleImageUrl]
    : [];

  const handlePickImage = async (file: File | undefined) => {
    if (!file) return;
    try {
      const { dataUrl } = await fileToNormalizedDataUrl(file, 1800);
      setSingleImageUrl(dataUrl);
      setExtractedText('');
      setHasRun(false);
      setShowUnlockCard(false);
      setErrorMsg(null);
    } catch (err) {
      setErrorMsg((err as Error).message || 'Could not load image');
    }
  };

  const handleRunOcr = async () => {
    if (availablePages.length === 0 || isProcessing) return;
    const pro = isProUnlocked();
    const limitExceeded = !pro && availablePages.length > APP_CONSTANTS.FREE_OCR_PAGE_LIMIT;
    const pagesToProcess = limitExceeded
      ? availablePages.slice(0, APP_CONSTANTS.FREE_OCR_PAGE_LIMIT)
      : availablePages;

    setIsProcessing(true);
    setErrorMsg(null);
    setProgress({ current: 1, total: pagesToProcess.length });

    try {
      const text = await runBrowserOcr(pagesToProcess, language, (curr, total) => {
        setProgress({ current: curr, total });
      });
      setExtractedText(text);
      setHasRun(true);
      setShowUnlockCard(limitExceeded);
    } catch (err) {
      setErrorMsg((err as Error).message || 'OCR recognition failed');
    } finally {
      setIsProcessing(false);
    }
  };

  const txtFileName = sanitizeFileName(document?.name || 'AuraScan_OCR', 'txt');

  const handleCopy = async () => {
    try {
      await navigator.clipboard.writeText(extractedText);
      onNotify(t.ocrCopied);
    } catch {
      onNotify('Copy failed');
    }
  };

  const handleShareTxt = async () => {
    const blob = new Blob([extractedText], { type: 'text/plain;charset=utf-8' });
    const mode = await shareOrDownloadFile(blob, txtFileName, 'text/plain', t.ocrShareTxt);
    onNotify(mode === 'shared' ? t.shareStarted : t.shareFallbackDownload);
  };

  const handleSaveTxt = () => {
    const blob = new Blob([extractedText], { type: 'text/plain;charset=utf-8' });
    downloadBlob(blob, txtFileName);
    onNotify(t.savedToDevice);
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
          <h1 className="text-lg font-semibold text-slate-900">{t.ocrTitle}</h1>
        </div>
      </header>

      <main className="mx-auto w-full max-w-3xl flex-1 space-y-5 px-4 py-5">
        {isStandalone && (
          <div className="space-y-3">
            <label className="inline-flex min-h-12 cursor-pointer items-center gap-2 rounded-xl bg-[#E8F4F6] px-4 py-2.5 text-sm font-semibold text-teal-900 ring-1 ring-teal-900/10 hover:bg-[#DCEEEF]">
              <ImagePlus className="h-4 w-4" aria-hidden="true" />
              <span>{singleImageUrl ? t.ocrChangeImage : t.ocrPickImage}</span>
              <input
                type="file"
                accept="image/*"
                className="sr-only"
                onChange={(e) => {
                  handlePickImage(e.target.files?.[0]);
                  e.target.value = '';
                }}
              />
            </label>
            {singleImageUrl && (
              <div className="overflow-hidden rounded-2xl bg-white p-2 ring-1 ring-slate-200">
                <img
                  src={singleImageUrl}
                  alt="Selected image for OCR"
                  className="mx-auto max-h-52 rounded-xl object-contain"
                />
              </div>
            )}
          </div>
        )}

        <div className="space-y-2">
          <div className="text-xs font-semibold text-slate-700">{t.ocrLanguageLabel}</div>
          <div className="flex flex-wrap gap-2">
            <button
              type="button"
              onClick={() => setLanguage('LATIN')}
              className={`min-h-12 rounded-xl px-4 py-2 text-sm font-semibold transition ${
                language === 'LATIN'
                  ? 'bg-teal-700 text-white'
                  : 'bg-white text-slate-700 ring-1 ring-slate-300 hover:bg-slate-50'
              }`}
            >
              {t.ocrLatin}
            </button>
            <button
              type="button"
              onClick={() => setLanguage('HINDI_DEVANAGARI')}
              className={`min-h-12 rounded-xl px-4 py-2 text-sm font-semibold transition ${
                language === 'HINDI_DEVANAGARI'
                  ? 'bg-teal-700 text-white'
                  : 'bg-white text-slate-700 ring-1 ring-slate-300 hover:bg-slate-50'
              }`}
            >
              {t.ocrHindi}
            </button>
          </div>
        </div>

        <button
          type="button"
          onClick={handleRunOcr}
          disabled={availablePages.length === 0 || isProcessing}
          className="flex min-h-12 w-full items-center justify-center gap-2 rounded-2xl bg-teal-700 px-5 py-3 text-sm font-semibold text-white transition hover:bg-teal-800 disabled:opacity-50"
        >
          {isProcessing ? (
            <Loader2 className="h-4 w-4 animate-spin" aria-hidden="true" />
          ) : (
            <Play className="h-4 w-4" aria-hidden="true" />
          )}
          <span>{hasRun ? t.ocrRerun : t.ocrRun}</span>
        </button>

        {isProcessing && (
          <div className="space-y-2 rounded-2xl bg-white p-4 ring-1 ring-slate-200">
            <div className="h-2 w-full overflow-hidden rounded-full bg-slate-200">
              <div
                className="h-full rounded-full bg-teal-700 transition-all"
                style={{
                  width: `${
                    progress.total > 0 ? Math.round((progress.current / progress.total) * 100) : 15
                  }%`,
                }}
              />
            </div>
            <p className="text-xs font-medium text-slate-600">
              {t.ocrProcessing(Math.max(1, progress.current), Math.max(1, progress.total))}
            </p>
          </div>
        )}

        {errorMsg && (
          <div className="rounded-2xl bg-red-50 p-4 text-sm text-red-700 ring-1 ring-red-200">
            {errorMsg}
          </div>
        )}

        {showUnlockCard && (
          <div className="rounded-2xl bg-[#D6F3EC] p-4 text-teal-950 ring-1 ring-teal-900/15">
            <div className="flex items-center gap-2 font-semibold">
              <LockOpen className="h-5 w-5" aria-hidden="true" />
              <span>{t.ocrUnlockTitle}</span>
            </div>
            <p className="mt-1.5 text-sm">
              {t.ocrUnlockDesc(APP_CONSTANTS.FREE_OCR_PAGE_LIMIT, availablePages.length)}
            </p>
            <button
              type="button"
              onClick={() => setShowProModal(true)}
              className="mt-3 min-h-11 rounded-xl bg-teal-800 px-4 py-2 text-xs font-semibold text-white hover:bg-teal-900"
            >
              {t.unlockPro}
            </button>
          </div>
        )}

        {availablePages.length === 0 && isStandalone && !isProcessing ? (
          <div className="rounded-2xl bg-slate-100/80 px-6 py-10 text-center ring-1 ring-slate-200">
            <Type className="mx-auto h-10 w-10 text-teal-700" aria-hidden="true" />
            <p className="mt-3 text-sm text-slate-600">{t.ocrEmptyPrompt}</p>
          </div>
        ) : (
          hasRun &&
          !isProcessing && (
            <div className="space-y-3">
              {!extractedText ? (
                <div className="rounded-2xl bg-slate-100 px-6 py-8 text-center text-sm text-slate-600">
                  {t.ocrNoText}
                </div>
              ) : (
                <>
                  <div className="flex flex-wrap gap-2">
                    <button
                      type="button"
                      onClick={handleCopy}
                      className="flex min-h-12 items-center gap-2 rounded-xl bg-[#E8F4F6] px-4 py-2 text-sm font-semibold text-teal-900 hover:bg-[#DCEEEF]"
                    >
                      <Copy className="h-4 w-4" aria-hidden="true" />
                      <span>{t.ocrCopy}</span>
                    </button>
                    <button
                      type="button"
                      onClick={handleShareTxt}
                      className="flex min-h-12 items-center gap-2 rounded-xl border border-slate-300 bg-white px-4 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-50"
                    >
                      <Share2 className="h-4 w-4" aria-hidden="true" />
                      <span>{t.ocrShareTxt}</span>
                    </button>
                    <button
                      type="button"
                      onClick={handleSaveTxt}
                      className="flex min-h-12 items-center gap-2 rounded-xl border border-slate-300 bg-white px-4 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-50"
                    >
                      <Download className="h-4 w-4" aria-hidden="true" />
                      <span>{t.ocrSaveTxt}</span>
                    </button>
                  </div>
                  <div className="select-text whitespace-pre-wrap rounded-2xl bg-white p-4 text-sm leading-relaxed text-slate-900 ring-1 ring-slate-200">
                    {extractedText}
                  </div>
                </>
              )}
            </div>
          )
        )}
      </main>

      {showProModal && (
        <ProUpsellModal
          locale={locale}
          title={t.ocrUnlockTitle}
          message={t.proComingSoon}
          onDismiss={() => setShowProModal(false)}
        />
      )}
    </div>
  );
}
