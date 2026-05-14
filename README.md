<div align="center">

<h1>ٱلْقَارِئ &nbsp; Al Qari</h1>

<p><strong>AI-Powered Quran Learning and Memorization Assistant</strong></p>

<p>
  <img src="https://img.shields.io/badge/Platform-Android-3DDC84?style=flat-square&logo=android&logoColor=white" alt="Android">
  <img src="https://img.shields.io/badge/Backend-Flask%20%2F%20Python-3776AB?style=flat-square&logo=python&logoColor=white" alt="Flask">
  <img src="https://img.shields.io/badge/AI-Gemini%20LLM-4285F4?style=flat-square&logo=google&logoColor=white" alt="LLMs">
  <img src="https://img.shields.io/badge/ASR-Whisper--Tarteel-black?style=flat-square" alt="Whisper Tarteel">
  <img src="https://img.shields.io/badge/Type-Final%20Year%20Project-orange?style=flat-square" alt="FYP">
</p>

<p><em>A fully voice enabled AI-powered mobile assistant for Quranic pronunciation practice and memorization, combining speech recognition, conversational AI, and a custom Makharij evaluation model in a privacy-first design.</em></p>

</div>

---

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [System Architecture](#system-architecture)
- [Core Modules](#core-modules)
- [Tech Stack](#tech-stack)
- [Speech Pipelines](#speech-pipelines)
- [Database Schema](#database-schema)
- [Design Decisions](#design-decisions)
- [Setup and Installation](#setup-and-installation)
- [API Overview](#api-overview)
- [Future Work](#future-work)
- [Team](#team)
- [License](#license)

---

## Overview

**Al Qari** is a Final Year Project that makes expert-level Quranic recitation guidance accessible on a smartphone. Traditional Quranic learning depends heavily on in-person teachers. Al Qari brings pronunciation feedback and memorization testing to anyone, anytime.

The system addresses three core challenges:

1. **Pronunciation accuracy** -- Arabic Makharij (articulation points) are difficult to learn without a teacher present.
2. **Memorization verification** -- Testing Hifz normally requires a listener to catch mistakes. Al Qari does this automatically.
3. **Consistency** -- Keeping a revision schedule is hard. Personalized reminders keep learners on track.

---

## Features

| Module | Capabilities |
|---|---|
| Conversational Tutor | Real-time speech-to-speech AI tutoring with barge-in support |
| Makharij Practice | Custom deep learning model evaluating Arabic articulation correctness |
| Memorization Tester | Tolerance-based recitation checking with session tracking and mistake detection |
| Progress Dashboard | Accuracy scores, revision history, Surah completion, practice frequency |
| Smart Reminders | Personalized revision alerts and engagement notifications via Firebase |

**System-wide highlights:**
- Audio is never stored. All speech inference is transient and in-memory only.
- Whisper-ASR-Tarteel handles Tajweed phonetics that generic ASR systems cannot.
- Native Android frontend for lower latency and better audio API access.
- Urdu display tag support for the interface.

---

## System Architecture

```
                    +------------------------+
                    |   Android Frontend     |
                    |     (Native App)       |
                    +----------+-------------+
                               |
                        REST API / Audio
                               |
                    +----------+-------------+
                    |   Flask Backend API    |
                    +----------+-------------+
                               |
          +--------------------+--------------------+
          |                    |                    |
+---------+--------+  +--------+---------+  +------+-----------+
|   Gemini LLM     |  | Whisper-Tarteel  |  |  SeamlessM4T v2  |
| Conversational   |  |  Quranic ASR     |  |    STT / TTS     |
+------------------+  +------------------+  +------------------+
                               |
                    +----------+-------------+
                    |  Custom Makharij Model |
                    |  Pronunciation Eval    |
                    +----------+-------------+
                               |
              +----------------+----------------+
              |                                 |
   +----------+---------+          +------------+----------+
   |  MySQL / SQLite DB |          |   Firebase FCM        |
   |  (Persistent Data) |          |   (Notifications)     |
   +--------------------+          +-----------------------+
```

---

## Core Modules

### 1. Conversational Learning Module

An AI speech tutor that listens and responds in real time.

**Features:**
- Real-time speech-to-speech interaction
- Barge-in and interruption handling
- Off-topic query redirection
- AI-generated contextual responses
- Urdu language tag support

**Flow:**
```
User Speech
    |
    v
SeamlessM4T STT
    |
    v
Gemini LLM
    |
    v
Response Generation
    |
    v
SeamlessM4T TTS
    |
    v
Audio Playback
```

---

### 2. Makharij Learning Module

Teaches and evaluates Arabic articulation points using a custom-trained deep learning model.

**Features:**
- Interactive Makharij pronunciation practice
- Real-time correctness feedback
- Per-phoneme articulation evaluation
- AI-generated corrective guidance

**Flow:**
```
User Pronunciation
    |
    v
Audio Preprocessing
    |
    v
Custom Makharij Model
    |
    v
Correctness Score
    |
    v
Feedback to User
```

---

### 3. Memorization Tester Module

Tests Hifz (memorization) of Quranic verses with tolerance-based checking.

**Features:**
- Multi-session memorization testing per Surah
- Tolerance-based comparison (not strict exact-match)
- Mistake type detection and localization
- Session history and revision support

**Flow:**
```
User Recitation
    |
    v
Whisper-ASR-Tarteel
    |
    v
Arabic Text Extraction
    |
    v
Tolerance-Based Comparator
    |
    +-------> Match     --> Mark correct, advance
    |
    +-------> Mismatch  --> Classify mistake
                                |
                                v
                         Feedback Generation
                                |
                                v
                       Update Session and Progress
```

---

### 4. Progress Tracking Dashboard

Tracks learning and memorization progress across all sessions.

**Metrics tracked:**
- Surahs memorized and in progress
- Per-session accuracy scores
- Mistake frequency and error patterns
- Revision timestamps
- Practice frequency trends

---

### 5. Personalized Notifications

Keeps users consistent with their learning schedule.

- Revision reminders based on last practice date
- Engagement alerts for inactive users
- Milestone notifications
- Powered by Firebase Cloud Messaging (FCM)

---

## Tech Stack

| Layer | Technology |
|---|---|
| Frontend | Android Native (Java / Kotlin) |
| Backend | Python, Flask |
| Conversational AI | Google Gemini LLM |
| Quranic ASR | Whisper-ASR-Tarteel |
| STT / TTS | SeamlessM4T v2 |
| Pronunciation Evaluation | Custom Deep Learning Model |
| Database | MySQL (central server), SQLite (local cache) |
| Notifications | Firebase Cloud Messaging |
| Audio Processing | Python audio preprocessing pipeline |

---

## Speech Pipelines

### Conversational Module

```
User Speech  -->  SeamlessM4T STT  -->  Gemini LLM  -->  SeamlessM4T TTS  -->  Audio Playback
```

### Memorization Tester

```
User Recites  -->  Whisper-ASR-Tarteel  -->  Text Extraction  -->  Tolerance Comparator  -->  Feedback
```

### Makharij Evaluation

```
User Pronounces  -->  Audio Preprocessing  -->  Custom Model  -->  Correctness Score  -->  Feedback
```

---

## Database Schema

### users

```sql
CREATE TABLE users (
    user_id     INT PRIMARY KEY AUTO_INCREMENT,
    name        VARCHAR(100),
    email       VARCHAR(255) UNIQUE,
    created_at  DATETIME
);
```

### surahs

```sql
CREATE TABLE surahs (
    surah_id     INT PRIMARY KEY,
    name_arabic  VARCHAR(100),
    name_urdu    VARCHAR(100),
    verse_count  INT
);
```

### memorization_sessions

```sql
CREATE TABLE memorization_sessions (
    session_id  INT PRIMARY KEY AUTO_INCREMENT,
    user_id     INT,
    surah_id    INT,
    accuracy    FLOAT,
    mistakes    JSON,
    started_at  DATETIME,
    ended_at    DATETIME,
    FOREIGN KEY (user_id)  REFERENCES users(user_id),
    FOREIGN KEY (surah_id) REFERENCES surahs(surah_id)
);
```

### progress

```sql
CREATE TABLE progress (
    user_id         INT,
    surah_id        INT,
    completion_pct  FLOAT,
    last_revised    DATETIME,
    revision_count  INT,
    PRIMARY KEY (user_id, surah_id),
    FOREIGN KEY (user_id)  REFERENCES users(user_id),
    FOREIGN KEY (surah_id) REFERENCES surahs(surah_id)
);
```

### notifications

```sql
CREATE TABLE notifications (
    notification_id  INT PRIMARY KEY AUTO_INCREMENT,
    user_id          INT,
    type             VARCHAR(50),
    scheduled_at     DATETIME,
    sent             BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (user_id) REFERENCES users(user_id)
);
```

---

## Design Decisions

### Privacy-First Architecture

Audio is never stored. All speech processing happens in-memory and is discarded after inference. This is a deliberate choice given the personal nature of Quranic recitation practice.

### Native Android Frontend

Chosen over cross-platform frameworks (React Native, Flutter) for:
- Lower audio capture and playback latency
- Direct access to Android audio APIs
- More reliable real-time streaming and barge-in behavior

### Specialized Quranic ASR

Generic speech recognition fails on Quranic Arabic because it cannot handle:
- Tajweed rules and recitation patterns
- Madd (elongations) and Ghunnah (nasalization)
- Arabic phonemes absent from standard training data

Whisper-ASR-Tarteel is fine-tuned specifically on Quranic recitation data, making it the right tool for this task.

### Tolerance-Based Memorization Checking

Strict exact-match would penalize acceptable phonetic variations. The tolerance system uses configurable similarity thresholds to distinguish genuine mistakes from minor acceptable differences.

---

## Setup and Installation

### Prerequisites

- Python 3.10 or higher
- Android Studio
- MySQL server
- Firebase project with Cloud Messaging enabled

### Backend Setup

```bash
# Clone the repository
git clone https://github.com/your-username/al-qari.git
cd al-qari/backend

# Create a virtual environment
python -m venv venv
source venv/bin/activate
# On Windows: venv\Scripts\activate

# Install dependencies
pip install -r requirements.txt

# Configure environment variables
cp .env.example .env
# Open .env and fill in your credentials (see below)

# Run database migrations
python manage.py migrate

# Start the Flask server
python app.py
```

### Environment Variables

Create a `.env` file in the `/backend` directory with the following:

```
GEMINI_API_KEY=your_gemini_api_key
DB_HOST=localhost
DB_NAME=alqari
DB_USER=root
DB_PASSWORD=your_password
FIREBASE_SERVER_KEY=your_fcm_server_key
```

### Android Setup

1. Open the `/android` folder in Android Studio.
2. Update `BASE_URL` in `Constants.kt` (or `Constants.java`) to your Flask server address.
3. Add `google-services.json` from your Firebase project into the `/app` directory.
4. Build and run on a physical device or emulator.

---

## API Overview

| Endpoint | Method | Description |
|---|---|---|
| `/api/conversation/start` | POST | Start a new conversational session |
| `/api/conversation/respond` | POST | Send user audio, receive AI response audio |
| `/api/makharij/evaluate` | POST | Submit pronunciation audio for Makharij evaluation |
| `/api/memorization/session/start` | POST | Begin a memorization test session |
| `/api/memorization/evaluate` | POST | Submit recitation audio for checking |
| `/api/progress/:user_id` | GET | Fetch all progress data for a user |
| `/api/notifications/schedule` | POST | Schedule a revision reminder notification |

---

## Future Work

- Reduced latency
- Streak tracking and gamification layer
- Full Urdu interface with RTL layout support
- Cloud deployment with scalable inference endpoints
- iOS port

---

## Team

**Final Year Project**
Department of Artificial Intelligence and Data Science
National University of Computer & Emerging Sciences
Academic Year: 2022 -- 2026

| Name | Role |
|---|---|
| Khadija Haider | AI Researcher & Developer |
| Maheen Rasool | Android Developer & Database handler |

**Supervisor:** Dr. Usama Imtiaz
**Co- supervisor:** Mr. Aadil ur Rehman

---

## License

This project is licensed under the Apache 2.0 License.

```
Apache 2.0 License

Copyright (c) 2025 Al Qari Team

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

---

<div align="center">
<p>Built with the intention of making Quranic learning more accessible.</p>
</div>
