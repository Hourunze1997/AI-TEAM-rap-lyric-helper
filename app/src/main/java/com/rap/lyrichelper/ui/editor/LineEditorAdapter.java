package com.rap.lyrichelper.ui.editor;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.rap.lyrichelper.R;
import com.rap.lyrichelper.data.model.Lyric;

import java.util.ArrayList;
import java.util.List;

/**
 * 分行编辑适配器：每行含 EditText + 段落标记 Spinner + 押韵标记按钮。
 */
public class LineEditorAdapter extends RecyclerView.Adapter<LineEditorAdapter.ViewHolder> {

    private final List<Lyric.Line> lines = new ArrayList<>();
    private final OnRhymeClickListener rhymeListener;

    /** 段落类型 */
    private static final String[] SECTIONS = {"verse", "chorus", "bridge", "hook"};
    private static final String[] SECTION_LABELS = {"主歌", "副歌", "桥段", "Hook"};

    public interface OnRhymeClickListener {
        void onRhymeClick(int position, String text);
    }

    public LineEditorAdapter(OnRhymeClickListener rhymeListener) {
        this.rhymeListener = rhymeListener;
    }

    /** 设置初始行数据 */
    public void setLines(List<Lyric.Line> newLines) {
        lines.clear();
        if (newLines != null) {
            lines.addAll(newLines);
        }
        if (lines.isEmpty()) {
            lines.add(new Lyric.Line("", "verse", ""));
        }
        notifyDataSetChanged();
    }

    /** 添加一行 */
    public void addLine() {
        lines.add(new Lyric.Line("", "verse", ""));
        notifyItemInserted(lines.size() - 1);
    }

    /** 在指定位置后插入一行 */
    public void addLineAfter(int position) {
        int pos = position + 1;
        if (pos > lines.size()) pos = lines.size();
        lines.add(pos, new Lyric.Line("", "verse", ""));
        notifyItemInserted(pos);
    }

    /** 删除一行 */
    public void removeLine(int position) {
        if (position >= 0 && position < lines.size()) {
            lines.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, lines.size() - position);
        }
    }

    /** 获取所有行数据 */
    public List<Lyric.Line> getLines() {
        return new ArrayList<>(lines);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_line_editor, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(position);
    }

    @Override
    public int getItemCount() {
        return lines.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        final EditText etText;
        final Spinner spSection;
        final Button btnRhyme;
        final Button btnDelete;
        final Button btnAdd;

        ViewHolder(View itemView) {
            super(itemView);
            etText = itemView.findViewById(R.id.et_line_text);
            spSection = itemView.findViewById(R.id.sp_section);
            btnRhyme = itemView.findViewById(R.id.btn_rhyme);
            btnDelete = itemView.findViewById(R.id.btn_delete_line);
            btnAdd = itemView.findViewById(R.id.btn_add_line);

            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                    itemView.getContext(),
                    android.R.layout.simple_spinner_item,
                    SECTION_LABELS);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spSection.setAdapter(adapter);
        }

        void bind(int position) {
            Lyric.Line line = lines.get(position);

            // 恢复文本（避免 setText 触发 watcher 循环）
            etText.removeTextChangedListener(textWatcher);
            etText.setText(line.text);
            etText.addTextChangedListener(textWatcher);

            // 设置段落选择
            int sectionIndex = 0;
            for (int i = 0; i < SECTIONS.length; i++) {
                if (SECTIONS[i].equals(line.section)) {
                    sectionIndex = i;
                    break;
                }
            }
            spSection.setSelection(sectionIndex);

            // 押韵标记
            btnRhyme.setText(line.rhymeMark != null && !line.rhymeMark.isEmpty()
                    ? line.rhymeMark : "押");

            // 按钮事件
            btnDelete.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    removeLine(pos);
                }
            });

            btnAdd.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    addLineAfter(pos);
                }
            });

            btnRhyme.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && rhymeListener != null) {
                    rhymeListener.onRhymeClick(pos, lines.get(pos).text);
                }
            });
        }

        private final TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && pos < lines.size()) {
                    lines.get(pos).text = s.toString();
                }
            }
        };
    }
}
