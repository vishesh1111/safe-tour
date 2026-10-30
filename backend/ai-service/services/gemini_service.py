import google.generativeai as genai
import json
import re
from typing import Optional
from services.key_rotator import key_manager

SYSTEM_PROMPT = """You are a concise Indian tourism and safety assistant. Help tourists with monuments, customs, safety, and navigation in India. Be brief and helpful.
Reply in JSON: {"text": "your response", "action_buttons": [{"label": "btn", "action": "act"}]}
Keep text under 150 words. Max 2 action buttons. No image_urls needed."""

def extract_json(text: str) -> dict:
    """Extract JSON from Gemini response, handling markdown code blocks."""
    match = re.search(r'```(?:json)?\s*(\{.*?\})\s*```', text, re.DOTALL)
    if match:
        return json.loads(match.group(1), strict=False)
    match = re.search(r'\{.*\}', text, re.DOTALL)
    if match:
        return json.loads(match.group(0), strict=False)
    raise json.JSONDecodeError("No JSON found", text, 0)

class GeminiService:
    def _get_model(self):
        return genai.GenerativeModel(
            'gemini-3.6-flash',
            generation_config=genai.types.GenerationConfig(
                temperature=0.7,
                response_mime_type="application/json"
            )
        )

    def _get_vision_model(self):
        return genai.GenerativeModel('gemini-3.6-flash')
        
    async def chat(self, message: str, location: Optional[str], language: str):
        prompt = f"System: {SYSTEM_PROMPT}\nLocation: {location}\nLang: {language}\nUser: {message}"
        response = key_manager.execute_with_retry(lambda _: self._get_model().generate_content(prompt))
        try:
            parsed = extract_json(response.text)
            if 'image_urls' not in parsed:
                parsed['image_urls'] = []
            if 'action_buttons' not in parsed:
                parsed['action_buttons'] = []
            return parsed
        except Exception:
            clean_text = response.text.replace('{"text":', '').replace('}', '').replace('"', '').strip()
            return {"text": clean_text, "image_urls": [], "action_buttons": []}

    async def structure_fir(self, description: str):
        prompt = f"""Analyze this incident description and output a structured JSON for an e-FIR.
Extract BNS (Bharatiya Nyaya Sanhita) sections instead of IPC.
Format:
{{
    "complainant_details": "extracted or unknown",
    "incident_type": "type of incident",
    "bns_sections": ["section 1", "section 2"],
    "date_time_of_occurrence": "extracted or unknown",
    "place_of_occurrence": "extracted or unknown",
    "description_of_accused": "extracted or none",
    "property_lost": "extracted or none",
    "witnesses": "extracted or none",
    "narrative": "formalized narrative of {description}"
}}
Description: {description}"""
        response = key_manager.execute_with_retry(lambda _: self._get_model().generate_content(prompt))
        try:
            return json.loads(response.text.strip('```json\n').strip('```'))
        except json.JSONDecodeError:
            raise ValueError("Failed to structure FIR data")

    async def analyze_image(self, image):
        prompt = """Analyze this image for an incident report. Return ONLY JSON format:
{
    "scene_description": "detailed description",
    "detected_objects": ["obj1", "obj2"],
    "potential_incident_type": "type",
    "severity_assessment": "low/medium/high/critical"
}"""
        response = key_manager.execute_with_retry(lambda _: self._get_vision_model().generate_content([prompt, image]))
        try:
            return json.loads(response.text.strip('```json\n').strip('```'))
        except json.JSONDecodeError:
            raise ValueError("Failed to parse vision response")

    async def translate(self, text: str, source_lang: str, target_lang: str):
        prompt = f"Translate the following text from {source_lang} to {target_lang}. Return ONLY the translated text.\nText: {text}"
        response = key_manager.execute_with_retry(lambda _: self._get_model().generate_content(prompt))
        return response.text.strip()

_service_instance = None

def get_gemini_service():
    global _service_instance
    if _service_instance is None:
        _service_instance = GeminiService()
    return _service_instance
