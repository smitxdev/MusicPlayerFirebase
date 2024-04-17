package com.example.musicplayeradvance.viewmodels.fragments.playlists;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.musicplayeradvance.models.Playlist;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Calendar;

public class PlaylistsFragmentViewModel extends ViewModel {
    private DatabaseReference database_ref;
    private FirebaseAuth mAuth;
    private String key;
    private Playlist playlist;
    private MutableLiveData<Integer> _errorState = new MutableLiveData<>();
    private MutableLiveData<Boolean> _shouldChangeFragment = new MutableLiveData<>();


    public LiveData<Integer> getErrorState(){
       return _errorState;
    }
    public LiveData<Boolean> getShouldChangeFragment(){
       return _shouldChangeFragment;
    }

    public void createPlaylist(String name, String desc) {
        initializeFirebase();

        playlist = new Playlist();
        playlist.setTitle(name);
        playlist.setAlbum(false);
        playlist.setDescription(desc);
        playlist.setYear(String.valueOf(Calendar.getInstance().get(Calendar.YEAR)));
        playlist.setImage_id("");
        playlist.setSongs(null);

        key = database_ref.push().getKey();

        sendPlaylistToFirebase();
        }

    private void sendPlaylistToFirebase() {
        database_ref.child("music").child("playlists").child(mAuth.getCurrentUser().getUid()).child(key).setValue(playlist).addOnCompleteListener( new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (!task.isSuccessful()) {
                    _errorState.setValue(1);
                    _shouldChangeFragment.setValue(false);
                }
                else
                {
                    database_ref.child("music").child("playlists").child(mAuth.getCurrentUser().getUid()).child(key).child("album").removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                _shouldChangeFragment.setValue(true);
                            }
                            else
                            {
                                _shouldChangeFragment.setValue(false);
                                _errorState.setValue(3);
                            }
                        }
                    });

                }
            }
        });

    }

    private void initializeFirebase() {
        mAuth= FirebaseAuth.getInstance();
        database_ref = FirebaseDatabase.getInstance().getReference();
    }

    public Playlist getPlaylist() {
        return playlist;
    }
}
