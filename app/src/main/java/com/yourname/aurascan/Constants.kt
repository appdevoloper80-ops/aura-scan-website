package com.yourname.aurascan

object Constants {
    // Update these URLs and support email before publishing to Google Play.
    const val PRIVACY_POLICY_URL = "https://example.com/aurascan/privacy-policy"
    const val TERMS_OF_USE_URL = "https://example.com/aurascan/terms-of-use"
    const val SUPPORT_EMAIL = "support@example.com"

    // Feature & Free-tier limits
    const val FREE_OCR_PAGE_LIMIT = 3
    const val MAX_SCAN_PAGES = 20
    const val FREE_MIN_RESIZE_KB = 20

    // Ad & Privacy placeholders for the upcoming ads integration step
    const val SHOW_AD_PRIVACY_CHOICES = false
    const val AD_BANNER_HEIGHT_DP = 60

    /**
     * Central entitlement check for Pro features (unlimited OCR pages,
     * Custom resizer preset, and resizer targets below 20 KB).
     * Returns false until rewarded ads / billing are wired in the next step.
     */
    fun isProUnlocked(): Boolean = false
}
