/*
 * Copyright (c) 2021 by Titan Project Contributors.. All rights reserved.
 */

package io.titandata.titan.utils

import java.io.IOException
import java.util.concurrent.TimeUnit
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.ResponseBody
import org.json.JSONArray
import org.json.JSONObject

class HttpHandler(private val timeout: Long = 60, private val timeoutUnit: TimeUnit = TimeUnit.MINUTES) {

    private fun call(request: Request): ResponseBody {
        val caller = OkHttpClient.Builder()
                .readTimeout(timeout, timeoutUnit)
                .build()
        val response = caller.newCall(request).execute()
        if (!response.isSuccessful) {
            throw IOException("Unexpected Code: $response")
        }
        return response.body!!
    }

    fun get(url: String): ResponseBody {
        val request = Request.Builder()
                .url(url)
                .build()
        return call(request)
    }

    fun post(url: String, data: Map<String, Any>): ResponseBody {
        val json = "application/json; charset=utf-8".toMediaTypeOrNull()
        val requestBody = RequestBody.create(json, JSONObject(data).toString())
        val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()
        return call(request)
    }

    companion object {
        fun ResponseBody.asString(): String {
            return this.string()
        }

        fun ResponseBody.asBytes(): ByteArray {
            return this.bytes()
        }

        fun ResponseBody.asJsonArray(): JSONArray {
            return JSONArray(this.asString())
        }

        fun ResponseBody.asJsonObject(): JSONObject {
            return JSONObject(this.asString())
        }
    }
}
