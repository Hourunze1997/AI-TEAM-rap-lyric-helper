package com.rap.lyrichelper.ui.memorize;

import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.rap.lyrichelper.R;
import com.rap.lyrichelper.data.model.Lyric;

import java.util.List;

/**
 * 背诵模式页：逐句展示，显示/隐藏切换（隐藏部分字符为下划线），
 * 上一句/下一句导航，熟练度标记（1-5），进度指示器。
 */
public class MemorizeActivity extends AppCompatActivity {

    private MemorizeViewModel viewModel;
    private TextView tvLineText;
    private TextView tvSection;
    private TextView tvProgress;
    private TextView tvProficiency;
    private LinearLayout llStars;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_memorize);

        viewModel = new ViewModelProvider(this).get(MemorizeViewModel.class);

        long lyricId = getIntent().getLongExtra("lyric_id", 0);
        if (lyricId == 0) {
            finish();
            return;
        }

        initViews();

        viewModel.loadLyric(lyricId);

        // 观察行数据
        viewModel.getLines().observe(this, lines -> {
            updateContent();
        });

        // 观察当前索引
        viewModel.getCurrentIndex().observe(this, index -> {
            updateContent();
            updateProgress();
        });

        // 观察显示状态
        viewModel.getRevealed().observe(this, revealed -> {
            updateContent();
        });

        // 观察熟练度
        viewModel.getProficiency().observe(this, level -> {
            updateStars(level);
        });

        // 观察总行数
        viewModel.getTotalLines().observe(this, total -> {
            updateProgress();
        });
    }

    private void initViews() {
        tvLineText = findViewById(R.id.tv_memorize_text);
        tvSection = findViewById(R.id.tv_section);
        tvProgress = findViewById(R.id.tv_progress);
        tvProficiency = findViewById(R.id.tv_proficiency_label);
        llStars = findViewById(R.id.ll_stars);

        Button btnPrev = findViewById(R.id.btn_prev);
        Button btnNext = findViewById(R.id.btn_next);
        Button btnReveal = findViewById(R.id.btn_reveal);

        btnPrev.setOnClickListener(v -> viewModel.prev());
        btnNext.setOnClickListener(v -> viewModel.next());
        btnReveal.setOnClickListener(v -> viewModel.toggleReveal());

        // 熟练度星级点击
        for (int i = 0; i < llStars.getChildCount(); i++) {
            final int level = i + 1;
            View star = llStars.getChildAt(i);
            star.setOnClickListener(v -> viewModel.setProficiency(level));
        }
    }

    /** 显示当前行 */
    private void updateContent() {
        List<Lyric.Line> lines = viewModel.getLines().getValue();
        Integer index = viewModel.getCurrentIndex().getValue();
        Boolean revealed = viewModel.getRevealed().getValue();

        if (lines == null || lines.isEmpty() || index == null || index >= lines.size()) {
            tvLineText.setText("");
            tvSection.setText("");
            return;
        }

        Lyric.Line line = lines.get(index);
        String text = line.text != null ? line.text : "";

        if (revealed != null && revealed) {
            tvLineText.setText(text);
        } else {
            // 隐藏部分字符为下划线
            tvLineText.setText(hideText(text));
        }

        // 段落标记
        tvSection.setText(getSectionLabel(line.section));
        tvSection.setTextColor(getSectionColor(line.section));
    }

    /** 隐藏文字：部分字符替换为下划线 */
    private String hideText(String text) {
        if (text == null || text.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.isWhitespace(c)) {
                sb.append(c);
            } else {
                // 交替显示和隐藏（显示约30%）
                if (i % 3 == 0) {
                    sb.append(c);
                } else {
                    sb.append('_');
                }
            }
        }
        return sb.toString();
    }

    private void updateProgress() {
        Integer index = viewModel.getCurrentIndex().getValue();
        Integer total = viewModel.getTotalLines().getValue();
        if (index != null && total != null) {
            tvProgress.setText(getString(R.string.line_progress, index + 1, total));
        }
    }

    private void updateStars(int level) {
        for (int i = 0; i < llStars.getChildCount(); i++) {
            TextView star = (TextView) llStars.getChildAt(i);
            if (i < level) {
                star.setTextColor(ContextCompat.getColor(this, R.color.chorus_color));
                star.setText("★");
            } else {
                star.setTextColor(ContextCompat.getColor(this, R.color.text_hint));
                star.setText("☆");
            }
        }
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
