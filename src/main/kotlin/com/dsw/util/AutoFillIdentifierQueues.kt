package com.dsw.util

import okhttp3.OkHttpClient
import java.util.concurrent.ConcurrentHashMap

class AutoFillIdentifierQueues:IdentifierQueues<AutoFillQueue<String>> {

    companion object
    {
        private val queueMap=ConcurrentHashMap<String,AutoFillQueue<String>>()
    }

    override fun createQueue(key: String,httpClient: OkHttpClient,config:Map<String,String>, bucket: String, type: String, format: String) {
        if(!queueMap.contains(key))
        {
            queueMap[key]=AutoFillIdentifierQueue(bucket,type,format,httpClient,config)
        }
        else
        {
            throw IdentifierQueueException("Queue Already Exists!!")
        }
    }

    override fun getIdentifiers(key: String,count:Int): List<String>{
        return queueMap[key]!!.getIdentifiers(count)
    }

    override fun isQueueAvailable(key: String):Boolean {
        return queueMap.contains(key)
    }

    override fun getQueue(key: String): AutoFillQueue<String>? {
        return queueMap[key]
    }
}