package com.example.voicetranslator2.api

// 요청: 오디오 하나만 보냄
data class ServerlessRequest(
    val audio: String,
    val targetLang: String ,
    val sourceLang: String // [추가] 이 줄을 넣어주세요!// 기본값 영어. "ja"(일본어), "zh"(중국어) 등을 넣을 예정
)

// 응답: 원문, 번역문, 오디오 다 받음
data class ServerlessResponse(
    val original_text: String,
    val translated_text: String,
    val audio: String // Base64 (TTS 결과)
)