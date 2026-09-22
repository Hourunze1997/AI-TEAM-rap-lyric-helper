package com.rap.lyrichelper.ui.rhyme;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.rap.lyrichelper.R;
import com.rap.lyrichelper.data.model.RhymeWord;

import java.util.ArrayList;
import java.util.List;

/**
 * 推荐词列表适配器，展示词语和韵组标签。
 */
public class RhymeWordAdapter extends RecyclerView.Adapter<RhymeWordAdapter.ViewHolder> {

    private List<RhymeWord> words = new ArrayList<>();
    private final OnWordClickListener listener;

    public interface OnWordClickListener {
        void onWordClick(RhymeWord word);
    }

    public RhymeWordAdapter(OnWordClickListener listener) {
        this.listener = listener;
    }

    /** 设置推荐词列表 */
    public void setData(List<RhymeWord> newWords) {
        this.words = newWords != null ? newWords : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_rhyme_word, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(words.get(position));
    }

    @Override
    public int getItemCount() {
        return words.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvWord;
        final TextView tvGroup;

        ViewHolder(View itemView) {
            super(itemView);
            tvWord = itemView.findViewById(R.id.tv_rhyme_word);
            tvGroup = itemView.findViewById(R.id.tv_rhyme_group);
        }

        void bind(RhymeWord word) {
            tvWord.setText(word.getWord());
            tvGroup.setText(word.getRhymeGroupPair() != null ? word.getRhymeGroupPair() : "");
            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onWordClick(word);
            });
        }
    }
}
