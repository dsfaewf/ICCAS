package com.example.testfolder.utils

import opennlp.tools.stemmer.PorterStemmer
import opennlp.tools.tokenize.SimpleTokenizer
//import smile.nlp.stemmer.PorterStemmer
//import smile.nlp.tokenizer.SimpleTokenizer
import java.util.StringTokenizer
import kotlin.math.pow
import kotlin.math.sqrt

//fun getCharLocationInKeyboard(): MutableMap<Char, Pair<Int, Int>>{
//    val charToLocation = mutableMapOf<Char, Pair<Int, Int>>()
//    val keyboardLayout = listOf(
//        "1234567890",
//        "QWERTYUIOP",
//        "ASDFGHJKL",
//        "ZXCVBNM"
//    )
//    var row: Int
//    var col: Int
//    val rowStart = arrayOf<Int>(0, 2, 4, 6)
//    val colStart = arrayOf<Int>(0, 0, 1, 3)
//    for(i in 0..keyboardLayout.count()-1) {
//        row = rowStart.get(i)
//        col = colStart.get(i)
//        keyboardLayout.get(i).forEach {
//            charToLocation[it] = Pair(row, col)
//            col+=2
//        }
//    }
//    return charToLocation
//}
//
//fun euclideanDistance(x1: Double, y1: Double, x2: Double, y2: Double): Double {
//    return sqrt((x2 - x1).pow(2) + (y2 - y1).pow(2))
//}
//
//fun bothAreTheSame(str1: String, str2: String): Boolean {
//    // both of str1 and str2 are uppercased
//    var distanceSum = 0.0
//    var char1: Char
//    var char2: Char
//    var charLocation1: Pair<Int, Int>
//    var charLocation2: Pair<Int, Int>
//    val charLocationInKeyboard = getCharLocationInKeyboard()
//    val patienceDistance: Double
//    val sqrt2: Double = sqrt(2.0)
//
//    for(i in 0..str1.length-1){
//        charLocation1 = charLocationInKeyboard[str1.get(i)]!!
//        charLocation2 = charLocationInKeyboard[str2.get(i)]!!
//        distanceSum += euclideanDistance(charLocation1.first.toDouble(), charLocation1.second.toDouble(),
//            charLocation2.first.toDouble(), charLocation2.second.toDouble())
//    }
//    patienceDistance = str2.length/2*sqrt2
//    if(distanceSum <= patienceDistance) {
//        return true
//    } else {
//        return false
//    }
//}
//
//fun isCorrectAnswer(_inputStr: String, _answerStr: String): Boolean {
//    // Make every char to uppercase first
//    val inputStr = _inputStr.uppercase()
//    val answerStr = _answerStr.uppercase()
//    if (inputStr.length == answerStr.length) {
//        return bothAreTheSame(inputStr, answerStr)
//    } else {
//        var str1: String // short
//        var str2: String // long
//        if (inputStr.length < answerStr.length) {
//            str1 = inputStr
//            str2 = answerStr
//        } else {
//            str1 = answerStr
//            str2 = inputStr
//        }
//        val max_lendiff_tolerated = str2.length / 3
//        println("max_lendiff_tolerated: $max_lendiff_tolerated")
//        for (i in 0..max_lendiff_tolerated) {
//            // when length diff is 1
//            if (str2.length - str1.length == i) {
//                for (j in 0..i) {
//                    val k = i - j
//                    // Make their length same
//                    val bothAreTheSame =
//                        bothAreTheSame(str1, str2.substring(j, str2.length - k))
////                    val __str2 = str2.substring(j, str2.length - k)
////                    println("$str1, $__str2")
//                    if (bothAreTheSame) {
//                        return true
//                    } else {
//                        return false
//                    }
//                }
//            }
//        }
//        return false
//    }
//}
//
//fun main() {
//    val str1 = "abcdef"
//    val str2 = "abcd"
//    isCorrectAnswer(str1, str2)
//}

//fun main() {
//    // Create a 5x19 array with all elements initialized to 0
//    val arr = Array(7) { IntArray(19) }
//    val uppercaseLetters = ('A'..'Z').toList()
//    val numbers = listOf('1','2','3','4','5','6','7','8','9','0')
//    val allLetters = uppercaseLetters + numbers
//    val charToLocation = getCharLocationInKeyboard()
//
//    allLetters.forEach {
//        arr[charToLocation[it]!!.first][charToLocation[it]!!.second] = 1
//    }
//
//    // Print the array
//    for (row in arr) {
//        println(row.joinToString(" "))
//    }
//}

class PreprocessTexts {
    companion object {
        fun getNumOfTokens(corpus: String): Int {
            val myStringTokenizer = StringTokenizer(corpus)
            // Step 3: Lowercase the text
            val lowercasedTexts = myStringTokenizer.toList().map {it.toString().lowercase()}

            // Step 4: Remove punctuation
            val noPunctuationTexts = lowercasedTexts.map { it.replace(Regex("[^\\w\\s]"), "") }

            // Initialize tokenizer, stemmer, and stop words
            val tokenizer = SimpleTokenizer.INSTANCE
            val stemmer = PorterStemmer()
            val stopWords = setOf("is", "the", "this", "and")

            // Step 5, 6, and 7: Tokenization, remove stop words, and apply stemming
            var processedTexts = noPunctuationTexts.map { text ->
                tokenizer.tokenize(text)
                    .filter { it !in stopWords }
                    .map { stemmer.stem(it) }
                    .joinToString(" ")
            }
            processedTexts = processedTexts.toSet().toList()

            return processedTexts.size
        }

        fun getCharLocationInKeyboard(): MutableMap<Char, Pair<Int, Int>>{
            val charToLocation = mutableMapOf<Char, Pair<Int, Int>>()
            val keyboardLayout = listOf(
                "1234567890",
                "QWERTYUIOP",
                "ASDFGHJKL",
                "ZXCVBNM"
            )
            var row: Int
            var col: Int
            val rowStart = arrayOf<Int>(0, 2, 4, 6)
            val colStart = arrayOf<Int>(0, 0, 1, 3)
            for(i in 0..keyboardLayout.count()-1) {
                row = rowStart.get(i)
                col = colStart.get(i)
                keyboardLayout.get(i).forEach {
                    charToLocation[it] = Pair(row, col)
                    col+=2
                }
            }
            return charToLocation
        }

        fun euclideanDistance(x1: Double, y1: Double, x2: Double, y2: Double): Double {
            return sqrt((x2 - x1).pow(2) + (y2 - y1).pow(2))
        }

        fun bothAreTheSame(str1: String, str2: String): Boolean {
            // both of str1 and str2 are uppercased
            var distanceSum = 0.0
            var charLocation1: Pair<Int, Int>
            var charLocation2: Pair<Int, Int>
            val charLocationInKeyboard = getCharLocationInKeyboard()
            val patienceDistance: Double
            val sqrt5: Double = sqrt(5.0)

            for(i in 0..str1.length-1){
                charLocation1 = charLocationInKeyboard[str1.get(i)]!!
                charLocation2 = charLocationInKeyboard[str2.get(i)]!!
                distanceSum += euclideanDistance(charLocation1.first.toDouble(), charLocation1.second.toDouble(),
                    charLocation2.first.toDouble(), charLocation2.second.toDouble())
            }
            patienceDistance = str2.length/2*sqrt5
            if(distanceSum <= patienceDistance) {
                return true
            } else {
                return false
            }
        }

        fun isCorrectAnswer(_inputStr: String, _answerStr: String): Boolean {
            // Make every char to uppercase first
            val inputStr = _inputStr.uppercase()
            val answerStr = _answerStr.uppercase()
            if (inputStr.length == answerStr.length) {
                return bothAreTheSame(inputStr, answerStr)
            } else {
                val str1: String // short
                val str2: String // long
                if (inputStr.length < answerStr.length) {
                    str1 = inputStr
                    str2 = answerStr
                } else {
                    str1 = answerStr
                    str2 = inputStr
                }
                val max_lendiff_tolerated = str2.length / 3
//                println("max_lendiff_tolerated: $max_lendiff_tolerated")
                for (i in 0..max_lendiff_tolerated) {
                    // when length diff is 1
                    if (str2.length - str1.length == i) {
                        for (j in 0..i) {
                            val k = i - j
                            // Make their length same
                            return bothAreTheSame(str1, str2.substring(j, str2.length - k))
                        }
                    }
                }
                return false
            }
        }

        fun stringToStringArray(stringLookingLikeArray: String): List<String> {
            // Convert string looking like a string array to real string array
            var _stringLookingLikeArray = stringLookingLikeArray
            _stringLookingLikeArray = _stringLookingLikeArray.replace("[", "")
            _stringLookingLikeArray = _stringLookingLikeArray.replace("]", "")
            _stringLookingLikeArray = _stringLookingLikeArray.replace("\"", "") // 자꾸 오류나서 따옴표 제거 추가합니다-우석
            val stringArray = _stringLookingLikeArray.split(",").map { it.trim() }
            return stringArray
        }
    }

}