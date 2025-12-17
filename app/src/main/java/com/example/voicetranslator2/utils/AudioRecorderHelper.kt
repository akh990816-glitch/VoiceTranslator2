package com.example.voicetranslator2.utils

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

class AudioRecorderHelper {

    private var audioRecord: AudioRecord? = null

    // [수정 1] 여러 스레드에서 접근하므로 @Volatile 추가
    @Volatile
    private var isRecording = false

    private val sampleRate = 16000
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT

    private var outputStream: ByteArrayOutputStream? = null

    @SuppressLint("MissingPermission")
    suspend fun startRecording() {
        return withContext(Dispatchers.IO) {
            val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                sampleRate,
                channelConfig,
                audioFormat,
                minBufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e("AudioRecorder", "AudioRecord 초기화 실패!")
                return@withContext
            }

            outputStream = ByteArrayOutputStream()
            val buffer = ByteArray(minBufferSize)

            audioRecord?.startRecording()
            isRecording = true

            Log.d("AudioRecorder", "녹음 시작됨")

            while (isRecording) {
                // read는 블로킹 함수입니다. 데이터가 들어올 때까지 대기합니다.
                val read = audioRecord?.read(buffer, 0, minBufferSize) ?: 0

                if (read > 0) {
                    outputStream?.write(buffer, 0, read)

                    // [수정 2] 무음(Silence) 감지 로직 추가
                    // 데이터의 앞부분 10개만 확인해서 전부 0인지 체크
                    var isSilence = true
                    for (i in 0 until minOf(read, 10)) {
                        if (buffer[i].toInt() != 0) {
                            isSilence = false
                            break
                        }
                    }
                    if (isSilence) {
                        // 로그창에 이 메시지가 도배된다면 마이크 설정 문제입니다 (PC/에뮬레이터)
                        Log.w("AudioRecorder", "경고: 무음 데이터가 들어오고 있습니다 (Data=0)")
                    }

                } else if (read < 0) {
                    Log.e("AudioRecorder", "녹음 중 에러 발생: $read")
                }
            }
        }
    }

    suspend fun stopRecording(): String? {
        return withContext(Dispatchers.IO) {
            // 1. 녹음 플래그 끄기 (startRecording의 루프가 멈추도록 유도)
            isRecording = false

            try {
                // 2. 하드웨어 정지
                audioRecord?.stop()
                audioRecord?.release()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                audioRecord = null
            }

            // 3. 데이터 변환
            // outputStream이 null이면 이미 처리된 것이므로 null 반환
            val stream = outputStream ?: return@withContext null
            val audioBytes = stream.toByteArray()

            // 메모리 해제
            try {
                stream.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            outputStream = null

            if (audioBytes.isNotEmpty()) {
                Log.d("AudioRecorder", "녹음 종료. 최종 데이터 크기: ${audioBytes.size} bytes")
                // NO_WRAP: 줄바꿈 없이 한 줄로 만듦 (구글 API 필수)
                Base64.encodeToString(audioBytes, Base64.NO_WRAP)
            } else {
                Log.e("AudioRecorder", "녹음된 데이터가 없습니다 (Byte size 0)")
                null
            }
        }
    }
}