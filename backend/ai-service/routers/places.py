from fastapi import APIRouter, HTTPException
from pydantic import BaseModel
from typing import List
from services.gemini_service import get_gemini_service
import google.generativeai as genai
import json

router = APIRouter()

class Place(BaseModel):
    id: str
    name: str
    type: str # 'monument', 'hospital', 'police', 'food', 'shopping'
    latitude: float
    longitude: float
    distance: float
    rating: float

@router.get("/", response_model=List[Place])
async def get_nearby_places(lat: float, lng: float):
    try:
        service = get_gemini_service()
        # Prompt Gemini to act as a local places API
        prompt = f"""
You are a location API. The user is at GPS coordinates: {lat}, {lng}.
Identify 5-7 real, prominent places near this location that a tourist would need.
Include a mix of: monument/tourist attraction, hospital, police station, and food/shopping.
You must return the response EXACTLY as a raw JSON array of objects.
Do NOT wrap it in ```json blocks.
Each object must have:
- id: string (generate a random short string)
- name: string
- type: string (must be one of: 'monument', 'hospital', 'police', 'food', 'shopping')
- latitude: float (approximate real latitude of the place)
- longitude: float (approximate real longitude of the place)
- distance: float (approximate distance in km from user)
- rating: float (out of 5.0)

Example:
[
  {{"id": "p1", "name": "India Gate", "type": "monument", "latitude": 28.6129, "longitude": 77.2295, "distance": 1.2, "rating": 4.8}}
]
"""
        from services.key_rotator import key_manager
        response = key_manager.execute_with_retry(
            lambda _: genai.GenerativeModel('gemini-3.6-flash').generate_content(prompt)
        )
        text = response.text.strip()
        # Strip potential markdown blocks if Gemini ignores the prompt
        if text.startswith("```json"):
            text = text[7:]
        if text.endswith("```"):
            text = text[:-3]
        
        places = json.loads(text.strip())
        return places
    except Exception as e:
        # Fallback if Gemini fails
        print(f"Error fetching places from Gemini: {e}")
        return [
            {"id": "mock1", "name": "Local Monument", "type": "monument", "latitude": lat + 0.005, "longitude": lng + 0.005, "distance": 0.8, "rating": 4.5},
            {"id": "mock2", "name": "City Hospital", "type": "hospital", "latitude": lat - 0.003, "longitude": lng - 0.004, "distance": 0.5, "rating": 4.2},
            {"id": "mock3", "name": "Nearest Police Station", "type": "police", "latitude": lat + 0.007, "longitude": lng - 0.002, "distance": 1.1, "rating": 4.0},
            {"id": "mock4", "name": "Food Corner", "type": "food", "latitude": lat - 0.002, "longitude": lng + 0.006, "distance": 0.7, "rating": 4.3}
        ]
