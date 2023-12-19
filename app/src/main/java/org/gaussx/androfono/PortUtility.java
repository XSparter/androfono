package org.gaussx.androfono;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class PortUtility {

    public static String setPort(int port) {
        try {
            URL url = new URL("http://192.168.1.30/citofono_utility/getport.php?set=" + port);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                return response.toString();
            } else {
                return "Errore nella richiesta HTTP, codice di risposta: " + responseCode;
            }
        } catch (Exception e) {
            return "Errore durante la connessione: " + e.getMessage();
        }
    }

    public static String getPort() {
        try {
            URL url = new URL("http://192.168.1.30/citofono_utility/getport.php?get=true");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                return response.toString();
            } else {
                return "Errore nella richiesta HTTP, codice di risposta: " + responseCode;
            }
        } catch (Exception e) {
            return "Errore durante la connessione: " + e.getMessage();
        }
    }

    public static void main(String[] args) {
        // Esempio di utilizzo
        System.out.println("Risultato setPort: " + setPort(8080));
        System.out.println("Risultato getPort: " + getPort());
    }
}
