package com.dsw.util

interface IdentifierQueue<T> {
    fun getIdentifiers(count:Int):List<T>
}