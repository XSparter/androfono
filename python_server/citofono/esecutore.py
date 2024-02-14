import socket
import RPi.GPIO as GPIO
import time
import random
import subprocess
import os
import signal
import atexit
from datetime import datetime, timedelta
import threading
import requests
from requests.auth import HTTPBasicAuth

# Configura i pin GPIO
PIN_APRICANCELLO = 27
PIN_AZIONA_CITOFONO = 23 #18
PIN_AZIONA_CITOFONO2 = 22 #18
#nota che per il modulo relè si azionano quando fai LOW, chissà quale minchia è il motivo.
#occhio che attualmente, dato che sto usando un modulo che supporta i LOW per azionarsi, tutti i "LOW" corrisppndono al relè azionato.
pid_ricezione = None
pid_trasmissione = None
orario_di_riferimento = None


def set_port(port):
    # Esegui una richiesta GET a getport.php con il parametro set
    response = requests.get(f'http://192.168.1.30/citofono_utility/getport.php?set={port}')
    return response.text

def get_port():
    # Esegui una richiesta GET a getport.php senza parametri
    response = requests.get('http://192.168.1.30/citofono_utility/getport.php?get')
    return response.text

# Funzione per eseguire uno script come processo figlio
def esegui_script(script_path, arg1, arg2):
    arg1 = str(arg1)
    arg2 = str(arg2)

    processo_figlio = subprocess.Popen(['python', script_path, arg1, arg2],
                                       stdout=subprocess.PIPE,
                                       stderr=subprocess.PIPE,
                                       text=True)

    # Ottieni il PID del processo figlio
    pid_figlio = processo_figlio.pid
    print(f"PID del processo figlio: {pid_figlio}")
    return pid_figlio

# Funzione per terminare i processi figli
def termina_processi():
    print("Termination of child processes...")
    try:
        os.kill(pid_ricezione, signal.SIGTERM)
        os.kill(pid_trasmissione, signal.SIGTERM)
    except ProcessLookupError:
        pass

# Funzione per controllare l'orario e terminare i processi se necessario
def controllo_orario():
    while True:
        now = datetime.now()

        if orario_di_riferimento is not None and now > orario_di_riferimento + timedelta(seconds=5):
            termina_processi()
            GPIO.output(PIN_AZIONA_CITOFONO, GPIO.HIGH)
            GPIO.output(PIN_AZIONA_CITOFONO2, GPIO.HIGH)
            print(f"I deactivated the buzzer.")
            with lock:
                variabile_status = "green"
            # Esegui una richiesta GET a http://192.168.1.148/ledoff
            requests.get('http://192.168.1.148/ledoff')
            break
        else:
            # Esegui una richiesta GET a http://192.168.1.148/ledon
            requests.get('http://192.168.1.148/ledon')

        time.sleep(1)

# Registro la funzione per la terminazione dei processi alla chiusura
atexit.register(termina_processi)

# Inizializza il modulo GPIO
GPIO.setmode(GPIO.BCM)
GPIO.setup(PIN_APRICANCELLO, GPIO.OUT)
GPIO.setup(PIN_AZIONA_CITOFONO, GPIO.OUT)
GPIO.setup(PIN_AZIONA_CITOFONO2, GPIO.OUT)

# Crea un socket
server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
porta_esecutore = random.randint(35000, 40000)
print(set_port(porta_esecutore))
server_socket.bind(('0.0.0.0', porta_esecutore))
server_socket.listen(1)
GPIO.output(PIN_AZIONA_CITOFONO, GPIO.HIGH) #questo gestisce il pin di azionamento del citofono.
GPIO.output(PIN_AZIONA_CITOFONO2, GPIO.HIGH) #questo gestisce il pin di azionamento del citofono.
print("Current working directory:", os.getcwd())
print("Please check that the audio configuration in raspi-config is set to USB if you hear noise.")
print(f"Server waiting for connections... on port {porta_esecutore}")

variabile_status = "Green"  # Variabile di controllo per la connessione al citofono (Green, Yellow, Red)
lock = threading.Lock()

while True:
    # Accetta la connessione
    client_socket, client_address = server_socket.accept()
    #print("Connection from:", client_address)

    # Ricevi il comando
    command = client_socket.recv(1024).decode()

    # Esegui l'azione corrispondente al comando ricevuto
    if command == "apricancello":
        print("Opening gate.")
        GPIO.output(PIN_APRICANCELLO, GPIO.HIGH)
        time.sleep(0.5)
        GPIO.output(PIN_APRICANCELLO, GPIO.LOW)

    elif command == "azionacitofono":
        print("Attivo l'arrivo di audio dal cellulare")
        GPIO.output(PIN_AZIONA_CITOFONO2, GPIO.LOW)
        GPIO.output(PIN_AZIONA_CITOFONO, GPIO.LOW) #controlla questo pin in base al tipo  di modulo relè con cui stai lavorando.
    elif command == "disattivacitofono":
        print("Disattivo l'arrivo di audio dal cellulare.")
        GPIO.output(PIN_AZIONA_CITOFONO2, GPIO.LOW)
        GPIO.output(PIN_AZIONA_CITOFONO, GPIO.LOW) #controlla questo pin in base al tipo  di modulo relè con cui stai lavorando.
    elif command == "richiediportamicrofoni":
        print("Requesting microphone transmission configuration.")
        with lock:
            orario_di_riferimento = datetime.now()
        thread = threading.Thread(target=controllo_orario)
        thread.daemon = True
        thread.start()
        numero1 = random.randint(35000, 40000)
        numero2 = random.randint(35000, 40000)
        pid_ricezione = esegui_script("citofono_trasmissione.py", numero1, numero2) 
        print(f"Stored reception PID: {pid_ricezione}")
        risposta = f"{numero1}#{numero2}"
        print(f"Microphone Port and Control Port: {risposta}")
        client_socket.send(risposta.encode())

    elif command == "richiediportaaltoparlante":
        print("Requesting speaker transmission configuration.")
        numero1 = random.randint(35000, 40000)
        numero2 = random.randint(35000, 40000)
        pid_trasmissione = esegui_script("citofono_ricezione.py", numero1, numero2)
        print(f"Stored transmission PID: {pid_trasmissione}")
        risposta = f"{numero1}#{numero2}"
        client_socket.send(risposta.encode())

    elif command == "status":
        with lock:
            risposta = variabile_status
            client_socket.send(risposta.encode())

    elif command == "mantieniconnesione":
        with lock:
            orario_di_riferimento = datetime.now()
            variabile_status = "green"
        #print(f"Last communication from the buzzer: {orario_di_riferimento}")

    else:
        print("Unknown command.")

    # Chiudi la connessione
    client_socket.close()

# Pulizia GPIO alla chiusura
GPIO.cleanup()
