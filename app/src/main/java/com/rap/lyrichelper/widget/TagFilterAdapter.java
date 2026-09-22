package com.rap.lyrichelper.widget;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.rap.lyrichelper.R;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 标签筛选适配器，使用 Material Chip 渲染标签，点击切换选中态。
 */
public class TagFilterAdapter extends RecyclerView.Adapter<TagFilterAdapter.ViewHolder> {

    private final List<String> tags = new ArrayList<>();
    private final Set<String> selectedTags = new HashSet<>();
    private final OnTagToggleListener listener;

    public interface OnTagToggleListener {
        void onChipToggle(String tag, boolean selected);
    }

    public TagFilterAdapter(OnTagToggleListener listener) {
        this.listener = listener;
    }

    /** 设置可选标签列表 */
    public void setTags(List<String> newTags) {
        tags.clear();
        if (newTags != null) tags.addAll(newTags);
        notifyDataSetChanged();
    }

    /** 获取当前选中的标签 */
    public Set<String> getSelectedTags() {
        return new HashSet<>(selectedTags);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Chip chip = (Chip) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_tag_chip, parent, false);
        return new ViewHolder(chip);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String tag = tags.get(position);
        holder.bind(tag);
    }

    @Override
    public int getItemCount() {
        return tags.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        final Chip chip;

        ViewHolder(View itemView) {
            super(itemView);
            chip = (Chip) itemView;
        }

        void bind(String tag) {
            chip.setText(tag);
            chip.setChecked(selectedTags.contains(tag));
            chip.setOnClickListener(v -> {
                boolean isSelected = !selectedTags.contains(tag);
                if (isSelected) {
                    selectedTags.add(tag);
                } else {
                    selectedTags.remove(tag);
                }
                chip.setChecked(isSelected);
                if (listener != null) {
                    listener.onChipToggle(tag, isSelected);
                }
            });
        }
    }
}
