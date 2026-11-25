/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import { useEffect, useRef, useState } from 'react';
import { AgeGateModal, UnderAgeBlockedView } from './components/AgeGateModal';
import { DeleteDialog, RenameDialog } from './components/Dialogs';
import { DocumentView } from './components/DocumentView';
import { HomeView } from './components/HomeView';
import { ImagesToPdfView } from './components/ImagesToPdfView';
import { OcrView } from './components/OcrView';
import { ResizerView } from './components/ResizerView';
import { ScannerModal } from './components/ScannerModal';
import { SettingsView } from './components/SettingsView';
import type { AppLocale } from './lib/i18n';
import { STRINGS } from './lib/i18n';
import {
  deleteStoredDocument,
  downloadBlob,
  getAgeGateStatus,
  getAllDocuments,
  renameStoredDocument,
  sanitizeFileName,
  saveDocument,
  setAgeGateStatus,
  shareOrDownloadFile,
  type AgeGateStatus,
  type AuraScanDocument,
} from './lib/storage';

type Route =
  | { name: 'home' }
  | { name: 'scanner' }
  | { name: 'document'; docId: string }
  | { name: 'ocr'; docId?: string }
  | { name: 'images_to_pdf' }
  | { name: 'resizer' }
  | { name: 'settings' };

export default function App() {
  const [locale, setLocale] = useState<AppLocale>('en');
  const t = STRINGS[locale];

  const [ageStatus, setAgeStatus] = useState<AgeGateStatus>(() => {
    const params = new URLSearchParams(window.location.search);
    if (params.get('autostart') === '1') return 'ADULT_18_PLUS';
    return getAgeGateStatus();
  });

  const [route, setRoute] = useState<Route>({ name: 'home' });
  const [documents, setDocuments] = useState<AuraScanDocument[]>([]);
  const [docToRename, setDocToRename] = useState<AuraScanDocument | null>(null);
  const [docToDelete, setDocToDelete] = useState<AuraScanDocument | null>(null);
  const [toast, setToast] = useState<string | null>(null);
  const toastTimerRef = useRef<number | null>(null);

  const notify = (msg: string) => {
    setToast(msg);
    if (toastTimerRef.current) window.clearTimeout(toastTimerRef.current);
    toastTimerRef.current = window.setTimeout(() => setToast(null), 3200);
  };

  useEffect(() => {
    getAllDocuments()
      .then(setDocuments)
      .catch(() => setDocuments([]));
  }, []);

  const handleConfirmAdult = () => {
    setAgeGateStatus('ADULT_18_PLUS');
    setAgeStatus('ADULT_18_PLUS');
  };

  const handleUnder18 = () => {
    setAgeGateStatus('UNDER_18');
    setAgeStatus('UNDER_18');
  };

  const handleReverifyAge = () => {
    setAgeGateStatus('UNSET');
    setAgeStatus('UNSET');
  };

  const handleSaveNewDoc = async (doc: AuraScanDocument) => {
    await saveDocument(doc);
    setDocuments((prev) => [doc, ...prev]);
    setRoute({ name: 'document', docId: doc.id });
  };

  const handleRenameConfirm = async (newName: string) => {
    if (!docToRename) return;
    await renameStoredDocument(docToRename.id, newName);
    setDocuments((prev) =>
      prev.map((d) => (d.id === docToRename.id ? { ...d, name: newName } : d))
    );
    setDocToRename(null);
    notify(t.docRenamed);
  };

  const handleDeleteConfirm = async () => {
    if (!docToDelete) return;
    const deletedId = docToDelete.id;
    await deleteStoredDocument(deletedId);
    setDocuments((prev) => prev.filter((d) => d.id !== deletedId));
    setDocToDelete(null);
    if (route.name === 'document' && route.docId === deletedId) {
      setRoute({ name: 'home' });
    }
    notify(t.docDeleted);
  };

  const handleSharePdf = async (doc: AuraScanDocument) => {
    const mode = await shareOrDownloadFile(
      doc.pdfBlob,
      sanitizeFileName(doc.name, 'pdf'),
      'application/pdf',
      doc.name
    );
    notify(mode === 'shared' ? t.shareStarted : t.shareFallbackDownload);
  };

  const handleSavePdfToDevice = (doc: AuraScanDocument) => {
    downloadBlob(doc.pdfBlob, sanitizeFileName(doc.name, 'pdf'));
    notify(t.savedToDevice);
  };

  if (ageStatus === 'UNDER_18') {
    return <UnderAgeBlockedView locale={locale} onReverify={handleReverifyAge} />;
  }

  const activeDoc =
    route.name === 'document' || (route.name === 'ocr' && route.docId)
      ? documents.find((d) => d.id === route.docId)
      : undefined;

  return (
    <>
      {route.name === 'home' && (
        <HomeView
          locale={locale}
          onToggleLocale={() => setLocale((l) => (l === 'en' ? 'hi' : 'en'))}
          documents={documents}
          onStartScan={() => setRoute({ name: 'scanner' })}
          onOpenImagesToPdf={() => setRoute({ name: 'images_to_pdf' })}
          onOpenResizer={() => setRoute({ name: 'resizer' })}
          onOpenOcr={() => setRoute({ name: 'ocr' })}
          onOpenSettings={() => setRoute({ name: 'settings' })}
          onOpenDocument={(docId) => setRoute({ name: 'document', docId })}
          onRenameDocument={setDocToRename}
          onShareDocument={handleSharePdf}
          onSaveDocumentToDevice={handleSavePdfToDevice}
          onDeleteDocument={setDocToDelete}
        />
      )}

      {route.name === 'scanner' && (
        <ScannerModal
          locale={locale}
          onCancel={() => {
            setRoute({ name: 'home' });
            notify(t.scannerCancelled);
          }}
          onSaveDocument={handleSaveNewDoc}
          onError={(msg) => notify(msg)}
        />
      )}

      {route.name === 'document' && (
        <DocumentView
          locale={locale}
          document={activeDoc}
          onBack={() => setRoute({ name: 'home' })}
          onSharePdf={handleSharePdf}
          onSavePdf={handleSavePdfToDevice}
          onRename={setDocToRename}
          onDelete={setDocToDelete}
          onOpenOcr={(doc) => setRoute({ name: 'ocr', docId: doc.id })}
        />
      )}

      {route.name === 'ocr' && (
        <OcrView
          locale={locale}
          document={activeDoc}
          onBack={() =>
            route.docId
              ? setRoute({ name: 'document', docId: route.docId })
              : setRoute({ name: 'home' })
          }
          onNotify={notify}
        />
      )}

      {route.name === 'images_to_pdf' && (
        <ImagesToPdfView
          locale={locale}
          onBack={() => setRoute({ name: 'home' })}
          onPdfCreated={handleSaveNewDoc}
          onError={(msg) => notify(msg)}
        />
      )}

      {route.name === 'resizer' && (
        <ResizerView
          locale={locale}
          onBack={() => setRoute({ name: 'home' })}
          onNotify={notify}
        />
      )}

      {route.name === 'settings' && (
        <SettingsView locale={locale} onBack={() => setRoute({ name: 'home' })} />
      )}

      {ageStatus === 'UNSET' && (
        <AgeGateModal
          locale={locale}
          onConfirmAdult={handleConfirmAdult}
          onUnder18={handleUnder18}
        />
      )}

      {docToRename && (
        <RenameDialog
          locale={locale}
          initialName={docToRename.name}
          onDismiss={() => setDocToRename(null)}
          onConfirm={handleRenameConfirm}
        />
      )}

      {docToDelete && (
        <DeleteDialog
          locale={locale}
          documentName={docToDelete.name}
          onDismiss={() => setDocToDelete(null)}
          onConfirm={handleDeleteConfirm}
        />
      )}

      {toast && (
        <div
          role="status"
          aria-live="polite"
          className="fixed bottom-20 left-1/2 z-50 -translate-x-1/2 rounded-xl bg-slate-900 px-4 py-2.5 text-xs font-medium text-white shadow-lg"
        >
          {toast}
        </div>
      )}
    </>
  );
}
