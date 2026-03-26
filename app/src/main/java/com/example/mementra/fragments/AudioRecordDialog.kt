package com.example.mementra.fragments

import android.app.AlertDialog
import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object AudioRecordDialog {

    fun show(context: Context, onRecorded: (filePath: String, durationSec: Long) -> Unit) {
        var recorder: MediaRecorder? = null
        var isRecording = false
        var startTime = 0L
        val handler = Handler(Looper.getMainLooper())

        val dir = File(context.filesDir, "media")
        dir.mkdirs()
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val filePath = File(dir, "AUDIO_${timestamp}.m4a").absolutePath

        val density = context.resources.displayMetrics.density
        val pad = (24 * density).toInt()

        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(pad, pad, pad, pad)
        }

        val tvTimer = TextView(context).apply {
            text = "00:00"
            textSize = 32f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, (16 * density).toInt())
        }
        layout.addView(tvTimer)

        val btnRecord = MaterialButton(context).apply {
            text = "Начать запись"
        }
        layout.addView(btnRecord)

        val dialog = AlertDialog.Builder(context)
            .setTitle("Запись аудио")
            .setView(layout)
            .setNegativeButton("Отмена") { d, _ ->
                if (isRecording) {
                    try {
                        recorder?.stop()
                        recorder?.release()
                    } catch (_: Exception) { }
                }
                handler.removeCallbacksAndMessages(null)
                d.dismiss()
            }
            .setCancelable(false)
            .create()

        val timerRunnable = object : Runnable {
            override fun run() {
                if (isRecording) {
                    val elapsed = (System.currentTimeMillis() - startTime) / 1000
                    val min = elapsed / 60
                    val sec = elapsed % 60
                    tvTimer.text = String.format("%02d:%02d", min, sec)
                    handler.postDelayed(this, 500)
                }
            }
        }

        btnRecord.setOnClickListener {
            if (!isRecording) {
                try {
                    recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        MediaRecorder(context)
                    } else {
                        @Suppress("DEPRECATION")
                        MediaRecorder()
                    }
                    recorder?.apply {
                        setAudioSource(MediaRecorder.AudioSource.MIC)
                        setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                        setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                        setAudioEncodingBitRate(128000)
                        setAudioSamplingRate(44100)
                        setOutputFile(filePath)
                        prepare()
                        start()
                    }
                    isRecording = true
                    startTime = System.currentTimeMillis()
                    btnRecord.text = "Остановить"
                    handler.post(timerRunnable)
                } catch (e: Exception) {
                    Timber.e(e, "Error starting audio recording")
                }
            } else {
                isRecording = false
                handler.removeCallbacksAndMessages(null)
                try {
                    recorder?.stop()
                    recorder?.release()
                    recorder = null
                    val durationSec = (System.currentTimeMillis() - startTime) / 1000
                    dialog.dismiss()
                    onRecorded(filePath, durationSec)
                } catch (e: Exception) {
                    Timber.e(e, "Error stopping audio recording")
                    dialog.dismiss()
                }
            }
        }

        dialog.show()
    }
}
