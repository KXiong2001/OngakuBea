package com.example.ongakubea;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.api.services.youtube.model.Playlist;

import java.util.List;

public class PlaylistAdapter extends
        RecyclerView.Adapter<PlaylistAdapter.ViewHolder> {

    private List<Playlist> mPlaylists;

    public PlaylistAdapter(List<Playlist> playlists) {
        this.mPlaylists = playlists;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater layoutInflater = LayoutInflater.from(context);

        View playlistRowView = layoutInflater.inflate(R.layout.playlist_row_layout, parent, false);
        ViewHolder viewHolder = new ViewHolder(playlistRowView);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Playlist playlist = mPlaylists.get(position);

        TextView playlistName = holder.playlistName;
        playlistName.setText(playlist.getSnippet().getTitle());
        TextView playlistItems = holder.numVideos;
        playlistItems.setText(String.format("%d", playlist.getContentDetails().getItemCount()));


        ImageView snippitThumbnail = holder.thumbnail;
        String url = playlist.getSnippet().getThumbnails().getDefault().getUrl();
        System.out.println(url);

        Glide
            .with(holder.itemView.getContext())
            .load(url)
            .fitCenter()
            .placeholder(R.drawable.ic_launcher_foreground)
            .into(snippitThumbnail);
    }

    @Override
    public int getItemCount() {
        return mPlaylists.size();
    }

    protected class ViewHolder extends RecyclerView.ViewHolder {
        TextView playlistName;
        TextView numVideos;
        ImageView thumbnail;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            playlistName = itemView.findViewById(R.id.playlistName);
            numVideos = itemView.findViewById(R.id.playlistNumItems);
            thumbnail = itemView.findViewById(R.id.playlistThumbnail);
        }
    }
}
