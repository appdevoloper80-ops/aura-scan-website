export const APP_CONSTANTS = {
  APP_NAME: 'AuraScan',
  APP_VERSION: '1.0.0',
  PACKAGE_NAME: 'com.yourname.aurascan',
  PRIVACY_POLICY_URL: 'https://example.com/aurascan/privacy-policy',
  TERMS_OF_USE_URL: 'https://example.com/aurascan/terms-of-use',
  SUPPORT_EMAIL: 'support@example.com',
  FREE_OCR_PAGE_LIMIT: 3,
  MAX_SCAN_PAGES: 20,
  FREE_MIN_RESIZE_KB: 20,
  SHOW_AD_PRIVACY_CHOICES: false,
  AD_BANNER_HEIGHT_PX: 60,
} as const;

export function isProUnlocked(): boolean {
  return false;
}
