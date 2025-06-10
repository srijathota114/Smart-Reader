package com.example.smartreader

import android.content.Context
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions

class TranslationManager(private val context: Context) {
    private val translationMap = mapOf(
        "English" to TranslateLanguage.ENGLISH,
        "Spanish" to TranslateLanguage.SPANISH,
        "French" to TranslateLanguage.FRENCH,
        "German" to TranslateLanguage.GERMAN,
        "Hindi" to TranslateLanguage.HINDI,
        "Chinese" to TranslateLanguage.CHINESE,
        "Telugu" to TranslateLanguage.TELUGU
    )

    private var translators = mutableMapOf<String, Translator>()

    init {
        // Preload Telugu translator
        getOrCreateTranslator(TranslateLanguage.TELUGU)
    }

    private fun getOrCreateTranslator(targetLanguage: String): Translator {
        return translators.getOrPut(targetLanguage) {
            Translation.getClient(
                TranslatorOptions.Builder()
                    .setSourceLanguage(TranslateLanguage.ENGLISH)
                    .setTargetLanguage(targetLanguage)
                    .build()
            ).also { translator ->
                // Download model in background
                downloadModelIfNeeded(translator, targetLanguage)
            }
        }
    }

    private fun downloadModelIfNeeded(translator: Translator, targetLanguage: String) {
        translator.downloadModelIfNeeded()
            .addOnFailureListener { _ ->
                // Handle the error silently but remove from cache
                translators.remove(targetLanguage)
            }
    }

    fun translateText(text: String, targetLanguage: String, onResult: (String) -> Unit, onError: (Exception) -> Unit) {
        val translator = getOrCreateTranslator(targetLanguage)

        translator.translate(text)
            .addOnSuccessListener { translatedText ->
                onResult(translatedText)
            }
            .addOnFailureListener { exception ->
                // If translation fails, try recreating the translator
                translators.remove(targetLanguage)
                onError(exception)
            }
    }

    fun getAvailableLanguages(): List<String> {
        return translationMap.keys.toList()
    }

    fun getLanguageCode(language: String): String {
        return translationMap[language] ?: TranslateLanguage.ENGLISH
    }

    fun cleanup() {
        translators.values.forEach { it.close() }
        translators.clear()
    }
} 