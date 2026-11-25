import { ArrowLeft, FileText, Info, Mail, Shield, Star } from 'lucide-react';
import { APP_CONSTANTS } from '../lib/constants';
import type { AppLocale } from '../lib/i18n';
import { STRINGS } from '../lib/i18n';

interface SettingsViewProps {
  locale: AppLocale;
  onBack: () => void;
}

export function SettingsView({ locale, onBack }: SettingsViewProps) {
  const t = STRINGS[locale];
  const playStoreUrl = `https://play.google.com/store/apps/details?id=${APP_CONSTANTS.PACKAGE_NAME}`;
  const mailtoUrl = `mailto:${APP_CONSTANTS.SUPPORT_EMAIL}?subject=${encodeURIComponent(
    `AuraScan Support (v${APP_CONSTANTS.APP_VERSION})`
  )}`;

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
          <h1 className="text-lg font-semibold text-slate-900">{t.settingsTitle}</h1>
        </div>
      </header>

      <main className="mx-auto w-full max-w-3xl flex-1 space-y-6 px-4 py-5">
        <section className="space-y-2">
          <h2 className="px-1 text-xs font-semibold uppercase tracking-wide text-teal-800">
            {t.legalSection}
          </h2>
          <div className="divide-y divide-slate-200 overflow-hidden rounded-2xl bg-white ring-1 ring-slate-200">
            <a
              href={APP_CONSTANTS.PRIVACY_POLICY_URL}
              target="_blank"
              rel="noreferrer"
              className="flex min-h-14 items-center gap-3 px-4 py-3 text-sm font-medium text-slate-800 hover:bg-slate-50"
            >
              <Shield className="h-5 w-5 text-slate-500" aria-hidden="true" />
              <span>{t.privacyPolicy}</span>
            </a>
            <a
              href={APP_CONSTANTS.TERMS_OF_USE_URL}
              target="_blank"
              rel="noreferrer"
              className="flex min-h-14 items-center gap-3 px-4 py-3 text-sm font-medium text-slate-800 hover:bg-slate-50"
            >
              <FileText className="h-5 w-5 text-slate-500" aria-hidden="true" />
              <span>{t.termsOfUse}</span>
            </a>
            {APP_CONSTANTS.SHOW_AD_PRIVACY_CHOICES && (
              <div className="flex min-h-14 items-center gap-3 px-4 py-3 text-sm font-medium text-slate-800">
                <Shield className="h-5 w-5 text-slate-500" aria-hidden="true" />
                <span>Ad privacy choices</span>
              </div>
            )}
          </div>
        </section>

        <section className="space-y-2">
          <h2 className="px-1 text-xs font-semibold uppercase tracking-wide text-teal-800">
            {t.supportSection}
          </h2>
          <div className="divide-y divide-slate-200 overflow-hidden rounded-2xl bg-white ring-1 ring-slate-200">
            <a
              href={playStoreUrl}
              target="_blank"
              rel="noreferrer"
              className="flex min-h-14 items-center gap-3 px-4 py-3 text-sm font-medium text-slate-800 hover:bg-slate-50"
            >
              <Star className="h-5 w-5 text-slate-500" aria-hidden="true" />
              <span>{t.rateApp}</span>
            </a>
            <a
              href={mailtoUrl}
              className="flex min-h-14 items-center gap-3 px-4 py-3 text-sm font-medium text-slate-800 hover:bg-slate-50"
            >
              <Mail className="h-5 w-5 text-slate-500" aria-hidden="true" />
              <div>
                <div>{t.contactSupport}</div>
                <div className="text-xs text-slate-500">{APP_CONSTANTS.SUPPORT_EMAIL}</div>
              </div>
            </a>
            <div className="flex min-h-14 items-center gap-3 px-4 py-3 text-sm font-medium text-slate-800">
              <Info className="h-5 w-5 text-slate-500" aria-hidden="true" />
              <div>
                <div>{t.appVersion}</div>
                <div className="text-xs text-slate-500">{APP_CONSTANTS.APP_VERSION}</div>
              </div>
            </div>
          </div>
        </section>
      </main>
    </div>
  );
}
