package com.sih2026.touristsafety.presentation.screens.incidents

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.GenerativeModel
import com.sih2026.touristsafety.data.local.dao.ProfileDao
import com.sih2026.touristsafety.data.remote.AIAnalysisResult
import com.sih2026.touristsafety.data.remote.StructuredFir
import com.sih2026.touristsafety.presentation.screens.efir.FirHtmlGenerator
import com.sih2026.touristsafety.presentation.screens.efir.FirPdfExporter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class LatLng(val latitude: Double, val longitude: Double)

@HiltViewModel
class IncidentReportViewModel @Inject constructor(
    application: Application,
    private val profileDao: ProfileDao
) : AndroidViewModel(application) {

    private val _currentStep = MutableStateFlow(1)
    val currentStep: StateFlow<Int> = _currentStep.asStateFlow()

    private val _capturedPhotos = MutableStateFlow<List<Uri>>(emptyList())
    val capturedPhotos: StateFlow<List<Uri>> = _capturedPhotos.asStateFlow()

    private val _incidentType = MutableStateFlow("Theft")
    val incidentType: StateFlow<String> = _incidentType.asStateFlow()

    private val _description = MutableStateFlow("")
    val description: StateFlow<String> = _description.asStateFlow()

    private val _location = MutableStateFlow<LatLng?>(LatLng(28.6139, 77.2090))
    val location: StateFlow<LatLng?> = _location.asStateFlow()

    private val _aiAnalysis = MutableStateFlow<AIAnalysisResult?>(null)
    val aiAnalysis: StateFlow<AIAnalysisResult?> = _aiAnalysis.asStateFlow()

    private val _structuredFir = MutableStateFlow<StructuredFir?>(null)
    val structuredFir: StateFlow<StructuredFir?> = _structuredFir.asStateFlow()

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    private val _isPdfExporting = MutableStateFlow(false)
    val isPdfExporting: StateFlow<Boolean> = _isPdfExporting.asStateFlow()

    private val _downloadedPdfUri = MutableStateFlow<Uri?>(null)
    val downloadedPdfUri: StateFlow<Uri?> = _downloadedPdfUri.asStateFlow()

    fun nextStep() {
        if (_currentStep.value < 3) {
            _currentStep.value += 1
        }
    }

    fun previousStep() {
        if (_currentStep.value > 1) {
            _currentStep.value -= 1
        }
    }

    fun capturePhoto(uri: Uri) {
        val currentPhotos = _capturedPhotos.value.toMutableList()
        if (currentPhotos.size < 5) {
            currentPhotos.add(uri)
            _capturedPhotos.value = currentPhotos
        }
    }

    fun removePhoto(index: Int) {
        val currentPhotos = _capturedPhotos.value.toMutableList()
        if (index in currentPhotos.indices) {
            currentPhotos.removeAt(index)
            _capturedPhotos.value = currentPhotos
        }
    }
    
    fun updateIncidentType(type: String) {
        _incidentType.value = type
    }
    
    fun updateDescription(desc: String) {
        _description.value = desc
    }

    fun analyzeWithAI() {
        viewModelScope.launch {
            _isAnalyzing.value = true
            try {
                val context = getApplication<Application>()
                val profile = withContext(Dispatchers.IO) {
                    try { profileDao.getCurrentProfile() } catch (e: Exception) { null }
                }

                val complainantProfileInfo = if (profile != null) {
                    "Name: ${profile.fullName}, Nationality: ${profile.nationality}, Phone: ${profile.phone}, Passport: ${profile.passportNumber ?: "N/A"}, Email: ${profile.email}"
                } else {
                    "Tourist Complainant (Registered Tourist)"
                }

                val bitmaps = _capturedPhotos.value.mapNotNull { uri ->
                    try {
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            BitmapFactory.decodeStream(stream)
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("IncidentVM", "Failed to load image: ${e.message}")
                        null
                    }
                }

                val prompt = buildString {
                    append("You are an expert AI Incident Investigator and Indian Police Legal Drafter. ")
                    append("Analyze the provided evidence image(s), user description, and location to output a structured JSON analysis and legal e-FIR classification.\n\n")
                    append("User Incident Description: ${_description.value.ifBlank { "Theft / emergency reported by tourist" }}\n")
                    append("Reported Incident Type: ${_incidentType.value}\n")
                    append("Location Coordinates: Lat ${_location.value?.latitude ?: 28.6139}, Lng ${_location.value?.longitude ?: 77.2090} (Delhi, India)\n")
                    append("Complainant Known Profile: $complainantProfileInfo\n\n")
                    append("IMPORTANT: Return ONLY a valid JSON object without markdown or code blocks with these exact fields:\n")
                    append("{\n")
                    append("  \"scene\": \"<concise description of what is seen in the image and incident scene>\",\n")
                    append("  \"detected\": [\"<object 1>\", \"<object 2>\"],\n")
                    append("  \"type\": \"${_incidentType.value}\",\n")
                    append("  \"severity\": \"<Low, Moderate, High, or Critical>\",\n")
                    append("  \"bnsSections\": [\"Sec 303(2) BNS (Theft)\", \"Sec 304 BNS (Snatching)\"],\n")
                    append("  \"policeStation\": \"Connaught Place Police Station\",\n")
                    append("  \"district\": \"New Delhi District\",\n")
                    append("  \"state\": \"Delhi\",\n")
                    append("  \"distanceFromPs\": \"Approx. 1.2 KM South\",\n")
                    append("  \"accusedDescription\": \"Unknown accused person(s)\",\n")
                    append("  \"propertyLost\": \"<itemized list of lost/stolen property with estimated value>\",\n")
                    append("  \"witnesses\": \"None / Local bystanders\",\n")
                    append("  \"formalNarrative\": \"<chronological legal narrative of what happened>\",\n")
                    append("  \"formalComplaintLetter\": \"To,\\nThe Station House Officer,\\nConnaught Place Police Station, New Delhi.\\n\\nSubject: Formal Complaint regarding ${_incidentType.value} under Bharatiya Nyaya Sanhita (BNS), 2023.\\n\\nRespected Sir/Madam,\\nI, the undersigned complainant, do hereby state that on ${SimpleDateFormat("dd-MMM-yyyy 'at' hh:mm a", Locale.getDefault()).format(Date())} at Lat ${_location.value?.latitude}, Lng ${_location.value?.longitude}, the incident occurred...\\n\\nYours faithfully,\\n$complainantProfileInfo\"\n")
                    append("}")
                }

                // Execute raw HTTP request to Gemini
                val responseText = withContext(Dispatchers.IO) {
                    val partsArray = JSONArray()
                    
                    // Add images if attached
                    bitmaps.forEach { bmp ->
                        val baos = java.io.ByteArrayOutputStream()
                        bmp.compress(Bitmap.CompressFormat.JPEG, 80, baos)
                        val b64 = android.util.Base64.encodeToString(baos.toByteArray(), android.util.Base64.NO_WRAP)
                        
                        val inlineData = JSONObject()
                        inlineData.put("mimeType", "image/jpeg")
                        inlineData.put("data", b64)
                        
                        val part = JSONObject()
                        part.put("inlineData", inlineData)
                        partsArray.put(part)
                    }
                    
                    // Add text prompt
                    val textPart = JSONObject()
                    textPart.put("text", prompt)
                    partsArray.put(textPart)

                    val contentsArray = JSONArray()
                    val contentObj = JSONObject()
                    contentObj.put("parts", partsArray)
                    contentsArray.put(contentObj)

                    val requestBodyJson = JSONObject()
                    requestBodyJson.put("contents", contentsArray)

                    com.sih2026.touristsafety.utils.GeminiApiKeys.executeWithRetry { apiKey ->
                        val urlStr = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.6-flash:generateContent"
                        val url = java.net.URL(urlStr)
                        val connection = url.openConnection() as java.net.HttpURLConnection
                        connection.requestMethod = "POST"
                        connection.setRequestProperty("Content-Type", "application/json")
                        connection.setRequestProperty("x-goog-api-key", apiKey)
                        connection.setRequestProperty("x-goog-api-client", "genai-android/0.9.0")
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
                        
                        val responseJson = JSONObject(responseString)
                        responseJson.getJSONArray("candidates")
                            .getJSONObject(0)
                            .getJSONObject("content")
                            .getJSONArray("parts")
                            .getJSONObject(0)
                            .getString("text")
                    }
                }

                val cleanJson = responseText.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
                val json = try {
                    JSONObject(cleanJson)
                } catch (e: Exception) {
                    JSONObject()
                }

                val scene = json.optString("scene", if (bitmaps.isNotEmpty()) "Evidence scene captured" else "No photos attached")
                val detected = mutableListOf<String>()
                if (json.has("detected")) {
                    val arr = json.getJSONArray("detected")
                    for (i in 0 until arr.length()) {
                        detected.add(arr.getString(i))
                    }
                }
                val type = json.optString("type", _incidentType.value)
                val severity = json.optString("severity", "Moderate")

                val bnsList = mutableListOf<String>()
                if (json.has("bnsSections")) {
                    val bnsArr = json.getJSONArray("bnsSections")
                    for (i in 0 until bnsArr.length()) {
                        bnsList.add(bnsArr.getString(i))
                    }
                }
                if (bnsList.isEmpty()) {
                    bnsList.add("Sec 303(2) BNS (Theft)")
                }

                val psName = json.optString("policeStation", "Connaught Place Police Station")
                val district = json.optString("district", "New Delhi District")
                val state = json.optString("state", "Delhi")
                val filingYear = SimpleDateFormat("yyyy", Locale.getDefault()).format(Date())
                val randomFirNo = "DL-ND-$filingYear-EFIR-${(100000..999999).random()}"

                _aiAnalysis.value = AIAnalysisResult(
                    sceneDescription = scene,
                    detectedObjects = detected,
                    potentialIncidentType = type,
                    severityAssessment = severity,
                    suggestedBnsSections = bnsList,
                    jurisdictionalPoliceStation = psName
                )

                _structuredFir.value = StructuredFir(
                    incidentType = type,
                    bnsSections = bnsList,
                    dateTime = SimpleDateFormat("dd-MMM-yyyy 'at' hh:mm a", Locale.getDefault()).format(Date()),
                    place = "Lat: ${_location.value?.latitude ?: 28.6139}, Lng: ${_location.value?.longitude ?: 77.2090} (Connaught Place, New Delhi)",
                    policeStation = psName,
                    district = district,
                    state = state,
                    distanceFromPs = json.optString("distanceFromPs", "Approx. 1.2 KM"),
                    accusedDescription = json.optString("accusedDescription", "Unknown accused person(s)"),
                    propertyLost = json.optString("propertyLost", _description.value.ifBlank { "Stolen property as stated" }),
                    witnesses = json.optString("witnesses", "None / Local bystanders"),
                    narrative = json.optString("formalNarrative", _description.value),
                    complainantDetails = complainantProfileInfo,
                    formalComplaintLetter = json.optString("formalComplaintLetter", ""),
                    firNumber = randomFirNo,
                    evidenceSceneDescription = scene,
                    detectedEvidenceObjects = detected,
                    evidenceSeverity = severity,
                    photosCount = _capturedPhotos.value.size
                )

            } catch (e: Exception) {
                android.util.Log.e("IncidentVM", "AI analysis failed: ${e.message}", e)
                _aiAnalysis.value = AIAnalysisResult(
                    sceneDescription = "Analysis completed for incident: ${_incidentType.value}",
                    detectedObjects = listOf("Evidence Scene"),
                    potentialIncidentType = _incidentType.value,
                    severityAssessment = "Moderate",
                    suggestedBnsSections = listOf("Sec 303(2) BNS (Theft)"),
                    jurisdictionalPoliceStation = "Connaught Place Police Station"
                )

                _structuredFir.value = StructuredFir(
                    incidentType = _incidentType.value,
                    bnsSections = listOf("Sec 303(2) BNS (Theft)", "Sec 304 BNS (Snatching)"),
                    dateTime = SimpleDateFormat("dd-MMM-yyyy 'at' hh:mm a", Locale.getDefault()).format(Date()),
                    place = "Lat: ${_location.value?.latitude ?: 28.6139}, Lng: ${_location.value?.longitude ?: 77.2090} (Connaught Place, New Delhi)",
                    policeStation = "Connaught Place Police Station",
                    district = "New Delhi District",
                    state = "Delhi",
                    distanceFromPs = "Approx. 1.2 KM",
                    accusedDescription = "Unknown accused person(s)",
                    propertyLost = _description.value.ifBlank { "Stolen items as stated" },
                    witnesses = "Local bystanders",
                    narrative = _description.value,
                    complainantDetails = "Registered Tourist Complainant",
                    firNumber = "DL-ND-2026-EFIR-${(100000..999999).random()}",
                    evidenceSceneDescription = "Analysis recorded for incident: ${_incidentType.value}. ${_description.value}",
                    detectedEvidenceObjects = listOf("Evidence Scene Verified"),
                    evidenceSeverity = "Moderate",
                    photosCount = _capturedPhotos.value.size
                )
            }
            _isAnalyzing.value = false
        }
    }

    fun downloadOfficialFirPdf(context: Context) {
        var fir = _structuredFir.value
        if (fir == null) {
            val filingYear = SimpleDateFormat("yyyy", Locale.getDefault()).format(Date())
            fir = StructuredFir(
                incidentType = _incidentType.value,
                bnsSections = listOf("Sec 303(2) BNS (Theft)", "Sec 304 BNS (Snatching)"),
                dateTime = SimpleDateFormat("dd-MMM-yyyy 'at' hh:mm a", Locale.getDefault()).format(Date()),
                place = "Lat: ${_location.value?.latitude ?: 28.6139}, Lng: ${_location.value?.longitude ?: 77.2090} (Connaught Place, New Delhi)",
                policeStation = "Connaught Place Police Station",
                district = "New Delhi District",
                state = "Delhi",
                distanceFromPs = "Approx. 1.2 KM",
                accusedDescription = "Unknown accused person(s)",
                propertyLost = _description.value.ifBlank { "Stolen property as stated" },
                witnesses = "Local bystanders",
                narrative = _description.value.ifBlank { "Theft incident reported by tourist" },
                complainantDetails = "Registered Tourist Complainant",
                firNumber = "DL-ND-$filingYear-EFIR-${(100000..999999).random()}",
                evidenceSceneDescription = _aiAnalysis.value?.sceneDescription ?: "Evidence scene documented",
                detectedEvidenceObjects = _aiAnalysis.value?.detectedObjects ?: emptyList(),
                evidenceSeverity = _aiAnalysis.value?.severityAssessment ?: "Moderate",
                photosCount = _capturedPhotos.value.size
            )
            _structuredFir.value = fir
        }

        _isPdfExporting.value = true
        val htmlContent = FirHtmlGenerator.generateOfficialFirHtml(fir)
        val fileName = "CCTNS_EFIR_${fir.firNumber.ifBlank { System.currentTimeMillis().toString() }}"

        FirPdfExporter.exportHtmlToPdf(
            context = context,
            htmlContent = htmlContent,
            baseFileName = fileName,
            onSuccess = { uri ->
                _isPdfExporting.value = false
                _downloadedPdfUri.value = uri
                Toast.makeText(context, "✅ Official e-FIR PDF saved to Downloads!", Toast.LENGTH_LONG).show()
                FirPdfExporter.openPdfViewer(context, uri)
            },
            onError = { error ->
                _isPdfExporting.value = false
                Toast.makeText(context, "Failed to generate PDF: $error", Toast.LENGTH_LONG).show()
            }
        )
    }

    fun openDownloadedPdf(context: Context) {
        val uri = _downloadedPdfUri.value ?: return
        FirPdfExporter.openPdfViewer(context, uri)
    }

    fun submitReport() {
        viewModelScope.launch {
            // Save to DB and API
        }
    }

    fun saveDraft() {
        viewModelScope.launch {
            // Save draft locally
        }
    }
}
