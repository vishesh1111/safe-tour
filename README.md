# 🧭 Guiding Paths — Intelligent Tourist Safety Platform

<p align="center">
  <strong>Your Journey. Your Safety. Your Peace of Mind.</strong>
</p>

<p align="center">
  An AI-powered tourist safety platform designed to protect travelers through privacy-first monitoring, offline emergency communication, intelligent threat detection, and real-time disaster awareness.
</p>

<p align="center">
  <img src="https://img.shields.io/badge/AI-On--Device%20Intelligence-7C3AED?style=for-the-badge" />
  <img src="https://img.shields.io/badge/Safety-Privacy--First-16A34A?style=for-the-badge" />
  <img src="https://img.shields.io/badge/Emergency-Offline%20Ready-DC2626?style=for-the-badge" />
  <img src="https://img.shields.io/badge/Navigation-AR%20Powered-0284C7?style=for-the-badge" />
</p>

---

## 🌍 About the Project

**Guiding Paths** reimagines tourist safety by combining Artificial Intelligence, Machine Learning, Augmented Reality, peer-to-peer communication, and tamper-evident evidence management into a unified safety ecosystem.

From getting separated in a crowded marketplace to navigating unfamiliar streets at night, facing an emergency without internet access, or receiving warnings about natural disasters, the platform is designed to help tourists make safer, faster, and more informed decisions.

Unlike conventional safety applications that depend heavily on continuous internet connectivity or manual SOS activation, Guiding Paths aims to deliver **proactive protection, decentralized emergency communication, and privacy-conscious monitoring** throughout a tourist's journey.

### 🎯 Our Mission

To make travel safer, smarter, and more accessible by connecting travelers, nearby helpers, and emergency authorities through intelligent, resilient, and accountable technology.

---

## ✨ Key Features

### 1. 🔐 Session-Based Monitoring & Privacy-First AI

**Safety without constant surveillance.**

- **User-controlled journeys:** Monitoring begins only when a tourist manually starts a journey.
- **On-device audio analysis:** Locally process ambient audio to identify potential distress sounds without uploading raw microphone recordings to the cloud.
- **Automatic shutdown:** Monitoring stops immediately when the journey ends.
- **Temporary session cleanup:** Clear temporary journey-related data when the session is terminated, according to the app's data-retention policy.

> Privacy is a core design principle, not an afterthought.

### 2. 📡 Offline Emergency Mode & P2P Mesh Relay

**Emergency communication beyond network coverage.**

Guiding Paths is designed to support a Bluetooth-based, peer-to-peer relay network that can forward encrypted emergency messages between nearby participating devices.

**👥 Tour Group Reunification**
- Share encrypted location packets between participating devices.
- Relay messages through nearby devices when direct connectivity is unavailable.
- Help separated travelers reconnect with their group using available location information and offline navigation resources.

**🆘 Hyperlocal SOS for Solo Travelers**
- Broadcast emergency messages to compatible nearby devices within a configurable **200–300 metre target radius**.
- Help nearby participating users identify situations requiring assistance.
- Enable local assistance even when conventional mobile connectivity is unavailable.

**🌐 Mesh-to-Internet Emergency Relay**
- Forward emergency packets through participating devices.
- Relay an SOS to the configured emergency service when a device with internet connectivity is reached.
- Guide the tourist toward a relevant pre-downloaded landmark when supported by offline maps and location data.

*Implementation note: Bluetooth mesh relaying, delivery guarantees, background execution, encryption, and emergency-service integration require dedicated engineering and real-device testing.*

### 3. 👩‍🦰 Women's Safety ML Engine

**Intelligent anomaly detection during an active journey.**

An on-device machine learning engine is designed to analyze relevant sensor signals and identify patterns that may indicate a potential emergency.

- **Motion anomaly detection:** Identify unusual movement patterns, such as sudden falls, abrupt acceleration, or unexpected inactivity.
- **Acoustic distress recognition:** Detect supported distress sounds, including screams, using an audio classification model.
- **Configurable trigger words:** Support selected spoken emergency phrases where the speech-recognition implementation allows.
- **Automated SOS workflow:** Trigger a configurable emergency response when defined risk conditions are met.
- **Edge AI processing:** Prioritize local inference to reduce unnecessary transmission of sensitive sensor data.

The system should treat model predictions as indicators of potential danger rather than definitive proof of an incident, with safeguards designed to reduce false alarms.

### 4. 🥽 AR-Powered Safe-Route Navigation

**Keep your eyes up and find a safer way forward.**

Using Google ARCore and compatible location services, Guiding Paths aims to provide camera-based augmented-reality navigation.

- **AR Green Path:** Display directional guidance over the live camera view.
- **Verified Safe Havens:** Guide users toward mapped police stations and designated safe locations.
- **Danger-zone avoidance:** Incorporate available hazard geofences into route selection.
- **Head-up navigation:** Reduce the need to repeatedly look down at a conventional 2D map.
- **Context-aware guidance:** Adapt route recommendations to available location, hazard, and landmark data.

*AR guidance depends on device compatibility, positioning accuracy, available map data, and the reliability of safety-location information.*

### 5. 🚪 Smart Exit & 1.5 km Safety Radius

**Find the nearest suitable exit when every second matters.**

Designed for large parks, nature reserves, temples, festivals, and crowded venues.

- **Multiple exit selection:** Allow users to select up to five potential exit gates.
- **Nearest-exit calculation:** Rank candidate gates by route distance and accessibility.
- **1.5 km proximity constraint:** Mark gates beyond the configured radius as **Out of Range**.
- **Route-aware decisions:** Prefer a feasible route to a suitable exit instead of relying only on straight-line distance.
- **Clear visual feedback:** Highlight the nearest eligible exit and flag unavailable or distant alternatives.

The 1.5 km constraint is a configurable product rule, not a guarantee that every in-range exit is safe or reachable.

### 6. 🚨 Multi-Modal SOS & Transparent Emergency Ledger

**Make emergency reporting faster and more accountable.**

The SOS system is designed to support multiple configurable emergency triggers.

- **Manual SOS:** Allow users to request help immediately.
- **Distress-triggered SOS:** Initiate an emergency workflow following supported acoustic distress detection.
- **Inactivity alerts:** Detect prolonged inactivity when configured thresholds and context justify an alert.
- **Geofence alerts:** Identify entry into or movement through configured restricted or hazardous areas.
- **Location-aware reporting:** Include the latest available coordinates and incident timestamp in emergency messages.
- **Audit trail:** Record SOS events, relevant timestamps, delivery status, and available authority-response information in a tamper-evident ledger.

Blockchain-backed records can improve auditability when implemented with appropriate access controls and data minimization. Sensitive personal information should not be exposed on a public blockchain.

### 7. 👥 Early-Warning Crowd & Stampede Risk Detection

**Recognize dangerous crowd conditions before they escalate.**

Popular tourist destinations, temples, festivals, and public gatherings can experience sudden overcrowding and hazardous crowd movement.

Guiding Paths aims to combine privacy-conscious crowd-density indicators with movement-anomaly analysis to identify potentially dangerous conditions.

- **Crowd-density monitoring:** Estimate crowd concentration using permitted, aggregated signals.
- **Abnormal movement detection:** Identify patterns that may indicate sudden crowd surges or chaotic movement.
- **High Crowd Alerts:** Warn nearby tourists when configured risk thresholds are exceeded.
- **Authority notifications:** Send relevant alerts to connected crowd-management authorities.
- **Safer route suggestions:** Encourage users to avoid affected areas where reliable alternate-route data is available.

The feature should rely on validated thresholds and appropriate sensor or infrastructure data; smartphone density alone cannot reliably confirm an impending stampede.

### 8. ⚖️ AI-Powered e-FIR & Tamper-Evident Evidence Ledger

**Simplify incident documentation and preserve evidence integrity.**

The AI-assisted reporting workflow is designed to help tourists turn incident descriptions into structured reports.

- **Natural-language reporting:** Convert a user's incident description into an organized draft.
- **Structured incident fields:** Capture relevant details, timestamps, locations, and supporting evidence.
- **BNS reference assistance:** Suggest potentially relevant provisions of the Bharatiya Nyaya Sanhita (BNS), subject to legal validation.
- **Evidence integrity:** Generate SHA-256 cryptographic hashes for submitted documents and supporting media.
- **Tamper-evident records:** Store hashes and relevant audit metadata in a suitable ledger to help verify whether evidence has changed.
- **Review before submission:** Allow users to verify and approve the report before submitting it through an authorized channel.

**Important:** An AI-generated report is a draft, not an automatically registered FIR. Actual FIR registration requires the appropriate authorized process. A blockchain record or cryptographic hash can help establish integrity but does not, by itself, prove that evidence is authentic or legally admissible.

### 9. 🌪️ Real-Time Disaster Alerts — SACHET Integration

**Stay informed when environmental conditions change.**

Guiding Paths is designed to integrate with the NDMA's SACHET disaster-alert platform to deliver relevant emergency information.

- **CAP alert parsing:** Process supported Common Alerting Protocol (CAP) messages from an authorized or publicly accessible SACHET feed.
- **Location-based warnings:** Identify alerts relevant to the user's location and affected area.
- **Disaster awareness:** Present supported warnings about floods, cyclones, earthquakes, and other hazards.
- **Actionable notifications:** Display available severity, affected locations, timing, and official safety instructions.
- **Travel-aware alerts:** Help users reconsider routes or destinations affected by active warnings.

Alert freshness, feed availability, location matching, and notification delivery must be validated against the actual integration.

### 10. 🤖 AI Safety Assistant & Real-Time Translator

**A multilingual companion for unfamiliar situations.**

The integrated AI assistant aims to help tourists understand local safety information and communicate during emergencies.

- **Conversational safety guidance:** Answer travel-safety questions and explain relevant emergency procedures.
- **Context-aware assistance:** Use permitted journey context to provide relevant guidance.
- **Real-time translation:** Translate supported languages to reduce communication barriers.
- **Emergency phrase support:** Help users communicate essential needs to nearby people.
- **Accessible interaction:** Support text-based interaction, with voice capabilities where implemented.
- **Responsible responses:** Prioritize verified emergency resources and official instructions in critical situations.

---

## 🛡️ Security, Privacy & Reliability

Guiding Paths is designed around the following principles:

| Principle | Implementation Goal |
|---|---|
| Privacy by design | Session-based monitoring and data minimization |
| Local-first AI | On-device inference for supported safety models |
| Secure communication | Authenticated, encrypted emergency messages |
| Offline resilience | Local maps and opportunistic P2P relaying |
| Evidence integrity | SHA-256 hashes and tamper-evident audit records |
| Responsible AI | Confidence thresholds, false-positive safeguards, and user controls |
| Transparent reporting | Clear SOS status and auditable event records |
| Human oversight | Review and authorized submission of legal reports |

## 🧰 Technology & Integration Targets

The following technologies are potential implementation choices for the platform; the final stack depends on the actual project architecture

11. Emergency Contact & Social Tracking:- Manages prioritised contacts for automated emergency notifications, alongside an opt-in social map for real-time location sharing and coordination among tourist groups. 



