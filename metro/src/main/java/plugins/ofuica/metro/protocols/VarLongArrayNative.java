/**
 * 
 */
package plugins.ofuica.metro.protocols;

import plugins.adufour.vars.lang.Var;
import plugins.adufour.vars.lang.VarGenericArray;
import plugins.adufour.vars.util.VarListener;

/**
 * Native long array implementation for protocols.
 * 
 * @author osvaldo
 */

public final class VarLongArrayNative extends VarGenericArray<long[]> {
   
   public VarLongArrayNative(String name, long[] defaultValue) {
       this(name, defaultValue, null);
   }
   
   public VarLongArrayNative(String name, long[] defaultValue, VarListener<long[]> defaultListener) {
       super(name, long[].class, defaultValue, defaultListener);
   }
   
   @Override
   public Object parseComponent(String s) {
       return Long.parseLong(s);
   }
   
   @Override
   public long[] getValue(boolean forbidNull)
   {
       @SuppressWarnings("rawtypes")
       Var reference = getReference();
       if (reference == null) 
          return super.getValue(forbidNull);
       
       final Object value = reference.getValue();
       
       if (value == null) 
          return super.getValue(forbidNull);
       
       if (value instanceof Number) 
          return new long[] { ((Number) value).intValue() };
       
       return (long[]) value;
   }
}
