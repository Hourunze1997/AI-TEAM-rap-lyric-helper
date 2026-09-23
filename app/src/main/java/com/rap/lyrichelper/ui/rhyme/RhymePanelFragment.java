package com.rap.lyrichelper.ui.rhyme;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.rap.lyrichelper.R;
import com.rap.lyrichelper.data.model.RhymeWord;
import com.rap.lyrichelper.rhyme.LlmRhymeService;
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
    private Context appContext;
    /** 输入防抖：实时搜索每次按键都触发，LLM 模式下会连发请求 */
    private final Handler debounce = new Handler(Looper.getMainLooper());
    private Runnable pendingSearch;

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
        // 初始化 RhymeMatcher，2.5 万词词典的解析+建索引放后台线程，避免卡主线程
        rhymeMatcher = new RhymeMatcher();
        appContext = requireContext().getApplicationContext();
        new Thread(() -> rhymeMatcher.loadDict(appContext)).start();
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

    /** 执行押韵搜索：先防抖，再按"配置了大模型走在线 / 没配置走本地"分流 */
    private void doSearch(String input, TextView tvEmpty) {
        if (pendingSearch != null) {
            debounce.removeCallbacks(pendingSearch);
        }
        if (input.isEmpty()) {
            adapter.setData(new ArrayList<>());
            if (tvEmpty != null) tvEmpty.setVisibility(View.GONE);
            return;
        }
        pendingSearch = () -> runSearch(input, tvEmpty);
        debounce.postDelayed(pendingSearch, 600);
    }

    private void runSearch(String input, TextView tvEmpty) {
        // 大模型优先：中英文通吃、双押词组、按说唱可用性排序
        if (LlmRhymeService.hasKey(appContext)) {
            showEmpty(tvEmpty, "🤖 大模型生成押韵词中…", null);
            LlmRhymeService.fetchRhymes(appContext, input, rhymeType == 1, (words, error) -> {
                if (!isAdded()) return;
                if (words != null && !words.isEmpty()) {
                    adapter.setData(words);
                    if (tvEmpty != null) tvEmpty.setVisibility(View.GONE);
                } else {
                    searchLocal(input, tvEmpty,
                            "在线推荐失败：" + (error == null ? "无结果" : error) + "，已回退本地词库");
                }
            });
        } else {
            searchLocal(input, tvEmpty, null);
        }
    }

    /** 本地词典匹配；无结果时引导配置大模型 */
    private void searchLocal(String input, TextView tvEmpty, String notice) {
        List<RhymeWord> results;
        if (rhymeType == 0) {
            results = rhymeMatcher.findSingleRhymes(input);
        } else {
            results = rhymeMatcher.findDoubleRhymes(input);
        }

        adapter.setData(results);
        if (tvEmpty == null) return;
        if (!results.isEmpty()) {
            if (notice != null) {
                showEmpty(tvEmpty, notice, null);
            } else {
                tvEmpty.setVisibility(View.GONE);
            }
            return;
        }
        if (!rhymeMatcher.isLoaded()) {
            showEmpty(tvEmpty, "本地词库加载中…", null);
        } else if (notice != null) {
            showEmpty(tvEmpty, notice, null);
        } else {
            showEmpty(tvEmpty, "本地词库没找到。\n点此配置大模型，解锁智能押韵推荐",
                    v -> showConfigDialog());
        }
    }

    private void showEmpty(TextView tv, String text, View.OnClickListener l) {
        if (tv == null) return;
        tv.setText(text);
        tv.setVisibility(View.VISIBLE);
        tv.setOnClickListener(l);
        tv.setClickable(l != null);
    }

    /** 大模型接入配置：Key / Base URL / 模型名，存 SharedPreferences */
    private void showConfigDialog() {
        Context ctx = getContext();
        if (ctx == null) return;
        SharedPreferences sp = LlmRhymeService.prefs(ctx);
        LinearLayout box = new LinearLayout(ctx);
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        box.setPadding(pad, pad / 2, pad, 0);

        EditText etKey = new EditText(ctx);
        etKey.setHint("API Key（Anthropic 兼容接口）");
        etKey.setText(sp.getString("api_key", ""));
        box.addView(etKey);
        EditText etBase = new EditText(ctx);
        etBase.setHint("Base URL");
        etBase.setText(sp.getString("base_url", "https://api.minimaxi.com/anthropic"));
        box.addView(etBase);
        EditText etModel = new EditText(ctx);
        etModel.setHint("模型名");
        etModel.setText(sp.getString("model", "glm-5.2"));
        box.addView(etModel);

        new AlertDialog.Builder(ctx)
                .setTitle("接入大模型押韵推荐")
                .setView(box)
                .setPositiveButton("保存", (d, w) -> sp.edit()
                        .putString("api_key", etKey.getText().toString().trim())
                        .putString("base_url", etBase.getText().toString().trim())
                        .putString("model", etModel.getText().toString().trim())
                        .apply())
                .setNegativeButton("取消", null)
                .show();
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
