package com.example.craftcart

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.Firebase
import com.google.firebase.ai.Chat
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import com.google.firebase.ai.type.thinkingConfig
import java.util.concurrent.TimeoutException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

class ChatbotActivity : AppCompatActivity() {

    private lateinit var chatScrollView: ScrollView
    private lateinit var chatContainer: LinearLayout
    private lateinit var messageInput: EditText
    private lateinit var sendButton: TextView

    private val conversationHistory = mutableListOf<Content>()
    private var hasShownFallbackNotice = false
    private var isGenerating = false

    companion object {
        private const val TAG = "CraftyAI"
        private const val PRIMARY_MODEL = "gemini-2.5-flash"
        private const val FALLBACK_MODEL = "gemini-2.0-flash"
    }

    private val systemPrompt = """
        You are Crafty, the friendly, creative, and strictly accurate AI shopping assistant for CraftCart, an Indian handmade crafts marketplace.

        EXACT PRODUCT CATALOG AND OFFICIAL PRICES (DO NOT INVENT PRODUCTS OR PRICES):
        1. "Miffy Keychain" - ₹349 (Category: Keychains / Acrylic Keychains). Cute handmade character keychain, great budget gift.
        2. "Cute Christmas" - ₹399 (Category: Seasonal Art). Festive handmade holiday craft.
        3. "Tasty Little Things" - ₹449 (Category: Canvas Painting / Food Art). Hand-painted miniature food frame.
        4. "Meow Christmas" - ₹449 (Category: Seasonal Art). Cute cat holiday painting.
        5. "Flower Planters" - ₹449 (Category: Planters / Home Decor). Handcrafted ceramic planters.
        6. "Puppy Portrait" - ₹499 (Category: Canvas Painting). Detailed puppy art canvas.
        7. "Puppy Dreams" - ₹499 (Category: Canvas Painting). Whimsical puppy illustration canvas.
        8. "Garden Portrait" - ₹499 (Category: Handmade Art). Botanical floral nature painting.
        9. "Clay House" - ₹599 (Category: Home Decor / Clay Art). Miniature ceramic cottage sculpture.

        STRICT GUIDELINES:
        1. BUDGET STRICTNESS: If the user states a budget (e.g., "under ₹500", "below 400", "budget 450"), NEVER recommend any item that exceeds their limit. For instance, if the budget is under ₹500, "Clay House" (₹599) MUST NOT be suggested. Instead recommend items strictly within the budget (e.g., Puppy Portrait at ₹499, Tasty Little Things at ₹449, or Miffy Keychain at ₹349).
        2. CATEGORY SPECIFICITY:
           - For clay art / pottery: recommend Clay House (₹599) and Flower Planters (₹449).
           - For keychains: recommend Miffy Keychain (₹349).
           - For canvas paintings: recommend Puppy Portrait (₹499), Puppy Dreams (₹499), or Tasty Little Things (₹449).
        3. STORE INFO & CAMPUS VISITS:
           - Campus stall is at Fr. Conceicao Rodrigues Institute of Technology (FCRIT), Sector 9A, Vashi, Navi Mumbai. Users can navigate using the in-app Campus Map.
           - Orders take 3–5 business days for delivery, with offline-safe checkout.
        4. TONE & FORMAT: Concise, cheerful, warm, and helpful. Use clear bullet points and emojis. Never mention products from outside CraftCart.
    """.trimIndent()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_chatbot)

        val backButton = findViewById<TextView>(R.id.backButton)
        backButton?.setOnClickListener {
            finish()
        }

        chatScrollView = findViewById(R.id.chatScrollView)
        chatContainer = findViewById(R.id.chatContainer)
        messageInput = findViewById(R.id.messageInput)
        sendButton = findViewById(R.id.sendButton)

        // Quick action option buttons
        findViewById<Button>(R.id.giftButton)?.setOnClickListener {
            handleUserMessage("I need help choosing a gift 🎁")
        }

        findViewById<Button>(R.id.productHelpButton)?.setOnClickListener {
            handleUserMessage("Help me choose a product 🛍")
        }

        findViewById<Button>(R.id.orderHelpButton)?.setOnClickListener {
            handleUserMessage("Help with my orders 📦")
        }

        sendButton.setOnClickListener {
            val text = messageInput.text.toString().trim()
            if (text.isNotEmpty()) {
                handleUserMessage(text)
                messageInput.setText("")
            }
        }

        messageInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND || actionId == EditorInfo.IME_ACTION_DONE) {
                val text = messageInput.text.toString().trim()
                if (text.isNotEmpty()) {
                    handleUserMessage(text)
                    messageInput.setText("")
                }
                true
            } else {
                false
            }
        }
    }

    private fun getGenerativeModel(modelName: String): GenerativeModel {
        return Firebase.ai.generativeModel(
            modelName = modelName,
            systemInstruction = content {
                text(systemPrompt)
            },
            generationConfig = generationConfig {
                temperature = 0.5f
                topP = 0.9f
                maxOutputTokens = 1024
                if (modelName == PRIMARY_MODEL) {
                    thinkingConfig = thinkingConfig {
                        thinkingBudget = 0
                    }
                }
            }
        )
    }

    private fun isModelNotFoundError(e: Throwable): Boolean {
        val msg = (e.message ?: "").lowercase()
        val causeMsg = (e.cause?.message ?: "").lowercase()
        val className = e.javaClass.name.lowercase()
        return msg.contains("404") || msg.contains("not_found") || msg.contains("not found") ||
                causeMsg.contains("404") || causeMsg.contains("not_found") || causeMsg.contains("not found") ||
                className.contains("notfound")
    }

    private fun recordHistory(userMessage: String, botMessage: String) {
        conversationHistory.add(content("user") { text(userMessage) })
        conversationHistory.add(content("model") { text(botMessage) })
        while (conversationHistory.size > 6) {
            conversationHistory.removeAt(0)
        }
    }

    private suspend fun streamGeminiResponse(
        userText: String,
        modelName: String,
        botTextView: TextView
    ): String = withContext(Dispatchers.IO) {
        val model = getGenerativeModel(modelName)
        val chat = model.startChat(conversationHistory.toList())
        val flow = chat.sendMessageStream(userText)

        val accumulated = StringBuilder()
        var firstTokenReceived = false

        withTimeout(30000L) {
            coroutineScope {
                val firstTokenTimer = launch {
                    delay(10000L)
                    if (!firstTokenReceived) {
                        throw TimeoutException("Time-to-first-token exceeded 10s for model $modelName")
                    }
                }
                try {
                    flow.collect { chunk ->
                        val chunkText = chunk.text ?: ""
                        if (chunkText.isNotEmpty()) {
                            if (!firstTokenReceived) {
                                firstTokenReceived = true
                                firstTokenTimer.cancel()
                            }
                            accumulated.append(chunkText)
                            val cleaned = cleanMarkdown(accumulated.toString())
                            withContext(Dispatchers.Main) {
                                botTextView.setTextColor(Color.parseColor("#555555"))
                                botTextView.text = cleaned
                                scrollToBottom()
                            }
                        }
                    }
                } finally {
                    firstTokenTimer.cancel()
                }
            }
        }
        accumulated.toString().trim()
    }

    private fun handleUserMessage(userText: String) {
        val trimmed = userText.trim()
        if (trimmed.isEmpty() || isGenerating) return

        addUserBubble(trimmed)
        scrollToBottom()

        isGenerating = true
        sendButton.isEnabled = false
        sendButton.alpha = 0.5f

        lifecycleScope.launch {
            var streamingRow: LinearLayout? = null
            try {
                val (row, bubble) = createBotStreamingBubble()
                streamingRow = row
                scrollToBottom()

                var responseText: String? = null
                var activeModel = PRIMARY_MODEL

                // Attempt 1: Primary Model
                try {
                    responseText = streamGeminiResponse(trimmed, activeModel, bubble)
                } catch (e: Exception) {
                    if (e is CancellationException && e !is TimeoutCancellationException) throw e
                    Log.e(TAG, "Primary model ($activeModel) attempt failed: ${e.message}", e)
                    if (isModelNotFoundError(e)) {
                        Log.w(TAG, "Model $activeModel not found or unsupported. Switching to fallback model $FALLBACK_MODEL.")
                        activeModel = FALLBACK_MODEL
                    }
                }

                // Attempt 2: Retry once before falling back to local response
                if (responseText.isNullOrEmpty()) {
                    try {
                        withContext(Dispatchers.Main) {
                            bubble.text = "Crafty is typing..."
                            bubble.setTextColor(Color.parseColor("#888888"))
                        }
                        responseText = streamGeminiResponse(trimmed, activeModel, bubble)
                    } catch (e: Exception) {
                        if (e is CancellationException && e !is TimeoutCancellationException) throw e
                        Log.e(TAG, "Retry attempt with $activeModel failed: ${e.message}", e)
                    }
                }

                if (!responseText.isNullOrEmpty()) {
                    withContext(Dispatchers.Main) {
                        bubble.text = cleanMarkdown(responseText)
                        scrollToBottom()
                    }
                    recordHistory(trimmed, responseText)
                } else {
                    // Real failure occurred after retry -> fall back to local answer
                    if (streamingRow != null) {
                        chatContainer.removeView(streamingRow)
                        streamingRow = null
                    }
                    val localReply = generateBotReply(trimmed)
                    val fullReply = if (!hasShownFallbackNotice) {
                        hasShownFallbackNotice = true
                        "$localReply\n\n(I'm currently replying offline. Please try again in a moment! 💗)"
                    } else {
                        localReply
                    }
                    addBotBubble(fullReply)
                    recordHistory(trimmed, fullReply)
                }
            } catch (e: Exception) {
                if (e is CancellationException && e !is TimeoutCancellationException) throw e
                Log.e(TAG, "Unexpected error handling message: ${e.message}", e)
                if (streamingRow != null) {
                    chatContainer.removeView(streamingRow)
                }
                val localReply = generateBotReply(trimmed)
                val fullReply = if (!hasShownFallbackNotice) {
                    hasShownFallbackNotice = true
                    "$localReply\n\n(I'm currently replying offline. Please try again in a moment! 💗)"
                } else {
                    localReply
                }
                addBotBubble(fullReply)
                recordHistory(trimmed, fullReply)
            } finally {
                isGenerating = false
                sendButton.isEnabled = true
                sendButton.alpha = 1.0f
                scrollToBottom()
            }
        }
    }

    private fun addUserBubble(message: String) {
        val row = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dpToPx(14)
            }
            gravity = Gravity.END
        }

        val bubble = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            maxWidth = dpToPx(260)
            setBackgroundColor(Color.parseColor("#B56576"))
            setPadding(dpToPx(12), dpToPx(10), dpToPx(12), dpToPx(10))
            text = message
            setTextColor(Color.WHITE)
            textSize = 12f
        }

        row.addView(bubble)
        chatContainer.addView(row)
    }

    private fun cleanMarkdown(text: String): String {
        return text
            // Remove code block wrappers
            .replace(Regex("```[a-zA-Z]*\\n?"), "")
            .replace("```", "")
            // Convert markdown headers (e.g. ### Heading) to plain text
            .replace(Regex("(?m)^#{1,6}\\s*"), "")
            // Convert list item asterisks/hyphens to bullet points
            .replace(Regex("(?m)^[*-]\\s+"), "• ")
            // Strip bold and italic formatting
            .replace(Regex("\\*{3}(.*?)\\*{3}"), "$1")
            .replace(Regex("\\*{2}(.*?)\\*{2}"), "$1")
            .replace(Regex("_{2}(.*?)_{2}"), "$1")
            .replace(Regex("\\*(.*?)\\*"), "$1")
            .replace(Regex("_(.*?)_"), "$1")
            // Strip inline code backticks
            .replace(Regex("`([^`]+)`"), "$1")
            // Clean up any remaining stray markdown characters
            .replace("*", "")
            .replace("`", "")
            .trim()
    }

    private fun addBotBubble(message: String) {
        val cleaned = cleanMarkdown(message)
        val row = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dpToPx(10)
            }
            gravity = Gravity.START
            orientation = LinearLayout.HORIZONTAL
        }

        val avatar = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(dpToPx(32), dpToPx(32))
            setBackgroundColor(Color.parseColor("#B56576"))
            gravity = Gravity.CENTER
            text = "✦"
            setTextColor(Color.WHITE)
            textSize = 15f
        }

        val bubble = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginStart = dpToPx(8)
            }
            maxWidth = dpToPx(250)
            setBackgroundColor(Color.WHITE)
            setPadding(dpToPx(12), dpToPx(10), dpToPx(12), dpToPx(10))
            text = cleaned
            setTextColor(Color.parseColor("#555555"))
            textSize = 12f
        }

        row.addView(avatar)
        row.addView(bubble)
        chatContainer.addView(row)
    }

    private fun createBotStreamingBubble(): Pair<LinearLayout, TextView> {
        val row = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dpToPx(10)
            }
            gravity = Gravity.START
            orientation = LinearLayout.HORIZONTAL
        }

        val avatar = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(dpToPx(32), dpToPx(32))
            setBackgroundColor(Color.parseColor("#B56576"))
            gravity = Gravity.CENTER
            text = "✦"
            setTextColor(Color.WHITE)
            textSize = 15f
        }

        val bubble = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginStart = dpToPx(8)
            }
            maxWidth = dpToPx(250)
            setBackgroundColor(Color.WHITE)
            setPadding(dpToPx(12), dpToPx(10), dpToPx(12), dpToPx(10))
            text = "Crafty is typing..."
            setTextColor(Color.parseColor("#888888"))
            textSize = 12f
        }

        row.addView(avatar)
        row.addView(bubble)
        chatContainer.addView(row)
        return Pair(row, bubble)
    }

    private fun generateBotReply(query: String): String {
        val q = query.lowercase().trim()

        // 1. Specific budget parsing (e.g., "under 500", "under ₹500", "below 500")
        if (q.contains("under 500") || q.contains("under ₹500") || q.contains("below 500") || q.contains("less than 500")) {
            return "Here are wonderful handmade gifts under ₹500! 🎁\n" +
                    "• Puppy Portrait canvas painting (₹499)\n" +
                    "• Tasty Little Things food frame (₹449)\n" +
                    "• Flower Planters (₹449)\n" +
                    "• Miffy Keychain (₹349)\n" +
                    "All within your ₹500 budget and perfect for your best friend! 💕"
        }

        // 2. Keychain specific queries
        if (q.contains("keychain") || q.contains("key chain") || q.contains("key ring")) {
            return "🔑 Looking for keychains? Check out our adorable 'Miffy Keychain' (₹349) made from high-grade acrylic! It's one of our most popular accessories."
        }

        // 3. Clay art / Pottery specific queries
        if (q.contains("clay") || q.contains("ceramic") || q.contains("pottery") || q.contains("sculpture")) {
            return "🏺 Looking for clay art & ceramics? We have:\n" +
                    "• Clay House (₹599) - A charming miniature handcrafted cottage\n" +
                    "• Flower Planters (₹449) - Artisanal hand-painted planters\n" +
                    "Both add an artistic, rustic warmth to any space! ✨"
        }

        // 4. Painting / Canvas specific queries
        if (q.contains("paint") || q.contains("canvas") || q.contains("portrait") || q.contains("frame")) {
            return "🎨 Here are our featured canvas paintings:\n" +
                    "• Puppy Dreams (₹499)\n" +
                    "• Puppy Portrait (₹499)\n" +
                    "• Tasty Little Things (₹449)\n" +
                    "• Garden Portrait (₹499)\n" +
                    "Handcrafted on quality canvas with acrylic detailing!"
        }

        // 5. General gift recommendations
        if (q.contains("gift") || q.contains("present")) {
            return "Looking for a gift? 🎁 Here are top recommendations by budget:\n" +
                    "• Under ₹400: Miffy Keychain (₹349)\n" +
                    "• Under ₹500: Tasty Little Things (₹449) or Puppy Portrait (₹499)\n" +
                    "• Signature Gift: Clay House (₹599)\n" +
                    "Let me know your exact budget or recipient, and I'll find the best fit! ✨"
        }

        // 6. Product recommendations
        if (q.contains("product") || q.contains("recommend") || q.contains("choose") || q.contains("buy")) {
            return "We have wonderful handcrafted collections! 🛍️\n" +
                    "• Canvas Paintings: Puppy Portrait (₹499), Tasty Little Things (₹449)\n" +
                    "• Home Decor: Clay House (₹599), Flower Planters (₹449)\n" +
                    "• Keychains: Miffy Keychain (₹349)\n" +
                    "Tap 'See All' on the Home screen to explore everything!"
        }

        // 7. Orders & tracking
        if (q.contains("order") || q.contains("track") || q.contains("status") || q.contains("delivery")) {
            return "📦 You can view and track all your placed orders directly in the Orders screen from the bottom navigation bar. Standard delivery takes 3–5 business days!"
        }

        // 8. Cart & checkout
        if (q.contains("cart") || q.contains("checkout")) {
            return "🛒 Your cart can be accessed anytime from the top-right header or by clicking 'Add to Cart' on any item. We provide instant offline-safe checkout!"
        }

        // 9. Wishlist
        if (q.contains("wishlist") || q.contains("favorite") || q.contains("heart")) {
            return "💖 Found something you love? Tap the heart icon on any product page to save it to your Wishlist for later!"
        }

        // 10. Campus location & stall
        if (q.contains("campus") || q.contains("location") || q.contains("map") || q.contains("fcrit") || q.contains("college")) {
            return "📍 Visit our campus stall at Fr. Conceicao Rodrigues Institute of Technology (FCRIT), Sector 9A, Vashi, Navi Mumbai! You can view the live interactive campus map under Categories → Campus Map."
        }

        // 11. Greetings
        if (q.contains("hello") || q.contains("hi") || q.contains("hey")) {
            return "Hi there! 😊 I'm Crafty. How can I help you discover something handcrafted today?"
        }

        // 12. Thanks
        if (q.contains("thank") || q.contains("thanks")) {
            return "You're very welcome! 💕 Let me know if you need anything else for your crafty journey!"
        }

        // 13. Pricing & budget overview
        if (q.contains("price") || q.contains("cost") || q.contains("cheap") || q.contains("budget")) {
            return "Our handcrafted items range from budget-friendly Miffy Keychains at ₹349 to our signature Clay House at ₹599. Quality handmade crafts at accessible prices!"
        }

        // Default response
        return "I'm here to help with handmade gifts, product recommendations, order tracking, and campus store navigation. Feel free to ask me anything about CraftCart! ✨"
    }

    private fun scrollToBottom() {
        chatScrollView.post {
            chatScrollView.fullScroll(ScrollView.FOCUS_DOWN)
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}
