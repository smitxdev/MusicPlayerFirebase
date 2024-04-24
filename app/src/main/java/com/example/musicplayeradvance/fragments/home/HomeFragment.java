package com.example.musicplayeradvance.fragments.home;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SearchView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.GridLayoutManager;

import com.bumptech.glide.Glide;
import com.example.musicplayeradvance.MainActivity;
import com.example.musicplayeradvance.R;
import com.example.musicplayeradvance.adapters.HomeFragmentAdapter;
import com.example.musicplayeradvance.databinding.FragmentHomeBinding;
import com.example.musicplayeradvance.fragments.login_and_register.LoginFragment;
import com.example.musicplayeradvance.fragments.profile.ProfileFragment;
import com.example.musicplayeradvance.models.Playlist;
import com.example.musicplayeradvance.models.Song;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class HomeFragment extends Fragment {

    private final String[] album_array = new String[1];
    private final String[] author_array = new String[1];

    private HomeFragmentAdapter adapter;
    private DatabaseReference database_ref;
    private FirebaseDatabase database;
    private FirebaseAuth mAuth;
    private ArrayList<Playlist> playlists;
    private ArrayList<String> authors;
    private ArrayList<String> albums;
    private MainActivity mainActivity;
    private final Boolean isOpenByLogin;

    public static FragmentHomeBinding binding;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View view = binding.getRoot();
        mainActivity = (MainActivity) getActivity();

        initalizeFirebaseConnection();

        setUpArrays();

        binding.homeLogoutBtn.setOnClickListener(v -> {
            MainActivity.clearCurrentSong();
            mainActivity.logoutUser();
        });

        MainActivity.viewModel.getPhotoUrl().observe(mainActivity, new Observer<String>() {
            @Override
            public void onChanged(String s) {
                if (MainActivity.viewModel.getPhotoUrl() != null) {
                    Glide.with(mainActivity).load(MainActivity.viewModel.getPhotoUrl().getValue()).error(R.drawable.img_not_found).into(binding.homeProfileBtn);
                }
            }
        });


        binding.homeProfileBtn.setOnClickListener(v -> {
            mainActivity.getSupportFragmentManager().beginTransaction().setCustomAnimations(R.anim.slide_in_up, R.anim.fade_out, R.anim.fade_in, R.anim.slide_out_up).replace(R.id.mainFragmentContainer, new ProfileFragment()).addToBackStack(null).commit();
        });
        // Initialize RecyclerView
//        initializeRecyclerView();
        loadData();



        SearchView searchView = view.findViewById(R.id.searchView);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterPlaylist(newText);
                return true;
            }
        });

        if (mAuth.getCurrentUser() == null) {
            mainActivity.getSupportFragmentManager().beginTransaction().setCustomAnimations(R.anim.slide_in_up, R.anim.fade_out, R.anim.fade_in, R.anim.slide_out_up).replace(R.id.mainFragmentContainer, new LoginFragment()).commit();
        }
        return view;
    }

    private void setUpArrays() {
        albums = new ArrayList<>();
        authors = new ArrayList<>();
        playlists = new ArrayList<>();
    }









    private void filterPlaylist(String query) {
        ArrayList<Playlist> filteredList = new ArrayList<>();

        for (Playlist playlist : playlists) {
            boolean playlistAdded = false; // Flag to track if the playlist matches the query

            // Check if the playlist title contains the query
            if (playlist.getTitle().toLowerCase().contains(query.toLowerCase())) {
                filteredList.add(playlist);
                playlistAdded = true;
            }

            ArrayList<Song> songs = playlist.getSongs();

            if (songs != null) {
                // Check if any song title in the playlist contains the query
                for (Song song : songs) {
                    if (song.getTitle().toLowerCase().contains(query.toLowerCase())) {
                        // If the song title matches the query and the playlist hasn't been added yet, add the playlist
                        if (!playlistAdded) {
                            filteredList.add(playlist);
                            playlistAdded = true;
                        }
                        break; // No need to continue checking songs in the playlist once a match is found
                    }
                }
            } else {
                Log.d("Filtered Playlist", "Songs list is null for playlist: " + playlist.getTitle());
            }
        }

        adapter.filterList(filteredList);
    }














    private void initalizeFirebaseConnection() {
        mAuth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();
        database_ref = database.getReference();
    }

    private void initializeRecyclerView() {
        adapter = new HomeFragmentAdapter(mainActivity, authors, albums, playlists);
        binding.homeRecyclerView.setLayoutManager(new GridLayoutManager(mainActivity, 2));
        binding.homeRecyclerView.setAdapter(adapter);
        mainActivity.activityMainBinding.mainActivityBox.setBackground(getResources().getDrawable(R.drawable.custom_home_fragment_bg));
    }

    public HomeFragment(Boolean isOpenByLogin) {
        this.isOpenByLogin = isOpenByLogin;
    }


    private void initializePlaylists() {

        if (mAuth.getCurrentUser() != null) {

            database_ref.child("music").child("albums").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {

                    for (DataSnapshot ds_author : snapshot.getChildren()) {
                        for (DataSnapshot ds_album : ds_author.getChildren()) {
                            if (ds_album != null) {
                                Playlist x = new Playlist();
                                String author = ds_author.getKey();
                                String album = ds_album.getKey();
                                album_array[0] = ds_album.child("title").getValue().toString();
                                author_array[0] = ds_album.child("description").getValue().toString();


                                albums.add(album);
                                authors.add(author);

                                x.setImage_id(ds_album.child("image_id").getValue().toString());
                                x.setYear(ds_album.child("year").getValue().toString());
                                x.setTitle(album_array[0]);
                                x.setDescription(author_array[0]);
                                x.setDir_title(album);
                                x.setDir_desc(author);

                                playlists.add(x);
                                adapter.notifyDataSetChanged();

                            }
                        }
                    }

                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.d(MainActivity.FIREBASE, "Error: " + error.getMessage());
                }

            });


        }
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        mainActivity.activityMainBinding.mainActivityBox.setBackground(getResources().getDrawable(R.drawable.custom_home_fragment_bg));
    }

    @Override
    public void onResume() {
        super.onResume();

//        initializeRecyclerView();
//        initializePlaylists();

        if (isOpenByLogin) {
            loadData();
            mainActivity.downloadPhoto();
        }
    }
    private void loadData() {
        initializeRecyclerView();
        initializePlaylists();
    }
}