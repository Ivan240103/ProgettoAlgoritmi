# ProgettoAlgoritmi
Progetto di Algoritmi e Strutture Dati 2023-24

## DEBUG
Comando per avviare il gioco con grafica:
```bash
/usr/bin/env /usr/lib/jvm/java-17-openjdk-amd64/bin/java -XX:+ShowCodeDetailsInExceptionMessages -cp /home/ivan/.config/Code/User/workspaceStorage/8e146f0cc4710eb650eb9badd0eb4402/redhat.java/jdt_ws/Unibo_c8ba3648/bin connectx.CXGame 5 6 4 connectx.PojamDesi.PojamDesi connectx.L1.L1 
```

## APPUNTI
Intuizione: se alla sua prossima mossa l'avversario ha una mossa vincente, rimuovendo tutte le mosse che mi portano a perdere mi rimane come giocabile la mossa che blocca la sua vittoria.

Gli output di letterine sono per il debug.

Uso un ArrayList per memorizzare le possibili mosse da giocare anzichè una lista concatenata. Alle mosse si accede sempre sequenzialmente perchè le valuto una dopo l'altra (tempo O(n) per entrambe). L'ArrayList è meno efficiente in fase di rimozione delle LinkedList, ma poichè aggiungo e rimuovo sempre e solo in coda entrambi lavorano in O(1). CONTROLLARE USO DELLA MEMORIA per decidere tra i due.

Tentativo di calcolare euristicamente la massima profondità di visita basato su una funzione esponenziale che rappresenta "quanto diminuire la profondità in base alla dimensione della scacchiera -> maggiore dimensione = maggior numero di mosse da valutare"

TODO: se creo un oggetto che contiene configurazione e punteggio di promettenza per le prime mosse posso riordinare le mosse in ordine di promettenza nell'array A
Riconoscere configurazioni già visitate lo faccio tramite una tabella hash

evaluate() restituisce stato finale (-1, 0, 1) moltiplicato per un calcolo sulla profondità della configurazione in modo che la vittoria più in alto abbia il valore maggiore e la sconfitta più in basso il valore minore
