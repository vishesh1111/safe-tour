package com.sih2026.touristsafety.data.remote

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

data class FirRequest(val description: String, val language: String)

data class StructuredFir(
    val incidentType: String,
    val bnsSections: List<String>,
    val dateTime: String,
    val place: String,
    val accusedDescription: String,
    val propertyLost: String,
    val witnesses: String,
    val narrative: String,
    val complainantDetails: String,
    val firNumber: String = "",
    val policeStation: String = "",
    val district: String = "",
    val state: String = "Delhi",
    val distanceFromPs: String = "",
    val formalComplaintLetter: String = "",
    val digitalHash: String = "",
    val qrCodeBase64: String = "",
    val evidenceSceneDescription: String = "",
    val detectedEvidenceObjects: List<String> = emptyList(),
    val evidenceSeverity: String = "Moderate",
    val photosCount: Int = 0
)


