package com.dsw.util

import com.dsw.util.dto.IDServiceResponse
import com.fasterxml.jackson.databind.ObjectMapper
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Semaphore
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class AutoFillIdentifierQueue(private val bucket: String,private val type: String,private val format: String,private val okHttpClient:OkHttpClient, private val config:Map<String,String>):AutoFill,IdentifierQueue<String> {

    private val concurrentQueue=ConcurrentLinkedQueue<String>()
    private val mapper=ObjectMapper()
    private val isReplenishing=AtomicBoolean(false)
    private val batchSize=config["batchSize"]!!.toInt()
    private val lock=Semaphore(1)
    private val totalCount=AtomicInteger(0)

    private fun _getIdentifiersFromService(bucket: String, type: String, format: String, count: Int):IDServiceResponse
    {
        var configUrl=config["url"]!!.also {
            if(!it.endsWith("/")) "${it}/"
        }
        var url="${configUrl}$bucket"
        val httpBuilder = url.toHttpUrl().newBuilder().addQueryParameter("count","$count").addQueryParameter("format",format)
        val request = Request.Builder().get()
            .url(httpBuilder.build())
            .build()

        return okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful)
                IDServiceResponse(id= emptyList())
            else
                mapper.readValue(response.body!!.string(), IDServiceResponse::class.java)
        }
    }

    override fun replenish() {
       replenish(batchSize)
    }

    override fun replenish(count: Int) {
        try {
            lock.acquire(1)
            isReplenishing.set(true)
            val identifiers= _getIdentifiersFromService(bucket,type, format, count)
            if(identifiers.id.isNotEmpty())
            {
                concurrentQueue.addAll(identifiers.id)
                totalCount.addAndGet(count)
            }
        }
        finally {
            lock.release(1)
            isReplenishing.set(false)
        }
    }

    override fun isReplenishing(): Boolean {
      return isReplenishing.get()
    }

    override fun getIdentifiers(count: Int): List<String> {
       return if(concurrentQueue.isNotEmpty() && count<totalCount.get()) {
           totalCount.set(totalCount.get()-count)
           concurrentQueue.take(count).toList()
       }
       else {
           if(count<batchSize)
            {
                replenish()
            }
            else
            {
                replenish(count)
            }
           concurrentQueue.take(count).toList()
       }
    }
}