package com.spider.videoandaudiocall.activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.View;
import android.view.ViewGroup;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.gson.Gson;
import com.spider.videoandaudiocall.databinding.ActivityMainBinding;
import com.spider.videoandaudiocall.databinding.ItemContainerUserBinding;
import com.spider.videoandaudiocall.listener.UsersListener;
import com.spider.videoandaudiocall.model.User;
import com.spider.videoandaudiocall.utilites.BaseActivity;
import com.spider.videoandaudiocall.utilites.Constants;
import com.spider.videoandaudiocall.utilites.PreferenceManager;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends BaseActivity implements UsersListener {

    ActivityMainBinding mainBinding;
    private final Context context = MainActivity.this;
    private PreferenceManager preferenceManager;
    private List<User> users;
    private UsersAdapter usersAdapter;
    private final int REQUEST_CODE_BATTERY_OPTIMIZATIONS = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainBinding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(mainBinding.getRoot());
        preferenceManager = new PreferenceManager(context);
        mainBinding.textTitle.setText(String.format("%s %s", preferenceManager.getString(Constants.KEY_FIRST_NAME), preferenceManager.getString(Constants.KEY_LAST_NAME)));

        FirebaseInstanceId.getInstance().getInstanceId().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                sendFCMTokenToDatabase(task.getResult().getToken());
            }
        });

        users = new ArrayList<>();
        usersAdapter = new UsersAdapter(users, this);
        GridLayoutManager layoutManager = new GridLayoutManager(context, 2);
        mainBinding.recyclerViewUsers.setLayoutManager(layoutManager);
        mainBinding.recyclerViewUsers.setAdapter(usersAdapter);
        mainBinding.swipeRefreshLayout.setOnRefreshListener(this::getUsers);

        mainBinding.signOut.setOnClickListener(view -> signOut());
        getUsers();
        checkForBatteryOptimizations();
    }

    private void getUsers() {
        mainBinding.swipeRefreshLayout.setRefreshing(true);
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        database.collection(Constants.KEY_COLLECTION_USERS)
                .get()
                .addOnCompleteListener(task -> {
                    mainBinding.swipeRefreshLayout.setRefreshing(false);
                    String myUsersId = preferenceManager.getString(Constants.KEY_USER_ID);
                    if (task.isSuccessful() && task.getResult() != null) {
                        users.clear();
                        for (QueryDocumentSnapshot documentSnapshot : task.getResult()) {
                            if (myUsersId.equals(documentSnapshot.getId())) {
                                continue;
                            }
                            User user = new User();
                            user.firstName = documentSnapshot.getString(Constants.KEY_FIRST_NAME);
                            user.lastName = documentSnapshot.getString(Constants.KEY_LAST_NAME);
                            user.email = documentSnapshot.getString(Constants.KEY_EMAIL);
                            user.token = documentSnapshot.getString(Constants.KEY_FCM_TOKEN);
                            users.add(user);
                        }
                        if (users.size() > 0) {
                            usersAdapter.notifyDataSetChanged();
                        } else {
                            mainBinding.textErrorMessage.setText(String.format("%s", "No users available"));
                            mainBinding.textErrorMessage.setVisibility(View.VISIBLE);
                        }
                    } else {
                        mainBinding.textErrorMessage.setText(String.format("%s", "No users available"));
                        mainBinding.textErrorMessage.setVisibility(View.VISIBLE);
                    }
                });
    }

    private void sendFCMTokenToDatabase(String token) {
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        DocumentReference documentReference = database.collection(Constants.KEY_COLLECTION_USERS).document(preferenceManager.getString(Constants.KEY_USER_ID));
        documentReference.update(Constants.KEY_FCM_TOKEN, token)
                .addOnFailureListener(e -> toast("Unable to send token: ", context));
    }

    private void signOut() {
        toast("Signing Out...", context);
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        DocumentReference documentReference = database.collection(Constants.KEY_COLLECTION_USERS).document(preferenceManager.getString(Constants.KEY_USER_ID));
        HashMap<String, Object> updates = new HashMap<>();
        updates.put(Constants.KEY_FCM_TOKEN, FieldValue.delete());
        documentReference.update(updates)
                .addOnSuccessListener(aVoid -> {
                    preferenceManager.clearPreferences();
                    startActivity(new Intent(getApplicationContext(), SignInActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> toast("Unable to sign out", context));
    }

    @Override
    public void initiateVideoMeeting(User user) {
        if (user.token == null || user.token.trim().isEmpty()) {
            toast(user.firstName+ " " +user.lastName+ " is not available for meeting",context);
        } else {
            Intent intent = new Intent(context, OutGoingActivity.class);
            intent.putExtra("user", user);
            intent.putExtra("type", "video");
            startActivity(intent);        }
    }

    @Override
    public void initiateAudioMeeting(User user) {
        if (user.token == null || user.token.trim().isEmpty()) {
            toast(user.firstName+ " " +user.lastName+ " is not available for meeting",context);
        } else {
            Intent intent = new Intent(getApplicationContext(), OutGoingActivity.class);
            intent.putExtra("user", user);
            intent.putExtra("type", "audio");
            startActivity(intent);
        }
    }

    @Override
    public void onMultipleUsersAction(Boolean isMultipleUsersSelected) {
        if (isMultipleUsersSelected) {
            mainBinding.imageConference.setVisibility(View.VISIBLE);
            mainBinding.imageConference.setOnClickListener(view -> {
                Intent intent = new Intent(getApplicationContext(), OutGoingActivity.class);
                intent.putExtra("selectedUsers", new Gson().toJson(usersAdapter.getSelectedUsers()));
                intent.putExtra("type", "video");
                intent.putExtra("isMultiple", true);
                startActivity(intent);
            });
        } else {
            mainBinding.imageConference.setVisibility(View.GONE);
        }
    }

    private void checkForBatteryOptimizations() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
            if (!powerManager.isIgnoringBatteryOptimizations(getPackageName())) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Warning");
                builder.setMessage("Battery optimization is enabled. It can interrupt running background services.");
                builder.setPositiveButton("Disable", (dialogInterface, i) -> {
                    Intent intent = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                    startActivityForResult(intent, REQUEST_CODE_BATTERY_OPTIMIZATIONS);
                });
                builder.setNegativeButton("Cancel", (dialogInterface, i) -> dialogInterface.dismiss());
                builder.create().show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_BATTERY_OPTIMIZATIONS) {
            checkForBatteryOptimizations();
        }
    }

    public class UsersAdapter extends RecyclerView.Adapter<UsersAdapter.UserViewHolder> {

        ItemContainerUserBinding itemContainerUserBinding;
        private final List<User> users;
        private final UsersListener usersListener;
        private final List<User> selectedUsers;

        public UsersAdapter(List<User> users, UsersListener usersListener) {
            this.users = users;
            this.usersListener = usersListener;
            selectedUsers = new ArrayList<>();
        }

        public List<User> getSelectedUsers() {
            return selectedUsers;
        }

        @NonNull
        @NotNull
        @Override
        public UsersAdapter.UserViewHolder onCreateViewHolder(@NonNull @NotNull ViewGroup parent, int viewType) {
            itemContainerUserBinding = ItemContainerUserBinding.inflate(getLayoutInflater(),parent,false);
            return new UserViewHolder(itemContainerUserBinding.getRoot());
        }

        @Override
        public void onBindViewHolder(@NonNull @NotNull UsersAdapter.UserViewHolder holder, int position) {
            holder.setUserData(users.get(position));
        }

        @Override
        public int getItemCount() {
            return users.size();
        }

        public class UserViewHolder extends RecyclerView.ViewHolder {

            public UserViewHolder(@NonNull @NotNull View itemView) {
                super(itemView);
            }

            void setUserData(User user) {
                itemContainerUserBinding.textUserName.setText(String.format("%s %s", user.firstName, user.lastName));
                itemContainerUserBinding.imageAudioMeeting.setOnClickListener(view -> usersListener.initiateAudioMeeting(user));
                itemContainerUserBinding.imageVideoMeeting.setOnClickListener(view -> usersListener.initiateVideoMeeting(user));

                /*itemContainerUserBinding.userContainer.setOnLongClickListener(view -> {
                    if (itemContainerUserBinding.imageSelected.getVisibility() != View.VISIBLE) {
                        selectedUsers.add(user);
                        itemContainerUserBinding.imageSelected.setVisibility(View.VISIBLE);
                        itemContainerUserBinding.imageVideoMeeting.setVisibility(View.GONE);
                        itemContainerUserBinding.imageAudioMeeting.setVisibility(View.GONE);
                        usersListener.onMultipleUsersAction(true);
                    }
                    return true;
                });*/

                /*itemContainerUserBinding.userContainer.setOnClickListener(view -> {
                    if (itemContainerUserBinding.imageSelected.getVisibility() == View.VISIBLE) {
                        selectedUsers.remove(user);
                        itemContainerUserBinding.imageSelected.setVisibility(View.GONE);
                        itemContainerUserBinding.imageVideoMeeting.setVisibility(View.VISIBLE);
                        itemContainerUserBinding.imageAudioMeeting.setVisibility(View.VISIBLE);
                        if (selectedUsers.size() == 0) {
                            usersListener.onMultipleUsersAction(false);
                        }
                    } else {
                        if (selectedUsers.size() > 0) {
                            selectedUsers.add(user);
                            itemContainerUserBinding.imageSelected.setVisibility(View.VISIBLE);
                            itemContainerUserBinding.imageVideoMeeting.setVisibility(View.GONE);
                            itemContainerUserBinding.imageAudioMeeting.setVisibility(View.GONE);
                        }
                    }
                });*/
            }
        }
    }
}