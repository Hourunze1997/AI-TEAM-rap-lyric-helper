package com.rap.lyrichelper.audio;

import android.content.Context;
import android.media.MediaRecorder;
import android.os.SystemClock;

import com.rap.lyrichelper.data.model.Recording;

import java.io.File;
import java.io.IOException;

/**
 * 录音管理：使用 MediaRecorder 录制 AAC 到 app 私有目录。
 */
public class AudioRecorderManager {

    private MediaRecorder recorder;
    private String currentFilePath;
    private long startTime;
    private long lyricId;

    /**
     * 开始录音，文件保存到 app 私有目录 rec_<lyricId>_<timestamp>.aac。
     */
    public void startRecording(Context ctx, long lyricId) {
        this.lyricId = lyricId;
        File dir = ctx.getFilesDir();
        currentFilePath = new File(dir,
                "rec_" + lyricId + "_" + System.currentTimeMillis() + ".aac").getAbsolutePath();

        try {
            recorder = new MediaRecorder();
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.AAC_ADTS);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            recorder.setAudioSamplingRate(44100);
            recorder.setAudioEncodingBitRate(128000);
            recorder.setOutputFile(currentFilePath);
            recorder.prepare();
            recorder.start();
            startTime = SystemClock.elapsedRealtime();
        } catch (IOException | RuntimeException e) {
            if (recorder != null) {
                recorder.release();
                recorder = null;
            }
        }
    }

    /**
     * 停止录音返回 Recording 对象（含 file_path/duration）。
     */
    public Recording stopRecording() {
        if (recorder == null) return null;

        long duration = SystemClock.elapsedRealtime() - startTime;

        try {
            recorder.stop();
        } catch (RuntimeException e) {
            // 录音时间过短可能抛异常，忽略
        }
        recorder.release();
        recorder = null;

        Recording recording = new Recording();
        recording.setLyricId(lyricId);
        recording.setFilePath(currentFilePath);
        recording.setDuration(duration);
        recording.setCreatedAt(System.currentTimeMillis());

        return recording;
    }

    /** 获取当前录音已录时长（毫秒） */
    public long getDuration() {
        if (startTime == 0) return 0;
        return SystemClock.elapsedRealtime() - startTime;
    }

    /** 是否正在录音 */
    public boolean isRecording() {
        return recorder != null;
    }
}
