package com.sih2026.touristsafety.presentation.screens.chatbot

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sih2026.touristsafety.data.remote.ActionButton

import com.sih2026.touristsafety.data.remote.ChatRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class ChatMessage(
    val id: String,
    val role: String,
    val content: String,
    val actionButtons: List<ActionButton>? = null,
    val imageUrls: List<String>? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@HiltViewModel
class ChatbotViewModel @Inject constructor() : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isTyping = MutableStateFlow(false)
    val isTyping: StateFlow<Boolean> = _isTyping.asStateFlow()

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    init {
        loadHistory()
    }

    fun updateInputText(text: String) {
        _inputText.value = text
    }

    private fun loadHistory() {
        // Dummy welcome message
        _messages.value = listOf(
            ChatMessage(
                id = UUID.randomUUID().toString(),
                role = "assistant",
                content = "Hello! I'm your AI travel assistant. I can help you with:\n" +
                        "• \uD83C\uDFDB\uFE0F Monument & tourist info\n" +
                        "• \uD83D\uDEE1\uFE0F Safety tips\n" +
                        "• \uD83D\uDDFA\uFE0F Navigation help\n" +
                        "• \uD83C\uDD98 Emergency guidance\n" +
                        "Ask me anything!"
            )
        )
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return

        val userMessage = ChatMessage(
            id = UUID.randomUUID().toString(),
            role = "user",
            content = text
        )

        _messages.value = _messages.value + userMessage
        _inputText.value = ""
        _isTyping.value = true

        viewModelScope.launch {
            try {
                val responseText = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    val prompt = "You are a helpful AI travel assistant for a tourist safety app. Help the user with monuments, safety tips, navigation, and emergency guidance. Keep answers concise. User says: $text"
                    
                    val textPart = org.json.JSONObject()
                    textPart.put("text", prompt)
                    
                    val partsArray = org.json.JSONArray()
                    partsArray.put(textPart)

                    val contentObj = org.json.JSONObject()
                    contentObj.put("parts", partsArray)
                    
                    val contentsArray = org.json.JSONArray()
                    contentsArray.put(contentObj)

                    val requestBodyJson = org.json.JSONObject()
                    requestBodyJson.put("contents", contentsArray)

                    com.sih2026.touristsafety.utils.GeminiApiKeys.executeWithRetry { apiKey ->
                        val urlStr = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.6-flash:generateContent?key=$apiKey"
                        val url = java.net.URL(urlStr)
                        val connection = url.openConnection() as java.net.HttpURLConnection
                        connection.requestMethod = "POST"
                        connection.setRequestProperty("Content-Type", "application/json")
                        connection.connectTimeout = 15000
                        connection.readTimeout = 30000
                        connection.doOutput = true
                        
                        connection.outputStream.use { os ->
                            val input = requestBodyJson.toString().toByteArray(Charsets.UTF_8)
                            os.write(input, 0, input.size)
                        }
                        
                        val responseCode = connection.responseCode
                        val responseString = if (responseCode in 200..299) {
                            connection.inputStream.bufferedReader().use { it.readText() }
                        } else {
                            connection.errorStream.bufferedReader().use { it.readText() }
                        }
                        
                        if (responseCode !in 200..299) {
                            throw Exception("API Error: $responseCode - $responseString")
                        }
                        
                        val responseJson = org.json.JSONObject(responseString)
                        responseJson.getJSONArray("candidates")
                            .getJSONObject(0)
                            .getJSONObject("content")
                            .getJSONArray("parts")
                            .getJSONObject(0)
                            .getString("text")
                    }
                }

                val assistantResponse = ChatMessage(
                    id = UUID.randomUUID().toString(),
                    role = "assistant",
                    content = responseText
                )
                _messages.value = _messages.value + assistantResponse

            } catch (e: Exception) {
                val errorMsg = ChatMessage(
                    id = UUID.randomUUID().toString(),
                    role = "assistant",
                    content = "Sorry, I'm having trouble connecting right now.\nError: ${e.message}"
                )
                _messages.value = _messages.value + errorMsg
            } finally {
                _isTyping.value = false
            }
        }
    }
}
