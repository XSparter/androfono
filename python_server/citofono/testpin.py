import RPi.GPIO as GPIO
import time

# Imposta la modalità BCM per riferirsi ai pin con il loro numero di GPIO
GPIO.setmode(GPIO.BCM)

# Imposta i pin 22 e 23 come output
GPIO.setup(22, GPIO.OUT)
GPIO.setup(23, GPIO.OUT)

try:
    while True:
        # Accendi il pin 22 e spegni il pin 23
        GPIO.output(22, GPIO.HIGH)
        GPIO.output(23, GPIO.LOW)
        time.sleep(0.1)  # Attendi un secondo

        # Spegni il pin 22 e accendi il pin 23
        GPIO.output(22, GPIO.LOW)
        GPIO.output(23, GPIO.HIGH)
        time.sleep(0.1)  # Attendi un secondo

except KeyboardInterrupt:
    # Gestisci l'interruzione da tastiera (Ctrl+C)
    pass

finally:
    # Pulisci la configurazione GPIO prima di uscire
    GPIO.cleanup()
