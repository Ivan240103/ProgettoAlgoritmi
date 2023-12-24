package connectx.PojamDesi;

import connectx.CXBoard;
import connectx.CXPlayer;

/**
 * Implementation of a connectx player
 * 
 * @author Ivan De Simone - ivan.desimone@studio.unibo.it
 * @author Payam Salarieh - payam.salarieh@studio.unibo.it
 */
public class PojamDesi implements CXPlayer {

  /**
   * Player's name
   */
  private String name = "PojamDesi";
  /**
   * Rows, columns, coins to align and time to choose a column in seconds
   */
  private int M, N, X, timeout;
  /**
   * True if first to play, false otherwise
   */
  boolean first;

  @Override
  public void initPlayer(int M, int N, int X, boolean first, int timeout_in_secs) {
    this.M = M;
    this.N = N;
    this.X = X;
    this.first = first;
    this.timeout = timeout_in_secs;
  }

  @Override
  public int selectColumn(CXBoard B) {
    return 0;
  }

  @Override
  public String playerName() {
    return name;
  }
  
}
