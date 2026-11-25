import { createWorker } from 'tesseract.js';

export type OcrLanguage = 'LATIN' | 'HINDI_DEVANAGARI';

export async function runBrowserOcr(
  pageDataUrls: string[],
  language: OcrLanguage,
  onProgress: (currentPage: number, totalPages: number) => void
): Promise<string> {
  const langCode = language === 'LATIN' ? 'eng' : 'hin';
  const worker = await createWorker(langCode);

  try {
    const sections: string[] = [];
    const total = pageDataUrls.length;

    for (let i = 0; i < total; i++) {
      onProgress(i + 1, total);
      const {
        data: { text },
      } = await worker.recognize(pageDataUrls[i]);
      const cleaned = text.trim();
      if (total > 1) {
        sections.push(cleaned ? `--- Page ${i + 1} ---\n${cleaned}` : `--- Page ${i + 1} ---`);
      } else if (cleaned) {
        sections.push(cleaned);
      }
    }

    return sections.join('\n\n').trim();
  } finally {
    await worker.terminate();
  }
}
