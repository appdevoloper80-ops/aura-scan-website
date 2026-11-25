export interface ResizeOptimizationResult {
  blob: Blob;
  byteLength: number;
  finalWidth: number;
  finalHeight: number;
  finalQuality: number;
  reachedTargetSize: boolean;
}

export interface PureOptimizationResult {
  bytes: Uint8Array;
  finalWidth: number;
  finalHeight: number;
  finalQuality: number;
  reachedTargetSize: boolean;
}

export const DEFAULT_MAX_QUALITY = 95;
export const DEFAULT_MIN_QUALITY = 10;
export const DEFAULT_MIN_DIMENSION_PX = 16;
export const DOWNSCALE_FACTOR = 0.95;

export async function optimizeToMaxBytesAsync(
  targetWidth: number,
  targetHeight: number,
  maxBytes: number,
  compress: (width: number, height: number, quality: number) => Promise<Uint8Array>,
  minDimensionPx = DEFAULT_MIN_DIMENSION_PX,
  minQuality = DEFAULT_MIN_QUALITY,
  maxQuality = DEFAULT_MAX_QUALITY
): Promise<PureOptimizationResult> {
  if (targetWidth <= 0 || targetHeight <= 0 || maxBytes <= 0) {
    throw new Error('Target dimensions and maxBytes must be positive');
  }
  const effectiveMinDim = Math.max(1, minDimensionPx);
  let currentWidth = Math.round(targetWidth);
  let currentHeight = Math.round(targetHeight);

  while (true) {
    let low = minQuality;
    let high = maxQuality;
    let bestBytes: Uint8Array | null = null;
    let bestQuality = minQuality;

    while (low <= high) {
      const mid = (low + high) >> 1;
      const candidate = await compress(currentWidth, currentHeight, mid);
      if (candidate.byteLength <= maxBytes) {
        bestBytes = candidate;
        bestQuality = mid;
        low = mid + 1;
      } else {
        high = mid - 1;
      }
    }

    if (bestBytes !== null) {
      return {
        bytes: bestBytes,
        finalWidth: currentWidth,
        finalHeight: currentHeight,
        finalQuality: bestQuality,
        reachedTargetSize: true,
      };
    }

    const nextWidth = stepDown5Percent(currentWidth, effectiveMinDim);
    const nextHeight = stepDown5Percent(currentHeight, effectiveMinDim);

    if (nextWidth === currentWidth && nextHeight === currentHeight) {
      const fallback = await compress(currentWidth, currentHeight, minQuality);
      return {
        bytes: fallback,
        finalWidth: currentWidth,
        finalHeight: currentHeight,
        finalQuality: minQuality,
        reachedTargetSize: fallback.byteLength <= maxBytes,
      };
    }

    currentWidth = nextWidth;
    currentHeight = nextHeight;
  }
}

function stepDown5Percent(value: number, minDim: number): number {
  if (value <= minDim) return value;
  const scaled = Math.round(value * DOWNSCALE_FACTOR);
  const guaranteedStep = scaled >= value ? value - 1 : scaled;
  return Math.max(minDim, guaranteedStep);
}

export function computeAspectPreservedDimension(
  changedDimension: number,
  sourceWidth: number,
  sourceHeight: number,
  widthChanged: boolean
): number {
  if (changedDimension <= 0 || sourceWidth <= 0 || sourceHeight <= 0) {
    return Math.max(1, changedDimension);
  }
  if (widthChanged) {
    return Math.max(1, Math.round((changedDimension * sourceHeight) / sourceWidth));
  }
  return Math.max(1, Math.round((changedDimension * sourceWidth) / sourceHeight));
}

export function requiresProForTarget(
  isCustomPreset: boolean,
  maxKb: number,
  proUnlocked: boolean,
  freeMinKb = 20
): boolean {
  if (proUnlocked) return false;
  return isCustomPreset || maxKb < freeMinKb;
}
