/**
 * 
 */
package plugins.ofuica.metro.utils;

import java.util.ArrayList;

/**
 * @author osvaldo
 */

public final class NotNullArrayList<T> extends ArrayList<T> {
   
   private static final long serialVersionUID = 7875375769099772232L;
   
   @Override
   public boolean add(T element) {
      
      if ( element == null )
         return false;
      
      return super.add(element);
   }
   
   @Override
   public void add(int index, T element) {
      
      if ( element == null ) {
         return;
      }
      
      super.add(index, element);
   }
}

