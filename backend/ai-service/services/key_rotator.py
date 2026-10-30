import os
import random
import logging
from typing import List, Callable, Any, Optional
import google.generativeai as genai

logger = logging.getLogger("gemini_key_rotator")

class GeminiKeyManager:
    """
    Manages a pool of Gemini API keys with automatic shuffling,
    round-robin rotation, and failover retry handling.
    """
    def __init__(self):
        self._keys: List[str] = []
        self._current_index = 0
        self.reload_keys()

    def reload_keys(self):
        raw_keys_env = os.getenv("GEMINI_API_KEYS", "")
        single_key_env = os.getenv("GEMINI_API_KEY", "")

        candidates = []
        for raw in [raw_keys_env, single_key_env]:
            for item in raw.replace(";", ",").replace("\n", ",").split(","):
                k = item.strip().strip("'\"")
                if k and k not in candidates:
                    candidates.append(k)

        self._keys = candidates
        if self._keys:
            random.shuffle(self._keys)  # Randomize on startup
        self._current_index = 0
        logger.info(f"Loaded {len(self._keys)} Gemini API key(s) with shuffling enabled.")

    def get_next_key(self) -> str:
        if not self._keys:
            raise ValueError("No Gemini API keys found. Set GEMINI_API_KEYS or GEMINI_API_KEY in .env")
        key = self._keys[self._current_index % len(self._keys)]
        self._current_index = (self._current_index + 1) % len(self._keys)
        return key

    def mark_key_failed(self, failed_key: str, reason: str = ""):
        if failed_key in self._keys and len(self._keys) > 1:
            self._keys.remove(failed_key)
            self._keys.append(failed_key)  # Send failed key to the end
            logger.warning(f"Demoted Gemini key {failed_key[:12]}... Reason: {reason}")

    def execute_with_retry(self, fn: Callable[[str], Any], max_attempts: Optional[int] = None) -> Any:
        attempts = max_attempts or len(self._keys) or 1
        last_error = None

        for attempt in range(attempts):
            key = self.get_next_key()
            try:
                genai.configure(api_key=key)
                return fn(key)
            except Exception as e:
                last_error = e
                logger.warning(f"Attempt {attempt + 1}/{attempts} failed with key {key[:12]}...: {e}")
                self.mark_key_failed(key, str(e))
                if attempt == attempts - 1:
                    break

        raise last_error or RuntimeError("All Gemini API keys failed.")

key_manager = GeminiKeyManager()
