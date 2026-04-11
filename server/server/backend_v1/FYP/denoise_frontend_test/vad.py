# vad.py
import torch
from pyannote.audio import Pipeline as VADPipeline
from pyannote.audio import Audio
from pyannote.core import Segment

print("🔊 Loading Pyannote VAD (CPU)...")
vad_pipeline = VADPipeline.from_pretrained("pyannote/voice-activity-detection", device="cpu")
audio_loader = Audio(sample_rate=16000)
print("✅ VAD ready (on CPU)")

def get_speech_segments(wav_path: str):
    """
    Detects and returns speech segments.
    """
    segments = vad_pipeline(wav_path)
    timeline = segments.get_timeline()
    print(f"[VAD] Detected {len(timeline)} speech segments")
    return timeline

def load_audio_segment(wav_path: str, segment):
    waveform, sample_rate = audio_loader.crop(wav_path, segment)
    if sample_rate != 16000:
        import torchaudio
        waveform = torchaudio.functional.resample(waveform, sample_rate, 16000)
    return waveform
