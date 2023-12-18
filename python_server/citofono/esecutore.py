import socket
import RPi.GPIO as GPIO
import time
import random
import subprocess
import os
import signal
import atexit
# Configura i pin GPIO
PIN_APRICANCELLO = 27 
PIN_AZIONA_CITOFONO = 18 
pid_ricezione = None
pid_trasmissione = None
def esegui_script(script_path, arg1, arg2):
    # Avvia lo script come processo figlio e intercetta l'output
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
    #processo_figlio.terminate()
def termina_processi():
    print("Terminazione dei processi figli...")
    try:
        # Invia un segnale di terminazione ai processi figli
        os.kill(pid_ricezione, signal.SIGTERM)
        os.kill(pid_trasmissione, signal.SIGTERM)
    except ProcessLookupError:
        # I processi potrebbero essere già terminati
        pass
        
atexit.register(termina_processi)           
# Inizializza il modulo GPIO
GPIO.setmode(GPIO.BCM)
GPIO.setup(PIN_APRICANCELLO, GPIO.OUT)
GPIO.setup(PIN_AZIONA_CITOFONO, GPIO.OUT)

# Crea un socket
server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
server_socket.bind(('0.0.0.0', 12343))
server_socket.listen(1)
print("Cartella di lavoro corrente:", os.getcwd())
print("Server in attesa di connessioni...")
stato_citofono = False
while True:
    # Accetta la connessione
    client_socket, client_address = server_socket.accept()
    print("Connessione da:", client_address)

    # Ricevi il comando
    command = client_socket.recv(1024).decode()

    # Esegui l'azione corrispondente al comando ricevuto
    if command == "apricancello":
        print("Apertura cancello.")
        GPIO.output(PIN_APRICANCELLO, GPIO.HIGH)
        time.sleep(1)  # Attendi un secondo
        GPIO.output(PIN_APRICANCELLO, GPIO.LOW)  # Disattiva il pin del cancello
       
    elif command == "azionacitofono":
        print("Aziona/Disattiva citofono.")
        stato_citofono = not stato_citofono  # Inverti lo stato del citofono
        GPIO.output(PIN_AZIONA_CITOFONO, GPIO.HIGH if stato_citofono else GPIO.LOW)
    elif command == "richiediportamicrofoni":
        # Genera due numeri casuali fra 35000 e 40000
        print(f"Richiesta configurazione trasmissione dei microfoni.")
        numero1 = random.randint(35000, 40000)
        numero2 = random.randint(35000, 40000)
        

        # Invia la risposta al client separando i numeri con "#"
        pid_ricezione = esegui_script("citofono_trasmissione.py",numero1,numero2)
        print(f"memorizzato pid di ricezione: {pid_ricezione}")
        # Invia la risposta al client separando i numeri con "#"
        risposta = f"{numero1}#{numero2}"
        print(f"Porta microfono e Porta controllo: {risposta}")
        client_socket.send(risposta.encode())

    elif command == "richiediportaaltoparlante":
        # Genera due numeri casuali fra 35000 e 40000
        numero1 = random.randint(35000, 40000)
        numero2 = random.randint(35000, 40000)
        pid_trasmissione = esegui_script("citofono_ricezione.py",numero1,numero2)
        print(f"memorizzato pid di trasmissione: {pid_trasmissione}")
        # Invia la risposta al client separando i numeri con "#"
        risposta = f"{numero1}#{numero2}"
        client_socket.send(risposta.encode())
        
    elif command == "status":
        risposta = "green"
        client_socket.send(risposta.encode())

    else:
        print("Comando sconosciuto.")

    # Chiudi la connessione
    client_socket.close()

# Pulizia GPIO alla chiusura
GPIO.cleanup()
