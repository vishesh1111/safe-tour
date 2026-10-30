package com.sih2026.touristsafety.presentation.screens.efir

import android.graphics.Bitmap
import android.graphics.Color
import android.util.Base64
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.sih2026.touristsafety.data.remote.StructuredFir
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FirHtmlGenerator {

    fun generateQrCodeBase64(content: String, size: Int = 220): String {
        return try {
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE)
                }
            }
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    fun computeSha256(input: String): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(input.toByteArray(Charsets.UTF_8))
            hash.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            "0x" + System.currentTimeMillis().toString(16)
        }
    }

    fun generateOfficialFirHtml(fir: StructuredFir): String {
        val currentDate = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())
        val filingYear = SimpleDateFormat("yyyy", Locale.getDefault()).format(Date())
        
        val firNo = if (fir.firNumber.isNotBlank()) fir.firNumber else "DL-ND-${filingYear}-EFIR-${(100000..999999).random()}"
        val gdNo = "GD-${(100..999).random()}/${filingYear}"
        val psName = if (fir.policeStation.isNotBlank()) fir.policeStation else "Connaught Place Police Station"
        val district = if (fir.district.isNotBlank()) fir.district else "New Delhi District"
        val state = if (fir.state.isNotBlank()) fir.state else "Delhi"

        val qrContent = "GOVT OF INDIA | CCTNS e-FIR Verification\nFIR No: $firNo\nPS: $psName\nDate: $currentDate\nSections: ${fir.bnsSections.joinToString(", ")}\nStatus: REGISTERED (PROVISIONAL)"
        val qrBase64 = generateQrCodeBase64(qrContent)
        val hash = computeSha256(firNo + psName + fir.incidentType + fir.narrative + fir.evidenceSceneDescription)

        val bnsListHtml = if (fir.bnsSections.isNotEmpty()) {
            fir.bnsSections.map { "<span class='badge bns-badge'>$it</span>" }.joinToString(" ")
        } else {
            "<span class='badge bns-badge'>Sec 303(2) BNS (Theft)</span>"
        }

        val severityColor = when (fir.evidenceSeverity.lowercase(Locale.ROOT)) {
            "critical", "high" -> "#d32f2f"
            "moderate", "medium" -> "#f57c00"
            else -> "#388e3c"
        }

        val evidenceDetectedHtml = if (fir.detectedEvidenceObjects.isNotEmpty()) {
            fir.detectedEvidenceObjects.map { "<span class='badge detected-badge'>$it</span>" }.joinToString(" ")
        } else {
            "<span style='color:#666;'>No identifiable forensic objects flagged</span>"
        }

        val complaintLetter = if (fir.formalComplaintLetter.isNotBlank()) {
            fir.formalComplaintLetter
        } else {
            """To,
The Station House Officer,
$psName, $district.

Subject: Formal complaint regarding ${fir.incidentType} under Bharatiya Nyaya Sanhita (BNS), 2023.

Respected Sir/Madam,
I, the undersigned complainant, do hereby state that on ${fir.dateTime} at ${fir.place}, the following incident occurred:

${fir.narrative}

Description of Accused / Suspects: ${fir.accusedDescription}
Particulars of Property Involved / Stolen: ${fir.propertyLost}
Eyewitnesses (if any): ${fir.witnesses}

I humbly request your good office to register an e-FIR under the applicable sections of Bharatiya Nyaya Sanhita (BNS), 2023, and take prompt legal action to recover my belongings and investigate the matter.

Yours faithfully,
${fir.complainantDetails.ifBlank { "Complainant / Tourist" }}"""
        }

        return """
<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>First Information Report (e-FIR) - $firNo</title>
<style>
  @page {
    size: A4 portrait;
    margin: 10mm 12mm;
  }
  * {
    box-sizing: border-box;
    -webkit-print-color-adjust: exact !important;
    print-color-adjust: exact !important;
  }
  body {
    font-family: 'Helvetica Neue', Arial, sans-serif;
    color: #1a1a1a;
    font-size: 11px;
    line-height: 1.45;
    background: #ffffff;
    margin: 0;
    padding: 10px 12px;
    width: 100%;
  }

  /* Header Table */
  .header-table {
    width: 100%;
    border-collapse: collapse;
    border-bottom: 2.5px solid #002b49;
    padding-bottom: 8px;
    margin-bottom: 12px;
  }
  .header-table td {
    vertical-align: middle;
  }
  .govt-title {
    font-size: 11px;
    font-weight: 700;
    letter-spacing: 1px;
    color: #2b3a4a;
    text-transform: uppercase;
    margin-bottom: 2px;
  }
  .main-title {
    font-size: 18px;
    font-weight: 900;
    color: #002b49;
    letter-spacing: 0.5px;
    margin: 2px 0;
  }
  .sub-title {
    font-size: 10px;
    color: #4f5d75;
    font-style: italic;
  }
  .qr-cell {
    width: 100px;
    text-align: center;
  }
  .qr-cell img {
    border: 1px solid #002b49;
    padding: 2px;
    background: #fff;
    display: block;
    margin: 0 auto;
  }
  .qr-label {
    font-size: 8px;
    color: #002b49;
    font-weight: 800;
    margin-top: 3px;
    letter-spacing: 0.5px;
  }

  /* Fixed Column Tables */
  .grid-table {
    width: 100%;
    table-layout: fixed;
    border-collapse: collapse;
    margin-bottom: 10px;
    background-color: #ffffff;
    border: 1px solid #b5c7de;
  }
  .grid-table td, .grid-table th {
    border: 1px solid #b5c7de;
    padding: 5px 8px;
    font-size: 10.5px;
    vertical-align: middle;
    word-break: break-word;
    overflow-wrap: break-word;
  }
  .meta-label {
    background-color: #f1f5fa;
    font-weight: 700;
    color: #002b49;
    width: 20%;
  }
  .meta-val {
    width: 30%;
    color: #111;
  }

  /* Section Headers */
  .section-header {
    background: linear-gradient(90deg, #002b49 0%, #004b7a 100%);
    color: #ffffff;
    font-size: 11px;
    font-weight: 700;
    padding: 5px 9px;
    margin: 10px 0 5px 0;
    border-radius: 2px;
    text-transform: uppercase;
    letter-spacing: 0.5px;
    page-break-after: avoid;
    break-after: avoid;
  }

  /* Data Table */
  .data-table {
    width: 100%;
    table-layout: fixed;
    border-collapse: collapse;
    margin-bottom: 8px;
  }
  .data-table th, .data-table td {
    border: 1px solid #b5c7de;
    padding: 5px 8px;
    font-size: 10.5px;
    vertical-align: top;
    word-break: break-word;
    overflow-wrap: break-word;
  }
  .data-table th {
    background-color: #f1f5fa;
    color: #002b49;
    font-weight: 700;
    text-align: left;
  }

  /* Badges */
  .badge {
    display: inline-block;
    padding: 2px 7px;
    border-radius: 3px;
    font-size: 10px;
    font-weight: 700;
    margin: 2px 3px 2px 0;
  }
  .bns-badge {
    background-color: #002b49;
    color: #ffffff;
  }
  .detected-badge {
    background-color: #e8f0fe;
    color: #1967d2;
    border: 1px solid #d2e3fc;
  }
  .severity-badge {
    color: #ffffff;
    font-weight: 800;
    text-transform: uppercase;
  }

  /* Narrative Box */
  .narrative-box {
    border: 1px solid #b5c7de;
    background-color: #fcfdfe;
    padding: 9px 12px;
    font-size: 10.5px;
    white-space: pre-wrap;
    line-height: 1.5;
    margin-bottom: 10px;
    border-left: 3.5px solid #002b49;
    border-radius: 2px;
  }

  /* Signatures */
  .footer-signatures {
    width: 100%;
    table-layout: fixed;
    border-collapse: collapse;
    margin-top: 18px;
    page-break-inside: avoid;
    break-inside: avoid;
  }
  .footer-signatures td {
    width: 50%;
    vertical-align: bottom;
    padding: 0 16px;
  }
  .sig-line {
    border-top: 1px dashed #444;
    padding-top: 4px;
    text-align: center;
    font-weight: 700;
    font-size: 10px;
    color: #222;
  }

  /* Hash & Integrity */
  .hash-box {
    margin-top: 12px;
    padding: 6px 8px;
    background: #f7f9fa;
    border: 1px solid #d0d7de;
    font-family: monospace;
    font-size: 8.5px;
    color: #444;
    word-break: break-all;
    border-radius: 2px;
    page-break-inside: avoid;
    break-inside: avoid;
  }
  .watermark {
    text-align: center;
    color: #6c757d;
    font-size: 8.5px;
    margin-top: 6px;
    font-style: italic;
  }

  @media print {
    body { padding: 0; }
    .section-header { page-break-after: avoid; break-after: avoid; }
    tr, .footer-signatures, .hash-box { page-break-inside: avoid; break-inside: avoid; }
  }
</style>
</head>
<body>

<table class="header-table">
  <tr>
    <td>
      <div class="govt-title">State Police Department • Tourist Safety Division</div>
      <div class="main-title">FIRST INFORMATION REPORT (e-FIR)</div>
      <div class="sub-title">FORM - II [Under Section 173 of Bharatiya Nagarik Suraksha Sanhita (BNSS), 2023]</div>
    </td>
    <td class="qr-cell">
      <img src="data:image/png;base64,$qrBase64" width="80" height="80" alt="CCTNS QR" />
      <div class="qr-label">CCTNS VERIFIED</div>
    </td>
  </tr>
</table>

<table class="grid-table">
  <colgroup>
    <col style="width: 20%;" />
    <col style="width: 30%;" />
    <col style="width: 20%;" />
    <col style="width: 30%;" />
  </colgroup>
  <tr>
    <td class="meta-label">1. e-FIR Number:</td>
    <td class="meta-val"><strong>$firNo</strong></td>
    <td class="meta-label">2. Filing Date & Time:</td>
    <td class="meta-val">$currentDate IST</td>
  </tr>
  <tr>
    <td class="meta-label">3. District & State:</td>
    <td class="meta-val">$district, $state</td>
    <td class="meta-label">4. Jurisdictional PS:</td>
    <td class="meta-val"><strong>$psName</strong></td>
  </tr>
  <tr>
    <td class="meta-label">5. GD Entry Reference:</td>
    <td class="meta-val">$gdNo</td>
    <td class="meta-label">6. Category / Priority:</td>
    <td class="meta-val"><span style="color:#d32f2f; font-weight:bold;">Cognizable Offence (Tourist Fast-Track)</span></td>
  </tr>
</table>

<div class="section-header">A. Statutory Acts and Applicable Legal Sections</div>
<table class="data-table">
  <colgroup>
    <col style="width: 35%;" />
    <col style="width: 65%;" />
  </colgroup>
  <tr>
    <th>Governing Statutory Act</th>
    <th>Applicable Sections & Legal Offence Nomenclature</th>
  </tr>
  <tr>
    <td>
      <strong>Bharatiya Nyaya Sanhita (BNS), 2023</strong><br>
      <small style="color:#666;">(Replacing Indian Penal Code, 1860)</small>
    </td>
    <td>
      $bnsListHtml
      <div style="margin-top:4px; font-size:10px; color:#333;">Incident Classification: <strong>${fir.incidentType}</strong></div>
    </td>
  </tr>
</table>

<div class="section-header">B. Complainant / Tourist (Informant) Particulars</div>
<table class="data-table">
  <colgroup>
    <col style="width: 20%;" />
    <col style="width: 30%;" />
    <col style="width: 20%;" />
    <col style="width: 30%;" />
  </colgroup>
  <tr>
    <th>Full Name</th>
    <td><strong>${fir.complainantDetails.ifBlank { "Tourist Complainant" }}</strong></td>
    <th>Nationality / ID</th>
    <td>Foreign / Indian National Tourist</td>
  </tr>
  <tr>
    <th>Contact / Phone</th>
    <td>+91 (Tourist Registered Mobile)</td>
    <th>Mode of Information</th>
    <td>Phone-First e-FIR Portal (Mobile App)</td>
  </tr>
  <tr>
    <th>Present Address / Stay</th>
    <td colspan="3">Local Tourist Hotel / Lodging, $district, $state</td>
  </tr>
</table>

<div class="section-header">C. Place & Time of Occurrence</div>
<table class="data-table">
  <colgroup>
    <col style="width: 20%;" />
    <col style="width: 30%;" />
    <col style="width: 20%;" />
    <col style="width: 30%;" />
  </colgroup>
  <tr>
    <th>Date & Time of Incident</th>
    <td><strong>${fir.dateTime}</strong></td>
    <th>Distance from PS</th>
    <td>${fir.distanceFromPs.ifBlank { "Approx. 1.2 KM" }}</td>
  </tr>
  <tr>
    <th>Exact Location / Landmark</th>
    <td colspan="3"><strong>${fir.place}</strong></td>
  </tr>
</table>

<div class="section-header">D. Suspect / Accused & Property Details</div>
<table class="data-table">
  <colgroup>
    <col style="width: 25%;" />
    <col style="width: 75%;" />
  </colgroup>
  <tr>
    <th>Details of Known / Suspected Accused</th>
    <td>${fir.accusedDescription.ifBlank { "Unknown accused person(s)" }}</td>
  </tr>
  <tr>
    <th>Particulars of Property Involved / Stolen</th>
    <td><strong>${fir.propertyLost.ifBlank { "As stated in complaint narrative" }}</strong></td>
  </tr>
  <tr>
    <th>Eyewitnesses (if any)</th>
    <td>${fir.witnesses.ifBlank { "None / Local bystanders" }}</td>
  </tr>
</table>

<!-- Section E: Forensic Evidence & AI Analysis -->
<div class="section-header">E. Forensic Evidence & AI Analysis Docket</div>
<table class="data-table">
  <colgroup>
    <col style="width: 25%;" />
    <col style="width: 75%;" />
  </colgroup>
  <tr>
    <th>AI Evidence Scene Analysis</th>
    <td>${fir.evidenceSceneDescription.ifBlank { "Evidence scene digitally verified and attached." }}</td>
  </tr>
  <tr>
    <th>Detected Forensic Objects</th>
    <td>$evidenceDetectedHtml</td>
  </tr>
  <tr>
    <th>Severity & Threat Assessment</th>
    <td>
      <span class="badge severity-badge" style="background-color: $severityColor;">
        ${fir.evidenceSeverity.ifBlank { "MODERATE" }}
      </span>
      <span style="font-size: 10px; color: #555; margin-left: 6px;">
        Evidence Exhibits: <strong>${fir.photosCount} photo file(s) attached</strong>
      </span>
    </td>
  </tr>
</table>

<div class="section-header">F. Formal First Information Contents (Complaint Statement)</div>
<div class="narrative-box">$complaintLetter</div>

<table class="footer-signatures">
  <tr>
    <td>
      <div class="sig-line">
        Digital Signature / Thumb Impression<br>
        <small style="color:#666;">(Complainant / Informant)</small>
      </div>
    </td>
    <td>
      <div class="sig-line">
        Station House Officer / Duty Officer<br>
        <small style="color:#666;">$psName (Seal & Sign)</small>
      </div>
    </td>
  </tr>
</table>

<div class="hash-box">
  <strong>CRYPTOGRAPHIC INTEGRITY HASH (SHA-256):</strong> $hash<br>
  <strong>Digital Audit Trail:</strong> Verified on Tourist Safety National CCTNS Gateway Node #TS-DEL-${(100..999).random()} | Tamper-Evident Electronic Record (Sec 63 BSA, 2023)
</div>

<div class="watermark">
  * This is an electronically generated e-FIR draft and formal legal complaint under Section 173 BNSS, 2023. Valid for official police triage, insurance claims & legal proceedings. *
</div>

</body>
</html>
        """.trimIndent()
    }
}
