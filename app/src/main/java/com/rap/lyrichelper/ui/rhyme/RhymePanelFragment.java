package com.rap.lyrichelper.ui.rhyme;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.rap.lyrichelper.R;
import com.rap.lyrichelper.data.model.RhymeWord;
import com.rap.lyrichelper.rhyme.RhymeMatcher;

import java.util.ArrayList;
import java.util.List;

/**
 * 押韵推荐面板（BottomSheet 形式）。
 * 输入词框、押韵类型选择（单押/双押）、推荐词列表、点击插入回调编辑页。
 */
public class RhymePanelFragment extends BottomSheetDialogFragment {

    private static final String ARG_LINE_POSITION = "line_position";
    private static final String ARG_INPUT_TEXT = "input_text";

    private int linePosition;
    private RhymeMatcher rhymeMatcher;
    private RhymeWordAdapter adapter;
    private OnWordInsertListener insertListener;

    /** 押韵类型：0=单押, 1=双押 */
    private int rhymeType = 0;

    public interface OnWordInsertListener {
        void onWordInsert(int linePosition, String word);
    }

    public static RhymePanelFragment newInstance(int linePosition, String inputText) {
        RhymePanelFragment fragment = new RhymePanelFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_LINE_POSITION, linePosition);
        args.putString(ARG_INPUT_TEXT, inputText);
        fragment.setArguments(args);
        return fragment;
    }

    public void setOnWordInsertListener(OnWordInsertListener listener) {
        this.insertListener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            linePosition = getArguments().getInt(ARG_LINE_POSITION, -1);
        }
        // 初始化 RhymeMatcher 并加载词典
        rhymeMatcher = new RhymeMatcher();
        Context ctx = requireContext();
        rhymeMatcher.loadDict(ctx);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_rhyme_panel, container, false);

        EditText etInput = view.findViewById(R.id.et_rhyme_input);
        Button btnSearch = view.findViewById(R.id.btn_rhyme_search);
        RadioGroup rgType = view.findViewById(R.id.rg_rhyme_type);
        RecyclerView rvResults = view.findViewById(R.id.rv_rhyme_results);
        TextView tvEmpty = view.findViewById(R.id.tv_rhyme_empty);

        // 初始输入
        if (getArguments() != null) {
            String inputText = getArguments().getString(ARG_INPUT_TEXT, "");
            if (!inputText.isEmpty()) {
                etInput.setText(inputText);
                etInput.setSelection(inputText.length());
            }
        }

        // 结果列表
        rvResults.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new RhymeWordAdapter(word -> onWordInsert(word.getWord()));
        rvResults.setAdapter(adapter);

        // 押韵类型切换
        rgType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rb_single) {
                rhymeType = 0;
            } else if (checkedId == R.id.rb_double) {
                rhymeType = 1;
            }
            doSearch(etInput.getText().toString().trim(), tvEmpty);
        });

        // 搜索按钮
        btnSearch.setOnClickListener(v -> {
            doSearch(etInput.getText().toString().trim(), tvEmpty);
        });

        // 输入实时搜索
        etInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                doSearch(s.toString().trim(), tvEmpty);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        return view;
    }

    /** 执行押韵搜索 */
    private void doSearch(String input, TextView tvEmpty) {
        if (input.isEmpty()) {
            adapter.setData(new ArrayList<>());
            if (tvEmpty != null) tvEmpty.setVisibility(View.GONE);
            return;
        }

        List<RhymeWord> results;
        if (rhymeType == 0) {
            results = rhymeMatcher.findSingleRhymes(input);
        } else {
            results = rhymeMatcher.findDoubleRhymes(input);
        }

        adapter.setData(results);
        if (tvEmpty != null) {
            tvEmpty.setVisibility(results.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }

    /** 押韵类型变化回调（外部调用） */
    public void onRhymeTypeChanged(int type) {
        this.rhymeType = type;
    }

    /** 点击推荐词插入 */
    private void onWordInsert(String word) {
        if (insertListener != null) {
            insertListener.onWordInsert(linePosition, word);
        }
        dismiss();
    }
}
