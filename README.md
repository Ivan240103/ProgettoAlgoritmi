# ProgettoAlgoritmi
Progetto di Algoritmi e Strutture Dati 2023-24

## DEBUG
Comando per avviare il gioco con grafica da portatile Ivan:
```bash
/usr/bin/env /usr/lib/jvm/java-17-openjdk-amd64/bin/java -XX:+ShowCodeDetailsInExceptionMessages -cp /home/ivan/.config/Code/User/workspaceStorage/8e146f0cc4710eb650eb9badd0eb4402/redhat.java/jdt_ws/Unibo_c8ba3648/bin connectx.CXGame 6 7 4 connectx.PojamDesi.PojamDesi connectx.L1.L1
```

## FONTI PER RELAZIONE
Algoritmo minimax e sue ottimizzazioni:  
`https://www.scirp.org/journal/paperinformation?paperid=125554`
Euristiche per i valori di indecisione:  
`https://www.scirp.org/journal/paperinformation?paperid=90972`

## APPUNTI
Intuizione: se alla sua prossima mossa l'avversario ha una mossa vincente, rimuovendo tutte le mosse che mi portano a perdere mi rimane come giocabile la mossa che blocca la sua vittoria.

Se sono il primo giocatore a giocare inserisco come prima mossa sempre la colonna centrale in quanto più vantaggiosa rispetto alle altre in mancanza di ulteriori informazioni. Se il numero di colonne è pari sceglie quella centro-sx dato che la valutazione delle mosse tende, a parità di valutazione, a mantenere come migliori le mosse valutate prima (da sinistra a destra).

Uso un ArrayList per memorizzare le possibili mosse da giocare anzichè una lista concatenata. Alle mosse si accede sempre sequenzialmente perchè le valuto una dopo l'altra (tempo O(n) per entrambe). L'ArrayList è meno efficiente in fase di rimozione delle LinkedList, ma poichè aggiungo e rimuovo sempre e solo in coda entrambi lavorano in O(1). CONTROLLARE USO DELLA MEMORIA per decidere tra i due.

evaluate() restituisce stato finale (-1, 0, 1) moltiplicato per un calcolo sulla profondità della configurazione in modo che la vittoria più in alto abbia il valore maggiore e la sconfitta più in basso il valore minore

Non c'è il problema di scegliere tra una mossa che porta al pareggio (eval = 0) e una con totale indecisione (nessuna pedina mia adiacente, eval = 0) perchè il pareggio si scopre solo arrivando in fondo all'albero di gioco, quindi ho abbastanza informazioni per non avere una mossa con totale indecisione.
Il valore di promettenza è sempre compreso tra 0 e 1 poichè 8 pedine adiacenti non si possono avere (la cella sopra è vuota o fuori dalla scacchiera)

In iterativeDeepening() al primo ciclo uso come profondità massima da raggiungere il max fra 3 (poichè le mosse a profondità 1 e 2 vengono valutate da immediateMove()) e la profondità massima valutata completamente al turno precedente meno 2 (che sono i livelli scesi nell'albero con le mosse mia e dell'avversario). Intuizione alla base: se ho valutato il livello d completamente, al prossimo turno (con due livelli in meno da valutare) sicuramente riesco ad arrivare fino a d completo, quindi risparmio il tempo di ri-valutazione di tutti i nodi sopra.

Si è provato ad implementare con le tabelle hash una sorta di controllo per verificare se una configurazione fosse già stata valutata, e nel caso recuperare il valore assegnato calcolato in precedenza. A seguito di analisi sperimentale questo metodo si è rivelato inefficiente in quanto i calcoli per mantenere l'HashMap rallentavano il giocatore, portandolo ad esplorare una profondità minore dell'implentazione senza HashMap.