package com.example.travelbuddy

import com.example.travelbuddy.AppwriteClientManager
import com.google.gson.Gson
import com.google.gson.JsonObject
import io.appwrite.Client
import io.appwrite.exceptions.AppwriteException
import io.appwrite.services.Account
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Response
import java.io.IOException

class AppwriteUserHelper {
    private val client: Client = AppwriteClientManager.getClient()
    private val account: Account = AppwriteClientManager.getAccount()
    private var userId: String? = null

    init {
        CoroutineScope(Dispatchers.IO).launch {
            userId = fetchUserId()
        }
    }

    private suspend fun fetchUserId(): String? {
        return withContext(Dispatchers.IO) {
            try {
                val account = AppwriteClientManager.getAccount()
                val user = account.get()
                user.id
            } catch (e: AppwriteException) {
                e.printStackTrace()
                null
            }
        }
    }

    fun getUserId(): String? {
        while (userId == null) {
            try {
                Thread.sleep(100)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }
        return userId
    }
}
