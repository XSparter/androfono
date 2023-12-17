import socket
import RPi.GPIO as GPIO
import time

# Configura i pin GPIO
PIN_APRICANCELLO = 27 
PIN_AZIONA_CITOFONO = 18 

# Inizializza il modulo GPIO
GPIO.setmode(GPIO.BCM)
GPIO.setup(PIN_APRICANCELLO, GPIO.OUT)
GPIO.setup(PIN_AZIONA_CITOFONO, GPIO.OUT)

# Crea un socket
server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
server_socket.bind(('0.0.0.0', 12343))
server_socket.listen(1)

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
       
    else:
        print("Comando sconosciuto.")

    # Chiudi la connessione
    client_socket.close()

# Pulizia GPIO alla chiusura
GPIO.cleanup()
