import os
from contextlib import asynccontextmanager
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from dotenv import load_dotenv
import google.generativeai as genai

from routers import chatbot, efir, incidents, translator, places, documents

load_dotenv()

@asynccontextmanager
async def lifespan(app: FastAPI):
    # Initialize Gemini client & key pool
    try:
        from services.key_rotator import key_manager
        key_manager.reload_keys()
        if key_manager._keys:
            genai.configure(api_key=key_manager.get_next_key())
    except Exception as e:
        print(f"Key manager initialization notice: {e}")
    yield
    # Cleanup if needed

app = FastAPI(title="Smart Tourist Safety AI Service", lifespan=lifespan)

# CORS setup
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(chatbot.router, prefix="/api/chat", tags=["Chatbot"])
app.include_router(efir.router, prefix="/api/efir", tags=["e-FIR"])
app.include_router(incidents.router, prefix="/api/incidents", tags=["Incidents"])
app.include_router(translator.router, prefix="/api/translate", tags=["Translator"])
app.include_router(places.router, prefix="/api/places", tags=["Places"])
app.include_router(documents.router, prefix="/api/documents", tags=["Documents"])

@app.get("/health")
async def health_check():
    return {"status": "ok", "service": "AI Service"}
