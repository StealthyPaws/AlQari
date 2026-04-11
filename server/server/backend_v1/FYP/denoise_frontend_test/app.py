# app.py
from flask import Flask, request
from stt import transcribe_audio
import tempfile

app = Flask(__name__)

@app.route("/process_audio", methods=["POST"])
def process_audio():
    file = request.files["audio"]
    tmp = tempfile.NamedTemporaryFile(delete=False, suffix=".wav")
    file.save(tmp.name)
    transcribe_audio(tmp.name)  # just prints result
    return "✅ Transcription printed on backend console."

if __name__ == "__main__":
    app.run(port=5000)
