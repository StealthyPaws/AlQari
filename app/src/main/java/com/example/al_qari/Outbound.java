// Outbound.java  (receive audio -> base64)
package com.example.al_qari;

import com.google.gson.annotations.SerializedName;

public class Outbound {

    @SerializedName("audio_b64")
    private String audioB64;  // maps to JSON audio_b64

    private String mime;

    private String intent;

    public String getAudioB64() { return audioB64; }
    public String getMime() { return mime; }
    public String getIntent() { return intent; }
    public void setIntent(String intent) { this.intent = intent; }
}

