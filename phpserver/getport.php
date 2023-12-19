<?php

function setPort($port) {
    // Verifica se è stato fornito un numero di porta valido
    if (is_numeric($port) && $port > 0 && $port <= 65535) {
        // Memorizza il numero di porta in un file ini
        $config = ['port' => $port];
        $iniString = "; Configurazione del numero di porta\n" . http_build_query($config, '', '=');
        file_put_contents('config.ini', $iniString);
        return "Numero di porta memorizzato con successo.";
    } else {
        return "Errore: Fornire un numero di porta valido.";
    }
}

function getPort() {
    // Leggi il numero di porta dal file ini
    $config = parse_ini_file('config.ini');
    return isset($config['port']) ? $config['port'] : "Numero di porta non impostato.";
}

// Gestisci le richieste GET
if ($_SERVER['REQUEST_METHOD'] === 'GET') {
    // Ottieni il parametro dalla query string
    $param = isset($_GET['set']) ? $_GET['set'] : '';

    // Esegui le azioni in base al parametro
    if ($_GET['set'] != "") {
        echo setPort($param);
    } else if ($_GET['get'] == "true") {
        echo getPort();
    } else {
        echo "Parametro non valido.";
    }
} else {
    echo "Metodo non supportato.";
}

?>
