import socketserver
import pyaudio
import netifaces as ni
import threading
import os

muted = False
muted_lock = threading.Lock()
def get_local_ip():
    try:
        interfaces = ni.interfaces()
        for interface in interfaces:
            if interface.startswith('lo'):
                continue
            addresses = ni.ifaddresses(interface)
            if ni.AF_INET in addresses:
                return addresses[ni.AF_INET][0]['addr']
        print("Nessuna interfaccia di rete disponibile.")
        return None
    except Exception as e:
        print(f"Errore nell'ottenere l'indirizzo IP: {e}")
        return None
def set_muted(value):
    global muted
    with muted_lock:
        muted = value

def get_muted():
    with muted_lock:
        return muted
class AudioStreamHandler(socketserver.StreamRequestHandler):
    def handle(self):
        print(f"Accepted connection from {self.client_address}")
        os.environ['PYALSA_DEBUG'] = '0'
        while True:
            if not get_muted():
                p = pyaudio.PyAudio()
                stream = p.open(format=pyaudio.paInt16, channels=1, rate=44100, output=True)

                try:
                    while True:
                        data = self.rfile.read(1024)
                        if not data:
                            break
                        if get_muted():
                            stream.stop_stream()
                            stream.close()
                            p.terminate()
                            break
                        stream.write(data)
                except Exception as e:
                    print(f"Errore nella ricezione dei dati audio: {e}")
                finally:
                    stream.stop_stream()
                    stream.close()
                    p.terminate()

class CommandHandler(socketserver.BaseRequestHandler):
    def handle(self):
        try:
            command = self.request.recv(1024).decode('utf-8')
            if command:
                print(f"Received command: {command}")
                if command == "muteonn":
                    set_muted(True)
                    print(f"cambio il valore della variabile muted")
                elif command == "muteoff":
                    set_muted(False)
                # Aggiungi qui la logica per gestire il comando ricevuto
        except Exception as e:
            print(f"Error receiving commands: {e}")

def start_server(audio_port=12345, command_port=54322):
    local_ip = get_local_ip()

    if local_ip is None:
        print("Impossibile ottenere l'indirizzo IP. Uscita.")
        return

    audio_server = socketserver.TCPServer((local_ip, audio_port), AudioStreamHandler)
    command_server = socketserver.TCPServer((local_ip, command_port), CommandHandler)

    try:
        print(f"Listening on {local_ip}:{audio_port} for audio stream")
        print(f"Listening on {local_ip}:{command_port} for commands")

        # Avvia il thread per la gestione dei comandi
        command_thread = threading.Thread(target=command_server.serve_forever)
        command_thread.start()

        # Avvia il server principale per la trasmissione audio
        audio_server.serve_forever()
    except KeyboardInterrupt:
        print("Server interrotto manualmente.")
    finally:
        audio_server.server_close()
        command_server.server_close()

if __name__ == "__main__":
    start_server()
