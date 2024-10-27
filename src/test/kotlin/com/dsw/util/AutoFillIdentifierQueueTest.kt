package com.dsw.util

import okhttp3.OkHttpClient
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class AutoFillIdentifierQueueTest {

    @Test
    fun `testing auto filling of identifiers`()
    {
        val config= mapOf(Pair("host","localhost:9000"),Pair("scheme","http"),Pair("batchSize","1000"))
        val autoFill=AutoFillIdentifierQueue(config = config, okHttpClient = OkHttpClient(), bucket = "testing_bucket_${System.currentTimeMillis()}", type = "id", format = "none")
        val firstVal=autoFill.getIdentifiers(1).get(0).toInt()
        Assertions.assertEquals(firstVal,1)
    }

    @Test
    fun `testing auto filling of identifiers with internal replenishment`()
    {
        val config= mapOf(Pair("host","localhost:9000"),Pair("scheme","http"),Pair("batchSize","1000"))
        val autoFill=AutoFillIdentifierQueue(config = config, okHttpClient = OkHttpClient(), bucket = "testing_bucket_${System.currentTimeMillis()}", type = "id", format = "none")
        val firstResult=autoFill.getIdentifiers(1000)
        val resultToCompare=autoFill.getIdentifiers(1000)
        Assertions.assertEquals(2000,resultToCompare.get(999).toInt())
    }

    @Test
    fun `testing auto filling of identifiers with internal replenishment with 3 calls`()
    {
        val config= mapOf(Pair("host","localhost:9000"),Pair("scheme","http"),Pair("batchSize","1000"))
        val autoFill=AutoFillIdentifierQueue(config = config, okHttpClient = OkHttpClient(), bucket = "testing_bucket_${System.currentTimeMillis()}", type = "id", format = "none")
        autoFill.getIdentifiers(1000)
        autoFill.getIdentifiers(1000)
        val resultToCompare=autoFill.getIdentifiers(1000)
        Assertions.assertEquals(3000,resultToCompare.get(999).toInt())
    }
}