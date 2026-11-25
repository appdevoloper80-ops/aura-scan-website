import { jsPDF } from 'jspdf';
import { loadImageElement } from './canvasUtils';

export type PdfPageSize = 'A4' | 'FIT_TO_IMAGE';
export type PdfMargin = 'NONE' | 'SMALL' | 'MEDIUM';

const MARGIN_POINTS: Record<PdfMargin, number> = {
  NONE: 0,
  SMALL: 24,
  MEDIUM: 48,
};

const A4_WIDTH_PT = 595;
const A4_HEIGHT_PT = 842;
const MAX_FIT_DIM_PT = 1200;

export async function buildPdfFromDataUrls(
  pageDataUrls: string[],
  pageSize: PdfPageSize,
  margin: PdfMargin
): Promise<Blob> {
  if (pageDataUrls.length === 0) {
    throw new Error('At least one image is required to build a PDF');
  }

  const marginPt = MARGIN_POINTS[margin];
  let pdf: jsPDF | null = null;

  for (let i = 0; i < pageDataUrls.length; i++) {
    const img = await loadImageElement(pageDataUrls[i]);
    const imgW = img.naturalWidth || 800;
    const imgH = img.naturalHeight || 1000;

    let pageW = A4_WIDTH_PT;
    let pageH = A4_HEIGHT_PT;

    if (pageSize === 'FIT_TO_IMAGE') {
      const maxContent = MAX_FIT_DIM_PT - marginPt * 2;
      const largest = Math.max(imgW, imgH);
      const scale = largest > maxContent && maxContent > 0 ? maxContent / largest : 1;
      pageW = Math.max(1, Math.round(imgW * scale)) + marginPt * 2;
      pageH = Math.max(1, Math.round(imgH * scale)) + marginPt * 2;
    }

    const orientation = pageW > pageH ? 'landscape' : 'portrait';

    if (i === 0) {
      pdf = new jsPDF({
        orientation,
        unit: 'pt',
        format: [pageW, pageH],
        compress: true,
      });
    } else if (pdf) {
      pdf.addPage([pageW, pageH], orientation);
    }

    if (pdf) {
      const availW = Math.max(1, pageW - marginPt * 2);
      const availH = Math.max(1, pageH - marginPt * 2);
      const scale = Math.min(availW / imgW, availH / imgH);
      const drawW = imgW * scale;
      const drawH = imgH * scale;
      const x = marginPt + (availW - drawW) / 2;
      const y = marginPt + (availH - drawH) / 2;
      pdf.addImage(pageDataUrls[i], 'JPEG', x, y, drawW, drawH, undefined, 'FAST');
    }
  }

  if (!pdf) {
    throw new Error('Could not initialize PDF document');
  }

  return pdf.output('blob');
}
