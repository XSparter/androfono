#!/bin/bash

# Funzione per gestire il segnale di terminazione
cleanup() {
    # Invia il segnale di terminazione ai processi Python
    pkill -TERM -f "python citofono_trasmissione.py"
    pkill -TERM -f "python citofono_ricezione.py"
    pkill -TERM -f "python esecutore.py"
}

# Registra la funzione di pulizia per il segnale di terminazione
trap cleanup EXIT

# Avvia citofono_trasmissione.py in background senza output
nohup python citofono_trasmissione.py > /dev/null 2>&1 &

# Avvia citofono_ricezione.py in background senza output
nohup python citofono_ricezione.py > /dev/null 2>&1 &

# Attendi un breve periodo di tempo per assicurarti che i processi siano avviati
sleep 2

# Esegui esecutore.py mostrando l'output a video
python esecutore.py
