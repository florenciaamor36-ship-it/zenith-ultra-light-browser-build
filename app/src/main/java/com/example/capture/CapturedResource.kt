package com.example.capture

/** A resource discovered in the current page; its bytes are downloaded only after user confirmation. */
data class CapturedResource(
    val url: String,
    val mimeType: String,
    val kind: String,
    val fileName: String
)
