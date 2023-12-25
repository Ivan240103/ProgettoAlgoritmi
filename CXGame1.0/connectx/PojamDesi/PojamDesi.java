package connectx.PojamDesi;

import java.util.ArrayList;

import connectx.CXBoard;
import connectx.CXGameState;
import connectx.CXPlayer;

/**
 * Implementation of a connectx player
 * 
 * @author Ivan De Simone - ivan.desimone@studio.unibo.it
 * @author Payam Salarieh - payam.salarieh@studio.unibo.it
 */
public class PojamDesi implements CXPlayer {

  private String name = "PojamDesi";
  /* private int M, N, timeout;
  boolean first; */

  // stati di vittoria e sconfitta riguardo il mio giocatore
  private CXGameState win, lose;
  // mosse fattibili dopo il controllo delle mosse immediate
  private ArrayList<Integer> A;
  
  @Override
  public void initPlayer(int M, int N, int X, boolean first, int timeout_in_secs) {
    /* this.M = M;
    this.N = N;
    this.first = first;
    this.timeout = timeout_in_secs; */

    // imposta stati di vittoria e sconfitta in base a chi gioca per primo
    this.win = first ? CXGameState.WINP1 : CXGameState.WINP2;
    this.lose = first ? CXGameState.WINP2 : CXGameState.WINP1;
    A = new ArrayList<Integer>();
  }

  @Override
  public int selectColumn(CXBoard B) {
    System.err.println("a");
    // svuota l'array dalle mosse valutate precedentemente
    A.clear();
    // selected column
    int sc = immediateMove(B);
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

    // se ho delle mosse giocabili scelgo quella che mi può portare
    // eventualmente al risultato migliore
    int eval = -3;
    for (int a : A) {
      B.markColumn(a);
      System.err.println("e");
      eval = Integer.max(eval, minimax(B, false));
      System.err.println("f");
      // se una mossa mi può portare alla vittoria la gioco subito
      if (eval == 1) return a;
      B.unmarkColumn();
      System.err.println("g");
      // altrimenti scelgo una mossa di indecisione/patta
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
    }
  }

  @Override
  public String playerName() {
    return name;
  }

  // cerca una mossa vincente e contemporanemente analizza le
  // contromosse dell'avversario
  // ritorna la mossa da giocare, -1 se non ne trova una specifica
  // lascia in A le mosse giocabili
  private int immediateMove(CXBoard B) {
    System.err.println("k");
    Integer[] ac = B.getAvailableColumns();
    for (int i : ac) {
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
          if (B.markColumn(j) == lose) {
            System.err.println("o");
            // se la mossa i mi porta a perdere meglio non giocarla
            A.remove(A.indexOf(i));
            B.unmarkColumn();
            break;
          } else {
            System.err.println("p");
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

  // TODO: moltiplicare l'eval per la profondità per poter velocizzare la vittoria o rallentare la sconfitta
  private int minimax(CXBoard B, boolean myTurn) {
    int eval;
    if (isLeaf(B.gameState())) {
      System.err.println("s");
      eval = evaluate(B.gameState());
    } else if (myTurn) {
      System.err.println("t");
      // giocatore che massimizza: io (vittoria = 1)
      eval = -3;
      Integer[] ac = B.getAvailableColumns();
      for (int c : ac) {
        B.markColumn(c);
        eval = Integer.max(eval, minimax(B, !myTurn));
        B.unmarkColumn();
        if (eval == 1) break;
      }
    } else {
      System.err.println("u");
      // giocatore che minimizza: avversario (vittoria = -1)
      eval = 3;
      Integer[] ac = B.getAvailableColumns();
      for (int c : ac) {
        B.markColumn(c);
        eval = Integer.min(eval, minimax(B, !myTurn));
        B.unmarkColumn();
        if (eval == -1) break;
      }
    }
    return eval;
  }

  // controlla se è uno stato di fine partita
  private boolean isLeaf(CXGameState s) {
    System.err.println("v");
    return s == win || s == lose || s == CXGameState.DRAW;
  }

  // assegna un valore allo stato passato
  private int evaluate(CXGameState state) {
    System.err.println("x");
    if (state == win) {
      return 1;
    } else if (state == lose) {
      return -1;
    } else {
      return 0;
    }
  }
  
}
