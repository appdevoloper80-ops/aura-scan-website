import { useState } from 'react';
import { Lock } from 'lucide-react';
import type { AppLocale } from '../lib/i18n';
import { STRINGS } from '../lib/i18n';

interface RenameDialogProps {
  locale: AppLocale;
  initialName: string;
  onDismiss: () => void;
  onConfirm: (newName: string) => void;
}

export function RenameDialog({ locale, initialName, onDismiss, onConfirm }: RenameDialogProps) {
  const t = STRINGS[locale];
  const [name, setName] = useState(initialName);
  const [error, setError] = useState(false);

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/50 p-4"
      role="dialog"
      aria-modal="true"
      aria-labelledby="rename-dialog-title"
    >
      <div className="w-full max-w-md rounded-3xl bg-white p-6 shadow-xl ring-1 ring-slate-200">
        <h2 id="rename-dialog-title" className="text-lg font-semibold text-slate-900">
          {t.renameTitle}
        </h2>
        <div className="mt-4">
          <label htmlFor="rename-doc-input" className="block text-xs font-medium text-slate-600">
            {t.renameLabel}
          </label>
          <input
            id="rename-doc-input"
            type="text"
            value={name}
            onChange={(e) => {
              setName(e.target.value);
              if (error && e.target.value.trim()) setError(false);
            }}
            onKeyDown={(e) => {
              if (e.key === 'Enter') {
                const trimmed = name.trim();
                if (!trimmed) setError(true);
                else onConfirm(trimmed);
              }
            }}
            autoFocus
            className="mt-1.5 min-h-12 w-full rounded-xl border border-slate-300 px-3.5 py-2.5 text-sm text-slate-900 focus:border-teal-700 focus:outline-none focus:ring-2 focus:ring-teal-700/20"
          />
          {error && <p className="mt-1.5 text-xs text-red-600">{t.renameError}</p>}
        </div>
        <div className="mt-6 flex justify-end gap-2">
          <button
            type="button"
            onClick={onDismiss}
            className="min-h-12 rounded-xl px-4 py-2 text-sm font-medium text-slate-600 hover:bg-slate-100"
          >
            {t.cancel}
          </button>
          <button
            type="button"
            onClick={() => {
              const trimmed = name.trim();
              if (!trimmed) setError(true);
              else onConfirm(trimmed);
            }}
            className="min-h-12 rounded-xl bg-teal-700 px-5 py-2 text-sm font-semibold text-white hover:bg-teal-800"
          >
            {t.save}
          </button>
        </div>
      </div>
    </div>
  );
}

interface DeleteDialogProps {
  locale: AppLocale;
  documentName: string;
  onDismiss: () => void;
  onConfirm: () => void;
}

export function DeleteDialog({ locale, documentName, onDismiss, onConfirm }: DeleteDialogProps) {
  const t = STRINGS[locale];
  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/50 p-4"
      role="dialog"
      aria-modal="true"
      aria-labelledby="delete-dialog-title"
    >
      <div className="w-full max-w-md rounded-3xl bg-white p-6 shadow-xl ring-1 ring-slate-200">
        <h2 id="delete-dialog-title" className="text-lg font-semibold text-slate-900">
          {t.deleteTitle}
        </h2>
        <p className="mt-2 text-sm text-slate-600">{t.deleteMessage(documentName)}</p>
        <div className="mt-6 flex justify-end gap-2">
          <button
            type="button"
            onClick={onDismiss}
            className="min-h-12 rounded-xl px-4 py-2 text-sm font-medium text-slate-600 hover:bg-slate-100"
          >
            {t.cancel}
          </button>
          <button
            type="button"
            onClick={onConfirm}
            className="min-h-12 rounded-xl bg-red-600 px-5 py-2 text-sm font-semibold text-white hover:bg-red-700"
          >
            {t.delete}
          </button>
        </div>
      </div>
    </div>
  );
}

interface ProUpsellModalProps {
  locale: AppLocale;
  title: string;
  message: string;
  onDismiss: () => void;
}

export function ProUpsellModal({ locale, title, message, onDismiss }: ProUpsellModalProps) {
  const t = STRINGS[locale];
  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/50 p-4"
      role="dialog"
      aria-modal="true"
      aria-labelledby="pro-dialog-title"
    >
      <div className="w-full max-w-md rounded-3xl bg-white p-6 shadow-xl ring-1 ring-slate-200">
        <div className="flex items-center gap-3 text-teal-700">
          <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-teal-50">
            <Lock className="h-5 w-5" aria-hidden="true" />
          </div>
          <h2 id="pro-dialog-title" className="text-lg font-semibold text-slate-900">
            {title}
          </h2>
        </div>
        <p className="mt-3 text-sm leading-relaxed text-slate-600">{message}</p>
        <div className="mt-6 flex justify-end">
          <button
            type="button"
            onClick={onDismiss}
            className="min-h-12 rounded-xl bg-teal-700 px-5 py-2 text-sm font-semibold text-white hover:bg-teal-800"
          >
            {t.confirm}
          </button>
        </div>
      </div>
    </div>
  );
}
