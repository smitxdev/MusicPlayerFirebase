package com.example.musicplayeradvance.adapters;

import android.app.Dialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.musicplayeradvance.MainActivity;
import com.example.musicplayeradvance.R;
import com.example.musicplayeradvance.fragments.player.PlayerFragment;
import com.example.musicplayeradvance.fragments.playlists.CurrentPlaylistFragment;
import com.example.musicplayeradvance.fragments.playlists.PlaylistsFragment;
import com.example.musicplayeradvance.models.FavoriteFirebaseSong;
import com.example.musicplayeradvance.models.Playlist;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomsheet.BottomSheetDialog;
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

public class PlaylistsFragmentAdapter extends RecyclerView.Adapter<PlaylistsFragmentAdapter.MyViewHolder> {
    private final ArrayList<Playlist> playlists;
    private final MainActivity mainActivity;

    private DatabaseReference database_ref;
    private FirebaseAuth mAuth;
    private StorageReference storageReference;

    private BottomSheetDialog bottomSheetDialog;

    public PlaylistsFragmentAdapter(MainActivity mainActivity, ArrayList<Playlist> playlists) {
        this.mainActivity = mainActivity;
        this.playlists = playlists;

        reinitializeState(mainActivity);
    }

    private void reinitializeState(MainActivity mainActivity) {
        if (mainActivity != null) {
            Fragment currentFragment = mainActivity.getSupportFragmentManager().findFragmentById(R.id.mainFragmentContainer);
            if (currentFragment instanceof PlaylistsFragment) {
                if (playlists.size() == 0) {
                    PlaylistsFragment.binding.playlistsRecyclerView.setVisibility(View.GONE);
                    PlaylistsFragment.binding.playlistsRecyclerState.setVisibility(View.VISIBLE);
                    PlaylistsFragment.binding.playlistsRecyclerState.setText("No Playlist's");
                } else {
                    PlaylistsFragment.binding.playlistsRecyclerView.setVisibility(View.VISIBLE);
                    PlaylistsFragment.binding.playlistsRecyclerState.setVisibility(View.GONE);
                }
            }
        }
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        database_ref = FirebaseDatabase.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();
        storageReference = FirebaseStorage.getInstance().getReference();

        return new MyViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.playlists_recycler_row, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        holder.setIsRecyclable(false);

        String title = playlists.get(position).getTitle();

        loadPhoto(holder,position);
        updateDefaultData(title,holder,position);

        holder.settings_btn.setOnClickListener(v -> openPlaylistSongSettingsDialog(position, holder));

        holder.linearLayout.setOnClickListener(v -> mainActivity.getSupportFragmentManager().beginTransaction().setCustomAnimations(R.anim.slide_in_up,R.anim.fade_out, R.anim.fade_in, R.anim.slide_out_up).replace(R.id.mainFragmentContainer, new CurrentPlaylistFragment(title, "", playlists.get(position), 0)).addToBackStack(null).commit());
    }

    private void updateDefaultData(String title, MyViewHolder holder, int position) {
        holder.title.setText(title);
        holder.desc.setText(playlists.get(position).getDescription());
    }

    private void loadPhoto(MyViewHolder holder, int position) {
        //Load photo
        database_ref.child("music").child("playlists").child(mAuth.getCurrentUser().getUid()).orderByKey().addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot != null) {
                    for (DataSnapshot ds : snapshot.getChildren()) {
                        if (ds.child("title").getValue().toString().equals(playlists.get(position).getTitle())) {
                            storageReference.child("images/playlists/" + mAuth.getCurrentUser().getUid() + "/" + ds.getKey()).getDownloadUrl().addOnSuccessListener(mainActivity, new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri uri) {
                                    Glide.with(mainActivity).load(uri).apply(RequestOptions.centerCropTransform()).override(holder.photo.getWidth(), holder.photo.getWidth()).into(holder.photo);
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception exception) {
                                    Log.d(MainActivity.FIREBASE, "Photo wasn't found"+".\nError: " + exception.getMessage());
                                    Glide.with(mainActivity.getApplicationContext()).load(R.drawable.img_not_found).apply(RequestOptions.centerCropTransform()).override(holder.photo.getWidth(), holder.photo.getWidth()).into(holder.photo);

                                }
                            });

                        }
                    }
                } else {
                    Glide.with(mainActivity.getApplicationContext()).load(R.drawable.img_not_found).apply(RequestOptions.centerCropTransform()).override(holder.photo.getWidth(), holder.photo.getWidth()).into(holder.photo);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Glide.with(mainActivity.getApplicationContext()).load(R.drawable.img_not_found).apply(RequestOptions.centerCropTransform()).override(holder.photo.getWidth(), holder.photo.getWidth()).into(holder.photo);
            }
        });
    }

    @Override
    public int getItemCount() {
        return playlists.size();
    }

    private void openPlaylistSongSettingsDialog(Integer position, MyViewHolder holder) {
        bottomSheetDialog = new BottomSheetDialog(holder.itemView.getContext());
        bottomSheetDialog.setContentView(R.layout.bottom_playlist_settings);

        LinearLayout copy = bottomSheetDialog.findViewById(R.id.bottomSettingsCopyBox);
        LinearLayout delete = bottomSheetDialog.findViewById(R.id.bottomSettingsDeleteBox);
        LinearLayout dismissDialog = bottomSheetDialog.findViewById(R.id.bottomSettingsDismissBox);

        copy.setOnClickListener(v -> {
            settingsCopy(playlists.get(position), holder);
            bottomSheetDialog.dismiss();
        });

        delete.setOnClickListener(v -> {
            deletePlaylist(playlists.get(position));
            bottomSheetDialog.dismiss();
        });
        dismissDialog.setOnClickListener(v -> bottomSheetDialog.dismiss());

        bottomSheetDialog.show();
    }

    private void settingsCopy(Playlist playlist, MyViewHolder holder) {
        database_ref.child("music").child("playlists").child(mAuth.getCurrentUser().getUid()).orderByKey().addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds : snapshot.getChildren()) {
                    if (
                            ds.child("title").getValue().toString().trim().equals(playlist.getTitle())
                                    &&
                                    ds.child("description").getValue().toString().trim().equals(playlist.getDescription())
                    ) {
                        openDialog(ds.getKey(), holder, playlist);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void deletePlaylist(Playlist playlist) {
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

                        database_ref.child("music").child("playlists").child(mAuth.getCurrentUser().getUid()).child(key).removeValue().addOnCompleteListener(mainActivity, new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {

                                    playlists.remove(playlist);
                                    notifyDataSetChanged();

                                    deleteImage(playlist,key);

                                } else {
                                    Log.d(MainActivity.FIREBASE, "Error while  deleting Playlist");
                                }
                            }
                        });
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.d(MainActivity.FIREBASE, "Error while  deleting Playlist");
            }
        });
    }

    private void deleteImage(Playlist playlist, String key) {
        Fragment currentFragment = mainActivity.getSupportFragmentManager().findFragmentById(R.id.mainFragmentContainer);
        if (currentFragment instanceof PlaylistsFragment) {
            playlists.remove(playlist);
            notifyDataSetChanged();
            storageReference.child("images/playlists/" + mAuth.getCurrentUser().getUid() + "/" + key).getDownloadUrl().addOnSuccessListener(mainActivity, new OnSuccessListener<Uri>() {
                @Override
                public void onSuccess(Uri uri) {
                    storageReference.child("images/playlists/" + mAuth.getCurrentUser().getUid() + "/" + key).delete().addOnCompleteListener(mainActivity, new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (!task.isSuccessful())  {
                                Toast.makeText(mainActivity, "Error while deleting Photo", Toast.LENGTH_SHORT).show();
                                Log.d(MainActivity.FIREBASE, "Error while deleting Photo");
                            }
                        }
                    });
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    Log.d(MainActivity.FIREBASE, "Photo wasn't found");
                }
            });


            //    activity.getSupportFragmentManager().beginTransaction().replace(R.id.main_fragment_container,new FavoritesFragment()).commit();
            if (playlists.size() == 0) {
                PlaylistsFragment.binding.playlistsRecyclerView.setVisibility(View.GONE);
                PlaylistsFragment.binding.playlistsRecyclerState.setVisibility(View.VISIBLE);
            }

            if(MainActivity.viewModel.getCurrentSongAlbum().getValue().equals(playlist.getTitle())){
                Fragment miniFrag = mainActivity.getSupportFragmentManager().findFragmentById(R.id.slidingLayoutFrag);
                if(miniFrag instanceof PlayerFragment){
                    ((PlayerFragment) miniFrag).dismissPlayer();
                }
            }
        }
    }

    private void openDialog(String key, MyViewHolder holder, Playlist playlist) {


        holder.dialog = new Dialog(mainActivity, R.style.Theme_AltasNotas);
        holder.dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        holder.dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        holder.dialog.setCanceledOnTouchOutside(true);
        holder.dialog.setContentView(R.layout.add_playlists_dialog);
        holder.dialog.getWindow().setLayout(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT);
        holder.dialog.getWindow().setGravity(Gravity.CENTER);

        ImageButton cancel, accept;

        holder.dialog_playlist_name = holder.dialog.getWindow().getDecorView().findViewById(R.id.addPlaylistDialogName);
        holder.dialog_playlist_desc = holder.dialog.getWindow().getDecorView().findViewById(R.id.addPlaylistDialogDesc);

        holder.dialog_playlist_name.setText(playlist.getTitle());
        holder.dialog_playlist_desc.setText(playlist.getDescription());
        cancel = holder.dialog.getWindow().getDecorView().findViewById(R.id.addPlaylistDialogCancelBtn);
        accept = holder.dialog.getWindow().getDecorView().findViewById(R.id.addPlaylistDialogAcceptBtn);


        cancel.setOnClickListener(v -> holder.dialog.dismiss());


        accept.setOnClickListener(v -> validInput(key, holder.dialog_playlist_name.getText().toString(), holder.dialog_playlist_desc.getText().toString(), holder, playlist));

        holder.dialog.show();

    }

    private void validInput(String key, String name, String desc, MyViewHolder holder, Playlist playlist) {
        name = name.trim();
        desc = desc.trim();


        if (name.isEmpty() && desc.isEmpty()) {
            Toast.makeText(mainActivity, "Both fields are empty.\nPlease fill data.", Toast.LENGTH_SHORT).show();
        } else {

            if (name.isEmpty() || desc.isEmpty()) {
                if (name.isEmpty()) {
                    Toast.makeText(mainActivity, "Name is empty.\nPlease fill data.", Toast.LENGTH_SHORT).show();
                }

                if (desc.isEmpty()) {
                    Toast.makeText(mainActivity, "Description is empty.\nPlease fill data.", Toast.LENGTH_SHORT).show();
                }
            } else {
                name = name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase();
                desc = desc.substring(0, 1).toUpperCase() + desc.substring(1).toLowerCase();
                String finalName = name;
                String finalDesc = desc;
                database_ref.child("music").child("playlists").child(mAuth.getCurrentUser().getUid()).orderByKey().addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot != null) {
                            int x = (int) snapshot.getChildrenCount();
                            for (DataSnapshot ds : snapshot.getChildren()) {
                                if (ds.child("title").getValue().toString().trim().equals(finalName)) {
                                    x--;
                                    Toast.makeText(mainActivity, "Playlist exist with same title!", Toast.LENGTH_SHORT).show();
                                }
                            }

                            if (x == snapshot.getChildrenCount()) {
                                holder.dialog.dismiss();
                                copyPlaylist(key, finalName, finalDesc, playlist);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.d(MainActivity.FIREBASE, "Firebase DB error");
                    }
                });

            }
        }
    }

    private void copyPlaylist(String old_key, String name, String desc, Playlist p) {

        p.setTitle(name);
        p.setDescription(desc);

        String key = database_ref.push().getKey();

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
                                        int[] x = {0};
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


                                                                                Uri uri = task.getResult();


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
                                                                                                        Log.d(MainActivity.FIREBASE, "Error while  copying photo");
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

                                                                            Fragment currentFragment = mainActivity.getSupportFragmentManager().findFragmentById(R.id.mainFragmentContainer);
                                                                            if (currentFragment instanceof PlaylistsFragment) {

                                                                                mainActivity.getSupportFragmentManager().beginTransaction().setCustomAnimations(R.anim.slide_in_up,R.anim.fade_out, R.anim.fade_in, R.anim.slide_out_up).replace(R.id.mainFragmentContainer, new CurrentPlaylistFragment(p.getTitle(), "", p, 0)).commit();
                                                                            }
                                                                        }
                                                                    });
                                                                }
                                                            }

                                                        }).addOnFailureListener(mainActivity, new OnFailureListener() {
                                                            @Override
                                                            public void onFailure(@NonNull Exception e) {
                                                                Log.d(MainActivity.FIREBASE, "Error while  setting songs");

                                                            }
                                                        });
                                                    }
                                                }

                                                if (snapshot.getChildrenCount() == 0) {
                                                    //Copy photo
                                                    storageReference = FirebaseStorage.getInstance().getReference();
                                                    storageReference.child("images/playlists/" + mAuth.getCurrentUser().getUid() + "/" + old_key).getDownloadUrl()
                                                            .addOnCompleteListener(mainActivity, new OnCompleteListener<Uri>() {
                                                        @Override
                                                        public void onComplete(@NonNull Task<Uri> task) {
                                                            if (task.isComplete() && task.isSuccessful()) {


                                                                Uri uri = task.getResult();


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
                                                                                        Log.d(MainActivity.FIREBASE, "Error while  copying photo");
                                                                                        Fragment currentFragment = mainActivity.getSupportFragmentManager().findFragmentById(R.id.mainFragmentContainer);
                                                                                        if (currentFragment instanceof PlaylistsFragment) {

                                                                                            mainActivity.getSupportFragmentManager().beginTransaction().setCustomAnimations(R.anim.slide_in_up,R.anim.fade_out, R.anim.fade_in, R.anim.slide_out_up).replace(R.id.mainFragmentContainer, new CurrentPlaylistFragment(p.getTitle(), "", p, 0)).commit();
                                                                                        }

                                                                                    }
                                                                                }
                                                                            });


                                                                        } catch (Exception e) {
                                                                            Log.e("Thread", e.getMessage());
                                                                        }
                                                                    }
                                                                });
                                                                thread.start();

                                                                Fragment currentFragment = mainActivity.getSupportFragmentManager().findFragmentById(R.id.mainFragmentContainer);
                                                                if (currentFragment instanceof PlaylistsFragment) {

                                                                    mainActivity.getSupportFragmentManager().beginTransaction().setCustomAnimations(R.anim.slide_in_up,R.anim.fade_out, R.anim.fade_in, R.anim.slide_out_up).replace(R.id.mainFragmentContainer, new CurrentPlaylistFragment(p.getTitle(), "", p, 0)).commit();
                                                                }
                                                            }


                                                        }
                                                    })
                                                            .addOnFailureListener(mainActivity, new OnFailureListener() {
                                                        @Override
                                                        public void onFailure(@NonNull Exception e) {
                                                            Fragment currentFragment = mainActivity.getSupportFragmentManager().findFragmentById(R.id.mainFragmentContainer);
                                                            if (currentFragment instanceof PlaylistsFragment) {

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
                                });
                            }
                        }
                    });
                } else {
                    Toast.makeText(mainActivity, "Error while adding Playlist.", Toast.LENGTH_SHORT).show();

                }
            }
        });
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

    public class MyViewHolder extends RecyclerView.ViewHolder {
        ImageView photo;
        TextView title, desc;
        ImageButton settings_btn;
        LinearLayout linearLayout;

        TextView dialog_playlist_name, dialog_playlist_desc;
        Dialog dialog;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            photo = itemView.findViewById(R.id.playlistsRecyclerRowPhoto);
            title = itemView.findViewById(R.id.playlistsRecyclerRowTitle);
            desc = itemView.findViewById(R.id.playlistsRecyclerRowDescription);

            settings_btn = itemView.findViewById(R.id.playlistsRecyclerRowSettingsBtn);
            linearLayout = itemView.findViewById(R.id.playlistsRecyclerRowBox);
        }
    }
}
