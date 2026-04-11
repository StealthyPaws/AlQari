package com.example.al_qari;
public class InboundPractice {
    private String audio_b64;
    private String gt;
    private String mime;

    public InboundPractice(String audio_b64, String gt, String mime) {
        this.audio_b64 = audio_b64;
        this.gt = gt;
        this.mime = mime;
    }
}
