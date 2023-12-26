package connectx.PojamDesi;

import java.util.ArrayList;
import java.util.concurrent.TimeoutException;
import connectx.CXPlayer;
import connectx.CXBoard;
import connectx.CXCellState;
import connectx.CXGameState;

/**
 * Implementazione di un giocatore per connectx
 * 
 * @author Ivan De Simone - ivan.desimone@studio.unibo.it
 * @author Payam Salarieh - payam.salarieh@studio.unibo.it
 */
public class PojamDesi implements CXPlayer {

  // massima profondità dell'albero e tempo in secondi per una mossa
  private int MAX_DEPTH, TIMEOUT;
  // tempo all'inizio del turno in millisecondi
  private long start;
  // stati di vittoria e sconfitta riguardo il mio giocatore
  private CXGameState win, lose;
  // stato della mia cella
  private CXCellState myCoin;
  // mosse fattibili dopo il controllo delle mosse immediate
  private ArrayList<Move> A;
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
    this.TIMEOUT = timeout_in_secs;
    // imposta stati di vittoria e sconfitta in base a chi gioca per primo
    this.win = first ? CXGameState.WINP1 : CXGameState.WINP2;
    this.lose = first ? CXGameState.WINP2 : CXGameState.WINP1;
    this.myCoin = first ? CXCellState.P1 : CXCellState.P2;
    A = new ArrayList<Move>();
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
    // selected column
    int sc = 0;
    // svuota l'array dalle mosse valutate precedentemente
    A.clear();

    this.MAX_DEPTH = B.numOfFreeCells();

    try {
      sc = immediateMove(B);

      // se ho una mossa vincente la gioco
      if (sc != -1) return sc;

      if (A.isEmpty()) {
        // nessuna mossa giocabile / vittoria avversario
        System.err.println("Sconfitta assicurata");
        // gioco la prima che mi capita tanto perdo ugualmente
        return B.getAvailableColumns()[0];
      }

      // se una sola mossa mi porta a non perdere immediatamente la gioco
      if (A.size() == 1) return A.get(0).getColumn();

      /* // ALPHABETA INIZIALE CON LIMITE EURISTICO
      // se ho delle mosse giocabili scelgo quella che mi può portare
      // eventualmente al risultato migliore
      int eval = MIN_EVAL;
      for (int a : A) {
        B.markColumn(a);
        // calcolo euristico per la massima profondità
        eval = Integer.max(eval, alphabeta(B, false, MIN_EVAL, MAX_EVAL, 1, 17 - ((int) Math.pow(1.045, MAX_DEPTH))));
        // se una mossa mi può portare alla vittoria la gioco subito
        if (eval == MAX_EVAL) return a;
        B.unmarkColumn();
        // altrimenti scelgo una mossa di indecisione/patta
        if (eval == 0) sc = a;
      }
      
      if (sc != -1) {
        // mossa che potrebbe non farmi perdere
        return sc;
      } else {
        // indifferente perchè ogni mossa porta alla sconfitta (non immediata)
        return A.get(A.size() / 2);
      } */
    } catch (TimeoutException e) {
      // ritorno una mossa a caso perchè non ho trovato nulla
      System.err.println("TIMEOUT PORCODIO");
      return A.get(A.size() / 2).getColumn();
    }

    // ordino le mosse per promettenza decrescente
    A.sort(null);
    
    return iterativeDeepening(B);
  }
  
  @Override
  public String playerName() {
    return "PojamDesi";
  }
  
  // controlla se il tempo sta per scadere
  private void checkTime() throws TimeoutException {
    if ((System.currentTimeMillis() - start) >= TIMEOUT * 998.0)
    throw new TimeoutException();
	}
  
  // cerca una mossa vincente e contemporanemente analizza le contromosse dell'avversario
  // ritorna la mossa da giocare, -1 se non ne trova una specifica
  // lascia in A le mosse giocabili
  private int immediateMove(CXBoard B) throws TimeoutException {
    boolean keep = true;
    Integer[] ac = B.getAvailableColumns();
    for (int i : ac) {
      checkTime();
      A.add(new Move(i, 0));
      keep = true;
      // se vinco subito ritorno la mossa
      if (B.markColumn(i) == win) return i;
      // se non vinco controllo di non perdere alla prossima (blocco)
      Integer[] acNext = B.getAvailableColumns();
      for (int j : acNext) {
        checkTime();
        if (B.markColumn(j) == lose) {
          // se la mossa i mi porta a perdere meglio non giocarla
          A.remove(A.size() - 1);
          keep = false;
          B.unmarkColumn();
          break;
        } else {
          B.unmarkColumn();
        }
      }
      if (keep) A.get(A.size() - 1).setQuality(coinsAround(B));
      // undo mossa i
      B.unmarkColumn();
    }
    return -1;
  }
  
  // TODO: tenere salvata in una variabile la profondità raggiunta prima che scadesse il tempo, quando è nuovamente il proprio turno decrementarla di 1 o 2 in quanto la cima dell'albero viene "tagliata", e ricominciare dalla profondità inesplorata anzichè ripartire da zero
  private int iterativeDeepening(CXBoard B) {
    int sc = A.get(A.size() / 2).getColumn(), tmp = sc;
    float tmpEval, eval;
    try {
      // TODO: controllare a che profondità arrivare al primo ciclo
      // parte da 3 perchè le mosse a depth 1 e 2 le ho già valutate in immediateMove()
      for (int d = 3; d < MAX_DEPTH; d++) {
        eval = MIN_EVAL;
        // minore del valore minimo così la prima mossa legale la metto da parte intanto e non rischio di perdere a tavolino per mossa illegale
        tmpEval = MIN_EVAL - 1;
        for (Move m : A) {
          B.markColumn(m.getColumn());
          eval = Float.max(eval, alphabeta(B, false, MIN_EVAL, MAX_EVAL, 1, d));
          // se la mossa è la migliore in assoluto che possa trovare la ritorno senza valutare le altre
          if (eval == MAX_EVAL) return m.getColumn();
          B.unmarkColumn();
          // in base alla promettenza delle mosse tengo quella con il valore più alto
          if (eval > tmpEval) {
            tmp = m.getColumn();
            tmpEval = eval;
          }
        }
        // salvo la miglior mossa trovata
        sc = tmp;
      }
    } catch (TimeoutException e) {
      System.err.println("TIMEOUT ITERATIVE");
    }
    System.err.println("Tempo usato: " + (System.currentTimeMillis() - start) + "ms");
    return sc;
  }
  
  // TODO: trovare un modo per riconoscere le configurazioni già valutate. (es: usare una tabella hash per tenere tracciate le configurazioni di gioco (chiave) con associato il valore calcolato da evaluate (valore). Se una configurazione di gioco è nella tabella hash vuol dire che ho valutato già tutti i suoi figli quindi posso passare direttamente alla profondità successiva ???)
  // maxDepth = 0 e depth = 1 se non si vuole usare il limite di profondità
  private float alphabeta(CXBoard B, boolean myTurn, int alpha, int beta, int depth, int maxDepth) throws TimeoutException {
    checkTime();
    float eval;
    if (depth == maxDepth || isLeaf(B.gameState())) {
      eval = evaluate(B, depth);
    } else if (myTurn) {
      // giocatore che massimizza: io (vittoria = 1)
      eval = MIN_EVAL;
      Integer[] ac = B.getAvailableColumns();
      for (int c : ac) {
        B.markColumn(c);
        eval = Float.max(eval, alphabeta(B, !myTurn, alpha, beta, depth + 1, maxDepth));
        B.unmarkColumn();
        alpha = Integer.max((int) eval, alpha);
        if (beta <= alpha) break; // beta cutoff
      }
    } else {
      // giocatore che minimizza: avversario (vittoria = -1)
      eval = MAX_EVAL;
      Integer[] ac = B.getAvailableColumns();
      for (int c : ac) {
        B.markColumn(c);
        eval = Float.min(eval, alphabeta(B, !myTurn, alpha, beta, depth + 1, maxDepth));
        B.unmarkColumn();
        beta = Integer.min((int) eval, beta);
        if (beta <= alpha) break; // alpha cutoff
      }
    }
    return eval;
  }

  // controlla se è uno stato di fine partita
  private boolean isLeaf(CXGameState s) {
    return s == win || s == lose || s == CXGameState.DRAW;
  }

  // TODO: usare la profondità per dare un punteggio migliore ad una mossa che velocizza la vittoria o rallenta la sconfitta. La vittoria a profondità minore deve restituire il valore più alto tra le vittorie, la sconfitta a profondità maggiore deve restituire il valore più alto tra le sconfitte. Questo si potrebbe fare moltiplicando 1 o -1 con MAX_DEPTH - depth
  // assegna un valore allo stato passato
  private float evaluate(CXBoard B, int depth) {
    System.err.println("Profondità: " + depth);
    if (B.gameState() == win) {
      return 1;
    } else if (B.gameState() == lose) {
      return -1;
    } else if (B.gameState() == CXGameState.DRAW) {
      return 0;
    } else {
      // in caso di indecisione assegna un punteggio di promettenza della mossa tra 0 e 1 in base alla quantità di mie coin adiacenti
      return coinsAround(B) / 8.0f;
    }
  }

  // ritorna il numero delle celle adiacenti all'ultima occupata che contengono una mia pedina
  private int coinsAround(CXBoard B) {
    int i = B.getLastMove().i, j = B.getLastMove().j, ca = 0;
    // colonna a dx
    try {
      if (B.cellState(i - 1, j + 1) == myCoin) ca++;
    } catch (IndexOutOfBoundsException e) {}
    try {
      if (B.cellState(i, j + 1) == myCoin) ca++;
    } catch (IndexOutOfBoundsException e) {}
    try {
      if (B.cellState(i + 1, j + 1) == myCoin) ca++;
    } catch (IndexOutOfBoundsException e) {}
    // colonna a sx
    try {
      if (B.cellState(i - 1, j - 1) == myCoin) ca++;
    } catch (IndexOutOfBoundsException e) {}
    try {
      if (B.cellState(i, j - 1) == myCoin) ca++;
    } catch (IndexOutOfBoundsException e) {}
    try {
      if (B.cellState(i + 1, j - 1) == myCoin) ca++;
    } catch (IndexOutOfBoundsException e) {}
    // cella sotto
    try {
      if (B.cellState(i + 1, j) == myCoin) ca++;
    } catch (IndexOutOfBoundsException e) {}
    return ca;
  }
  
}
