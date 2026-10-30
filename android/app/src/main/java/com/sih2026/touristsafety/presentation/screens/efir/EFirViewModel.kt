package com.sih2026.touristsafety.presentation.screens.efir

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.GenerativeModel
import com.sih2026.touristsafety.data.local.dao.ProfileDao
import com.sih2026.touristsafety.data.remote.StructuredFir
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import javax.inject.Inject

@HiltViewModel
class EFirViewModel @Inject constructor(
    private val profileDao: ProfileDao
) : ViewModel() {

    private val _currentStep = MutableStateFlow(1)
    val currentStep: StateFlow<Int> = _currentStep.asStateFlow()

    private val _rawDescription = MutableStateFlow("")
    val rawDescription: StateFlow<String> = _rawDescription.asStateFlow()

    private val _structuredFir = MutableStateFlow<StructuredFir?>(null)
    val structuredFir: StateFlow<StructuredFir?> = _structuredFir.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _isPdfExporting = MutableStateFlow(false)
    val isPdfExporting: StateFlow<Boolean> = _isPdfExporting.asStateFlow()

    private val _downloadedPdfUri = MutableStateFlow<Uri?>(null)
    val downloadedPdfUri: StateFlow<Uri?> = _downloadedPdfUri.asStateFlow()

    private val _attachedEvidence = MutableStateFlow<List<Uri>>(emptyList())
    val attachedEvidence: StateFlow<List<Uri>> = _attachedEvidence.asStateFlow()

    fun setRawDescription(desc: String) {
        _rawDescription.value = desc
    }

    fun nextStep() {
        if (_currentStep.value < 4) {
            _currentStep.value += 1
        }
    }

    fun previousStep() {
        if (_currentStep.value > 1) {
            _currentStep.value -= 1
        }
    }

    fun generateFir() {
        if (_rawDescription.value.isBlank()) return
        
        viewModelScope.launch {
            _isGenerating.value = true
            try {
                val profile = withContext(Dispatchers.IO) {
                    try { profileDao.getCurrentProfile() } catch (e: Exception) { null }
                }

                val complainantProfileInfo = if (profile != null) {
                    "Name: ${profile.fullName}, Nationality: ${profile.nationality}, Phone: ${profile.phone}, Passport: ${profile.passportNumber ?: "N/A"}, Email: ${profile.email}"
                } else {
                    "Tourist Complainant (Registered Tourist)"
                }

                val prompt = """
                    You are an expert Indian Police Legal Drafter and Public Prosecutor assisting in drafting an official First Information Report (e-FIR) under Section 173 of the Bharatiya Nagarik Suraksha Sanhita (BNSS), 2023.
                    
                    Analyze the user's incident description and generate a complete, authentic legal FIR dataset. Map offences strictly to the Bharatiya Nyaya Sanhita (BNS), 2023 (NOT old IPC).
                    
                    User Incident Description: "${_rawDescription.value}"
                    Complainant Known Profile: "$complainantProfileInfo"
                    
                    Return ONLY a single valid JSON object (no markdown, no backticks) with these exact string fields:
                    - incidentType: e.g. "Theft / Snatching", "Physical Assault", "Extortion", "Cheating / Fraud", "Lost Item / Property"
                    - bnsSections: Comma-separated list of applicable BNS 2023 sections (e.g. "Sec 303(2) BNS (Theft)", "Sec 304 BNS (Snatching)", "Sec 318(4) BNS (Cheating)")
                    - dateTime: Formatted date and approximate time (e.g. "30-Aug-2026 at 01:30 PM")
                    - place: Full place of occurrence with landmarks and city
                    - policeStation: Name of the jurisdictional police station (e.g. "Connaught Place Police Station", "Colaba Police Station", "Paharganj Police Station")
                    - district: District name (e.g. "New Delhi District", "Central Delhi", "South Mumbai")
                    - state: State name (e.g. "Delhi", "Maharashtra", "Goa", "Rajasthan")
                    - distanceFromPs: Estimated distance & direction from PS (e.g. "Approx. 1.2 KM South-East")
                    - complainantDetails: Full complainant name, nationality, contact details
                    - accusedDescription: Physical features, approximate age, clothing, vehicle details, or "Unknown accused person(s)"
                    - propertyLost: Itemized list of lost/stolen property with estimated value and identifiers (e.g. "1x Apple iPhone 15 Pro, Space Black, IMEI: 359..., Est. Value ₹1,20,000")
                    - witnesses: Eyewitnesses if any or "None / Local shopkeepers"
                    - narrative: A formal, chronological legal summary of what happened.
                    - formalComplaintLetter: A formal legal petition addressed to the Station House Officer (SHO) of the police station in first-person legal style:
                    "To,\nThe Station House Officer,\n[Police Station Name], [District].\n\nSubject: Formal Complaint regarding [Incident Type] under Section [BNS Sections].\n\nRespected Sir/Madam,\nI, the undersigned complainant [Name/Tourist], resident of [Address/Hotel], do hereby state that on [Date/Time] at [Place]... [chronological narrative].\n\nTherefore, I humbly request your good office to register an FIR under relevant provisions of the Bharatiya Nyaya Sanhita (BNS), 2023, and initiate an urgent investigation to recover my belongings and apprehend the culprits.\n\nYours faithfully,\n[Complainant Name]"
                """.trimIndent()

                val response = com.sih2026.touristsafety.utils.GeminiApiKeys.executeWithRetry { apiKey ->
                    val model = GenerativeModel(
                        modelName = "gemini-3.6-flash",
                        apiKey = apiKey
                    )
                    model.generateContent(prompt)
                }
                val responseText = response.text?.trim()?.removePrefix("```json")?.removeSuffix("```")?.trim() ?: "{}"
                
                val json = JSONObject(responseText)
                
                val bnsList = if (json.has("bnsSections")) {
                    json.getString("bnsSections").split(",").map { it.trim() }
                } else {
                    listOf("Sec 303(2) BNS (Theft)")
                }

                val filingYear = java.text.SimpleDateFormat("yyyy", java.util.Locale.getDefault()).format(java.util.Date())
                val randomFirNo = "DL-ND-$filingYear-EFIR-${(100000..999999).random()}"

                _structuredFir.value = StructuredFir(
                    incidentType = json.optString("incidentType", "Theft / Loss"),
                    bnsSections = bnsList,
                    dateTime = json.optString("dateTime", java.text.SimpleDateFormat("dd-MMM-yyyy 'at' hh:mm a", java.util.Locale.getDefault()).format(java.util.Date())),
                    place = json.optString("place", "New Delhi, India"),
                    policeStation = json.optString("policeStation", "Connaught Place Police Station"),
                    district = json.optString("district", "New Delhi District"),
                    state = json.optString("state", "Delhi"),
                    distanceFromPs = json.optString("distanceFromPs", "Approx. 1.5 KM"),
                    accusedDescription = json.optString("accusedDescription", "Unknown accused person(s)"),
                    propertyLost = json.optString("propertyLost", "As stated in narrative"),
                    witnesses = json.optString("witnesses", "None / Local shopkeepers"),
                    narrative = json.optString("narrative", _rawDescription.value),
                    complainantDetails = json.optString("complainantDetails", complainantProfileInfo),
                    formalComplaintLetter = json.optString("formalComplaintLetter", ""),
                    firNumber = randomFirNo
                )
                
                _currentStep.value = 2
            } catch (e: Exception) {
                e.printStackTrace()
                // Fallback structured FIR if AI parsing fails
                _structuredFir.value = StructuredFir(
                    incidentType = "Theft / Snatching",
                    bnsSections = listOf("Sec 303(2) BNS (Theft)", "Sec 304 BNS (Snatching)"),
                    dateTime = java.text.SimpleDateFormat("dd-MMM-yyyy 'at' hh:mm a", java.util.Locale.getDefault()).format(java.util.Date()),
                    place = "Connaught Place, New Delhi",
                    policeStation = "Connaught Place Police Station",
                    district = "New Delhi",
                    state = "Delhi",
                    distanceFromPs = "Approx. 1.2 KM South",
                    accusedDescription = "Unknown person(s)",
                    propertyLost = "Stolen items as stated",
                    witnesses = "Local bystanders",
                    narrative = _rawDescription.value,
                    complainantDetails = "Registered Tourist Complainant",
                    firNumber = "DL-ND-2026-EFIR-${(100000..999999).random()}"
                )
                _currentStep.value = 2
            } finally {
                _isGenerating.value = false
            }
        }
    }

    fun updateField(field: String, value: Any) {
        val currentFir = _structuredFir.value ?: return
        _structuredFir.value = when(field) {
            "incidentType" -> currentFir.copy(incidentType = value as String)
            "dateTime" -> currentFir.copy(dateTime = value as String)
            "place" -> currentFir.copy(place = value as String)
            "policeStation" -> currentFir.copy(policeStation = value as String)
            "district" -> currentFir.copy(district = value as String)
            "accusedDescription" -> currentFir.copy(accusedDescription = value as String)
            "propertyLost" -> currentFir.copy(propertyLost = value as String)
            "witnesses" -> currentFir.copy(witnesses = value as String)
            "narrative" -> currentFir.copy(narrative = value as String)
            "complainantDetails" -> currentFir.copy(complainantDetails = value as String)
            "formalComplaintLetter" -> currentFir.copy(formalComplaintLetter = value as String)
            else -> currentFir
        }
    }

    fun downloadPdf(context: Context) {
        val fir = _structuredFir.value ?: return
        
        _isPdfExporting.value = true
        val htmlContent = FirHtmlGenerator.generateOfficialFirHtml(fir)
        val firFileName = "CCTNS_EFIR_${fir.firNumber.ifBlank { System.currentTimeMillis().toString() }}"

        FirPdfExporter.exportHtmlToPdf(
            context = context,
            htmlContent = htmlContent,
            baseFileName = firFileName,
            onSuccess = { uri ->
                _isPdfExporting.value = false
                _downloadedPdfUri.value = uri
                Toast.makeText(context, "✅ Official e-FIR PDF saved to Downloads!", Toast.LENGTH_LONG).show()
                // Automatically prompt open PDF viewer
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

    fun getPolicePortalUrl(state: String): String {
        return when(state.lowercase()) {
            "delhi" -> "https://digitalpolice.gov.in/"
            "maharashtra" -> "https://citizen.mahapolice.gov.in/"
            "karnataka" -> "https://ksp.karnataka.gov.in/"
            "tamil nadu" -> "https://eservices.tnpolice.gov.in/"
            "kerala" -> "https://keralapolice.gov.in/"
            "goa" -> "https://goapolice.gov.in/"
            "rajasthan" -> "https://police.rajasthan.gov.in/"
            "uttar pradesh" -> "https://uppolice.gov.in/"
            "gujarat" -> "https://gujhome.gujarat.gov.in/"
            "west bengal" -> "https://wbpolice.gov.in/"
            else -> "https://digitalpolice.gov.in/"
        }
    }
}
