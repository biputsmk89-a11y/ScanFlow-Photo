package com.scanflow.photocompressor.work

/**
 * Common keys used for WorkManager inputData, outputData, and progress data.
 */
object WorkKeys {
    const val KEY_JOB_ID = "key_job_id"
    const val KEY_SOURCE_URIS = "key_source_uris"
    const val KEY_OUTPUT_URIS = "key_output_uris"
    const val KEY_SOURCE_URI = "key_source_uri"
    const val KEY_OUTPUT_URI = "key_output_uri"

    // Progress & Summary metrics
    const val KEY_TOTAL_COUNT = "key_total_count"
    const val KEY_SUCCESS_COUNT = "key_success_count"
    const val KEY_FAILED_COUNT = "key_failed_count"
    const val KEY_CANCELLED_COUNT = "key_cancelled_count"
    const val KEY_PROGRESS_PERCENT = "key_progress_percent"
    const val KEY_CURRENT_URI = "key_current_uri"
    const val KEY_ERROR_MESSAGE = "key_error_message"
    const val KEY_STAGE = "key_stage"

    // Pipeline options
    const val KEY_QUALITY = "key_quality"
    const val KEY_MAX_WIDTH = "key_max_width"
    const val KEY_MAX_HEIGHT = "key_max_height"
    const val KEY_FORMAT = "key_format"
    const val KEY_REMOVE_METADATA = "key_remove_metadata"

    // PDF options
    const val KEY_PDF_TITLE = "key_pdf_title"
    const val KEY_PDF_OUTPUT_URI = "key_pdf_output_uri"
    const val KEY_PDF_PAGE_COUNT = "key_pdf_page_count"
}
