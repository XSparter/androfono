package org.gaussx.androfono;
import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    private static final int AUDIO_SAMPLE_RATE = 44100;
    private static final int AUDIO_CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final int BUFFER_SIZE = AudioRecord.getMinBufferSize(AUDIO_SAMPLE_RATE, AUDIO_CHANNEL_CONFIG, AUDIO_FORMAT);

    private AudioRecord audioRecord;
    private boolean isRecording = false;
    private SocketTask socketTask;
    private Thread recordingThread;

    private AudioTrack audioTrack;
    private boolean isPlaying = false;
    private boolean isAudioStreaming = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button startButton = findViewById(R.id.startButton);
        Button stopButton = findViewById(R.id.stopButton);
        Button playButton = findViewById(R.id.playButton);
        Button apricancello = findViewById(R.id.azionacancello);
        Button azionacitofono = findViewById(R.id.azionacitofono);
        Button stopPlayButton = findViewById(R.id.stopPlayButton);

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
        apricancello.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        inviaComando("apricancello",12343);
                    }
                });
                thread.start();
            }
        });
        azionacitofono.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        inviaComando("azionacitofono",12343);
                    }
                });
                thread.start();
            }
        });
        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopAudioStreaming();
            }
        });

        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startAudioPlaying();
            }
        });

        stopPlayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopAudioPlaying();
            }
        });
    }

    public static void inviaComando(String comando, int porta) {
        try {
            // Sostituisci con l'indirizzo IP e la porta del tuo server
            String serverAddress = "192.168.1.220";
            //int serverPort = 12343;
            int serverPort = porta;

            // Crea una connessione socket
            Socket socket = new Socket(serverAddress, serverPort);

            // Ottieni lo stream di output
            OutputStream out = socket.getOutputStream();

            // Invia il comando al server
            out.write(comando.getBytes());

            // Chiudi la connessione
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void startAudioStreaming() {
        if (!isRecording) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
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
        setMute();
    }
    public boolean mute = false;
    public boolean mute_microfono_speaker = true;
    public void setMute(){
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                if (mute) {
                    mute = false;
                    inviaComando("muteoff",54321);
                    setSpeaker();
                } else {
                    mute = true;
                    inviaComando("muteonn",54321);
                    setSpeaker();

                }
                Thread.currentThread().interrupt();
            }
        });
        thread.start();

    }
    public void setSpeaker(){
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                if (mute_microfono_speaker) {
                    mute_microfono_speaker = false;
                    inviaComando("muteoff",54322);
                } else {
                    mute_microfono_speaker = true;
                    inviaComando("muteonn",54322);

                }
                Thread.currentThread().interrupt();
            }
        });
        thread.start();

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

    private void startAudioPlaying() {
        if (!isPlaying) {
            isPlaying = true;
            audioTrack = new AudioTrack(
                    AudioManager.STREAM_MUSIC, //questo riproduce dallo speaker
                    //AudioManager.STREAM_VOICE_CALL, //questo riproduce dall'auricolare
                    AUDIO_SAMPLE_RATE,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AUDIO_FORMAT,
                    BUFFER_SIZE,
                    AudioTrack.MODE_STREAM);

            audioTrack.play();

            Thread playThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    receiveAudio();
                }
            });

            playThread.start();
        }
    }

    private void stopAudioPlaying() {
        if (isPlaying) {
            isPlaying = false;

            if (audioTrack != null) {
                audioTrack.stop();
                audioTrack.release();
            }
        }
    }

    private void receiveAudio() {
        try {
            Socket socket = new Socket("192.168.1.220", 12859);
            DataInputStream inputStream = new DataInputStream(socket.getInputStream());

            byte[] buffer = new byte[BUFFER_SIZE];

            while (isPlaying) {
                int bytesRead = inputStream.read(buffer, 0, BUFFER_SIZE);
                if (bytesRead == -1) {
                    break;  // Fine del flusso, esci dal ciclo
                }
                audioTrack.write(buffer, 0, bytesRead);
            }

            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
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

