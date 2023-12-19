import socketserver
import socket
import pyaudio
import threading
import netifaces as ni
import sounddevice
import sys


#questo trasmette l'audio al cellulare
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

def send_microphone_audio(client_socket, chunk_size=1024):
    while True:
        if not get_muted():
            p = pyaudio.PyAudio()
            format = pyaudio.paInt16
            channels = 1
            rate = 44100

            stream = p.open(format=format,
                            channels=channels,
                            rate=rate,
                            input=True,
                            frames_per_buffer=chunk_size)

            try:
                print(f"Sending audio to the client...")
                while True:
                    if not get_muted():
                        data = stream.read(chunk_size)
                        client_socket.sendall(data)
                    else:
                        print(f"canale mutato")
                        stream.stop_stream()
                        stream.close()
                        p.terminate()
                        break

            except Exception as e:
                print(f"Error during audio transmission: {e}")

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

def start_server(port, command_port):
    local_ip = get_local_ip()

    if local_ip is None:
        print("Impossibile ottenere l'indirizzo IP. Uscita.")
        return

    server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server_socket.bind((local_ip, port))
    server_socket.listen(1)
    command_server = socketserver.TCPServer((local_ip, command_port), CommandHandler)
    
    print(f"Listening on {local_ip}:{port}")

    try:
        client_socket, client_address = server_socket.accept()
        print(f"Accepted connection from {client_address}")
        # Avvia il thread per la gestione dei comandi
        command_thread = threading.Thread(target=command_server.serve_forever)
        command_thread.start()

        send_microphone_audio(client_socket)

    except KeyboardInterrupt:
        print("Server manually interrupted.")
    finally:
        server_socket.close()
        command_server.server_close()

if __name__ == "__main__":
    if len(sys.argv) != 3:
        print("Usage: python script_name.py <port> <command_port>")
        sys.exit(1)

    port = int(sys.argv[1])
    command_port = int(sys.argv[2])

    start_server(port, command_port)

