package com.dsw.util

import com.dsw.util.dto.IDServiceResponse
import com.google.gson.Gson
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Semaphore
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class AutoFillIdentifierQueue(private val bucket: String,private val type: String,private val format: String,private val okHttpClient:OkHttpClient, private val config:Map<String,String>):AutoFillQueue<String> {

    private val concurrentQueue= ConcurrentLinkedQueue<String>()
    private val mapper=Gson()
    private val isReplenishing=AtomicBoolean(false)
    private val batchSize=config["batchSize"]!!.toInt()
    private val lock=Semaphore(1)
    private val totalCount=AtomicInteger(0)

    private fun _getIdentifiersFromService(bucket: String, type: String, format: String, count: Int):IDServiceResponse
    {
        val url=config["host"]!!.let {
            "${config["scheme"]?:"http"}://${it.lowercase()}/id-service/${type.lowercase()}/$bucket"
        }

        val httpBuilder = url.toHttpUrl().newBuilder().addQueryParameter("count","$count").addQueryParameter("format",format)
        val request = Request.Builder().get()
            .url(httpBuilder.build())
            .build()

        return okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful)
                IDServiceResponse(id= emptyList())
            else
                mapper.fromJson(response.body!!.string(), IDServiceResponse::class.java)
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
                identifiers.id.forEach {
                    concurrentQueue.offer(it)
                    totalCount.addAndGet(1)
                }

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
           val mutableList= mutableListOf<String>()
           for(i in 0..count)
           {
               totalCount.decrementAndGet()
               mutableList.add(concurrentQueue.poll())
           }
           mutableList
       }
       else {
           if(count<batchSize)
            {
                replenish()
                totalCount.set(totalCount.get()-batchSize)
            }
            else
            {
                replenish(count)
                totalCount.set(totalCount.get()-count)
            }

           val mutableList= mutableListOf<String>()
           for(i in 1..count)
           {
               totalCount.decrementAndGet()
               mutableList.add(concurrentQueue.poll())
           }
           mutableList
       }
    }
}