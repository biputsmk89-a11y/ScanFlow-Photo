package com.scanflow.photocompressor.data.local

/**
 * Legacy entity reference, superseded by [ProcessingHistoryEntity] in Database Version 2.
 */
@Deprecated("Superseded by ProcessingHistoryEntity in Room Database Version 2", ReplaceWith("ProcessingHistoryEntity"))
typealias HistoryEntity = ProcessingHistoryEntity
