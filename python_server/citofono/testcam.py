import cv2
import urllib.request
import numpy as np

url = "http://192.168.1.148/image"

while True:
    try:
        # Scarica l'immagine dal URL
        resp = urllib.request.urlopen(url)
        img = np.asarray(bytearray(resp.read()), dtype="uint8")
        img = cv2.imdecode(img, cv2.IMREAD_COLOR)

        # Mostra l'immagine a video
        cv2.imshow("Immagine", img)
        if cv2.waitKey(100) == ord('q'):
            break

    except Exception as e:
        print(f"Errore durante il download dell'immagine: {e}")
        break

cv2.destroyAllWindows()