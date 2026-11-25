import { ShieldAlert, ShieldCheck } from 'lucide-react';
import type { AppLocale } from '../lib/i18n';
import { STRINGS } from '../lib/i18n';

interface AgeGateModalProps {
  locale: AppLocale;
  onConfirmAdult: () => void;
  onUnder18: () => void;
}

export function AgeGateModal({ locale, onConfirmAdult, onUnder18 }: AgeGateModalProps) {
  const t = STRINGS[locale];
  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/60 p-4 backdrop-blur-xs"
      role="dialog"
      aria-modal="true"
      aria-labelledby="age-gate-title"
    >
      <div className="w-full max-w-md rounded-3xl bg-white p-6 shadow-2xl ring-1 ring-slate-200">
        <div className="flex items-center gap-3 text-teal-700">
          <div className="flex h-11 w-11 items-center justify-center rounded-2xl bg-teal-50">
            <ShieldCheck className="h-6 w-6" aria-hidden="true" />
          </div>
          <h2 id="age-gate-title" className="text-xl font-semibold text-slate-900">
            {t.ageGateTitle}
          </h2>
        </div>
        <p className="mt-4 text-sm leading-relaxed text-slate-600">{t.ageGateMessage}</p>
        <div className="mt-6 flex flex-col gap-2.5">
          <button
            type="button"
            onClick={onConfirmAdult}
            className="min-h-12 w-full rounded-2xl bg-teal-700 px-5 py-3 text-sm font-semibold text-white shadow-xs transition hover:bg-teal-800 focus:outline-none focus:ring-2 focus:ring-teal-600 focus:ring-offset-2"
          >
            {t.ageGateConfirm}
          </button>
          <button
            type="button"
            onClick={onUnder18}
            className="min-h-12 w-full rounded-2xl border border-slate-300 bg-white px-5 py-3 text-sm font-semibold text-slate-700 transition hover:bg-slate-50 focus:outline-none focus:ring-2 focus:ring-slate-400 focus:ring-offset-2"
          >
            {t.ageGateUnder18}
          </button>
        </div>
      </div>
    </div>
  );
}

interface UnderAgeBlockedViewProps {
  locale: AppLocale;
  onReverify: () => void;
}

export function UnderAgeBlockedView({ locale, onReverify }: UnderAgeBlockedViewProps) {
  const t = STRINGS[locale];
  return (
    <div className="flex min-h-screen items-center justify-center bg-slate-50 p-6">
      <div className="w-full max-w-md rounded-3xl bg-white p-8 text-center shadow-sm ring-1 ring-slate-200">
        <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-2xl bg-amber-50 text-amber-700">
          <ShieldAlert className="h-7 w-7" aria-hidden="true" />
        </div>
        <h1 className="mt-4 text-xl font-semibold text-slate-900">{t.ageBlockedTitle}</h1>
        <p className="mt-2 text-sm leading-relaxed text-slate-600">{t.ageBlockedMessage}</p>
        <button
          type="button"
          onClick={onReverify}
          className="mt-6 min-h-12 rounded-xl border border-slate-300 px-5 py-2.5 text-sm font-medium text-slate-700 transition hover:bg-slate-50"
        >
          {t.ageResetForDemo}
        </button>
      </div>
    </div>
  );
}
