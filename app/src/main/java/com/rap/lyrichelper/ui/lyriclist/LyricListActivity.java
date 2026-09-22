package com.rap.lyrichelper.ui.lyriclist;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.rap.lyrichelper.R;
import com.rap.lyrichelper.data.model.Lyric;
import com.rap.lyrichelper.ui.detail.LyricDetailActivity;
import com.rap.lyrichelper.ui.editor.LyricEditActivity;

import java.util.ArrayList;

/**
 * 首页/歌词列表页：RecyclerView 展示歌词卡片，搜索框过滤，新建按钮跳转编辑页，底部导航。
 */
public class LyricListActivity extends AppCompatActivity {

    private LyricListViewModel viewModel;
    private LyricListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lyric_list);

        viewModel = new ViewModelProvider(this).get(LyricListViewModel.class);

        setupRecyclerView();
        setupSearch();
        setupFab();
        setupBottomNav();

        viewModel.getLyrics().observe(this, lyrics -> {
            adapter.setLyrics(lyrics != null ? lyrics : new ArrayList<>());
            View emptyView = findViewById(R.id.tv_empty);
            if (emptyView != null) {
                emptyView.setVisibility(
                        (lyrics == null || lyrics.isEmpty()) ? View.VISIBLE : View.GONE);
            }
        });
    }

    /** 初始化 RecyclerView */
    private void setupRecyclerView() {
        RecyclerView recyclerView = findViewById(R.id.rv_lyrics);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new LyricListAdapter(this::onLyricClick);
        recyclerView.setAdapter(adapter);
    }

    /** 搜索框过滤 */
    private void setupSearch() {
        EditText etSearch = findViewById(R.id.et_search);
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                viewModel.search(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    /** 新建歌词按钮 */
    private void setupFab() {
        FloatingActionButton fab = findViewById(R.id.fab_new);
        fab.setOnClickListener(v -> onNewLyric());
    }

    /** 底部导航 */
    private void setupBottomNav() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_list) {
                return true;
            } else if (id == R.id.nav_memorize) {
                // 跳转背诵模式（选择歌词后进入）
                if (adapter.getItemCount() > 0) {
                    // 简化：直接显示列表页，用户点击歌词进入详情再背诵
                }
                return true;
            }
            return false;
        });
    }

    /** 新建歌词：跳转编辑页 */
    private void onNewLyric() {
        Intent intent = new Intent(this, LyricEditActivity.class);
        startActivity(intent);
    }

    /** 点击歌词：跳转详情页 */
    private void onLyricClick(Lyric lyric) {
        Intent intent = new Intent(this, LyricDetailActivity.class);
        intent.putExtra("lyric_id", lyric.getId());
        startActivity(intent);
    }
}
