package com.aj.udharbook.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.aj.udharbook.database.AppDatabase

class CloudSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val database = AppDatabase.getDatabase(applicationContext)
        val syncManager = FirestoreSyncManager(
            customerDao = database.customerDao(),
            transactionDao = database.transactionDao()
        )

        val isSignedIn = syncManager.isUserSignedIn()
        if (!isSignedIn) {
            return Result.success()
        }

        return try {
            syncManager.syncLocalToCloud()
            Result.success()
        } catch (e: Exception) {
            if (SyncRetryPolicy.shouldRetry(isSignedIn, syncSucceeded = false)) {
                if (runAttemptCount >= 5) Result.failure() else Result.retry()
            } else {
                Result.success()
            }
        }
    }
}
