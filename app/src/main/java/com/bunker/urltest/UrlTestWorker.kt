package com.bunker.urltest

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UrlTestWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val targets = UrlChecker.targets(applicationContext)
        val results = targets.map { UrlChecker.check(it) }
        UrlTestWidget.render(applicationContext, results)
        Result.success()
    }
}
