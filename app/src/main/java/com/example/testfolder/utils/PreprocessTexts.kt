package com.example.testfolder.utils

import opennlp.tools.stemmer.PorterStemmer
import opennlp.tools.tokenize.SimpleTokenizer
//import smile.nlp.stemmer.PorterStemmer
//import smile.nlp.tokenizer.SimpleTokenizer
import java.util.StringTokenizer

//fun main(){
//    val test_diary = "I like an apple. He likes apples."
//    val numOfTokens = PreprocessTexts.get_num_of_tokens(test_diary)
//    println(numOfTokens/5)
//}

class PreprocessTexts {
    companion object {
        fun getNumOfTokens(corpus: String): Int {
            val myStringTokenizer = StringTokenizer(corpus)
            // Step 3: Lowercase the text
            val lowercasedTexts = myStringTokenizer.toList().map {it.toString().lowercase()}
//            println(lowercasedTexts)

            // Step 4: Remove punctuation
            val noPunctuationTexts = lowercasedTexts.map { it.replace(Regex("[^\\w\\s]"), "") }
//            println(noPunctuationTexts)
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
//            println(processedTexts)
//            println(processedTexts.size)

            return processedTexts.size
        }
    }

}