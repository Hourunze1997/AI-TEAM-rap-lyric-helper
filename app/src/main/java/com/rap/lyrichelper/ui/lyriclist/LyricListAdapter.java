package com.rap.lyrichelper.ui.lyriclist;

import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.rap.lyrichelper.R;
import com.rap.lyrichelper.data.model.Lyric;

import java.util.ArrayList;
import java.util.List;

/**
 * 歌词卡片适配器，展示标题、更新时间、标签。
 */
public class LyricListAdapter extends RecyclerView.Adapter<LyricListAdapter.ViewHolder> {

    private List<Lyric> lyrics = new ArrayList<>();
    private final OnLyricClickListener listener;

    public interface OnLyricClickListener {
        void onLyricClick(Lyric lyric);
    }

    public LyricListAdapter(OnLyricClickListener listener) {
        this.listener = listener;
    }

    public void setLyrics(List<Lyric> newLyrics) {
        this.lyrics = newLyrics != null ? newLyrics : new ArrayList<>();
        notifyDataSetChanged();
    }

    /** 过滤（简单委托给 ViewModel 的 search） */
    public void filter(String keyword) {
        // 实际过滤逻辑由 ViewModel 负责，此方法保留兼容
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_lyric_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Lyric lyric = lyrics.get(position);
        holder.bind(lyric);
    }

    @Override
    public int getItemCount() {
        return lyrics.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvTitle;
        final TextView tvTime;
        final TextView tvTags;

        ViewHolder(View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvTime = itemView.findViewById(R.id.tv_time);
            tvTags = itemView.findViewById(R.id.tv_tags);
        }

        void bind(Lyric lyric) {
            tvTitle.setText(lyric.getTitle() != null ? lyric.getTitle() : "");
            tvTime.setText(DateUtils.getRelativeTimeSpanString(lyric.getUpdatedAt()));

            if (lyric.getTags() != null && !lyric.getTags().isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < lyric.getTags().size(); i++) {
                    if (i > 0) sb.append(" · ");
                    sb.append(lyric.getTags().get(i));
                }
                tvTags.setText(sb.toString());
                tvTags.setVisibility(View.VISIBLE);
            } else {
                tvTags.setVisibility(View.GONE);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onLyricClick(lyric);
            });
        }
    }
}
