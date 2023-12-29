package connectx.PojamDesi;

import java.util.ArrayList;
import java.util.concurrent.TimeoutException;
import connectx.CXPlayer;
import connectx.CXBoard;
import connectx.CXCellState;
import connectx.CXGameState;

/**
 * Progetto di Algoritmi e Strutture Dati 2023-24.
 * <p>Implementazione di un giocatore per connectx
 * 
 * @author Ivan De Simone - ivan.desimone@studio.unibo.it
 * @author Payam Salarieh - payam.salarieh@studio.unibo.it
 */
public class PojamDesi implements CXPlayer {

  // tempo in secondi per scegliere la mossa
  private int TIMEOUT;
  // stati di vittoria e sconfitta visti dal giocatore
  private CXGameState WIN, LOSE;
  // stato della cella occupata dal giocatore
  private CXCellState MY_COIN;
  // true se gioca la prima mossa della partita, false altrimenti
  private boolean firstMove;
  // tempo all'inizio del turno in millisecondi
  private long start;
  // massima profondità dell'albero
  private int maxDepth;
  // massimo e minimo valore che può essere associato ad una mossa
  private float maxEval, minEval;
  // massima profondità esplorata completamente al turno precedente
  private int lastDepth;
  // mosse giocabili
  private ArrayList<Move> A;

  /**
   * Costruttore vuoto default
   */
  public PojamDesi() {}
  
  @Override
  public void initPlayer(int M, int N, int X, boolean first, int timeout_in_secs) {
    this.TIMEOUT = timeout_in_secs;
    // stati di gioco in base a chi è il primo a giocare
    this.WIN = first ? CXGameState.WINP1 : CXGameState.WINP2;
    this.LOSE = first ? CXGameState.WINP2 : CXGameState.WINP1;
    this.MY_COIN = first ? CXCellState.P1 : CXCellState.P2;
    this.firstMove = true;
    this.lastDepth = 0;
    this.A = new ArrayList<Move>();
  }

  @Override
  public int selectColumn(CXBoard B) {
    this.start = System.currentTimeMillis();
    this.maxDepth = B.numOfFreeCells();
    this.maxEval = maxDepth;
    this.minEval = -maxDepth;
    
    // se è la prima mossa gioca al centro (o centro-sx)
    if (firstMove) {
      firstMove = false;
      return B.N / 2;
    }
    
    try {
      // selected column
      int sc = immediateMove(B, A);
      // se ha una mossa vincente la gioca
      if (sc != -1) return sc;
      // se non ci sono mosse giocabili sconfitta inevitabile
      if (A.isEmpty()) return B.getAvailableColumns()[0];
      // se ha una sola mossa disponibile gioca quella
      if (A.size() == 1) return A.get(0).getColumn();
    } catch (TimeoutException e) {
      // se il tempo scade gioca una mossa a caso
      return B.getAvailableColumns()[0];
    }
    // altrimenti esplora l'albero di gioco
    return iterativeDeepening(B, A);
  }
  
  @Override
  public String playerName() {
    return "PojamDesi";
  }
  
  /**
   * Controlla il tempo del turno
   * 
   * @throws TimeoutException il tempo sta per scadere
   */
  private void checkTime() throws TimeoutException {
    // soglia di 20ms
    if ((System.currentTimeMillis() - start) >= TIMEOUT * 998) throw new TimeoutException();
	}
  
  /**
   * Cerca una mossa vincente ed analizza le contromosse dell'avversario.
   * <p>Inserisce in M le mosse giocabili
   * 
   * @param B configurazione di gioco
   * @param M array delle mosse giocabili
   * @return mossa vincente, -1 se non la trova
   * @throws TimeoutException il tempo sta per scadere
   */
  private int immediateMove(CXBoard B, ArrayList<Move> M) throws TimeoutException {
    // true se la mossa è giocabile, false altrimenti
    boolean keep;
    M.clear();
    Integer[] ac = B.getAvailableColumns();
    for (int i : ac) {
      checkTime();
      M.add(new Move(i, 0));
      keep = true;
      // se è una mossa vincente la ritorna subito
      if (B.markColumn(i) == WIN) return i;
      // altrimenti controlla di non perdere alla prossima
      Integer[] acNext = B.getAvailableColumns();
      for (int j : acNext) {
        checkTime();
        if (B.markColumn(j) == LOSE) {
          // se la mossa i mi porta a perdere non è giocabile
          M.remove(M.size() - 1);
          keep = false;
          B.unmarkColumn();
          break;
        } else {
          B.unmarkColumn();
        }
      }
      // se la mossa è giocabile ne calcola la qualità
      if (keep) M.get(M.size() - 1).setQuality(coinsAround(B));
      B.unmarkColumn();
    }
    return -1;
  }
  
  /**
   * Trova la giocata più vantaggiosa da effettuare esplorando l'albero di gioco in ampiezza
   * 
   * @param B configurazione di gioco attuale
   * @param M array delle mosse giocabili
   * @return mossa da giocare
   */
  private int iterativeDeepening(CXBoard B, ArrayList<Move> M) {
    // mossa da giocare (inizialmente casuale)
    int sc = M.get(M.size() / 2).getColumn();
    // mossa più vantaggiosa alla profondità corrente
    int tmp = sc;
    // valutazione della mossa tmp e della mossa attualmente analizzata 
    float tmpEval, eval;
    // ordina le mosse per promettenza decrescente
    M.sort(null);
    try {
      for (int d = Integer.max(lastDepth - 2, 3); d <= maxDepth; d++) {
        tmpEval = minEval;
        eval = minEval;
        for (Move m : M) {
          B.markColumn(m.getColumn());
          eval = alphabeta(B, false, minEval, maxEval, 1, d);
          B.unmarkColumn();
          // tiene la mossa con la valutazione maggiore
          if (eval > tmpEval) {
            tmp = m.getColumn();
            tmpEval = eval;
          }
        }
        // salva la migliore mossa trovata
        sc = tmp;
        // salva la profondità esplorata completamente
        lastDepth = d;
      }
    } catch (TimeoutException e) {}
    return sc;
  }

  /**
   * Assegna un valore ad una configurazione di gioco analizzando le possibili mosse successive.
   * <p>Valore >= 1 se la configurazione è vincente, <= -1 se la configurazione è perdente,
   * 0 se è una patta, 0.xyz in caso di indecisione (.xyz punteggio di promettenza).
   * <p>depth = 1 e maxDepth = 0 se non si vuole usare il limite di profondità
   * 
   * @param B configurazione di gioco
   * @param myTurn true se è il turno del giocatore, false se è dell'avversario
   * @param alpha punteggio minimo ottenibile dal giocatore
   * @param beta punteggio massimo ottenibile dall'avversario
   * @param depth profondità di B nell'albero di gioco
   * @param maxDepth massima profondità raggiungibile durante la visita
   * @return valore assegnato
   * @throws TimeoutException il tempo sta per scadere
   */
  private float alphabeta(CXBoard B, boolean myTurn, float alpha, float beta, int depth, int maxDepth) throws TimeoutException {
    // valutazione della mossa
    float eval;
    checkTime();
    if (depth == maxDepth || isLeaf(B.gameState())) {
      eval = evaluate(B, depth);
    } else if (myTurn) {
      // giocatore che massimizza (PojamDesi)
      eval = minEval;
      Integer[] ac = B.getAvailableColumns();
      for (int c : ac) {
        B.markColumn(c);
        eval = Float.max(eval, alphabeta(B, !myTurn, alpha, beta, depth + 1, maxDepth));
        B.unmarkColumn();
        alpha = Float.max(eval, alpha);
        if (beta <= alpha) break; // beta cutoff
      }
    } else {
      // giocatore che minimizza (avversario)
      eval = maxEval;
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

  /**
   * Controlla se uno stato è di fine partita
   * 
   * @param s stato di gioco
   * @return true se è uno stato terminale, false altrimenti
   */
  private boolean isLeaf(CXGameState s) {
    return s == WIN || s == LOSE || s == CXGameState.DRAW;
  }
  
  /**
   * Assegna un valore ad una configurazione di gioco.
   * <p>Valore >= 1 se la configurazione è vincente, <= -1 se la configurazione è perdente,
   * 0 se è una patta, 0.xyz in caso di indecisione (.xyz punteggio di promettenza)
   * 
   * @param B configurazione di gioco
   * @param depth profondità di B nell'albero di gioco
   * @return valore assegnato
   */
  private float evaluate(CXBoard B, int depth) {
    if (B.gameState() == WIN) {
      return 1 + (maxDepth - depth);
    } else if (B.gameState() == LOSE) {
      return -1 - (maxDepth - depth);
    } else if (B.gameState() == CXGameState.DRAW) {
      return 0;
    } else {
      // in caso di indecisione esegue una valutazione euristica
      return coinsAround(B) / 8.0f;
    }
  }

  /**
   * Conta il numero di pedine del giocatore adiacenti all'ultima giocata
   * 
   * @param B configurazione di gioco
   * @return numero di pedine adiacenti [0-7]
   */
  private int coinsAround(CXBoard B) {
    // numero di pedine adiacenti
    int ca = 0;
    int i = B.getLastMove().i, j = B.getLastMove().j;
    // colonna a dx
    try {
      if (B.cellState(i - 1, j + 1) == MY_COIN) ca++;
    } catch (IndexOutOfBoundsException e) {}
    try {
      if (B.cellState(i, j + 1) == MY_COIN) ca++;
    } catch (IndexOutOfBoundsException e) {}
    try {
      if (B.cellState(i + 1, j + 1) == MY_COIN) ca++;
    } catch (IndexOutOfBoundsException e) {}
    // colonna a sx
    try {
      if (B.cellState(i - 1, j - 1) == MY_COIN) ca++;
    } catch (IndexOutOfBoundsException e) {}
    try {
      if (B.cellState(i, j - 1) == MY_COIN) ca++;
    } catch (IndexOutOfBoundsException e) {}
    try {
      if (B.cellState(i + 1, j - 1) == MY_COIN) ca++;
    } catch (IndexOutOfBoundsException e) {}
    // cella sotto
    try {
      if (B.cellState(i + 1, j) == MY_COIN) ca++;
    } catch (IndexOutOfBoundsException e) {}
    return ca;
  }
  
}
