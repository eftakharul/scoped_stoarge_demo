package com.example.storage;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.storage.databinding.ActivityMainBinding;

import java.io.InputStream;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "test";
    private ActivityMainBinding binding;
    private static final int READ_EXTERNAL_STORAGE_PERMISSION_CODE = 123;
    private static final int PERMISSION_ALL = 124;
    private static final String[] PERMISSIONS = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);
        initUI();
    }

    private void initUI() {
        binding.writePictureButton.setOnClickListener(this);
        binding.readPictureButton.setOnClickListener(this);
        binding.writeMusicButton.setOnClickListener(this);
        binding.readMusicButton.setOnClickListener(this);
        binding.readOtherPictureButton.setOnClickListener(this);
        binding.readOtherMusicButton.setOnClickListener(this);
        binding.writeFileButton.setOnClickListener(this);
        binding.readFileButton.setOnClickListener(this);
        binding.manageAllFilesButton.setOnClickListener(this);
    }

    @RequiresApi(api = Build.VERSION_CODES.R)
    private void askManageExternalStoragePermission() {
        try {
            Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
            intent.addCategory("android.intent.category.DEFAULT");
            intent.setData(Uri.parse(String.format("package:%s", getApplicationContext().getPackageName())));
            startActivity(intent);

        } catch (Exception e) {
            Log.e(TAG, "_log : ask_manage_external_storage_permission : exception : " + e.getMessage());
            Intent intent = new Intent();
            intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
            startActivity(intent);
        }
    }

    private void getRequiredPermission(Context context) {
        if (!hasPermissions(context, PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
        } else {
            Toast.makeText(context, "Permission already granted", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean hasPermissions(Context context, String[] permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_ALL) {
            for (int i = 0, len = permissions.length; i < len; i++) {
                String permission = permissions[i];
                if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                    if (Manifest.permission.WRITE_EXTERNAL_STORAGE.equals(permission)) {
                        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_ALL);
                    } else if (Manifest.permission.READ_EXTERNAL_STORAGE.equals(permission)) {
                        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_ALL);
                    }
                }
            }
        } else if (requestCode == READ_EXTERNAL_STORAGE_PERMISSION_CODE) {
            for (int i = 0, len = permissions.length; i < len; i++) {
                String permission = permissions[i];
                if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                    if (Manifest.permission.READ_EXTERNAL_STORAGE.equals(permission)) {
                        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, READ_EXTERNAL_STORAGE_PERMISSION_CODE);
                    }
                }
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void writeImage() {
        AsyncTask.execute(() -> {
            InputStream in = getResources().openRawResource(R.raw.demo_image);
            Bitmap bmp = BitmapFactory.decodeStream(in);
            if (bmp != null)
                Utility.writeImage(MainActivity.this, bmp, Constants.IMAGE_NAME, Constants.IMAGE_EXTENSION);
        });
    }

    private void readImage() {
        Bitmap bmp = Utility.readImageScoped(this, Constants.IMAGE_NAME + "." + Constants.IMAGE_EXTENSION);
        if (bmp == null) Log.e(TAG, "initUI: null image returned");
        binding.ownImageView.setImageBitmap(bmp);
        binding.ownImageView.setVisibility(View.VISIBLE);
    }

    private void readOtherImage() {
        Bitmap bmp = Utility.readOtherImageScoped(this);
        if (bmp == null) Log.e(TAG, "initUI: null image returned");
        binding.otherImageView.setImageBitmap(bmp);
        binding.otherImageView.setVisibility(View.VISIBLE);
    }

    private void writeAudio() {
        AsyncTask.execute(() -> {
            InputStream audioStream = getResources().openRawResource(R.raw.demo);
            Utility.writeAudio(MainActivity.this, Constants.SONG_NAME, audioStream);
        });
    }

    private void readAudio() {
        String[] res = Utility.readAudioScoped(this, Constants.SONG_NAME + "." + Constants.SONG_EXTENSION);
        if (res != null)
            binding.musicInfoTextView.setText("name :" + res[0] + "\nalbum: " + res[1] + "\nartist: " + res[2]);
    }

    private void readOtherAudio() {
        String[] oRes = Utility.readOtherAudioScoped(this);
        if (oRes != null)
            binding.otherMusicInfoTextView.setText("name :" + oRes[0] + "\nalbum: " + oRes[1] + "\nartist: " + oRes[2]);
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void writeFile() {
        AsyncTask.execute(() -> Utility.saveFileScoped(MainActivity.this, "text written from demo application", Constants.FILE_NAME, Constants.FILE_EXTENSION));
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void readFile() {
        String contents = Utility.readFileScoped(this, Constants.FILE_NAME + "." + Constants.FILE_EXTENSION);
        Log.e(TAG, "read file returned " + contents);
        binding.fileTextView.setText(contents);
    }

    private void handleManagePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                getRequiredPermission(this);
            } else {
                askManageExternalStoragePermission();
            }
        } else {
            getRequiredPermission(this);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    public void onClick(View view) {

        switch (view.getId()) {
            case R.id.write_picture_button:
                writeImage();
                break;
            case R.id.read_picture_button:
                readImage();
                break;
            case R.id.read_other_picture_button:
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, READ_EXTERNAL_STORAGE_PERMISSION_CODE);
                } else {
                    readOtherImage();
                }
                break;

            case R.id.write_music_button:
                writeAudio();
                break;
            case R.id.read_music_button:
                readAudio();
                break;
            case R.id.read_other_music_button:
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, READ_EXTERNAL_STORAGE_PERMISSION_CODE);
                } else {
                    readOtherAudio();
                }
                break;
            case R.id.write_file_button:
                writeFile();
                break;
            case R.id.read_file_button:
                readFile();
                break;
            case R.id.manage_all_files_button:
                handleManagePermission();
                break;

            default:
                throw new IllegalStateException("Unexpected value: " + view.getId());
        }
    }
}