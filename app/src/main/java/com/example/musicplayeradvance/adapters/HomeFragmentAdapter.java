package com.example.musicplayeradvance.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.musicplayeradvance.MainActivity;
import com.example.musicplayeradvance.R;
import com.example.musicplayeradvance.fragments.playlists.CurrentPlaylistFragment;
import com.example.musicplayeradvance.models.Playlist;

import java.util.ArrayList;

public class HomeFragmentAdapter extends RecyclerView.Adapter<HomeFragmentAdapter.MyViewHolder> {
    MainActivity mainActivity;
    ArrayList<Playlist> playlists;
    ArrayList<String> authors;
    ArrayList<String> albums;
    private ArrayList<Playlist> originalList;
    private ArrayList<Playlist> filteredList;
    private boolean isFiltered = false; // Flag to indicate if filtering is active

    public HomeFragmentAdapter(MainActivity mainActivity, ArrayList<String> authors, ArrayList<String> albums, ArrayList<Playlist> playlists) {
        this.mainActivity = mainActivity;
        this.authors = authors;
        this.albums = albums;
        this.playlists = playlists;
        this.originalList = new ArrayList<>(playlists);
        this.filteredList = new ArrayList<>(playlists);
    }

    public void filterList(ArrayList<Playlist> filteredList) {
        this.filteredList = new ArrayList<>(filteredList);
        isFiltered = true; // Update flag when filtering is active
        notifyDataSetChanged();
    }

    // Method to clear the filter and display the original list
    public void clearFilter() {
        isFiltered = false;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MyViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.playlist_home_row, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        holder.setIsRecyclable(false);
        updateDefaultData(holder, isFiltered ? filteredList.get(position) : playlists.get(position));

        holder.home_row_img.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Get the clicked playlist object from the filteredList if filtering is active
                Playlist clickedPlaylist = isFiltered ? filteredList.get(holder.getAdapterPosition()) : playlists.get(holder.getAdapterPosition());
                mainActivity.getSupportFragmentManager()
                        .beginTransaction()
                        .setCustomAnimations(R.anim.slide_in_left, R.anim.fade_out, R.anim.fade_in, R.anim.slide_out_left)
                        .replace(R.id.mainFragmentContainer, new CurrentPlaylistFragment(clickedPlaylist.getDir_desc(), clickedPlaylist.getDir_title(), clickedPlaylist, 1))
                        .addToBackStack(null)
                        .commit();
            }
        });
    }

    private void updateDefaultData(MyViewHolder holder, Playlist playlist) {
        Glide.with(mainActivity.getApplicationContext())
                .load(playlist.getImage_id())
                .apply(RequestOptions.centerCropTransform())
                .error(R.drawable.img_not_found)
                .into(holder.home_row_img);

        holder.home_row_year.setText(playlist.getYear());
        holder.home_row_title.setText(playlist.getTitle());
        holder.home_row_author.setText(playlist.getDescription());
    }

    @Override
    public int getItemCount() {
        return isFiltered ? filteredList.size() : playlists.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {
        ImageView home_row_img;
        TextView home_row_year, home_row_author;
        AppCompatTextView home_row_title;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);

            home_row_img = itemView.findViewById(R.id.playlistHomeRowImg);
            home_row_year = itemView.findViewById(R.id.playlistHomeRowYear);
            home_row_title = itemView.findViewById(R.id.playlistHomeRowTitle);
            home_row_author = itemView.findViewById(R.id.playlistHomeRowDescription);
        }
    }
}
