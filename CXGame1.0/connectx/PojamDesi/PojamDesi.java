package connectx.PojamDesi;

import java.util.ArrayList;
import java.util.concurrent.TimeoutException;

import connectx.CXPlayer;
import connectx.CXBoard;
import connectx.CXGameState;

/**
 * Implementation of a connectx player
 * 
 * @author Ivan De Simone - ivan.desimone@studio.unibo.it
 * @author Payam Salarieh - payam.salarieh@studio.unibo.it
 */
public class PojamDesi implements CXPlayer {

  private String name = "PojamDesi";
  private int MAX_DEPTH, TIMEOUT;
  private long start;

  // stati di vittoria e sconfitta riguardo il mio giocatore
  private CXGameState win, lose;
  // mosse fattibili dopo il controllo delle mosse immediate
  private ArrayList<Integer> A;
  // massimo e minimo valore che può essere associato ad una mossa
  private int MAX_EVAL, MIN_EVAL;
  // ultima profondità raggiunta con iterative deepening
  /* private int lastDepth; */

  /**
   * Costruttore vuoto default
   */
  public PojamDesi() {}
  
  @Override
  public void initPlayer(int M, int N, int X, boolean first, int timeout_in_secs) {
    this.MAX_DEPTH = M*N;
    this.TIMEOUT = timeout_in_secs;
    // imposta stati di vittoria e sconfitta in base a chi gioca per primo
    this.win = first ? CXGameState.WINP1 : CXGameState.WINP2;
    this.lose = first ? CXGameState.WINP2 : CXGameState.WINP1;
    A = new ArrayList<Integer>();
    this.MAX_EVAL = 1;
    this.MIN_EVAL = -1;
    /* this.lastDepth = 0; */
  }

  // TODO: se il numero di mosse giocate è superiore ad una soglia tarata in base a MAX_DEPTH usare visita in profondità (c'è una funzione in CXBoard che restituisce il numero di celle già occupate o libere)
  // TODO: se sono il primo a giocare alcune mosse possono indirizzare la partita -> guardare su internet
  @Override
  public int selectColumn(CXBoard B) {
    // salvo l'inizio del mio countdown
    start = System.currentTimeMillis();
    System.err.println("a");
    // selected column
    int sc = 0;
    // svuota l'array dalle mosse valutate precedentemente
    A.clear();
    try {
      sc = immediateMove(B);
      System.err.println("b");

      // se ho una mossa vincente la gioco
      if (sc != -1) return sc;
      System.err.println("c");

      if (A.isEmpty()) {
        // nessuna mossa giocabile / vittoria avversario
        System.err.println("Sconfitta assicurata");
        // gioco la prima che mi capita tanto perdo ugualmente
        return B.getAvailableColumns()[0];
      }
      System.err.println("d");

      // TODO: riordinare le mosse di A in base a promettenza decrescente

      /* // ALPHABETA INIZIALE CON LIMITE EURISTICO
      // se ho delle mosse giocabili scelgo quella che mi può portare
      // eventualmente al risultato migliore
      int eval = MIN_EVAL;
      for (int a : A) {
        B.markColumn(a);
        System.err.println("e");
        // calcolo euristico per la massima profondità
        eval = Integer.max(eval, alphabeta(B, false, MIN_EVAL, MAX_EVAL, 1, 17 - ((int) Math.pow(1.045, MAX_DEPTH))));
        System.err.println("f");
        // se una mossa mi può portare alla vittoria la gioco subito
        if (eval == MAX_EVAL) return a;
        B.unmarkColumn();
        System.err.println("g");
        // altrimenti scelgo una mossa di indecisione/patta
        // TODO: in base alla promettenza delle mosse tengo quella con il valore più alto
        if (eval == 0) sc = a;
        System.err.println("h");
      }

      if (sc != -1) {
        System.err.println("i");
        // mossa che potrebbe non farmi perdere
        return sc;
      } else {
        System.err.println("j");
        // indifferente perchè ogni mossa porta alla sconfitta (non immediata)
        return A.get(A.size() / 2);
      } */
    } catch (TimeoutException e) {
      // ritorno una mossa a caso perchè non ho trovato nulla
      System.err.println("TIMEOUT PORCODIO");
      return A.get(A.size() / 2);
    }

    return iterativeDeepening(B);
  }
  
  // TODO: tenere salvata in una variabile la profondità raggiunta prima che scadesse il tempo, quando è nuovamente il proprio turno decrementarla di 1/2 in qunato la cima dell'albero viene "tagliata", e ricominciare dalla profondità inesplorata anzichè ripartire da zero
  // TODO: controllare a che profondità arrivare al primo ciclo
  // TODO: la profondità massima è MAX_DEPTH - il numero di pedine già giocate (ovvero il numero di caselle libere)
  private int iterativeDeepening(CXBoard B) {
    int sc = A.get(A.size() / 2);
    int tmp = sc, tmpEval, eval;
    try {
      // parte da 3 perchè le mosse a depth 1 e 2 le ho già valutate in immediateMove()
      for (int d = 3; d < MAX_DEPTH; d++) {
        eval = MIN_EVAL;
        // minore del valore minimo così la prima mossa legale la metto da parte intanto e non rischio di perdere a tavolino per mossa illegale
        tmpEval = MIN_EVAL - 1;
        for (int a : A) {
          B.markColumn(a);
          System.err.println("e");
          eval = Integer.max(eval, alphabeta(B, false, MIN_EVAL, MAX_EVAL, 1, d));
          System.err.println("f");
          // se la mossa è la migliore in assoluto che possa trovare la ritorno senza valutare le altre
          if (eval == MAX_EVAL) return a;
          B.unmarkColumn();
          System.err.println("g");
          // TODO: in base alla promettenza delle mosse tengo quella con il valore più alto
          // altrimenti scelgo una mossa di indecisione/patta
          if (eval > tmpEval) {
            tmp = a;
            tmpEval = eval;
          }
          System.err.println("h");
        }
        // salvo la miglior mossa trovata
        sc = tmp;
        System.err.println("i");
      }
    } catch (TimeoutException e) {
      System.err.println("TIMEOUT ITERATIVE");
    }
    System.err.println("j");
    return sc;
  }

  @Override
  public String playerName() {
    return name;
  }

  // controlla se il tempo sta per scadere
  private void checkTime() throws TimeoutException {
		if ((System.currentTimeMillis() - start) / 1000.0 >= TIMEOUT * (99.0 / 100.0))
			throw new TimeoutException();
	}

  // cerca una mossa vincente e contemporanemente analizza le
  // contromosse dell'avversario
  // ritorna la mossa da giocare, -1 se non ne trova una specifica
  // lascia in A le mosse giocabili
  private int immediateMove(CXBoard B) throws TimeoutException {
    System.err.println("k");
    Integer[] ac = B.getAvailableColumns();
    for (Integer i : ac) {
      checkTime();
      A.add(i);
      System.err.println("l");
      if (B.markColumn(i) == win) {
        System.err.println("m");
        // se vinco subito ritorno la mossa
        return i;
      } else {
        System.err.println("n");
        // se non vinco controllo di non perdere alla prossima (blocco)
        Integer[] acNext = B.getAvailableColumns();
        for (int j : acNext) {
          checkTime();
          if (B.markColumn(j) == lose) {
            System.err.println("o");
            // se la mossa i mi porta a perdere meglio non giocarla
            A.remove(i);
            B.unmarkColumn();
            break;
          } else {
            System.err.println("p");
            // TODO: calcolare il valore di promettenza della mossa come somma delle pedine mie ad essa adiacenti
            B.unmarkColumn();
          }
        }
      }
      System.err.println("q");
      // undo mossa i
      B.unmarkColumn();
    }
    System.err.println("r");
    return -1;
  }

  // TODO: trovare un modo per riconoscere le configurazioni già valutate. (es: usare una tabella hash per tenere tracciate le configurazioni di gioco (chiave) con associato il valore calcolato da evaluate (valore). Se una configurazione di gioco è nella tabella hash vuol dire che ho valutato già tutti i suoi figli quindi posso passare direttamente alla profondità successiva ???)
  // maxDepth = 0 e depth = 1 se non si vuole usare il limite di profondità
  private int alphabeta(CXBoard B, boolean myTurn, int alpha, int beta, int depth, int maxDepth) throws TimeoutException {
    checkTime();
    int eval;
    if (depth == maxDepth || isLeaf(B.gameState())) {
      System.err.println("s");
      eval = evaluate(B.gameState(), depth);
    } else if (myTurn) {
      System.err.println("t");
      // giocatore che massimizza: io (vittoria = 1)
      eval = MIN_EVAL;
      Integer[] ac = B.getAvailableColumns();
      for (int c : ac) {
        B.markColumn(c);
        eval = Integer.max(eval, alphabeta(B, !myTurn, alpha, beta, depth + 1, maxDepth));
        B.unmarkColumn();
        alpha = Integer.max(eval, alpha);
        if (beta <= alpha) break; // beta cutoff
      }
    } else {
      System.err.println("u");
      // giocatore che minimizza: avversario (vittoria = -1)
      eval = MAX_EVAL;
      Integer[] ac = B.getAvailableColumns();
      for (int c : ac) {
        B.markColumn(c);
        eval = Integer.min(eval, alphabeta(B, !myTurn, alpha, beta, depth + 1, maxDepth));
        B.unmarkColumn();
        beta = Integer.min(eval, beta);
        if (beta <= alpha) break; // alpha cutoff
      }
    }
    return eval;
  }

  // controlla se è uno stato di fine partita
  private boolean isLeaf(CXGameState s) {
    System.err.println("v");
    return s == win || s == lose || s == CXGameState.DRAW;
  }

  // TODO: usare la profondità per dare un punteggio migliore ad una mossa che velocizza la vittoria o rallenta la sconfitta (ad esempio moltiplicandola per il valore di eval in base a win/draw/lose?) La vittoria a profondità minore deve restituire il valore più alto, la sconfitta a profondità maggiore deve restituire il valore più basso
  // TODO: provare ad asseganre dei float anzichè int con parte decimale rappresentante la promettenza della mossa come (numero di pedine adiacenti) / 8 -> divisione reale QUANDO LA CONFIGURAZIONE È DI INDECISIONE (non patta, patta è solo 0). Giocatore avversario che minimizza deve scegliere la mossa che mi danneggerebbe maggiormente === quella con il maggior numero di mie pedine adiacenti (?).
  // assegna un valore allo stato passato
  private int evaluate(CXGameState state, int depth) {
    System.err.println("x");
    System.err.println("Profondità: " + depth);
    if (state == win) {
      return MAX_EVAL;
    } else if (state == lose) {
      return MIN_EVAL;
    } else {
      return 0;
    }
  }
  
}
