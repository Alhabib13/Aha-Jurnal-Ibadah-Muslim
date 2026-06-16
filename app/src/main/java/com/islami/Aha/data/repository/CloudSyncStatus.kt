package com.islami.Aha.data.repository

data class CloudSyncStatus(
    val changedCount: Int = 0,
    val userMessage: String? = null,
    val shouldRetry: Boolean = false
) {
    val hasIssue: Boolean
        get() = !userMessage.isNullOrBlank()

    companion object {
        fun success(changedCount: Int = 0): CloudSyncStatus = CloudSyncStatus(changedCount = changedCount)

        fun failure(
            message: String,
            shouldRetry: Boolean = false,
            changedCount: Int = 0
        ): CloudSyncStatus {
            return CloudSyncStatus(
                changedCount = changedCount,
                userMessage = message,
                shouldRetry = shouldRetry
            )
        }
    }
}
