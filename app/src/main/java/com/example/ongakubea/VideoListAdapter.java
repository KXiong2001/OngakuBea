package com.example.ongakubea;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.api.services.youtube.model.Video;


import java.util.List;

public class VideoListAdapter extends
        RecyclerView.Adapter<VideoListAdapter.ViewHolder> {
    Context context;
    List<Video> mVideos;

    VideoListAdapter(List<Video> videos) {
        this.mVideos = videos;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        LayoutInflater layoutInflater = LayoutInflater.from(context);

        View videoDetailRow = layoutInflater.inflate(R.layout.video_row_layout, parent, false);
        VideoListAdapter.ViewHolder viewHolder = new VideoListAdapter.ViewHolder(videoDetailRow);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Video video = this.mVideos.get(position);
        holder.name.setText(video.getSnippet().getTitle());

    }

    @Override
    public int getItemCount() {
        return this.mVideos.size();
    }

    protected class ViewHolder extends RecyclerView.ViewHolder {
        TextView name;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.videoDetailsName);
        }
    }
}
