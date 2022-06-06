package com.example.storage;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class Utility {

    //Method to write audio files
    public static void writeAudio(Context context, String name, InputStream stream) {
        AsyncTask.execute(() -> {
            OutputStream fos;
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ContentResolver resolver = context.getContentResolver();
                    ContentValues contentValues = new ContentValues();
                    contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, name + ".mp3");
                    contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "audio/mpeg");
                    contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_MUSIC);
                    Uri musicUri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, contentValues);
                    fos = resolver.openOutputStream(Objects.requireNonNull(musicUri));
                } else {
                    String musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).toString();
                    File musicStream = new File(musicDir, name + ".mp3");
                    fos = new FileOutputStream(musicStream);

                }
                byte[] buffer = new byte[1024];
                int length;
                while ((length = stream.read(buffer)) > 0) {
                    fos.write(buffer, 0, length);
                }

                stream.close();
                Log.e("test", "writeAudio: finished");
                Objects.requireNonNull(fos).close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

    }

    //Method to save an image
    static void writeImage(Context context, @NonNull Bitmap bitmap, @NonNull String name, String extension) {
        OutputStream fos;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentResolver resolver = context.getContentResolver();
                ContentValues contentValues = new ContentValues();
                contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, name + "." + extension);
                contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/" + extension);
                contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + File.separator + context.getPackageName());
                Uri imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
                fos = resolver.openOutputStream(Objects.requireNonNull(imageUri));
            } else {
                String imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString();
                File imagesDirFile = new File(imagesDir);

                File packageDir = new File(imagesDirFile, context.getPackageName());
                if (!packageDir.exists()) {
                    packageDir.mkdir();
                }
                File image = new File(packageDir, name + "." + extension);
                if (!image.exists()) image.createNewFile();
                fos = new FileOutputStream(image);
            }
            bitmap.compress(getImageFormat(extension), 100, fos);
            Objects.requireNonNull(fos).close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //method to read a file
    static String readFileScoped(Context context, String name) {
        ContentResolver cr = context.getContentResolver();
        Uri uri = MediaStore.Files.getContentUri("external");

        String[] projection = new String[]{
                MediaStore.Files.FileColumns._ID,
                MediaStore.Files.FileColumns.DISPLAY_NAME,
        };
        String selection = MediaStore.Files.FileColumns.DISPLAY_NAME + " = ? ";
        String[] selectionArgs = new String[]{name};
        String sortOrder = null; // unordered
        Cursor cursor = cr.query(uri, projection, selection, selectionArgs, sortOrder);
        // Cache column indices.
        int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID);

        while (cursor.moveToNext()) {
            // Get values of columns for a given video.
            long id = cursor.getLong(idColumn);

            Uri contentUri = ContentUris.withAppendedId(
                    MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL), id);
            ContentResolver resolver = context
                    .getContentResolver();
            try (InputStream stream = resolver.openInputStream(contentUri)) {
                BufferedReader r = new BufferedReader(new InputStreamReader(stream));
                StringBuilder total = new StringBuilder();
                for (String line; (line = r.readLine()) != null; ) {
                    total.append(line).append('\n');
                }
                return total.toString();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    //read an audio file
    static String[] readAudioScoped(Context context, String name) {
        ContentResolver cr = context.getContentResolver();
        Uri uri = MediaStore.Audio.Media.getContentUri("external");

        // every column, although that is huge waste, you probably need
        // BaseColumns.DATA (the path) only.
        String[] projection = new String[]{
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.ARTIST
        };
        String selection = MediaStore.Files.FileColumns.DISPLAY_NAME + " = ? ";
        String[] selectionArgs = new String[]{name};

        String sortOrder = MediaStore.Audio.Media.DATE_MODIFIED + " DESC ";
        Cursor cursor = cr.query(uri, projection, selection, selectionArgs, sortOrder);
        // Cache column indices.
        int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
        int nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME);
        int albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM);
        int artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST);

        while (cursor.moveToNext()) {
            // Get values of columns for a given video.
            long id = cursor.getLong(idColumn);
            String dName = cursor.getString(nameColumn);
            String album = cursor.getString(albumColumn);
            String artist = cursor.getString(artistColumn);

            Uri contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
            ContentResolver resolver = context
                    .getContentResolver();
            try (InputStream pfd = resolver.openInputStream(contentUri)) {
                return new String[]{dName, album, artist};

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public static String[] readOtherAudioScoped(Context context) {
        ContentResolver cr = context.getContentResolver();
        Uri uri = MediaStore.Audio.Media.getContentUri("external");

        // every column, although that is huge waste, you probably need
        // BaseColumns.DATA (the path) only.
        String[] projection = new String[]{
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.ARTIST
        };

        String sortOrder = null; // unordered

        Cursor cursor = cr.query(uri, projection, null, null, sortOrder);
        // Cache column indices.
        int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
        int nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME);
        int albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM);
        int artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST);

        while (cursor.moveToNext()) {
            // Get values of columns for a given video.
            long id = cursor.getLong(idColumn);
            String dName = cursor.getString(nameColumn);
            String album = cursor.getString(albumColumn);
            String artist = cursor.getString(artistColumn);

            Uri contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
            ContentResolver resolver = context
                    .getContentResolver();
            try (InputStream pfd = resolver.openInputStream(contentUri)) {
                return new String[]{dName, album, artist};

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    //read an image from self
    static Bitmap readImageScoped(Context context, String name) {
        ContentResolver cr = context.getContentResolver();
        Uri uri = MediaStore.Images.Media.getContentUri("external");

        // every column, although that is huge waste, you probably need
        // BaseColumns.DATA (the path) only.
        String[] projection = new String[]{
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
        };

        String selection = MediaStore.Images.Media.DISPLAY_NAME + " = ?";
        String[] selectionArgs = new String[]{name};

        String sortOrder = null; // unordered

        Cursor cursor = cr.query(uri, projection, selection, selectionArgs, sortOrder);
        // Cache column indices.
        int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);

        while (cursor.moveToNext()) {
            // Get values of columns for a given video.
            long id = cursor.getLong(idColumn);

            Uri contentUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);
            ContentResolver resolver = context
                    .getContentResolver();
            try (ParcelFileDescriptor pfd = resolver.openFileDescriptor(contentUri, "r")) {
                return BitmapFactory.decodeFileDescriptor(pfd.getFileDescriptor());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    //method to save a file
    static void saveFileScoped(Context context, String content, @NonNull String name, @NonNull String extension) {
        OutputStream fos;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentResolver resolver = context.getContentResolver();
                ContentValues contentValues = new ContentValues();
                contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, name + "." + extension);
                contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "application/" + extension);
                contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS + File.separator + context.getPackageName());
                Uri fileUri = resolver.insert(MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), contentValues);
                fos = resolver.openOutputStream(Objects.requireNonNull(fileUri));
            } else {
                String filesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).toString() + File.separator + context.getPackageName();
                File file = new File(filesDir, name + "." + extension);
                fos = new FileOutputStream(file);
            }
            fos.write(content.getBytes(StandardCharsets.UTF_8));
            Objects.requireNonNull(fos).close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //method to read an image from others
    static Bitmap readOtherImageScoped(Context context) {
        ContentResolver cr = context.getContentResolver();
        Uri uri = MediaStore.Images.Media.getContentUri("external");

        // every column, although that is huge waste, you probably need
        // BaseColumns.DATA (the path) only.
        String[] projection = new String[]{
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
        };

        String sortOrder = MediaStore.Images.Media.DATE_MODIFIED + " DESC ";
        Cursor cursor = cr.query(uri, projection, null, null, sortOrder);
        // Cache column indices.
        int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);

        while (cursor.moveToNext()) {
            // Get values of columns for a given video.
            long id = cursor.getLong(idColumn);

            Uri contentUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);
            ContentResolver resolver = context
                    .getContentResolver();
            try (ParcelFileDescriptor pfd = resolver.openFileDescriptor(contentUri, "r")) {

                return BitmapFactory.decodeFileDescriptor(pfd.getFileDescriptor());

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    //testing
    public static void fileApiWrite() {
        String SDCARD_ROOT_PATH = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + Environment.DIRECTORY_DOWNLOADS;
        File fileDirectory = new File(SDCARD_ROOT_PATH);
        if (!fileDirectory.exists()) {
            boolean res = fileDirectory.mkdir();
            Log.e("test", "fileWriter: " + res);
        }
        try {
            File file = new File(SDCARD_ROOT_PATH, "testingtesting" + ".txt");
            FileWriter writer = new FileWriter(file);
            writer.append("testing testing testing");
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
//            Utils.printErrorLog(Utils.class.getSimpleName(), "active_line_id : io_exception : " + e.getMessage());
        }
    }

    public static String fileApiRead() {
        String SDCARD_ROOT_PATH = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + Environment.DIRECTORY_DOWNLOADS + File.separator + "abcd";
        File fileDirectory = new File(SDCARD_ROOT_PATH);
        if (!fileDirectory.exists()) {
            return null;
        }
        try {
            File file = new File(SDCARD_ROOT_PATH, "testingtesting" + ".txt");
            StringBuilder resultStringBuilder = new StringBuilder();
            try (BufferedReader br
                         = new BufferedReader(new InputStreamReader(new FileInputStream(file)))) {
                String line;
                while ((line = br.readLine()) != null) {
                    resultStringBuilder.append(line).append("\n");
                }
            }
            return resultStringBuilder.toString();

        } catch (IOException e) {
            e.printStackTrace();
//            Utils.printErrorLog(Utils.class.getSimpleName(), "active_line_id : io_exception : " + e.getMessage());
        }
        return null;
    }

    //helper method to detect bitmap format
    private static Bitmap.CompressFormat getImageFormat(String type) {
        if (type.equals(Bitmap.CompressFormat.PNG.name()))
            return Bitmap.CompressFormat.PNG;
        if (type.equals(Bitmap.CompressFormat.WEBP.name()))
            return Bitmap.CompressFormat.WEBP;
        return Bitmap.CompressFormat.JPEG;

    }


}
