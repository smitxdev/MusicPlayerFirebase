package com.example.musicplayeradvance.fragments.favorites;

import static com.example.musicplayeradvance.fragments.playlists.CurrentPlaylistFragment.adapter;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.example.musicplayeradvance.MainActivity;
import com.example.musicplayeradvance.R;
import com.example.musicplayeradvance.adapters.CurrentPlaylistAdapter;
import com.example.musicplayeradvance.databinding.FragmentCurrentPlaylistBinding;
import com.example.musicplayeradvance.models.FavoriteFirebaseSong;
import com.example.musicplayeradvance.models.Song;
import com.example.musicplayeradvance.viewmodels.fragments.favorites.FavoritesFragmentViewModel;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;

public class FavoritesFragment extends Fragment {

    public static FavoritesFragmentViewModel viewModel;
    public static FragmentCurrentPlaylistBinding binding;

    private DatabaseReference database_ref;
    private MainActivity mainActivity;
    private String FRAGMENT ="FavoritesFragment";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        binding = FragmentCurrentPlaylistBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        database_ref = FirebaseDatabase.getInstance().getReference();

        mainActivity= (MainActivity) getActivity();
        mainActivity.activityMainBinding.mainActivityBox.setBackgroundColor(Color.WHITE);

        binding.currentPlaylistSettingsBtn.setVisibility(View.INVISIBLE);

        viewModel = new ViewModelProvider(mainActivity).get(FavoritesFragmentViewModel.class);

        initializeObservers();

        return view;
    }

    private void initializeObservers() {
        viewModel.getImageViewDrawable().observe(getViewLifecycleOwner(), new Observer<Integer>() {
            @Override
            public void onChanged(Integer integer) {
                Glide.with(mainActivity).load(integer).into(binding.currentPlaylistImg);
            }
        });
        viewModel.getTitleText().observe(getViewLifecycleOwner(), new Observer<String>() {
            @Override
            public void onChanged(String s) {
                binding.currentPlaylistTitle.setText(s);
            }
        });
        viewModel.getDescriptionText().observe(getViewLifecycleOwner(), new Observer<String>() {
            @Override
            public void onChanged(String s) {
                binding.currentPlaylistDescription.setText(s);
            }
        });
        viewModel.getFavStateBool().observe(getViewLifecycleOwner(), new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean aBoolean) {
                if (!aBoolean) {
                    binding.currentPlaylistRecyclerView.setVisibility(View.VISIBLE);
                    binding.currentPlaylistRecyclerState.setVisibility(View.GONE);
                    initializeFavoritesRecyclerView(viewModel.getFavoriteFirebaseSongs());
                } else {
                    binding.currentPlaylistRecyclerView.setVisibility(View.GONE);
                    binding.currentPlaylistRecyclerState.setVisibility(View.VISIBLE);
                    binding.currentPlaylistRecyclerState.setText("Empty Favorites");
                }
            }
        });
    }

    public void initializeFavoritesRecyclerView(ArrayList<FavoriteFirebaseSong> favoriteFirebaseSongs) {
        ArrayList<Song> songs = new ArrayList<>();
        for (int i = 0; i < favoriteFirebaseSongs.size(); i++) {
            FavoriteFirebaseSong song = favoriteFirebaseSongs.get(i);
            database_ref.child("music").child("albums").child(song.getAuthor()).child(song.getAlbum()).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {

                    if (snapshot.exists()) {
                        for (DataSnapshot ds : snapshot.child("songs").getChildren()) {
                            if (Integer.parseInt(ds.child("order").getValue().toString()) == song.getNumberInAlbum()) {

                                Object gif = ds.child("videoPath").getValue();
                                String gif_str="";
                                if(gif!=null){
                                    gif_str=gif.toString();
                                }


                                Song local_song = new Song(snapshot.child("dir_desc").getValue().toString(), snapshot.child("dir_title").getValue().toString(), ds.child("title").getValue().toString(), ds.child("path").getValue().toString(), snapshot.child("image_id").getValue().toString(), song.getNumberInAlbum(),gif_str);
                                local_song.setVisualAlbum(snapshot.child("title").getValue().toString());
                                local_song.setVisualAuthor(snapshot.child("description").getValue().toString());
                                local_song.setDateTime(song.getDateTime());
                                songs.add(local_song);
                            }
                        }

                        if (songs.size() == favoriteFirebaseSongs.size()) {
                            Collections.sort(songs, (f1, f2) -> f1.getDateTime().compareTo(f2.getDateTime()));


                            viewModel.getPlaylist().setSongs(songs);

                            if (viewModel.getPlaylist().getSongs() != null) {

                                adapter = new CurrentPlaylistAdapter(mainActivity, viewModel.getPlaylist(), 1);
                                binding.currentPlaylistRecyclerView.setLayoutManager(new LinearLayoutManager(mainActivity, LinearLayoutManager.VERTICAL, false));
                                binding.currentPlaylistRecyclerView.setAdapter(adapter);
                                adapter.notifyDataSetChanged();
                                if (mainActivity != null) {
                                    Fragment currentFragment = mainActivity.getSupportFragmentManager().findFragmentById(R.id.mainFragmentContainer);
                                    if (currentFragment instanceof FavoritesFragment) {
                                        Drawable songBg = AppCompatResources.getDrawable(mainActivity, R.drawable.custom_song_bg);
                                        binding.currentPlaylistRecyclerView.setBackground(songBg);
                                    }
                                }


                            }
                        }
                    }
                }


                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
        }
    }
    @Override
    public void onResume() {
        super.onResume();
        viewModel.initializeFavorites();
    }
}