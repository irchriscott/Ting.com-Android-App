package com.codepipes.ting.imageeditor.editimage.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;

import com.codepipes.ting.R;
import com.codepipes.ting.imageeditor.editimage.adapter.viewholders.StickerViewHolder;
import com.codepipes.ting.imageeditor.editimage.fragment.StickerFragment;


public class StickerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private StickerFragment stickerFragment;
    private ImageClick imageClick = new ImageClick();
    private List<String> pathList = new ArrayList<>();

    public StickerAdapter(StickerFragment fragment) {
        super();
        this.stickerFragment = fragment;
    }

    @Override
    public int getItemCount() {
        return pathList.size();
    }

    @Override
    public int getItemViewType(int position) {
        return 1;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewtype) {
        View view;
        view = LayoutInflater.from(parent.getContext()).inflate(
                R.layout.image_editor_view_sticker_item, parent, false);
        return new StickerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {
        StickerViewHolder stickerViewHolder = (StickerViewHolder) viewHolder;
        String path = pathList.get(position);

        String imageUrl = "drawable/" + path;
        int imageKey = stickerFragment.getResources().getIdentifier(imageUrl, "drawable", stickerFragment.getContext().getPackageName());
        stickerViewHolder.image.setImageDrawable(stickerFragment.getResources().getDrawable(imageKey));
        stickerViewHolder.image.setTag(imageUrl);
        stickerViewHolder.image.setOnClickListener(imageClick);
    }

    public void addStickerImages(String folderPath, int stickerCount) {
        pathList.clear();
        for (int i = 0; i < stickerCount; i++) {
            pathList.add(folderPath + "_" + Integer.toString(i + 1));
        }
        this.notifyDataSetChanged();
    }

    private final class ImageClick implements OnClickListener {
        @Override
        public void onClick(View v) {
            String data = (String) v.getTag();
            stickerFragment.selectedStickerItem(data);
        }
    }
}
