/**
 * 
 */
package plugins.ofuica.metro.misc;

import plugins.adufour.vars.gui.VarEditor;
import plugins.adufour.vars.lang.Var;

/**
 * @author osvaldo
 */

public class VarEntryObject extends Var<EntryObject> {
   
   public VarEntryObject(String name, Class<EntryObject> type) {
      super(name, type);
   }
   
   @Override
   public VarEditor<EntryObject> createVarViewer() {
      System.out.println("Creating VarEntry Object");
      return super.createVarViewer();
   }
}
