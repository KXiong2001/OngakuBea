package com.example.ongakubea;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

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
        playlistName.setText(playlist.getId());
        TextView playlistItems = holder.numVideos;
        playlistItems.setText("123123123");

    }

    @Override
    public int getItemCount() {
        return mPlaylists.size();
    }

    protected class ViewHolder extends RecyclerView.ViewHolder {
        TextView playlistName;
        TextView numVideos;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            playlistName = itemView.findViewById(R.id.playlistName);
            numVideos = itemView.findViewById(R.id.playlistNumItems);
        }
    }
}
