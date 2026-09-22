package com.rap.lyrichelper.ui.editor;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.rap.lyrichelper.R;
import com.rap.lyrichelper.audio.AudioRecorderManager;
import com.rap.lyrichelper.data.model.Lyric;
import com.rap.lyrichelper.data.model.Recording;
import com.rap.lyrichelper.ui.rhyme.RhymePanelFragment;

import java.util.List;

/**
 * 歌词编辑页：标题输入、分行编辑、段落标记、押韵推荐、保存、录音。
 */
public class LyricEditActivity extends AppCompatActivity
        implements LineEditorAdapter.OnRhymeClickListener,
                   RhymePanelFragment.OnWordInsertListener {

    private static final int REQUEST_RECORD_AUDIO = 100;

    private LyricEditViewModel viewModel;
    private LyricEditViewModel getViewModel() { return viewModel; }

    private EditText etTitle;
    private EditText etTags;
    private LineEditorAdapter lineAdapter;
    private AudioRecorderManager audioManager;

    private long currentLyricId = 0;
    private boolean isRecording = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lyric_edit);

        viewModel = new ViewModelProvider(this).get(LyricEditViewModel.class);

        currentLyricId = getIntent().getLongExtra("lyric_id", 0);

        initViews();

        // 加载歌词数据
        viewModel.loadLyric(currentLyricId, lyric -> {
            if (lyric != null) {
                currentLyricId = lyric.getId();
                etTitle.setText(lyric.getTitle());
                if (lyric.getTags() != null) {
                    etTitle.setText(lyric.getTitle());
                    etTags.setText(TextUtils.join(", ", lyric.getTags()));
                }
                List<Lyric.Line> lines = Lyric.jsonToLines(lyric.getContent());
                lineAdapter.setLines(lines);
            }
        });
    }

    private void initViews() {
        etTitle = findViewById(R.id.et_title);
        etTags = findViewById(R.id.et_tags);

        RecyclerView rvLines = findViewById(R.id.rv_lines);
        rvLines.setLayoutManager(new LinearLayoutManager(this));
        lineAdapter = new LineEditorAdapter(this);
        rvLines.setAdapter(lineAdapter);

        Button btnAddLine = findViewById(R.id.btn_add_line);
        btnAddLine.setOnClickListener(v -> lineAdapter.addLine());

        Button btnRhyme = findViewById(R.id.btn_rhyme_suggest);
        btnRhyme.setOnClickListener(v -> openRhymePanel(-1, ""));

        Button btnSave = findViewById(R.id.btn_save);
        btnSave.setOnClickListener(v -> saveLyric());

        Button btnRecord = findViewById(R.id.btn_record);
        btnRecord.setOnClickListener(v -> {
            if (isRecording) {
                stopRecording(btnRecord);
            } else {
                startRecording(btnRecord);
            }
        });
    }

    /** 保存歌词 */
    private void saveLyric() {
        String title = etTitle.getText().toString().trim();
        if (title.isEmpty()) {
            Toast.makeText(this, R.string.save_failed, Toast.LENGTH_SHORT).show();
            return;
        }

        Lyric lyric = new Lyric();
        lyric.setId(currentLyricId);
        lyric.setTitle(title);

        // 解析标签
        String tagsStr = etTags.getText().toString().trim();
        if (!tagsStr.isEmpty()) {
            String[] tagArray = tagsStr.split("[,，]");
            java.util.List<String> tagList = new java.util.ArrayList<>();
            for (String tag : tagArray) {
                String t = tag.trim();
                if (!t.isEmpty()) tagList.add(t);
            }
            lyric.setTags(tagList);
        }

        // 序列化行数据
        List<Lyric.Line> lines = lineAdapter.getLines();
        lyric.setContent(Lyric.linesToJson(lines));
        lyric.setProficiency(1);

        if (currentLyricId != 0) {
            // 保留原 created_at
            viewModel.loadLyric(currentLyricId, original -> {
                if (original != null) {
                    lyric.setCreatedAt(original.getCreatedAt());
                    lyric.setProficiency(original.getProficiency());
                }
                doSave(lyric);
            });
        } else {
            doSave(lyric);
        }
    }

    private void doSave(Lyric lyric) {
        viewModel.save(lyric, id -> {
            runOnUiThread(() -> {
                currentLyricId = id;
                Toast.makeText(this, R.string.save_success, Toast.LENGTH_SHORT).show();
            });
        });
    }

    /** 打开押韵推荐面板 */
    private void openRhymePanel(int linePosition, String inputText) {
        RhymePanelFragment fragment = RhymePanelFragment.newInstance(linePosition, inputText);
        fragment.setOnWordInsertListener(this);
        fragment.show(getSupportFragmentManager(), "rhyme_panel");
    }

    @Override
    public void onRhymeClick(int position, String text) {
        openRhymePanel(position, text);
    }

    /** 押韵面板插入词回调 */
    @Override
    public void onWordInsert(int linePosition, String word) {
        if (linePosition >= 0 && linePosition < lineAdapter.getItemCount()) {
            // 将推荐词追加到对应行
            List<Lyric.Line> lines = lineAdapter.getLines();
            String currentText = lines.get(linePosition).text;
            if (currentText != null && !currentText.isEmpty()
                    && !currentText.endsWith(" ")) {
                currentText += " ";
            }
            lines.get(linePosition).text = currentText + word;
            lineAdapter.setLines(lines);
        }
    }

    /** 开始录音 */
    private void startRecording(Button btnRecord) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    REQUEST_RECORD_AUDIO);
            return;
        }

        if (audioManager == null) {
            audioManager = new AudioRecorderManager();
        }
        long lyricId = currentLyricId > 0 ? currentLyricId : System.currentTimeMillis();
        audioManager.startRecording(this, lyricId);
        isRecording = true;
        btnRecord.setText(R.string.stop_recording);
    }

    /** 停止录音 */
    private void stopRecording(Button btnRecord) {
        if (audioManager != null && audioManager.isRecording()) {
            Recording recording = audioManager.stopRecording();
            if (recording != null && currentLyricId > 0) {
                recording.setLyricId(currentLyricId);
                viewModel.getRecordingRepository().saveRecording(recording, id -> {});
            }
        }
        isRecording = false;
        btnRecord.setText(R.string.start_recording);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Button btnRecord = findViewById(R.id.btn_record);
                startRecording(btnRecord);
            } else {
                Toast.makeText(this, R.string.record_permission_denied, Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (audioManager != null && audioManager.isRecording()) {
            audioManager.stopRecording();
        }
    }
}
