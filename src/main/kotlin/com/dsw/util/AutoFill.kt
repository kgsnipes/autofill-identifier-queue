package com.dsw.util

interface AutoFill {
    fun replenish()
    fun replenish(count:Int)
    fun isReplenishing():Boolean
}