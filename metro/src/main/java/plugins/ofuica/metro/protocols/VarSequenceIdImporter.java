/**
 * 
 */
package plugins.ofuica.metro.protocols;

import icy.sequence.SequenceIdImporter;
import plugins.adufour.vars.lang.Var;
import plugins.adufour.vars.util.VarListener;

/**
 * @author osvaldo
 * @deprecated
 */

public final class VarSequenceIdImporter extends Var<SequenceIdImporter> {
   
   public VarSequenceIdImporter(String name, SequenceIdImporter defaultValue) {
      this(name, defaultValue, null);
   }
   
   public VarSequenceIdImporter(String name, SequenceIdImporter defaultValue,
         VarListener<SequenceIdImporter> defaultListener) throws NullPointerException {
      super(name, defaultValue, defaultListener);
   }
}
