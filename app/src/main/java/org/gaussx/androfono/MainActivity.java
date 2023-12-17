package org.gaussx.androfono;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class MainActivity extends AppCompatActivity {

    private static final int AUDIO_SAMPLE_RATE = 44100;
    private static final int AUDIO_CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final int BUFFER_SIZE = AudioRecord.getMinBufferSize(AUDIO_SAMPLE_RATE, AUDIO_CHANNEL_CONFIG, AUDIO_FORMAT);

    private AudioRecord audioRecord;
    private boolean isRecording = false;
    private SocketTask socketTask;
    private Thread recordingThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button startButton = findViewById(R.id.startButton);
        Button stopButton = findViewById(R.id.stopButton);

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                    startAudioStreaming();
                } else {
                    requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 1);
                }
            }
        });

        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopAudioStreaming();
            }
        });
    }

    private void startAudioStreaming() {
        if (!isRecording) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                    AUDIO_SAMPLE_RATE, AUDIO_CHANNEL_CONFIG, AUDIO_FORMAT, BUFFER_SIZE);

            audioRecord.startRecording();
            isRecording = true;

            recordingThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    socketTask = new SocketTask();
                    socketTask.run();
                }
            });

            recordingThread.start();
        }
    }

    private void stopAudioStreaming() {
        if (isRecording) {
            isRecording = false;

            if (recordingThread != null) {
                try {
                    recordingThread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            if (audioRecord != null) {
                audioRecord.stop();
                audioRecord.release();
            }
        }
    }

    private class SocketTask implements Runnable {

        @Override
        public void run() {
            try {
                Socket socket = new Socket("192.168.1.220", 12345);
                DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());

                byte[] buffer = new byte[BUFFER_SIZE];

                while (isRecording) {
                    int bytesRead = audioRecord.read(buffer, 0, BUFFER_SIZE);
                    outputStream.write(buffer, 0, bytesRead);
                }

                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}