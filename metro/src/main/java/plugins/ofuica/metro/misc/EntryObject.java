/**
 * 
 */
package plugins.ofuica.metro.misc;

import java.util.Map.Entry;

/**
 * @author osvaldo
 */

public class EntryObject implements Entry<String, Object> {
   
   private String key_;
   private Object value_;
   
   public EntryObject(Entry<String, Object> entry) {
      key_ = entry.getKey();
      value_ = entry.getValue();
   }
   
   @Override
   public String getKey() {
      return key_;
   }
   
   @Override
   public Object getValue() {
      return value_;
   }
   
   @Override
   public Object setValue(Object value) {
      value_ = value;
      return value_;
   }
   
   @Override
   public String toString() {
      String str = "";
      str += "{ ";
      str += key_ + " => " + icy.util.StringUtil.limit(value_.toString(), 5) + "...";
      str += "}";
      str += "";
      return str;
   }
}
