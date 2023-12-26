package connectx.PojamDesi;

/**
 * Classe che modellizza una mossa da fare al turno corrente
 * 
 * @author Ivan De Simone - ivan.desimone@studio.unibo.it
 * @author Payam Salarieh - payam.salarieh@studio.unibo.it
 */
public class Move implements Comparable<Move> {
  
  /**
   * Colonna in cui giocare la pedina
   */
  private int column;
  /**
   * Promettenza della mossa da 0 a 7
   */
  private int quality;

  /**
   * Costruttore
   * @param column colonna in cui giocare
   * @param quality promettenza della mossa
   */
  public Move(int column, int quality) {
    this.column = column;
    this.quality = quality;
  }

  /**
   * Getter del campo column
   * @return colonna in cui giocare
   */
  public int getColumn() {
    return column;
  }

  /**
   * Getter del campo quality
   * @return promettenza della mossa
   */
  public int getQuality() {
    return quality;
  }

  /**
   * Setter per il campo quality
   * @param q promettenza della mossa
   */
  public void setQuality(int q) {
    this.quality = q;
  }

  @Override
  public int compareTo(Move m) {
    return -(this.quality - m.getQuality());
  }
  
}
