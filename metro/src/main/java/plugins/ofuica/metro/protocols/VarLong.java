/**
 * 
 */
package plugins.ofuica.metro.protocols;

import plugins.adufour.vars.gui.model.VarEditorModel;
import plugins.adufour.vars.lang.Var;
import plugins.adufour.vars.lang.VarNumber;
import plugins.adufour.vars.util.VarListener;

/**
 * holds a long value.
 * 
 * @author osvaldo
 */
public final class VarLong extends VarNumber<Long> {
   
   public VarLong(String name, long defaultValue)
   {
      this(name, defaultValue, null);
   }
   
   public VarLong(String name, VarEditorModel<Long> model) {
      super(name, model);
   }
   
   public VarLong(String name, long defaultValue, VarListener<Long> defaultListener)
   {
      super(name, Long.TYPE, defaultValue, defaultListener);
   }
   
   public VarLong(String name, Class<Long> type, Long defaultValue, VarListener<Long> defaultListener) 
   {
      super(name, type, defaultValue, defaultListener);
   }
   
   @Override
   public Long parse(String s)
   {
      return Long.parseLong(s);
   }
   
   @Override
   public int compareTo(Long integer)
   {
      return getValue().compareTo(integer);
   }
   
   @Override
   public Long getValue()
   {
      return getValue(false);
   }
   
   public Long getValue(boolean forbidNull)
   {
      Number number = super.getValue(forbidNull);
      return number == null ? null : number.longValue();
   }
   
   @Override
   public boolean isAssignableFrom(Var<?> source)
   {
       if (source.getType() == null) return false;
       
       Class<?> sourceType = source.getType();
       
       return sourceType == Double.TYPE || sourceType == Float.TYPE || 
             sourceType == Integer.TYPE || sourceType == Long.TYPE || Number.class.isAssignableFrom(source.getType());
   }
}
