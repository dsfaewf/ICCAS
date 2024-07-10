package com.example.testfolder.utils

import opennlp.tools.stemmer.PorterStemmer
import opennlp.tools.tokenize.SimpleTokenizer
//import smile.nlp.stemmer.PorterStemmer
//import smile.nlp.tokenizer.SimpleTokenizer
import java.util.StringTokenizer

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

        fun stringToStringArray(stringLookingLikeArray: String): List<String> {
            // Convert string looking like a string array to real string array
            var _stringLookingLikeArray = stringLookingLikeArray
            _stringLookingLikeArray = _stringLookingLikeArray.replace("[", "")
            _stringLookingLikeArray = _stringLookingLikeArray.replace("]", "")
            val stringArray = _stringLookingLikeArray.split(",").map { it.trim() }
            return stringArray
        }
    }

}