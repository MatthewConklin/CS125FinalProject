package com.example.mikem.finalproject;

import android.app.Activity;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.DragEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SeekBar;

import org.toilelibre.libe.soundtransform.actions.fluent.FluentClient;
import org.toilelibre.libe.soundtransform.model.converted.sound.transform.PitchSoundTransform;
import org.toilelibre.libe.soundtransform.model.exception.SoundTransformException;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;

import cafe.adriel.androidaudioconverter.AndroidAudioConverter;
import cafe.adriel.androidaudioconverter.callback.IConvertCallback;
import cafe.adriel.androidaudioconverter.callback.ILoadCallback;
import cafe.adriel.androidaudioconverter.model.AudioFormat;

import static org.toilelibre.libe.soundtransform.actions.fluent.FluentClient.start;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "Main Menu";
    private static final int READ_REQUEST_CODE = 42;
    private Uri currentAudioURI = null;
    SeekBar seekBar;
    MediaPlayer mediaPlayer = null;
    Handler handler;
    Runnable runnable;
    File currentFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        handler = new Handler();


        seekBar = findViewById(R.id.songProgressBar);


        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                Log.d(TAG, "progress bar is changed");
                if (mediaPlayer != null && fromUser) {
                    mediaPlayer.seekTo(progress);
                    Log.d(TAG, "progress bar is changed and seekTo is called");
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });


        final Button openFile = findViewById(R.id.openFile);
        openFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                Log.d(TAG, "Open file button clicked");
                startOpenFile();
            }
        });

        final Button bitify = findViewById(R.id.bitButton);
        bitify.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                Log.d(TAG, "8-bititfy button clicked");
            }
        });

        final Button save = findViewById(R.id.saveButton);
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                Log.d(TAG, "save button clicked");
            }
        });

        final ImageButton lessPitch = findViewById(R.id.pitchLess);
        lessPitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                Log.d(TAG, "Less Pitch button clicked");
                if (currentFile != null) {
                    Log.d(TAG, "currentFile does exist: " + Uri.fromFile(currentFile));
                    try {
                        start().withFile(currentFile).convertIntoSound().apply(new PitchSoundTransform(75)).exportToFile(currentFile);
                    } catch (SoundTransformException e) {
                        Log.d(TAG, "Pitch less passed a sound transform exception");
                    }
                }
                if (mediaPlayer != null) {
                    try {
                        currentAudioURI = Uri.fromFile(currentFile);
                        mediaPlayer.release();
                        mediaPlayer = new MediaPlayer();
                        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                        mediaPlayer.setDataSource(getApplicationContext(), currentAudioURI);
                        mediaPlayer.prepare();
                        seekBar.setMax(mediaPlayer.getDuration());

                    }
                    catch (IOException ex) {
                        Log.wtf(TAG, "file does not exist");
                    }
                }

            }
        });

        final ImageButton morePitch = findViewById(R.id.pitchMore);
        morePitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                Log.d(TAG, "more pitch button clicked");
            }
        });

        final ImageButton play = findViewById(R.id.playButton);
        play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                Log.d(TAG, "play button clicked");
                if (mediaPlayer != null) {
                    if (mediaPlayer.isPlaying()) {
                        mediaPlayer.pause();
                    } else {
                        mediaPlayer.start();
                        playCycle();
                    }
                }
            }
        });
    }

    public void playCycle() {
        if (mediaPlayer != null) {
            seekBar.setProgress(mediaPlayer.getCurrentPosition());

            if (mediaPlayer.isPlaying()) {
                runnable = new Runnable() {
                    @Override
                    public void run() {
                        playCycle();
                    }
                };
                handler.postDelayed(runnable, 1000);
            }
        }
    }
    private void startOpenFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("audio/*");
        startActivityForResult(intent, READ_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK) {
            Log.w(TAG, "onActivityResult with code " + requestCode + " failed");
            return;
        }

        if (requestCode == READ_REQUEST_CODE) {
            if (mediaPlayer != null) {
                mediaPlayer.release();
            }
            Log.w(TAG, "Storing this Audio URI from the upload button");
            currentAudioURI = data.getData();
            try {

                Log.d(TAG, "Uri produced from file: " + Uri.fromFile(currentFile));
                mediaPlayer = new MediaPlayer();
                mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                mediaPlayer.setDataSource(getApplicationContext(), currentAudioURI);
                mediaPlayer.prepare();
                if (seekBar != null) {
                    seekBar.setMax(mediaPlayer.getDuration());
                }

            }
            catch (IOException ex) {
                Log.wtf(TAG, "file does not exist");
            }

        } else {
            Log.d(TAG, "requestCode was not expected: " + requestCode);
        }

        Log.d(TAG, "Audio selection produced URI " + currentAudioURI);
    }

    public void convertToWav() {
        IConvertCallback callback = new IConvertCallback() {
            @Override
            public void onSuccess(File convertedFile) {
                // So fast? Love it!
            }
            @Override
            public void onFailure(Exception error) {
                // Oops! Something went wrong
            }
        };
        AndroidAudioConverter.with(this)
                    // Your current audio file
                .setFile(currentFile)

                            // Your desired audio format
                            .setFormat(AudioFormat.WAV)

                            // An callback to know when conversion is finished
                            .setCallback(callback)

                            // Start conversion
                            .convert();
    }

    private void copyInputStreamToFile(InputStream in, File file) {
        OutputStream out = null;

        try {
            out = new FileOutputStream(file);
            byte[] buf = new byte[1024];
            int len;
            while((len=in.read(buf))>0){
                out.write(buf,0,len);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            // Ensure that the InputStreams are closed even if there's an exception.
            try {
                if ( out != null ) {
                    out.close();
                }

                // If you want to close the "in" InputStream yourself then remove this
                // from here but ensure that you close it yourself eventually.
                in.close();
            }
            catch ( IOException e ) {
                e.printStackTrace();
            }
        }
    }
}
