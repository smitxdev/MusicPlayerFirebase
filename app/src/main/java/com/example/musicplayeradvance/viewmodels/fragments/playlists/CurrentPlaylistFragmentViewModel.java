package com.example.musicplayeradvance.viewmodels.fragments.playlists;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.musicplayeradvance.MainActivity;
import com.example.musicplayeradvance.R;
import com.example.musicplayeradvance.fragments.home.HomeFragment;
import com.example.musicplayeradvance.fragments.playlists.CurrentPlaylistFragment;
import com.example.musicplayeradvance.models.FavoriteFirebaseSong;
import com.example.musicplayeradvance.models.Playlist;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

public class CurrentPlaylistFragmentViewModel  extends ViewModel {
    private Playlist _playlist, p;
    private DatabaseReference database_ref;
    private FirebaseAuth mAuth;
    private StorageReference storageReference;
    private    int[] x;
    private String finalName, finalDesc;
    private String key;
    private String old_key;
    private MainActivity mainActivity;
    private MutableLiveData<Integer> _validErrorState = new MutableLiveData<>();
    public LiveData<Integer> getValidErrorState(){
        return _validErrorState;
    }

    private MutableLiveData<Boolean> _shouldOpenCopy = new MutableLiveData<>();
    public LiveData<Boolean> getShouldOpenCopy(){
        return _shouldOpenCopy;
    }

    private MutableLiveData<Boolean> _shouldOpenCurrentFragment = new MutableLiveData<>();
    public LiveData<Boolean> getShouldOpenCurrentFragment(){
        return _shouldOpenCurrentFragment;
    }

    private MutableLiveData<Integer> _deleteState = new MutableLiveData<>();
    public LiveData<Integer> getDeleteState(){
        return  _deleteState;
    }

    private void initializeFirebase(){
        database_ref = FirebaseDatabase.getInstance().getReference();
        mAuth= FirebaseAuth.getInstance();
        storageReference= FirebaseStorage.getInstance().getReference();
    }

    public Bitmap getResizedBitmap(Bitmap image, int maxSize) {
        int width = image.getWidth();
        int height = image.getHeight();

        float bitmapRatio = (float) width / (float) height;
        if (bitmapRatio > 1) {
            width = maxSize;
            height = (int) (width / bitmapRatio);
        } else {
            height = maxSize;
            width = (int) (height * bitmapRatio);
        }
        return Bitmap.createScaledBitmap(image, width, height, true);
    }

    public void setMainActivity(MainActivity mainActivity){
        this.mainActivity=mainActivity;
    }

    public void settingsCopy(Playlist playlist) {
        initializeFirebase();
        _shouldOpenCopy.setValue(false);
        database_ref.child("music").child("playlists").child(mAuth.getCurrentUser().getUid()).orderByKey().addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds : snapshot.getChildren()) {
                    if (
                            ds.child("title").getValue().toString().trim().equals(playlist.getTitle())
                                    &&
                                    ds.child("description").getValue().toString().trim().equals(playlist.getDescription())
                    ) {
                        key = ds.getKey();
                       _shouldOpenCopy.setValue(true);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    public void deletePlaylist(Playlist playlist) {
        initializeFirebase();
        database_ref.child("music").child("playlists").child(mAuth.getCurrentUser().getUid()).orderByKey().addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds : snapshot.getChildren()) {
                    if (
                            ds.child("title").getValue().toString().trim().equals(playlist.getTitle())
                                    &&
                                    ds.child("description").getValue().toString().trim().equals(playlist.getDescription())
                    ) {

                        String key = ds.getKey();

                        database_ref.child("music").child("playlists").child(mAuth.getCurrentUser().getUid()).child(key).removeValue().addOnCompleteListener( new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                 Log.d("Playlist", "Playlist deleted!");

                                    storageReference.child("images/playlists/" + mAuth.getCurrentUser().getUid() + "/" + key).getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                        @Override
                                        public void onSuccess(Uri uri) {
                                            storageReference.child("images/playlists/" + mAuth.getCurrentUser().getUid() + "/" + key).delete().addOnCompleteListener( new OnCompleteListener<Void>() {
                                                @Override
                                                public void onComplete(@NonNull Task<Void> task) {
                                                  if (task.isSuccessful()) {
                                                      _deleteState.setValue(0);
                                                      openHomeFragment();
                                                    } else {
                                                      _deleteState.setValue(1);
                                                      openHomeFragment();
                                                    }
                                                }
                                            });
                                        }
                                    }).addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception exception) {
                                            _deleteState.setValue(2);
                                            openHomeFragment();
                                        }
                                    });

                                } else {
                                    _deleteState.setValue(3);
                                    openHomeFragment();
                                }
                            }
                        });
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                _deleteState.setValue(4);
                openHomeFragment();
            }
        });
    }

    public void validInput(String name, String desc) {
        name = name.trim();
        desc = desc.trim();


        if (name.isEmpty() && desc.isEmpty()) {
            _validErrorState.setValue(1);
        } else {

            if (name.isEmpty() || desc.isEmpty()) {
                if (name.isEmpty()) {
                    _validErrorState.setValue(2);

                }

                if (desc.isEmpty()) {
                    _validErrorState.setValue(3);
                }
            } else {
                name = name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase();
                 finalName = name;
                 finalDesc= desc;
                database_ref.child("music").child("playlists").child(mAuth.getCurrentUser().getUid()).orderByKey().addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot != null) {
                            int x = (int) snapshot.getChildrenCount();
                            for (DataSnapshot ds : snapshot.getChildren()) {
                                if (ds.child("title").getValue().toString().trim().equals(finalName)) {
                                    x--;
                                    _validErrorState.setValue(4);
                                }
                            }

                            if (x == snapshot.getChildrenCount()) {
                                System.out.println("DODAJEMY COPY!");
                               copyPlaylist(old_key, finalName,finalDesc,p);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.d("Firebase DB error", "FirebaseDatabase");
                    }
                });

            }
        }
    }

    public void copyPlaylist(String old_key, String name, String desc, Playlist p) {
        initializeFirebase();

        p.setTitle(name);
        p.setDescription(desc);

       key = database_ref.push().getKey();

      setNewPlaylist(old_key, name,desc,p);

   }

    private void setNewPlaylist(String old_key, String name, String desc, Playlist p) {
        database_ref.child("music").child("playlists").child(mAuth.getCurrentUser().getUid()).child(key).setValue(p).addOnCompleteListener(mainActivity, new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    database_ref.child("music").child("playlists").child(mAuth.getCurrentUser().getUid()).child(key).child("isAlbum").setValue(p.isAlbum()).addOnCompleteListener(mainActivity, new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {

                                database_ref.child("music").child("playlists").child(mAuth.getCurrentUser().getUid()).child(key).child("album").removeValue().addOnCompleteListener(mainActivity, new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        //Copy songs
                                        x = new int[]{0};
                                        copySongs(old_key,name,desc,p);
                                      }
                                });
                            }else{
                            Log.d(MainActivity.FIREBASE, "Error while setting isAlbum");
                            }
                        }
                    });
                } else {
                    Toast.makeText(mainActivity, "Error while adding Playlist.", Toast.LENGTH_SHORT).show();

                }
            }
        });
    }

    private void copySongs(String old_key, String name, String desc, Playlist p) {
        database_ref.child("music").child("playlists").child(mAuth.getCurrentUser().getUid()).child(old_key).child("songs").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                ArrayList<FavoriteFirebaseSong> favoriteFirebaseSongs = new ArrayList<>();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    x[0]++;
                    FavoriteFirebaseSong favoriteFirebaseSong = new FavoriteFirebaseSong();
                    favoriteFirebaseSong.setAuthor(dataSnapshot.child("author").getValue().toString());
                    favoriteFirebaseSong.setAlbum(dataSnapshot.child("album").getValue().toString());
                    favoriteFirebaseSong.setNumberInAlbum(Integer.valueOf(dataSnapshot.child("numberInAlbum").getValue().toString()));
                    favoriteFirebaseSongs.add(favoriteFirebaseSong);
                    if (x[0] == snapshot.getChildrenCount()) {
                        database_ref.child("music").child("playlists").child(mAuth.getCurrentUser().getUid()).child(key).child("songs").setValue(favoriteFirebaseSongs).addOnCompleteListener(mainActivity, new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                    //Copy photo
                                    storageReference = FirebaseStorage.getInstance().getReference();
                                    storageReference.child("images/playlists/" + mAuth.getCurrentUser().getUid() + "/" + old_key).getDownloadUrl().addOnCompleteListener(mainActivity, new OnCompleteListener<Uri>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Uri> task) {
                                            if (task.isComplete() && task.isSuccessful()) {
                                                System.out.println("URI HERE FOUND");

                                                Uri uri = task.getResult();

                                                copyPhoto(uri);

                                            }

                                            Fragment currentFragment = mainActivity.getSupportFragmentManager().findFragmentById(R.id.mainFragmentContainer);
                                            if (currentFragment instanceof CurrentPlaylistFragment) {

                                                mainActivity.getSupportFragmentManager().beginTransaction().setCustomAnimations(R.anim.slide_in_up,R.anim.fade_out, R.anim.fade_in, R.anim.slide_out_up).replace(R.id.mainFragmentContainer, new CurrentPlaylistFragment(p.getTitle(), "", p, 0)).commit();
                                            }
                                        }
                                    });
                                }
                            }

                        }).addOnFailureListener(mainActivity, new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                System.out.println("Error while setting songs");


                            }
                        });
                    }
                }

                if (snapshot.getChildrenCount() == 0) {
                    //Copy photo
                    storageReference = FirebaseStorage.getInstance().getReference();
                    storageReference.child("images/playlists/" + mAuth.getCurrentUser().getUid() + "/" + old_key).getDownloadUrl().addOnCompleteListener(mainActivity, new OnCompleteListener<Uri>() {
                        @Override
                        public void onComplete(@NonNull Task<Uri> task) {
                            if (task.isComplete() && task.isSuccessful()) {
                                System.out.println("URI HERE FOUND");

                                Uri uri = task.getResult();

                                copyPhoto(uri);

                            }

                            Fragment currentFragment = mainActivity.getSupportFragmentManager().findFragmentById(R.id.mainFragmentContainer);
                            if (currentFragment instanceof CurrentPlaylistFragment) {

                                mainActivity.getSupportFragmentManager().beginTransaction().setCustomAnimations(R.anim.slide_in_up,R.anim.fade_out, R.anim.fade_in, R.anim.slide_out_up).replace(R.id.mainFragmentContainer, new CurrentPlaylistFragment(p.getTitle(), "", p, 0)).commit();
                            }
                        }
                    }).addOnFailureListener(mainActivity, new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Fragment currentFragment = mainActivity.getSupportFragmentManager().findFragmentById(R.id.mainFragmentContainer);
                            if (currentFragment instanceof CurrentPlaylistFragment) {

                                mainActivity.getSupportFragmentManager().beginTransaction().setCustomAnimations(R.anim.slide_in_up,R.anim.fade_out, R.anim.fade_in, R.anim.slide_out_up).replace(R.id.mainFragmentContainer, new CurrentPlaylistFragment(p.getTitle(), "", p, 0)).commit();
                            }
                        }
                    });

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void copyPhoto(Uri uri) {

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Bitmap bitmap = loadBitmap(uri.toString());
                    ByteArrayOutputStream bao = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, bao);
                    byte[] byteArray = bao.toByteArray();

                    storageReference.child("images/playlists/" + mAuth.getCurrentUser().getUid() + "/" + key).putBytes(byteArray).addOnCompleteListener(mainActivity, new OnCompleteListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                            if (!task.isSuccessful()) {
                                System.out.println("Error while copying photo");
                            } else {
                                storageReference.child("images/playlists/" + mAuth.getCurrentUser().getUid() + "/" + key).getDownloadUrl().addOnSuccessListener(mainActivity, new OnSuccessListener<Uri>() {
                                    @Override
                                    public void onSuccess(Uri u) {
                                        p.setImage_id(u.toString());
                                        database_ref.child("music").child("playlists").child(mAuth.getCurrentUser().getUid()).child(key).child("image_id").setValue(u.toString());
                                    }
                                });
                            }
                        }
                    });


                } catch (Exception e) {
                    Log.e("Thread", e.getMessage());
                }
            }
        });
        thread.start();

    }

    public Bitmap loadBitmap(String url) {
        Bitmap bm = null;
        InputStream is = null;
        BufferedInputStream bis = null;
        try {
            URLConnection conn = new URL(url).openConnection();
            conn.connect();
            is = conn.getInputStream();
            bis = new BufferedInputStream(is, 8192);
            bm = BitmapFactory.decodeStream(bis);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (bis != null) {
                try {
                    bis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return bm;
    }

    public Playlist getPlaylist(){
        return _playlist;
    }

    public void setPlaylist(Playlist playlist) {
        _playlist = playlist;
    }

    public String getKey() {
        return key;
    }

    public String  getFinalName() {
        return finalName;
    }

    public String  getFinalDesc() {
        return finalDesc;
    }

    public void setP(Playlist playlist) {
        p=playlist;
    }

    public Playlist getP(){
        return p;
    }

    public void setOldKey(String i){
        old_key=i;
    }

    public void setShouldOpenCopy(boolean b) {
        _shouldOpenCopy.setValue(b);
    }

    private void openHomeFragment() {
        for (int i = 0; i < mainActivity.getSupportFragmentManager().getBackStackEntryCount(); i++) {
            mainActivity.getSupportFragmentManager().popBackStack();
        }
        mainActivity.activityMainBinding.mainNavBottom.setSelectedItemId(R.id.nav_home_item);
        mainActivity.getSupportFragmentManager().beginTransaction().replace(R.id.mainFragmentContainer, new HomeFragment(false)).commit();
    }

}
