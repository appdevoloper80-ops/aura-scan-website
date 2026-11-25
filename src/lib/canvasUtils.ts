export interface CropRect {
  left: number;
  top: number;
  right: number;
  bottom: number;
}

export type ScanFilter = 'original' | 'clean' | 'grayscale';

export function loadImageElement(src: string): Promise<HTMLImageElement> {
  return new Promise((resolve, reject) => {
    const img = new Image();
    img.onload = () => resolve(img);
    img.onerror = () => reject(new Error('Failed to load image'));
    img.src = src;
  });
}

export async function fileToNormalizedDataUrl(
  file: File,
  maxDimension = 1800
): Promise<{ dataUrl: string; width: number; height: number }> {
  const objectUrl = URL.createObjectURL(file);
  try {
    const img = await loadImageElement(objectUrl);
    const scale = Math.min(1, maxDimension / Math.max(img.naturalWidth, img.naturalHeight));
    const width = Math.max(1, Math.round(img.naturalWidth * scale));
    const height = Math.max(1, Math.round(img.naturalHeight * scale));

    const canvas = document.createElement('canvas');
    canvas.width = width;
    canvas.height = height;
    const ctx = canvas.getContext('2d');
    if (!ctx) throw new Error('Canvas 2D context unavailable');
    ctx.fillStyle = '#ffffff';
    ctx.fillRect(0, 0, width, height);
    ctx.drawImage(img, 0, 0, width, height);

    const dataUrl = canvas.toDataURL('image/jpeg', 0.92);
    return { dataUrl, width, height };
  } finally {
    URL.revokeObjectURL(objectUrl);
  }
}

export async function applyScanFilterToDataUrl(
  srcDataUrl: string,
  filter: ScanFilter
): Promise<string> {
  if (filter === 'original') return srcDataUrl;
  const img = await loadImageElement(srcDataUrl);
  const canvas = document.createElement('canvas');
  canvas.width = img.naturalWidth;
  canvas.height = img.naturalHeight;
  const ctx = canvas.getContext('2d');
  if (!ctx) return srcDataUrl;

  ctx.drawImage(img, 0, 0);
  const imageData = ctx.getImageData(0, 0, canvas.width, canvas.height);
  const data = imageData.data;

  for (let i = 0; i < data.length; i += 4) {
    const lum = 0.299 * data[i] + 0.587 * data[i + 1] + 0.114 * data[i + 2];
    if (filter === 'grayscale') {
      data[i] = lum;
      data[i + 1] = lum;
      data[i + 2] = lum;
    } else if (filter === 'clean') {
      // High-contrast document boost: brightens paper background and sharpens ink
      const boosted = Math.min(255, Math.max(0, (lum - 128) * 1.45 + 145));
      data[i] = boosted;
      data[i + 1] = boosted;
      data[i + 2] = boosted;
    }
  }
  ctx.putImageData(imageData, 0, 0);
  return canvas.toDataURL('image/jpeg', 0.9);
}

export async function cropAndCompressJpeg(
  srcDataUrl: string,
  crop: CropRect,
  targetWidth: number,
  targetHeight: number,
  quality: number
): Promise<Uint8Array> {
  const img = await loadImageElement(srcDataUrl);
  const sx = Math.max(0, Math.round(crop.left * img.naturalWidth));
  const sy = Math.max(0, Math.round(crop.top * img.naturalHeight));
  const sw = Math.max(1, Math.round((crop.right - crop.left) * img.naturalWidth));
  const sh = Math.max(1, Math.round((crop.bottom - crop.top) * img.naturalHeight));

  const canvas = document.createElement('canvas');
  canvas.width = targetWidth;
  canvas.height = targetHeight;
  const ctx = canvas.getContext('2d');
  if (!ctx) throw new Error('Canvas 2D context unavailable');

  ctx.fillStyle = '#ffffff';
  ctx.fillRect(0, 0, targetWidth, targetHeight);
  ctx.imageSmoothingEnabled = true;
  ctx.imageSmoothingQuality = 'high';
  ctx.drawImage(img, sx, sy, sw, sh, 0, 0, targetWidth, targetHeight);

  const blob = await new Promise<Blob>((resolve, reject) => {
    canvas.toBlob(
      (b) => (b ? resolve(b) : reject(new Error('JPEG encode failed'))),
      'image/jpeg',
      Math.min(1, Math.max(0.01, quality / 100))
    );
  });
  const buf = await blob.arrayBuffer();
  return new Uint8Array(buf);
}
