package com.example.voicetranslator2

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.voicetranslator2.api.RetrofitClient
import com.example.voicetranslator2.api.ServerlessRequest
import com.example.voicetranslator2.databinding.ActivityMainBinding
import com.example.voicetranslator2.utils.AudioRecorderHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val audioRecorder = AudioRecorderHelper()
    private var isRecording = false

    // DB 및 리스트 관련
    private lateinit var db: AppDatabase
    private lateinit var historyAdapter: HistoryAdapter
    private val historyList = mutableListOf<HistoryItem>()

    // 언어 설정 관련
    private var currentTargetLangCode = "en"
    private var currentSourceLangCode = "ko"

    private var currentSourceLangName = "Korean"
    private var currentTargetLangName = "English"

    // 권한 요청 런처
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Toast.makeText(this, "권한 허용됨.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "마이크 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. DB 초기화
        db = AppDatabase.getDatabase(this)

        // 2. RecyclerView 설정
        historyAdapter = HistoryAdapter(historyList)
        binding.rvHistory.apply {
            adapter = historyAdapter
            layoutManager = LinearLayoutManager(this@MainActivity)
        }

        // 3. 버튼 리스너
        binding.btnRecord.setOnClickListener {
            if (checkPermission()) { handleRecordButton() }
            else { requestPermission() }
        }

        binding.tvSourceLang.setOnClickListener {
            showLanguageSelectionDialog(isSource = true)
        }

// 2. 대상 언어 선택 (Target)
        binding.tvTargetLang.setOnClickListener {
            showLanguageSelectionDialog(isSource = false)
        }

        // 4. 앱 시작 시 로컬 DB 기록 불러오기
        loadHistoryFromDb()
    }

    private fun loadHistoryFromDb() {
        lifecycleScope.launch(Dispatchers.IO) {
            val savedHistory = db.historyDao().getAll()
            val mappedItems = savedHistory.map { entity ->
                HistoryItem(
                    original = entity.original,
                    translated = entity.translated,
                    langCode = entity.langCode,
                    timestamp = entity.timestamp.toString()
                )
            }
            withContext(Dispatchers.Main) {
                historyList.clear()
                historyList.addAll(mappedItems)
                historyAdapter.notifyDataSetChanged()
            }
        }
    }

    private fun showLanguageSelectionDialog(isSource: Boolean) {
        val languages = arrayOf("Korean", "English", "Japanese", "Chinese")
        val codes = arrayOf("ko", "en", "ja", "zh")

        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle(if (isSource) "출발 언어 선택" else "대상 언어 선택")

        builder.setItems(languages) { _, which ->
            if (isSource) {
                currentSourceLangName = languages[which]
                currentSourceLangCode = codes[which]
                // XML에 tvSourceLang ID를 가진 뷰가 있어야 합니다.
                binding.tvSourceLang.text = currentSourceLangName
                Toast.makeText(this, "출발 언어: $currentSourceLangName", Toast.LENGTH_SHORT).show()
            } else {
                currentTargetLangName = languages[which]
                currentTargetLangCode = codes[which]
                // XML에 tvTargetLang ID를 가진 뷰가 있어야 합니다.
                binding.tvTargetLang.text = currentTargetLangName
                Toast.makeText(this, "대상 언어: $currentTargetLangName", Toast.LENGTH_SHORT).show()
            }
        }

        // 2. builder 설정이 모두 끝난 뒤에 show()를 호출해야 합니다.
        builder.show()
    }


    private fun startVoiceTranslation(base64Audio: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) {
                    binding.tvInputText.text = "서버 전송 중..."
                    binding.progressBar.visibility = View.VISIBLE
                }

                val request = ServerlessRequest(
                    audio = base64Audio,
                    sourceLang = currentSourceLangCode,
                    targetLang = currentTargetLangCode
                )

                val response = RetrofitClient.service.processAudio(request).execute()

                if (response.isSuccessful && response.body() != null) {
                    val result = response.body()!!

                    // DB 저장용 Entity 생성
                    val entity = HistoryEntity(
                        original = result.original_text,
                        translated = result.translated_text,
                        langCode = currentTargetLangCode,
                        timestamp = System.currentTimeMillis()
                    )
                    // DB 저장
                    db.historyDao().insert(entity)

                    withContext(Dispatchers.Main) {
                        binding.tvInputText.text = result.original_text
                        binding.tvOutputText.text = result.translated_text
                        binding.progressBar.visibility = View.GONE

                        // 리스트 최상단에 추가
                        historyAdapter.addItem(
                            HistoryItem(entity.original, entity.translated, entity.langCode, entity.timestamp.toString())
                        )
                        binding.rvHistory.scrollToPosition(0)

                        playAudio(result.audio)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this@MainActivity, "에러: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun handleRecordButton() {
        if (!isRecording) {
            isRecording = true
            binding.btnRecord.text = "⏹️ 중지"
            binding.btnRecord.backgroundTintList = ContextCompat.getColorStateList(this, android.R.color.holo_red_light)
            binding.tvInputText.text = "듣고 있습니다..."
            binding.progressBar.visibility = View.VISIBLE
            lifecycleScope.launch { audioRecorder.startRecording() }
        } else {
            isRecording = false
            binding.btnRecord.text = "🎤 눌러서 말하기"
            binding.btnRecord.backgroundTintList = ContextCompat.getColorStateList(this, android.R.color.holo_purple)
            lifecycleScope.launch {
                val audio = audioRecorder.stopRecording()
                if (audio != null) startVoiceTranslation(audio)
                else binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun checkPermission() = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    private fun requestPermission() = requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)

    private fun playAudio(base64Audio: String) {
        try {
            val audioBytes = Base64.decode(base64Audio, Base64.DEFAULT)
            val tempFile = File.createTempFile("tts_audio", ".mp3", cacheDir)
            FileOutputStream(tempFile).use { it.write(audioBytes) }
            MediaPlayer().apply {
                setDataSource(tempFile.absolutePath)
                prepare()
                start()
            }
        } catch (e: Exception) { Log.e("Audio", "Play failed", e) }
    }
}