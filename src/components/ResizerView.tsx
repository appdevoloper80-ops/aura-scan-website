import { useState } from 'react';
import {
  ArrowLeft,
  Crop,
  Download,
  ImagePlus,
  Loader2,
  Lock,
  Share2,
  SlidersHorizontal,
} from 'lucide-react';
import {
  cropAndCompressJpeg,
  fileToNormalizedDataUrl,
  type CropRect,
} from '../lib/canvasUtils';
import { APP_CONSTANTS, isProUnlocked } from '../lib/constants';
import type { AppLocale } from '../lib/i18n';
import { STRINGS } from '../lib/i18n';
import {
  computeAspectPreservedDimension,
  optimizeToMaxBytesAsync,
  requiresProForTarget,
} from '../lib/imageSizeOptimizer';
import { downloadBlob, shareOrDownloadFile } from '../lib/storage';
import { ProUpsellModal } from './Dialogs';

type PresetKey = 'EXAM_PHOTO' | 'EXAM_SIGNATURE' | 'PASSPORT_PHOTO' | 'CUSTOM';

const PRESETS: Record<PresetKey, { w: number; h: number; kb: number; custom: boolean }> = {
  EXAM_PHOTO: { w: 200, h: 230, kb: 50, custom: false },
  EXAM_SIGNATURE: { w: 140, h: 60, kb: 20, custom: false },
  PASSPORT_PHOTO: { w: 413, h: 531, kb: 200, custom: false },
  CUSTOM: { w: 600, h: 600, kb: 100, custom: true },
};

interface ResizerOutputState {
  previewUrl: string;
  blob: Blob;
  sizeKb: string;
  width: number;
  height: number;
  quality: number;
  reachedTargetSize: boolean;
}

interface ResizerViewProps {
  locale: AppLocale;
  onBack: () => void;
  onNotify: (msg: string) => void;
}

export function ResizerView({ locale, onBack, onNotify }: ResizerViewProps) {
  const t = STRINGS[locale];

  const [sourceUrl, setSourceUrl] = useState<string | null>(null);
  const [sourceDims, setSourceDims] = useState({ w: 0, h: 0 });
  const [cropRect, setCropRect] = useState<CropRect>({ left: 0, top: 0, right: 1, bottom: 1 });
  const [isCropOpen, setIsCropOpen] = useState(false);

  const [preset, setPreset] = useState<PresetKey>('EXAM_PHOTO');
  const [widthInput, setWidthInput] = useState('200');
  const [heightInput, setHeightInput] = useState('230');
  const [maxKbInput, setMaxKbInput] = useState('50');
  const [keepAspect, setKeepAspect] = useState(true);

  const [isProcessing, setIsProcessing] = useState(false);
  const [output, setOutput] = useState<ResizerOutputState | null>(null);
  const [invalidInput, setInvalidInput] = useState(false);
  const [showProModal, setShowProModal] = useState(false);

  const handlePickImage = async (file: File | undefined) => {
    if (!file) return;
    try {
      const { dataUrl, width, height } = await fileToNormalizedDataUrl(file, 2048);
      setSourceUrl(dataUrl);
      setSourceDims({ w: width, h: height });
      setCropRect({ left: 0, top: 0, right: 1, bottom: 1 });
      setOutput(null);
      setInvalidInput(false);
    } catch {
      setInvalidInput(true);
    }
  };

  const handleSelectPreset = (key: PresetKey) => {
    if (key === 'CUSTOM' && !isProUnlocked()) {
      setShowProModal(true);
      return;
    }
    const p = PRESETS[key];
    setPreset(key);
    setWidthInput(String(p.w));
    setHeightInput(String(p.h));
    setMaxKbInput(String(p.kb));
    setInvalidInput(false);
  };

  const handleWidthChange = (raw: string) => {
    const clean = raw.replace(/\D/g, '');
    setWidthInput(clean);
    if (keepAspect) {
      const num = parseInt(clean, 10);
      const effW = Math.max(1, Math.round(sourceDims.w * (cropRect.right - cropRect.left)));
      const effH = Math.max(1, Math.round(sourceDims.h * (cropRect.bottom - cropRect.top)));
      if (num > 0 && effW > 0 && effH > 0) {
        setHeightInput(String(computeAspectPreservedDimension(num, effW, effH, true)));
      }
    }
  };

  const handleHeightChange = (raw: string) => {
    const clean = raw.replace(/\D/g, '');
    setHeightInput(clean);
    if (keepAspect) {
      const num = parseInt(clean, 10);
      const effW = Math.max(1, Math.round(sourceDims.w * (cropRect.right - cropRect.left)));
      const effH = Math.max(1, Math.round(sourceDims.h * (cropRect.bottom - cropRect.top)));
      if (num > 0 && effW > 0 && effH > 0) {
        setWidthInput(String(computeAspectPreservedDimension(num, effW, effH, false)));
      }
    }
  };

  const handleResize = async () => {
    if (!sourceUrl || isProcessing) return;
    const w = parseInt(widthInput, 10);
    const h = parseInt(heightInput, 10);
    const maxKb = parseInt(maxKbInput, 10);

    if (!w || w <= 0 || !h || h <= 0 || !maxKb || maxKb <= 0) {
      setInvalidInput(true);
      return;
    }

    if (
      requiresProForTarget(
        PRESETS[preset].custom,
        maxKb,
        isProUnlocked(),
        APP_CONSTANTS.FREE_MIN_RESIZE_KB
      )
    ) {
      setShowProModal(true);
      return;
    }

    setIsProcessing(true);
    setInvalidInput(false);
    try {
      const result = await optimizeToMaxBytesAsync(w, h, maxKb * 1024, (cw, ch, quality) =>
        cropAndCompressJpeg(sourceUrl, cropRect, cw, ch, quality)
      );

      const blob = new Blob([result.bytes.buffer as ArrayBuffer], { type: 'image/jpeg' });
      const previewUrl = URL.createObjectURL(blob);
      setOutput((prev) => {
        if (prev?.previewUrl) URL.revokeObjectURL(prev.previewUrl);
        return {
          previewUrl,
          blob,
          sizeKb: (result.bytes.byteLength / 1024).toFixed(1),
          width: result.finalWidth,
          height: result.finalHeight,
          quality: result.finalQuality,
          reachedTargetSize: result.reachedTargetSize,
        };
      });
    } catch {
      setInvalidInput(true);
    } finally {
      setIsProcessing(false);
    }
  };

  return (
    <div className="flex min-h-screen flex-col bg-[#F6FAFB] text-slate-900">
      <header className="sticky top-0 z-20 border-b border-slate-200 bg-white">
        <div className="mx-auto flex max-w-3xl items-center gap-2 px-4 py-3">
          <button
            type="button"
            onClick={() => (isCropOpen ? setIsCropOpen(false) : onBack())}
            aria-label={t.back}
            className="flex min-h-12 min-w-12 items-center justify-center rounded-xl text-slate-700 hover:bg-slate-100"
          >
            <ArrowLeft className="h-5 w-5" aria-hidden="true" />
          </button>
          <h1 className="text-lg font-semibold text-slate-900">
            {isCropOpen ? t.cropImage : t.resizerTitle}
          </h1>
        </div>
      </header>

      <main className="mx-auto w-full max-w-3xl flex-1 space-y-5 px-4 py-5">
        {isCropOpen && sourceUrl ? (
          <div className="space-y-4">
            <p className="text-sm text-slate-600">{t.cropHint}</p>
            <div className="relative mx-auto max-w-md overflow-hidden rounded-2xl bg-slate-900 ring-1 ring-slate-300">
              <img src={sourceUrl} alt="Crop preview" className="block w-full select-none" />
              <div
                aria-hidden="true"
                className="pointer-events-none absolute border-2 border-teal-400 shadow-[0_0_0_9999px_rgba(0,0,0,0.5)]"
                style={{
                  left: `${cropRect.left * 100}%`,
                  top: `${cropRect.top * 100}%`,
                  width: `${(cropRect.right - cropRect.left) * 100}%`,
                  height: `${(cropRect.bottom - cropRect.top) * 100}%`,
                }}
              />
            </div>

            <div className="space-y-3 rounded-2xl bg-white p-4 ring-1 ring-slate-200">
              <CropSlider
                label="Left"
                value={cropRect.left}
                min={0}
                max={Math.max(0.05, cropRect.right - 0.1)}
                onChange={(v) => {
                  setOutput(null);
                  setCropRect((c) => ({ ...c, left: v }));
                }}
              />
              <CropSlider
                label="Top"
                value={cropRect.top}
                min={0}
                max={Math.max(0.05, cropRect.bottom - 0.1)}
                onChange={(v) => {
                  setOutput(null);
                  setCropRect((c) => ({ ...c, top: v }));
                }}
              />
              <CropSlider
                label="Right"
                value={cropRect.right}
                min={Math.min(0.95, cropRect.left + 0.1)}
                max={1}
                onChange={(v) => {
                  setOutput(null);
                  setCropRect((c) => ({ ...c, right: v }));
                }}
              />
              <CropSlider
                label="Bottom"
                value={cropRect.bottom}
                min={Math.min(0.95, cropRect.top + 0.1)}
                max={1}
                onChange={(v) => {
                  setOutput(null);
                  setCropRect((c) => ({ ...c, bottom: v }));
                }}
              />
            </div>

            <div className="flex gap-3">
              <button
                type="button"
                onClick={() => {
                  setCropRect({ left: 0, top: 0, right: 1, bottom: 1 });
                  setOutput(null);
                }}
                className="min-h-12 flex-1 rounded-xl border border-slate-300 bg-white px-4 py-2 text-sm font-semibold text-slate-700"
              >
                {t.resetCrop}
              </button>
              <button
                type="button"
                onClick={() => setIsCropOpen(false)}
                className="min-h-12 flex-1 rounded-xl bg-teal-700 px-4 py-2 text-sm font-semibold text-white"
              >
                {t.applyCrop}
              </button>
            </div>
          </div>
        ) : (
          <>
            <div className="flex flex-wrap gap-2.5">
              <label className="flex min-h-12 flex-1 cursor-pointer items-center justify-center gap-2 rounded-xl bg-[#E8F4F6] px-4 py-2.5 text-sm font-semibold text-teal-900 ring-1 ring-teal-900/10 hover:bg-[#DCEEEF]">
                <ImagePlus className="h-4 w-4" aria-hidden="true" />
                <span>{sourceUrl ? t.changeImage : t.pickPhotoOrSig}</span>
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

              {sourceUrl && (
                <button
                  type="button"
                  onClick={() => setIsCropOpen(true)}
                  className="flex min-h-12 items-center gap-2 rounded-xl border border-slate-300 bg-white px-4 py-2.5 text-sm font-semibold text-slate-700 hover:bg-slate-50"
                >
                  <Crop className="h-4 w-4" aria-hidden="true" />
                  <span>{t.cropImage}</span>
                </button>
              )}
            </div>

            {!sourceUrl ? (
              <div className="rounded-2xl bg-slate-100/80 px-6 py-10 text-center ring-1 ring-slate-200">
                <SlidersHorizontal className="mx-auto h-10 w-10 text-teal-700" aria-hidden="true" />
                <p className="mt-3 text-sm text-slate-600">{t.pickPhotoOrSig}</p>
              </div>
            ) : (
              <>
                <div className="space-y-2">
                  <div className="text-xs font-semibold text-slate-700">{t.presetsLabel}</div>
                  <div className="flex flex-wrap gap-2">
                    <button
                      type="button"
                      onClick={() => handleSelectPreset('EXAM_PHOTO')}
                      className={`min-h-12 rounded-xl px-3.5 py-2 text-xs font-semibold ${
                        preset === 'EXAM_PHOTO'
                          ? 'bg-teal-700 text-white'
                          : 'bg-white text-slate-700 ring-1 ring-slate-300'
                      }`}
                    >
                      {t.presetExamPhoto}
                    </button>
                    <button
                      type="button"
                      onClick={() => handleSelectPreset('EXAM_SIGNATURE')}
                      className={`min-h-12 rounded-xl px-3.5 py-2 text-xs font-semibold ${
                        preset === 'EXAM_SIGNATURE'
                          ? 'bg-teal-700 text-white'
                          : 'bg-white text-slate-700 ring-1 ring-slate-300'
                      }`}
                    >
                      {t.presetExamSig}
                    </button>
                    <button
                      type="button"
                      onClick={() => handleSelectPreset('PASSPORT_PHOTO')}
                      className={`min-h-12 rounded-xl px-3.5 py-2 text-xs font-semibold ${
                        preset === 'PASSPORT_PHOTO'
                          ? 'bg-teal-700 text-white'
                          : 'bg-white text-slate-700 ring-1 ring-slate-300'
                      }`}
                    >
                      {t.presetPassport}
                    </button>
                    <button
                      type="button"
                      onClick={() => handleSelectPreset('CUSTOM')}
                      className={`flex min-h-12 items-center gap-1.5 rounded-xl px-3.5 py-2 text-xs font-semibold ${
                        preset === 'CUSTOM'
                          ? 'bg-teal-700 text-white'
                          : 'bg-white text-slate-700 ring-1 ring-slate-300'
                      }`}
                    >
                      {!isProUnlocked() && <Lock className="h-3.5 w-3.5" aria-hidden="true" />}
                      <span>{t.presetCustom}</span>
                    </button>
                  </div>
                </div>

                {preset === 'CUSTOM' && (
                  <div className="grid gap-3 rounded-2xl bg-white p-4 ring-1 ring-slate-200 sm:grid-cols-3">
                    <div>
                      <label htmlFor="resizer-width" className="block text-xs font-medium text-slate-600">
                        {t.widthPx}
                      </label>
                      <input
                        id="resizer-width"
                        type="text"
                        inputMode="numeric"
                        value={widthInput}
                        onChange={(e) => handleWidthChange(e.target.value)}
                        className="mt-1 min-h-11 w-full rounded-xl border border-slate-300 px-3 text-sm"
                      />
                    </div>
                    <div>
                      <label htmlFor="resizer-height" className="block text-xs font-medium text-slate-600">
                        {t.heightPx}
                      </label>
                      <input
                        id="resizer-height"
                        type="text"
                        inputMode="numeric"
                        value={heightInput}
                        onChange={(e) => handleHeightChange(e.target.value)}
                        className="mt-1 min-h-11 w-full rounded-xl border border-slate-300 px-3 text-sm"
                      />
                    </div>
                    <div>
                      <label htmlFor="resizer-maxkb" className="block text-xs font-medium text-slate-600">
                        {t.maxKb}
                      </label>
                      <input
                        id="resizer-maxkb"
                        type="text"
                        inputMode="numeric"
                        value={maxKbInput}
                        onChange={(e) => setMaxKbInput(e.target.value.replace(/\D/g, ''))}
                        className="mt-1 min-h-11 w-full rounded-xl border border-slate-300 px-3 text-sm"
                      />
                    </div>
                    <label className="flex items-center gap-2 text-sm text-slate-700 sm:col-span-3">
                      <input
                        type="checkbox"
                        checked={keepAspect}
                        onChange={(e) => setKeepAspect(e.target.checked)}
                        className="h-4 w-4 accent-teal-700"
                      />
                      <span>{t.keepAspect}</span>
                    </label>
                  </div>
                )}

                {invalidInput && (
                  <p className="text-sm text-red-600">{t.resizerInvalidInput}</p>
                )}

                <button
                  type="button"
                  onClick={handleResize}
                  disabled={isProcessing}
                  className="flex min-h-12 w-full items-center justify-center gap-2 rounded-2xl bg-teal-700 px-5 py-3 text-sm font-semibold text-white hover:bg-teal-800 disabled:opacity-60"
                >
                  {isProcessing ? (
                    <Loader2 className="h-4 w-4 animate-spin" aria-hidden="true" />
                  ) : (
                    <SlidersHorizontal className="h-4 w-4" aria-hidden="true" />
                  )}
                  <span>{isProcessing ? t.resizing : t.resizeCompress}</span>
                </button>

                {output && (
                  <div className="space-y-3 rounded-2xl bg-[#E8F4F6] p-4 ring-1 ring-teal-900/10">
                    <div className="text-base font-semibold text-slate-900">{t.resizedOutput}</div>
                    <div className="text-sm font-semibold text-teal-800">
                      {output.sizeKb} KB &nbsp;•&nbsp; {output.width} × {output.height} px &nbsp;•&nbsp; Quality {output.quality}%
                    </div>
                    {!output.reachedTargetSize && (
                      <p className="text-xs font-medium text-red-700">
                        {t.resizerMinWarning(output.sizeKb)}
                      </p>
                    )}
                    <div className="overflow-hidden rounded-xl bg-white p-2 ring-1 ring-slate-200">
                      <img
                        src={output.previewUrl}
                        alt="Resized image preview"
                        className="mx-auto max-h-60 object-contain"
                      />
                    </div>
                    <div className="flex gap-2.5">
                      <button
                        type="button"
                        onClick={() => {
                          downloadBlob(
                            output.blob,
                            `AuraScan_${output.width}x${output.height}.jpg`
                          );
                          onNotify(t.savedToDevice);
                        }}
                        className="flex min-h-12 flex-1 items-center justify-center gap-2 rounded-xl bg-teal-700 px-4 py-2 text-sm font-semibold text-white hover:bg-teal-800"
                      >
                        <Download className="h-4 w-4" aria-hidden="true" />
                        <span>{t.saveToDevice}</span>
                      </button>
                      <button
                        type="button"
                        onClick={async () => {
                          const mode = await shareOrDownloadFile(
                            output.blob,
                            `AuraScan_${output.width}x${output.height}.jpg`,
                            'image/jpeg',
                            t.resizerTitle
                          );
                          onNotify(mode === 'shared' ? t.shareStarted : t.shareFallbackDownload);
                        }}
                        className="flex min-h-12 flex-1 items-center justify-center gap-2 rounded-xl border border-slate-300 bg-white px-4 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-50"
                      >
                        <Share2 className="h-4 w-4" aria-hidden="true" />
                        <span>{t.share}</span>
                      </button>
                    </div>
                  </div>
                )}
              </>
            )}
          </>
        )}
      </main>

      {showProModal && (
        <ProUpsellModal
          locale={locale}
          title={t.resizerProTitle}
          message={t.resizerProMessage}
          onDismiss={() => setShowProModal(false)}
        />
      )}
    </div>
  );
}

function CropSlider({
  label,
  value,
  min,
  max,
  onChange,
}: {
  label: string;
  value: number;
  min: number;
  max: number;
  onChange: (val: number) => void;
}) {
  return (
    <label className="flex items-center gap-3 text-xs font-semibold text-slate-700">
      <span className="w-14">{label}</span>
      <input
        type="range"
        min={min}
        max={max}
        step={0.01}
        value={Math.min(max, Math.max(min, value))}
        onChange={(e) => onChange(parseFloat(e.target.value))}
        className="flex-1 accent-teal-700"
      />
    </label>
  );
}
