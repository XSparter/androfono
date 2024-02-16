import RPi.GPIO as GPIO
import sys
import signal

def cleanup_gpio(signal, frame):
    print("\nProgramma terminato. Pulizia GPIO.")
    GPIO.cleanup()
    sys.exit(0)

# Impostazioni del segnale di interruzione (Ctrl+C)
signal.signal(signal.SIGINT, cleanup_gpio)

# Inizializzazione della libreria GPIO
GPIO.setmode(GPIO.BCM)

try:
    # Ottieni il numero del pin e lo stato da riga di comando
    pin_da_controllare = int(sys.argv[1])
    stato_desiderato = int(sys.argv[2])

    # Imposta il pin come OUTPUT
    GPIO.setup(pin_da_controllare, GPIO.OUT)

    # Imposta lo stato del pin
    if stato_desiderato == 1:
        GPIO.output(pin_da_controllare, GPIO.HIGH)
        print(f"Il pin {pin_da_controllare} è stato impostato su HIGH.")
    elif stato_desiderato == 0:
        GPIO.output(pin_da_controllare, GPIO.LOW)
        print(f"Il pin {pin_da_controllare} è stato impostato su LOW.")
    else:
        print("Il valore dello stato deve essere 0 o 1.")
        sys.exit(1)

    # Attendi l'interruzione da tastiera
    signal.pause()

except (ValueError, IndexError):
    print("Utilizzo: python script.py <numero_pin> <stato: 0 o 1>")
    sys.exit(1)
except KeyboardInterrupt:
    cleanup_gpio(None, None)
