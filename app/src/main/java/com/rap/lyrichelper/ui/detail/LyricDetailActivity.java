package com.rap.lyrichelper.ui.detail;

import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.rap.lyrichelper.R;
import com.rap.lyrichelper.data.model.Lyric;
import com.rap.lyrichelper.data.model.Recording;
import com.rap.lyrichelper.ui.editor.LyricEditActivity;
import com.rap.lyrichelper.ui.memorize.MemorizeActivity;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * 歌词详情页：展示完整内容（按段落分组着色）、关联录音播放器、编辑/删除/背诵入口。
 */
public class LyricDetailActivity extends AppCompatActivity {

    private LyricDetailViewModel viewModel;
    private MediaPlayer mediaPlayer;
    private long lyricId;
    private Button btnPlay; // 当前播放按钮

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lyric_detail);

        viewModel = new ViewModelProvider(this).get(LyricDetailViewModel.class);

        lyricId = getIntent().getLongExtra("lyric_id", 0);
        if (lyricId == 0) {
            finish();
            return;
        }

        initButtons();
        viewModel.loadLyric(lyricId);

        // 观察歌词
        viewModel.getLyric().observe(this, this::displayLyric);

        // 观察录音列表
        viewModel.getRecordings(lyricId).observe(this, this::displayRecordings);
    }

    private void initButtons() {
        Button btnEdit = findViewById(R.id.btn_edit);
        Button btnDelete = findViewById(R.id.btn_delete);
        Button btnMemorize = findViewById(R.id.btn_memorize);

        btnEdit.setOnClickListener(v -> editLyric());
        btnDelete.setOnClickListener(v -> deleteLyric());
        btnMemorize.setOnClickListener(v -> enterMemorize());
    }

    /** 展示歌词详情（按段落着色） */
    private void displayLyric(Lyric lyric) {
        if (lyric == null) return;

        TextView tvTitle = findViewById(R.id.tv_detail_title);
        tvTitle.setText(lyric.getTitle());

        TextView tvTags = findViewById(R.id.tv_detail_tags);
        if (lyric.getTags() != null && !lyric.getTags().isEmpty()) {
            tvTags.setText(android.text.TextUtils.join(" · ", lyric.getTags()));
            tvTags.setVisibility(View.VISIBLE);
        } else {
            tvTags.setVisibility(View.GONE);
        }

        // 按段落分组着色展示
        LinearLayout llContent = findViewById(R.id.ll_detail_content);
        llContent.removeAllViews();

        List<Lyric.Line> lines = Lyric.jsonToLines(lyric.getContent());
        for (Lyric.Line line : lines) {
            TextView tvLine = new TextView(this);
            tvLine.setTextSize(16);
            tvLine.setPadding(0, 8, 0, 8);

            // 段落着色
            int color = getSectionColor(line.section);
            SpannableStringBuilder builder = new SpannableStringBuilder();

            // 段落标签
            String label = getSectionLabel(line.section);
            if (!label.isEmpty()) {
                SpannableStringBuilder labelBuilder = new SpannableStringBuilder("[" + label + "] ");
                labelBuilder.setSpan(new ForegroundColorSpan(color), 0, labelBuilder.length(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                builder.append(labelBuilder);
            }

            // 歌词文本
            builder.append(line.text != null ? line.text : "");

            // 押韵标记
            if (line.rhymeMark != null && !line.rhymeMark.isEmpty()) {
                builder.append("  (" + line.rhymeMark + ")");
            }

            tvLine.setText(builder);
            llContent.addView(tvLine);
        }
    }

    /** 展示录音列表 */
    private void displayRecordings(List<Recording> recordings) {
        LinearLayout llRecordings = findViewById(R.id.ll_recordings);
        TextView tvNoRecordings = findViewById(R.id.tv_no_recordings);
        llRecordings.removeAllViews();

        if (recordings == null || recordings.isEmpty()) {
            tvNoRecordings.setVisibility(View.VISIBLE);
            return;
        }
        tvNoRecordings.setVisibility(View.GONE);

        for (Recording rec : recordings) {
            View item = getLayoutInflater().inflate(R.layout.item_recording, llRecordings, false);
            TextView tvDuration = item.findViewById(R.id.tv_rec_duration);
            Button btnPlay = item.findViewById(R.id.btn_rec_play);

            long seconds = rec.getDuration() / 1000;
            tvDuration.setText(String.format(Locale.getDefault(), "%02d:%02d",
                    seconds / 60, seconds % 60));

            btnPlay.setOnClickListener(v -> playRecording(rec.getFilePath(), btnPlay));
            llRecordings.addView(item);
        }
    }

    /** 播放录音 */
    private void playRecording(String filePath, Button playBtn) {
        // 释放之前的播放器
        releaseMediaPlayer();
        if (btnPlay != null) {
            btnPlay.setText(R.string.play);
        }

        if (filePath == null || !new File(filePath).exists()) {
            Toast.makeText(this, "录音文件不存在", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(filePath);
            mediaPlayer.prepare();
            mediaPlayer.setOnCompletionListener(mp -> {
                playBtn.setText(R.string.play);
                releaseMediaPlayer();
            });
            mediaPlayer.start();
            playBtn.setText(R.string.nav_list); // 显示"播放中"...
            btnPlay = playBtn;
        } catch (IOException e) {
            Toast.makeText(this, "播放失败", Toast.LENGTH_SHORT).show();
        }
    }

    /** 编辑歌词 */
    private void editLyric() {
        Intent intent = new Intent(this, LyricEditActivity.class);
        intent.putExtra("lyric_id", lyricId);
        startActivity(intent);
    }

    /** 删除歌词 */
    private void deleteLyric() {
        new AlertDialog.Builder(this)
                .setMessage(R.string.delete_confirm)
                .setPositiveButton(R.string.confirm, (dialog, which) -> {
                    Lyric lyric = viewModel.getLyric().getValue();
                    if (lyric != null) {
                        viewModel.getLyricRepository().deleteLyric(lyric, v -> {
                            runOnUiThread(this::finish);
                        });
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    /** 进入背诵模式 */
    private void enterMemorize() {
        Intent intent = new Intent(this, MemorizeActivity.class);
        intent.putExtra("lyric_id", lyricId);
        startActivity(intent);
    }

    private void releaseMediaPlayer() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releaseMediaPlayer();
    }

    private String getSectionLabel(String section) {
        if (section == null) return "";
        switch (section) {
            case "verse": return getString(R.string.section_verse);
            case "chorus": return getString(R.string.section_chorus);
            case "bridge": return getString(R.string.section_bridge);
            case "hook": return getString(R.string.section_hook);
            default: return section;
        }
    }

    private int getSectionColor(String section) {
        if (section == null) return ContextCompat.getColor(this, R.color.text_secondary);
        switch (section) {
            case "verse": return ContextCompat.getColor(this, R.color.verse_color);
            case "chorus": return ContextCompat.getColor(this, R.color.chorus_color);
            case "bridge": return ContextCompat.getColor(this, R.color.bridge_color);
            case "hook": return ContextCompat.getColor(this, R.color.hook_color);
            default: return ContextCompat.getColor(this, R.color.text_secondary);
        }
    }
}
