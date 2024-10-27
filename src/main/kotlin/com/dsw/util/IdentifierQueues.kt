package com.dsw.util

import okhttp3.OkHttpClient

interface IdentifierQueues<AutoFillQueue> {

    fun createQueue(key: String, httpClient: OkHttpClient, config:Map<String,String>, bucket: String, type: String, format: String)
    fun getIdentifiers(key:String,count:Int):List<*>
    fun isQueueAvailable(key:String):Boolean
    fun getQueue(key:String): AutoFillQueue?
}