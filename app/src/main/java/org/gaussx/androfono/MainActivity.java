package org.gaussx.androfono;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.util.Log;

import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.filters.LowPassFS;

import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
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

    private AudioTrack audioTrack;
    LinearLayout visualizzatore;
    private boolean isPlaying = false;
    private boolean isAudioStreaming = false;
    private ImageView ball;
    private boolean isRed = true;
    public boolean is_alredy_recording_pressed = false;
    public int porta_altoparlante = 0;
    public int porta_microfono = 0;
    private static final int SAMPLE_RATE = 44100;
    private static final int WINDOW_SIZE = 3;
    public int porta_altoparlante_controllo = 0;
    public int porta_microfono_controllo = 0;
    ImageView redCircleImageView;
    TextView viewdilog;
    ImageView greenCircleImageView;
    public String status_citofono = "red";
    public int portacomandiesecutore = 0; // porta del comando esecutore è anche quella che viene interrogata all'inizio e che restituisce le porte dei microfoni e degli altoparlanti

    //la porta dell'esecutore viene comunque aggiornata all'avvio.
    // Creare un ImageView per il cerchio verde
    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        redCircleImageView = createCircleImageView(R.drawable.red_ball);
        viewdilog = findViewById(R.id.logterminal);
        greenCircleImageView = createCircleImageView(R.drawable.green_ball);
        Button parlalacifotono = findViewById(R.id.startButton);
        Button ascoltacitofono = findViewById(R.id.playButton);
        Button apricancello = findViewById(R.id.azionacancello);
        Button azionacitofono = findViewById(R.id.azionacitofono);
        EditText ip_field = findViewById(R.id.ip_citofono);
        TextView portelog = findViewById(R.id.portelog);
        visualizzatore = findViewById(R.id.visualizzatorestatus);
        Thread threadporta = new Thread(new Runnable() {
            @Override
            public void run() {
                portacomandiesecutore = Integer.parseInt(PortUtility.getPort());
                Thread.currentThread().interrupt();
            }
        });
        threadporta.start();
        while (portacomandiesecutore == 0) {

        }
        mantieniconnesione();
        azionaCitofono();
        aggiornacam();

       /** parlalacifotono.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                    //Log.d("citofono", "Sto tentando di azionare il citofono --> inside");
                    //Toast.makeText(MainActivity.this, "Sto tentando di azionare il microfono", Toast.LENGTH_SHORT).show();
                    if (motionEvent.getAction() == MotionEvent.ACTION_DOWN && !is_alredy_recording_pressed) {
                        //view.setBackgroundColor(Color.parseColor("#00FF00"));
                        Log.d("citofono", "Il microfono del citofono è stato azionato");
                        is_alredy_recording_pressed = true;
                        view.setBackground(getDrawable(R.drawable.button_dark_1_state_1));
                        controlloidlecitofono(); //questo serve semplicemente perché talvolta non riesco a rilevare l'action up
                        startAudioStreaming(false);
                    } else if (motionEvent.getAction() == MotionEvent.ACTION_UP && is_alredy_recording_pressed) {
                        Log.d("citofono", "Il microfono del citofono è stato disattivato");
                        is_alredy_recording_pressed = false;
                        view.setBackground(getDrawable(R.drawable.button_dark_1_state_0));
                        startAudioStreaming(true);
                    }

                } else {
                    requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 1);
                }
                return true;
            }
        }); **/
         parlalacifotono.setOnClickListener(new View.OnClickListener() {
        @Override public void onClick(View view) {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
        if (!is_alredy_recording_pressed) {
        view.setBackgroundColor(Color.parseColor("#00FF00"));
        is_alredy_recording_pressed = true;
            startAudioStreaming(true);
        } else {
        view.setBackgroundColor(Color.parseColor("#FF0000"));
        is_alredy_recording_pressed = false;
            startAudioStreaming(false);
        }

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
                        inviaComando("apricancello", portacomandiesecutore);
                    }
                });
                thread.start();
            }
        });
        azionacitofono.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                azionaCitofono();

            }
        });
        azionacitofono.setVisibility(View.INVISIBLE);
        azionacitofono.setVisibility(View.GONE);
        ascoltacitofono.setVisibility(View.INVISIBLE);
        ascoltacitofono.setVisibility(View.GONE);

        ascoltacitofono.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                startAudioPlaying(true);
            }
        });
        // ora inizializzo la connessione con il server
        citofono_ip = ip_field.getText().toString();
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    String status = inviaComando("status", portacomandiesecutore);
                    if (status.equals("green")) {
                        //se il citofono è disponibile procedo con l'attuazione delle operazioni di connessione richiedendo le porte di comunicazione al server
                        String portemicrofoni = inviaComando("richiediportamicrofoni", portacomandiesecutore);
                        String portealtoparlanti = inviaComando("richiediportaaltoparlante", portacomandiesecutore);
                        String[] portealtoparlanti_split = portealtoparlanti.split("#");
                        porta_altoparlante = Integer.parseInt(portealtoparlanti_split[0]);
                        porta_altoparlante_controllo = Integer.parseInt(portealtoparlanti_split[1]);
                        String[] portemicrofoni_split = portemicrofoni.split("#");
                        porta_microfono = Integer.parseInt(portemicrofoni_split[0]);
                        porta_microfono_controllo = Integer.parseInt(portemicrofoni_split[1]);
                        portelog.setText("PM: " + portemicrofoni + " PA:" + portealtoparlanti);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                updateStatus(true);
                            }
                        });
                        break;

                    } else {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                updateStatus(false);
                                portelog.setText("Il citofono non è disponibile.");
                            }
                        });
                    }


                }
                Thread.currentThread().interrupt();
            }
        });
        thread.start();

    }


    private ImageView createCircleImageView(int drawableResourceId) {
        ImageView imageView = new ImageView(this);
        imageView.setImageResource(drawableResourceId);
        return imageView;
    }

    public boolean isReady = false;

    public void aggiornacam() {
        Toast.makeText(this, "Aggiornamento cam.", Toast.LENGTH_SHORT).show();
        Thread t0 = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    ImageView camera = findViewById(R.id.telecamera);
                }
            }
        });
        t0.start();
    }

    private void updateStatus(boolean isGreen) {
        if (isGreen) {
            //removeViewById(56);
            findViewById(R.id.visualizzatorestatus).setBackground(getDrawable(R.drawable.green_ball));
            isReady = true;

        } else {
            //removeViewById(57);
            findViewById(R.id.visualizzatorestatus).setBackground(getDrawable(R.drawable.red_ball));
            isReady = false;

        }
    }

    private int contatoreidle = 0;
    private boolean refresher = true;

    private void controlloidlecitofono() {
        if (refresher) {
            refresher = false;
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (true) {
                        try {
                            if (contatoreidle == 3) {
                                refresher = true;
                                TextView viewdilog = findViewById(R.id.logterminal);
                                viewdilog.setText("Idle azionato a: " + String.valueOf(contatoreidle));
                                inviaComando("disattivacitofono", portacomandiesecutore);
                                startAudioStreaming(true);
                                Thread.currentThread().interrupt();

                            }
                            Thread.sleep(1000);
                            contatoreidle++;
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
            t.start();
        } else {
            contatoreidle = 0;
        }
    }

    private void azionaCitofono() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!isReady) {
                }
                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        inviaComando("azionacitofono", portacomandiesecutore);
                        Thread.currentThread().interrupt();
                    }
                });
                thread.start();
                startAudioPlaying(false); //questo è il metodo che fa partire l'audio all'avvio del programma. Commentando
                //questa riga si può fare in modo che l'audio parta solo quando si preme il tasto play
                //attualmente il tasto play viene nascosto programmaticamente.
            }
        });
        thread.start();
    }

    public String citofono_ip = "";

    private void removeViewById(int viewId) {
        View viewToRemove = findViewById(viewId);
        if (viewToRemove != null) {
            ViewGroup parentView = (ViewGroup) viewToRemove.getParent();
            if (parentView != null) {
                parentView.removeView(viewToRemove);
            }
        }
    }

    public String inviaComando(String comando, int porta) {
        Socket socket = null;
        String risposta = null;

        try {
            // Sostituisci con l'indirizzo IP e la porta del tuo server
            String serverAddress = citofono_ip;
            // int serverPort = 12343;
            int serverPort = porta;

            // Crea una connessione socket
            socket = new Socket(serverAddress, serverPort);

            // Ottieni lo stream di output
            OutputStream out = socket.getOutputStream();
            // Invia il comando al server
            out.write(comando.getBytes());

            // Ottieni lo stream di input
            InputStream in = socket.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));

            // Leggi la risposta dal server
            risposta = reader.readLine();

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                // Chiudi la connessione solo se è stata aperta
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return risposta;
    }

    private void startAudioStreaming(boolean mutestatuscontroller) {
        if (isReady) {
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
            setMute(mutestatuscontroller);
        } else {
            Log.d("citofono", "il citofono non è pronto");
            Toast.makeText(this, "Il citofono non è pronto", Toast.LENGTH_SHORT).show();
            return;
        }


    }

    public boolean mute = true; //verificare la gestione del mute 18 12 2023
    public boolean mute_microfono_speaker = false;
    public boolean old_mute = false;

    public void setMute(boolean controller) {
        mute = controller;
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                if (mute == true && mute != old_mute) {
                    //mute = false;
                    old_mute = mute;
                    inviaComando("muteoff", porta_altoparlante_controllo);
                    setSpeaker();
                } else if (mute == false && mute != old_mute) {
                    // = true;
                    old_mute = mute;
                    inviaComando("muteonn", porta_altoparlante_controllo);
                    setSpeaker();

                }
                Thread.currentThread().interrupt();
            }
        });
        thread.start();

    }

    public void setSpeaker() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                if (mute_microfono_speaker) {
                    mute_microfono_speaker = false;
                    inviaComando("muteoff", porta_microfono_controllo); //54322
                } else {
                    mute_microfono_speaker = true;
                    inviaComando("muteonn", porta_microfono_controllo);

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

    private void startAudioPlaying(boolean toastcontroller) {
        if (isReady) {
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
        } else {
            Log.d("citofono", "il citofono non è pronto");
            if (toastcontroller) {
                Toast.makeText(this, "Il citofono non è pronto", Toast.LENGTH_SHORT).show();
            }
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

    private int numerodirefresh = 0;

    public void mantieniconnesione() {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    inviaComando("mantieniconnesione", portacomandiesecutore);
                    try {
                        Thread.sleep(1000);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                viewdilog.setText("Refresh inviati: " + String.valueOf(numerodirefresh));
                            }
                        });

                        numerodirefresh++;
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        t.start();
    }


    private void applyLowPassFilter(byte[] buffer, int bytesRead) {
        for (int i = 0; i < bytesRead; i++) {
            // Calcola la media mobile
            int sum = 0;
            for (int j = Math.max(0, i - WINDOW_SIZE / 2); j < Math.min(bytesRead, i + WINDOW_SIZE / 2 + 1); j++) {
                sum += buffer[j];
            }
            buffer[i] = (byte) (sum / WINDOW_SIZE);
        }
    }

    private void receiveAudio() {
        while (true) {
            try {
                Socket socket = new Socket(citofono_ip, porta_microfono); //porta_microfono
                DataInputStream inputStream = new DataInputStream(socket.getInputStream());

                byte[] buffer = new byte[BUFFER_SIZE];

                while (isPlaying) {
                    int bytesRead = inputStream.read(buffer, 0, BUFFER_SIZE);
                    //Log.d("dimensione buffer: ", String.valueOf(BUFFER_SIZE));
                    //if (bytesRead == -1) {
                    //  break;  // Fine del flusso, esci dal ciclo
                    //applyLowPassFilter(buffer, bytesRead);
                    AudioProcessor.processAudioBuffer(buffer, bytesRead);

                    //} //questa parte fa chiudere il programma se il citofono chiude la connessione, non so se è il caso di lasciarla o meno, per ora la commento
                    audioTrack.write(buffer, 0, bytesRead);

                }

                socket.close();
            } catch (IOException e) {

                e.printStackTrace();
            }
        }
    }

    private class SocketTask implements Runnable {

        @Override
        public void run() {
            try {
                Socket socket = new Socket(citofono_ip, porta_altoparlante);
                DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());

                byte[] buffer = new byte[BUFFER_SIZE];
                //Log.d("dimensione buffer registratore: ", String.valueOf(BUFFER_SIZE));
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

