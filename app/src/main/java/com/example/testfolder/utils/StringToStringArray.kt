package com.example.testfolder.utils

fun main() {
    // Example String looking like an string array
    var jsonString = "[\"8 am\",\"9 am\",\"10 am\",\"11 am\"]"

    // Convert string looking like an string array to string array
    jsonString = jsonString.replace("[", "")
    jsonString = jsonString.replace("]", "")
    val stringArray = jsonString.split(",").map { it.trim() }
    stringArray.forEach { str ->
        println(str)
    }
}

class StringToStringArray {

}