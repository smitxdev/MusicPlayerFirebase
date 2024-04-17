package com.example.musicplayeradvance.viewmodels.fragments.player;

import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModel;

import com.example.musicplayeradvance.MainActivity;
import com.example.musicplayeradvance.R;
import com.example.musicplayeradvance.adapters.CurrentPlaylistAdapter;
import com.example.musicplayeradvance.fragments.favorites.FavoritesFragment;
import com.example.musicplayeradvance.fragments.player.PlayerFragment;
import com.example.musicplayeradvance.fragments.playlists.CurrentPlaylistFragment;
import com.example.musicplayeradvance.models.Playlist;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

public class PlayerFragmentViewModel extends ViewModel {

    public void removeFromFav(FragmentActivity activity, DatabaseReference database_ref, FirebaseAuth mAuth, Playlist playlist, Integer position, ImageButton fav_btn,ImageButton mini_fav_btn, CurrentPlaylistAdapter adapter) {
        Fragment frag = activity.getSupportFragmentManager().findFragmentById(R.id.slidingLayoutFrag);
        database_ref.child("fav_music").child(mAuth.getCurrentUser().getUid()).orderByKey().addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot firebaseFav : snapshot.getChildren()) {
                    if (firebaseFav.child("album").getValue().toString().trim().equals(playlist.getSongs().get(position).getAlbum().trim())
                            &&
                            firebaseFav.child("numberInAlbum").getValue().toString().trim().equals(playlist.getSongs().get(position).getOrder().toString().trim())
                    ) {
                        database_ref.child("fav_music").child(mAuth.getCurrentUser().getUid()).child(firebaseFav.getKey()).removeValue().addOnCompleteListener(activity, new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                    fav_btn.setImageResource(R.drawable.ic_heart_empty);
                                    mini_fav_btn.setImageResource(R.drawable.ic_heart_empty);
                                    if(activity!=null)
                                        {
                                        Fragment currentFragment = activity.getSupportFragmentManager().findFragmentById(R.id.mainFragmentContainer);
                                        if (currentFragment instanceof CurrentPlaylistFragment) {
                                            adapter.notifyDataSetChanged();
                                        }

                                        if (currentFragment instanceof FavoritesFragment) {
                                            FavoritesFragment fav = (FavoritesFragment) currentFragment;

                                            if (frag instanceof PlayerFragment) {

                                                if(MainActivity.viewModel.getCurrentSongTitle().getValue().equals(fav.viewModel.getPlaylist().getSongs().get(position).getTitle()) &&
                                                        MainActivity.viewModel.getCurrentSongAlbum().getValue().equals(fav.viewModel.getPlaylist().getTitle()) &&
                                                        MainActivity.viewModel.getCurrentSongAuthor().getValue().equals( fav.viewModel.getPlaylist().getDescription())
                                                ){
                                                ((PlayerFragment) frag).dismissPlayer();
                                                }


                                            }
                                            fav.viewModel.initializeFavorites();
                                        }
                                        else{

                                       if(FavoritesFragment.viewModel!=null){
                                           if(MainActivity.viewModel.getCurrentSongTitle().getValue().equals(  FavoritesFragment.viewModel.getPlaylist().getSongs().get(position).getTitle()) &&
                                                    MainActivity.viewModel.getCurrentSongAlbum().getValue().equals( FavoritesFragment.viewModel.getPlaylist().getTitle()) &&
                                                    MainActivity.viewModel.getCurrentSongAuthor().getValue().equals( FavoritesFragment.viewModel.getPlaylist().getDescription())
                                            ) {
                                                System.out.println("F: "+playlist.getTitle());
                                                if(playlist.getTitle().equals("Favorites")){
                                                    ((PlayerFragment) frag).dismissPlayer();
                                                }
                                            }
                                       }

                                        }

                                        Fragment playerF = activity.getSupportFragmentManager().findFragmentById(R.id.slidingLayoutFrag);
                                        if(playerF instanceof PlayerFragment)
                                        {
                                            fav_btn.getDrawable().setTint(ContextCompat.getColor(activity, R.color.project_light_orange));
                                            mini_fav_btn.getDrawable().setTint(ContextCompat.getColor(activity, R.color.black));
                                         }

                                    }
                                }
                            }
                        });
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    public void addToFav(FragmentActivity activity, DatabaseReference database_ref, FirebaseAuth mAuth, Playlist playlist, Integer position, ImageButton fav_btn,ImageButton mini_fav_btn, CurrentPlaylistAdapter adapter) {
        String key = database_ref.push().getKey();

        database_ref
                .child("music")
                .child("albums")
                .child(playlist.getSongs().get(position).getAuthor())
                .child(playlist.getSongs().get(position).getAlbum()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot dataSnapshot : snapshot.child("songs").getChildren()) {
                    if (dataSnapshot.child("title").getValue().equals(playlist.getSongs().get(position).getTitle())) {
                        database_ref.child("fav_music").child(mAuth.getCurrentUser().getUid()).child(key).child("numberInAlbum").setValue(dataSnapshot.child("order").getValue().toString());
                        database_ref.child("fav_music").child(mAuth.getCurrentUser().getUid()).child(key).child("album").setValue(playlist.getSongs().get(position).getAlbum());
                        database_ref.child("fav_music").child(mAuth.getCurrentUser().getUid()).child(key).child("author").setValue(playlist.getSongs().get(position).getAuthor());
                        fav_btn.setImageResource(R.drawable.ic_heart_full);
                        mini_fav_btn.setImageResource(R.drawable.ic_heart_full);
                        if(activity!=null){
                            Fragment currentFragment = activity.getSupportFragmentManager().findFragmentById(R.id.mainFragmentContainer);
                            if (currentFragment instanceof CurrentPlaylistFragment) {
                                adapter.notifyDataSetChanged();
                            }

                            if (currentFragment instanceof FavoritesFragment) {
                                FavoritesFragment favoritesFragment = (FavoritesFragment) currentFragment;
                                favoritesFragment.viewModel.initializeFavorites();
                            }

                            Fragment playerF = activity.getSupportFragmentManager().findFragmentById(R.id.slidingLayoutFrag);

                            if(playerF instanceof PlayerFragment){
                                fav_btn.getDrawable().setTint(ContextCompat.getColor(activity, R.color.project_light_orange));
                                mini_fav_btn.getDrawable().setTint(ContextCompat.getColor(activity, R.color.project_dark_velvet));
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
