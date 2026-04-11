// Inbound.java  (send audio -> base64)
package com.example.al_qari;

public class Inbound {
    private String audio_b64;    // e.g., WAV/MP3 bytes encoded as Base64
    private String mime;        // e.g., "audio/wav" or "audio/mpeg"

    public Inbound(String audio_b64, String mime) {
        this.audio_b64 = audio_b64;
        this.mime = mime;
    }

    public String getAudioB64() { return audio_b64; }
    public String getMime()     { return mime; }
}
