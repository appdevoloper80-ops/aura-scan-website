import test from 'node:test';
import assert from 'node:assert/strict';
import {
  computeAspectPreservedDimension,
  optimizeToMaxBytesAsync,
  requiresProForTarget,
} from './imageSizeOptimizer';

test('returns max quality 95 when target fits immediately', async () => {
  const result = await optimizeToMaxBytesAsync(
    200,
    230,
    50 * 1024,
    async (w, h, q) => new Uint8Array(Math.floor((w * h * q) / 500))
  );

  assert.equal(result.reachedTargetSize, true);
  assert.equal(result.finalWidth, 200);
  assert.equal(result.finalHeight, 230);
  assert.equal(result.finalQuality, 95);
});

test('binary search finds exact highest quality under maxBytes', async () => {
  const maxBytes = 20_000;
  const result = await optimizeToMaxBytesAsync(
    140,
    60,
    maxBytes,
    async (_w, _h, q) => new Uint8Array(q * 250)
  );

  assert.equal(result.reachedTargetSize, true);
  assert.equal(result.finalWidth, 140);
  assert.equal(result.finalHeight, 60);
  assert.equal(result.finalQuality, 80);
  assert.equal(result.bytes.byteLength, 20_000);
});

test('downscales in 5 percent steps when quality 10 still exceeds maxBytes', async () => {
  const maxBytes = 10_000;
  const result = await optimizeToMaxBytesAsync(
    200,
    200,
    maxBytes,
    async (w, h, q) => new Uint8Array(Math.floor((w * h * q) / 10))
  );

  assert.equal(result.reachedTargetSize, true);
  assert.ok(result.finalWidth < 200);
  assert.ok(result.finalHeight < 200);
  assert.ok(result.bytes.byteLength <= maxBytes);
  assert.ok(result.finalQuality >= 10 && result.finalQuality <= 95);
});

test('stops gracefully at minDimensionPx when target is unreachable', async () => {
  const result = await optimizeToMaxBytesAsync(
    60,
    60,
    100,
    async () => new Uint8Array(500),
    16
  );

  assert.equal(result.reachedTargetSize, false);
  assert.equal(result.finalWidth, 16);
  assert.equal(result.finalHeight, 16);
  assert.equal(result.finalQuality, 10);
});

test('aspect ratio dimension calculation preserves proportions', () => {
  assert.equal(computeAspectPreservedDimension(200, 1000, 1500, true), 300);
  assert.equal(computeAspectPreservedDimension(300, 1000, 1500, false), 200);
});

test('pro gating enforces custom preset and sub-20KB limit', () => {
  assert.equal(requiresProForTarget(false, 20, false), false);
  assert.equal(requiresProForTarget(false, 50, false), false);
  assert.equal(requiresProForTarget(true, 100, false), true);
  assert.equal(requiresProForTarget(false, 15, false), true);
  assert.equal(requiresProForTarget(true, 10, true), false);
});
