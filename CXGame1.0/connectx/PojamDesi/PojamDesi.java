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
  private float MAX_EVAL, MIN_EVAL;
  // ultima profondità raggiunta con iterative deepening
  private int lastDepth;

  // DEBUG: monitora le prestazioni euristicamente
  private int[] nodesEvaluated = new int[30];

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
    this.lastDepth = 0;
  }

  // TODO: se sono il primo a giocare alcune mosse possono indirizzare la partita -> guardare su internet
  @Override
  public int selectColumn(CXBoard B) {
    // salvo l'inizio del mio countdown
    start = System.currentTimeMillis();
    // selected column
    int sc = 0;
    // svuota l'array dalle mosse valutate precedentemente
    A.clear();

    // DEBUG:
    System.err.println("VIA");
    for (int i = 0; i < nodesEvaluated.length; i++) {
      nodesEvaluated[i] = 0;
    }

    // variabili che cambiano ogni turno
    // massima profondità dell'albero
    this.MAX_DEPTH = B.numOfFreeCells();
    // massimo valore assegnabile da evaluate()
    this.MAX_EVAL = MAX_DEPTH;
    // minimo valore assegnabile da evaluate()
    this.MIN_EVAL = -MAX_DEPTH;

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

    } catch (TimeoutException e) {
      // ritorno una mossa a caso perchè non ho trovato nulla
      System.err.println("TIMEOUT PORCODIO");
      return A.get(A.size() / 2).getColumn();
    }

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
  
  private int iterativeDeepening(CXBoard B) {
    int sc = A.get(A.size() / 2).getColumn(), tmp = sc;
    float tmpEval, eval;

    // ordino le mosse per promettenza decrescente
    A.sort(null);
    
    // DEBUG:
    float scEval = 0.0f;

    try {
      // cerco di ri-valutare il minor numero di nodi possibili
      for (int d = Integer.max(lastDepth - 2, 3); d < MAX_DEPTH; d++) {
        eval = MIN_EVAL;
        // minore del valore minimo così la prima mossa legale la metto da parte intanto e non rischio di perdere a tavolino per mossa illegale
        tmpEval = MIN_EVAL - 1;
        for (Move m : A) {
          B.markColumn(m.getColumn());
          eval = Float.max(eval, alphabeta(B, false, MIN_EVAL, MAX_EVAL, 1, d));
          B.unmarkColumn();
          // in base alla promettenza delle mosse tengo quella con il valore più alto
          if (eval > tmpEval) {
            tmp = m.getColumn();
            tmpEval = eval;
          }
        }
        // salvo la miglior mossa trovata
        sc = tmp;

        //DEBUG:
        scEval = tmpEval;

        // salvo la profondità raggiunta
        lastDepth = d;
      }
    } catch (TimeoutException e) {
      System.err.println("TIMEOUT ITERATIVE");
    }

    // DEBUG:
    System.err.println("Eval mossa: " + scEval);
    System.err.println("Tempo usato: " + (System.currentTimeMillis() - start) + "ms");
    for (int i = 0; i < nodesEvaluated.length; i++) {
      if (nodesEvaluated[i] > 0) System.err.println("Profondità " + i + ", nodi valutati = " + nodesEvaluated[i]);
    }

    return sc;
  }
  
  // TODO: trovare un modo per riconoscere le configurazioni già valutate. (es: usare una tabella hash per tenere tracciate le configurazioni di gioco (chiave) con associato il valore calcolato da evaluate (valore). Se una configurazione di gioco è nella tabella hash vuol dire che ho valutato già tutti i suoi figli quindi posso passare direttamente alla profondità successiva ???)
  // maxDepth = 0 e depth = 1 se non si vuole usare il limite di profondità
  private float alphabeta(CXBoard B, boolean myTurn, float alpha, float beta, int depth, int maxDepth) throws TimeoutException {
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
        alpha = Float.max(eval, alpha);
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
        beta = Float.min(eval, beta);
        if (beta <= alpha) break; // alpha cutoff
      }
    }
    return eval;
  }

  // controlla se è uno stato di fine partita
  private boolean isLeaf(CXGameState s) {
    return s == win || s == lose || s == CXGameState.DRAW;
  }

  // assegna un valore alla configurazione di gioco
  private float evaluate(CXBoard B, int depth) {

    // DEBUG:
    nodesEvaluated[depth]++;
    
    if (B.gameState() == win) {
      return 1 + (MAX_DEPTH - depth);
    } else if (B.gameState() == lose) {
      return -1 - (MAX_DEPTH - depth);
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
