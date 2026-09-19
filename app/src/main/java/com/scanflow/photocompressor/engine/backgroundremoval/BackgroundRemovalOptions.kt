package com.scanflow.photocompressor.engine.backgroundremoval

/**
 * Options and fine-tuning parameters for the AI background removal pipeline.
 */
data class BackgroundRemovalOptions(
    /** Threshold below which confidence is clamped to pure 0.0 (background) to cut off noise */
    val lowConfidenceThreshold: Float = 0.28f,

    /** Threshold above which confidence is clamped to pure 1.0 (foreground) for solid core */
    val highConfidenceThreshold: Float = 0.82f,

    /** Number of smoothing passes for edge transition */
    val edgeFeatherRadius: Int = 2,

    /** Whether to apply foreground color bleeding to eradicate halo/fringing on edges */
    val enableColorDecontamination: Boolean = true,

    /** Whether to close pinholes inside hair, face, and clothing */
    val enableHoleClosing: Boolean = true,

    /** Whether to prune disconnected background noise islands and floating dot pixels */
    val enableIslandPruning: Boolean = true
)
