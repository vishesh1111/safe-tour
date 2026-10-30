from fastapi import APIRouter, UploadFile, File, HTTPException
from pydantic import BaseModel
import google.generativeai as genai
import tempfile
import os

router = APIRouter()

class DocumentOCRResponse(BaseModel):
    document_type: str
    document_number: str
    expiry_date: str
    extracted_text: str

@router.post("/extract", response_model=DocumentOCRResponse)
async def extract_document_info(file: UploadFile = File(...)):
    if not file.filename:
        raise HTTPException(status_code=400, detail="No file uploaded")
    
    # Save the uploaded file temporarily
    temp_file_path = ""
    try:
        with tempfile.NamedTemporaryFile(delete=False, suffix=".jpg") as temp_file:
            content = await file.read()
            temp_file.write(content)
            temp_file_path = temp_file.name
        
        # Upload to Gemini
        gemini_file = genai.upload_file(temp_file_path)
        
        # Initialize the model
        from services.key_rotator import key_manager
        model = genai.GenerativeModel('gemini-3.6-flash')
        
        prompt = """
        Analyze this image of a document (passport, visa, flight ticket, hotel booking, id card, etc.).
        Extract the following information:
        1. Document Type (e.g., Passport, Visa, Flight Ticket, ID Card)
        2. Document Number (if applicable, e.g., Passport Number, PNR, ID Number)
        3. Expiry Date or Validity Date (if applicable, e.g., YYYY-MM-DD or DD/MM/YYYY)
        4. Any other important text on the document.
        
        Return the result EXACTLY as a JSON object with these keys:
        {
            "document_type": "...",
            "document_number": "...",
            "expiry_date": "...",
            "extracted_text": "..."
        }
        If a field is not found, return an empty string for it.
        """
        
        response = key_manager.execute_with_retry(
            lambda _: model.generate_content([prompt, gemini_file])
        )
        
        # Parse the JSON response
        text_resp = response.text.strip()
        if text_resp.startswith('```json'):
            text_resp = text_resp[7:]
        if text_resp.endswith('```'):
            text_resp = text_resp[:-3]
        
        import json
        try:
            data = json.loads(text_resp.strip())
            return DocumentOCRResponse(
                document_type=data.get("document_type", ""),
                document_number=data.get("document_number", ""),
                expiry_date=data.get("expiry_date", ""),
                extracted_text=data.get("extracted_text", "")
            )
        except json.JSONDecodeError:
            return DocumentOCRResponse(
                document_type="Unknown",
                document_number="",
                expiry_date="",
                extracted_text=response.text
            )
            
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
    finally:
        if temp_file_path and os.path.exists(temp_file_path):
            os.remove(temp_file_path)
