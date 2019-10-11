/**
 * 
 */
package plugins.ofuica.internals.utils;

/**
 * @author osvaldo
 */

public class MNPair<T extends Number> implements Comparable<MNPair<T> > {
   final private T m_;
   final private T n_;
   private T index_;
   
   public MNPair(T m, T n, T index ) {
      m_ = m;
      n_ = n;
      index_ = index;
   }
   
   public int hashCode() {
      return toString().hashCode();
   }
   
   public T getM() { 
      return m_; 
   }
   
   public T getN() { 
      return n_; 
   }
   
   public T getIndex() {
      return index_;
   }
   
   public boolean equals(Object obj) {
      final MNPair<Number> pair = (MNPair<Number>) obj;
      return (pair.getM().doubleValue() == m_.doubleValue()) && (pair.getN().doubleValue() == n_.doubleValue());
   }
   
   public String toString() {
      return "m_:" + m_ + "," + "n_" + n_;
   }
   
   @Override
   public int compareTo(MNPair<T> o) {
      return index_.intValue() - o.getIndex().intValue();
   }
}
