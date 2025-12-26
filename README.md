# Sign Language Gesture Android Application

An Android-based mini project that converts **speech, text, and audio input** into **visual sign language gestures** using **3D avatars and animations**.  
The application focuses on accessibility and inclusive communication for deaf and hard-of-hearing users.

---

##  Features

-  Secure Login & Registration using **Firebase Authentication**
-  Sign Language selection (ISL / ASL)
-  Text input for translation
-  Real-time speech input
-  Audio file upload and speech extraction
-  3D Avatar-based sign language gesture output
-  Fragment-based navigation (Android Navigation Component)

---

## Application Screenshots

###  Login Screen
Secure user authentication using Firebase backend.

<img width="340" height="734" alt="{E6129811-B320-4F78-9BEC-BB1DC18A527F}" src="https://github.com/user-attachments/assets/093871a4-b7f6-4697-ac95-50f2bf935253" />



---

###  Language Selection
Users can choose their preferred sign language.

<img width="324" height="705" alt="{7D9E0207-CAA1-459C-A486-DE30983BB55F}" src="https://github.com/user-attachments/assets/88871034-8ece-477f-8b53-6f4c6bda13e7" />



---

###  Sign Language Output – Male Avatar
Text input translated into sign language gestures using a 3D avatar.

<img width="337" height="720" alt="image" src="https://github.com/user-attachments/assets/7fa665b4-e2b8-4579-bc81-31a50bf7f9a1" />


---

###  Sign Language Output – Female Avatar
Alternate avatar view for sign language gesture visualization.

<img width="352" height="709" alt="{5F15EE09-5457-49BE-8C85-CF1A32B98596}" src="https://github.com/user-attachments/assets/a3270532-64d0-4c18-b461-507af208cb0c" />


---

###  Real-Time Speech Input
Speech is converted to text using ML-based speech recognition and translated into sign language.

<img width="341" height="752" alt="{DD0C3C76-2636-40EB-954A-E23337017A1B}" src="https://github.com/user-attachments/assets/1185a3cf-f903-41e1-8705-02c450b3508d" />


---

##  Voice Input Support

The application supports **real-time voice input** through the device microphone.  
Spoken words are converted into text using **Google Speech Services**, and the extracted text is then mapped to corresponding sign language gestures.

Additionally, uploaded audio files are processed using the **Vosk pre-trained ML model** for offline speech-to-text conversion.

This enables users to interact with the system using natural voice commands.

<img width="337" height="721" alt="{BAB96AAE-8D3B-4656-9AF5-24F9C32FEA96}" src="https://github.com/user-attachments/assets/c522da19-0ae0-468f-864a-c62d9b428dd7" />


##  Project Flow
<img width="440" height="670" alt="{C1A30D11-E8E6-43D1-AEB0-5C59593A39F1}" src="https://github.com/user-attachments/assets/6cf79222-d08d-4e97-9138-76d4abefd754" />

##  Machine Learning Usage

- The project uses **pre-trained ML-based speech recognition models**
  - Google Speech Services (real-time speech)
  - Vosk (audio file speech-to-text)
- No custom ML model is trained in the current version.

##  Technologies Used

- Android Studio
- Kotlin
- Firebase Authentication
- Firebase Firestore
- Vosk Speech Recognition
- Google Speech Services
- Android Navigation Component
- XML Layouts
- 3D Avatars & Animations

##  Future Enhancements

- Full Indian Sign Language (ISL) grammar support
- Custom-trained ML models for gesture recognition
- Camera-based Sign-to-Text recognition
- Advanced 3D avatar animation
- Offline translation support

---

##  Note

- This project focuses on **sign language gesture generation**, not sign recognition.
- Grammar-level sign language translation is part of future scope.

---

##  Author

**NATARAJ KUMARAN S**  
https://github.com/nataraj26/Sign-Language-MiniProject?tab=readme-ov-file

---

##  License

This project is developed as part of an academic mini project and is intended for educational purposes.

