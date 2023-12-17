import subprocess
import signal
import sys

# Impostazioni dell'audio
CHANNELS = 1  # Numero di canali audio (1 per mono, 2 per stereo)
RATE = 44100  # Frequenza di campionamento in Hz

# Impostazioni FFmpeg
ffmpeg_command = [
    'ffmpeg',
    '-f', 'wav',
    '-ac', str(CHANNELS),
    '-ar', str(RATE),
    '-i', '-',
    '-acodec', 'pcm_s16le',
    '-f', 'mpegts',
    'udp://127.0.0.1:12345'  # Cambia l'indirizzo e la porta secondo le tue esigenze
]

# Avvio del processo FFmpeg
ffmpeg_process = subprocess.Popen(ffmpeg_command, stdin=subprocess.PIPE)

# Gestione dell'interruzione da tastiera
def signal_handler(sig, frame):
    print("\nTerminazione dello script.")
    ffmpeg_process.terminate()
    sys.exit(0)

# Collegamento del gestore del segnale all'interruzione da tastiera
signal.signal(signal.SIGINT, signal_handler)

# Cattura e trasmissione dell'audio
try:
    with subprocess.Popen(['arecord', '-f', 'S16_LE', '--rate', str(RATE), '-c', str(CHANNELS), '-'], stdout=subprocess.PIPE) as arecord_process:
        try:
            for chunk in iter(lambda: arecord_process.stdout.read(1024), b''):
                ffmpeg_process.stdin.write(chunk)
        finally:
            # Attendi che il processo 'arecord' termini
            arecord_process.wait()
    
            # Chiusura del processo FFmpeg
            ffmpeg_process.stdin.close()

except Exception as e:
    print(f"Errore durante l'esecuzione del processo di registrazione: {e}")
finally:
    # Chiusura del processo FFmpeg
    ffmpeg_process.stdin.close()
    ffmpeg_process.wait()
