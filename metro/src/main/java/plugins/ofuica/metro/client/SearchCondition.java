/**
 * 
 */
package plugins.ofuica.metro.client;

/**
 * Search Condition for search!.
 * 
 * Independent condition definition which should be different to each implementation.
 * 
 * @author osvaldo
 */

public class SearchCondition {
   
   private String field_;
   private Object lowerLimit_;
   private Object upperLimit_;
   
   public SearchCondition(String field, Object lower, Object upper) {
      field_ = field;
      lowerLimit_ = lower;
      upperLimit_ = upper;
   }
   
   public String getField() {
      return field_;
   }
   
   public Object getLowerLimit() {
      return lowerLimit_;
   }
   
   public Object getUpperLimit() {
      return upperLimit_;
   }
   
   public static SearchCondition parse(String input) {
      final String[] arr = input.split(" ");
      
      if ( arr.length < 2 || arr.length > 3 ) {
         System.out.println("Search conditions defined is not valid!. ");
         return null;
      }
      
      final String field = arr[0];
      String lowerLimit = "";
      String upperLimit = "";
            
      if ( arr.length == 2 ) {
         lowerLimit = arr[1];
         upperLimit = arr[1];
      }
      
      else {
         lowerLimit = arr[1];
         upperLimit = arr[2];
      }
      
      return new SearchCondition(field, lowerLimit, upperLimit);
   }
}
