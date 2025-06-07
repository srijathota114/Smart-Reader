# 👁️‍🗨️ Smart Reader for Blind People

**Smart Reader** is an Android application designed to assist visually impaired individuals by converting printed or digital text into speech. Leveraging OCR (Optical Character Recognition), Text-to-Speech (TTS), and accessibility services, the app helps users read books, documents, and signs with ease using their smartphone camera.

---

## ✨ Features

- 📷 **Camera-based Text Recognition** using Google ML Kit or Tesseract OCR
- 🔊 **Text-to-Speech (TTS)** functionality using Google Text-to-Speech or Amazon Polly
- 🧭 **Voice Commands** for hands-free operation
- 🌐 **Language Translation Support** (optional) using Google Translate API
- ♿ **Accessibility Enhancements** (TalkBack compatibility, large buttons, simple UI)
- 📂 **Text History** to save or replay previously read content
- 🔄 **Auto Language Detection** for multilingual environments

---

## 🛠️ Tech Stack

- **Frontend**: Android (Java/Kotlin)
- **OCR**: [Google ML Kit](https://developers.google.com/ml-kit/vision/text-recognition) / Tesseract OCR
- **TTS**: Google TTS / Amazon Polly
- **Camera**: Android CameraX
- **Translation API**: Google Translate / Microsoft Translator
- **Accessibility Services**: Android AccessibilityManager, TalkBack support

---

## 🧑‍💻 Installation

1. Clone this repository:

```bash
git clone https://github.com/your-username/smart-reader-blind.git

## 🧪 How It Works

1. **Capture Image** – User captures text using the phone camera.  
2. **Extract Text** – OCR processes the image and extracts readable text.  
3. **Speak Out** – The app reads the text aloud using TTS.  
4. **Optional Translation** – If enabled, text is translated before being spoken.

---

## 🔒 Permissions Required

- `CAMERA` – To capture images of text  
- `INTERNET` – For cloud-based OCR/translation (if used)  
- `RECORD_AUDIO` – For voice command support  
- `READ_EXTERNAL_STORAGE` – For loading images (optional)

---

## 🤝 Contributing

Contributions are welcome! If you'd like to improve the app, feel free to:

- Fork the repo  
- Create a feature branch  
- Submit a pull request

---

## 🧠 Future Enhancements

- Braille display integration  
- Offline OCR and TTS  
- Real-time scene description  
- Voice-controlled navigation

---

## 📄 License

This project is licensed under the MIT License.

---

## 🙌 Acknowledgements

- Google ML Kit  
- Tesseract OCR  
- Google Text-to-Speech  
- Android Accessibility

---
