/**
 * 
 */
package plugins.ofuica.metro.misc;

import plugins.adufour.vars.gui.VarEditor;
import plugins.adufour.vars.lang.VarGenericArray;

/**
 * @author osvaldo
 */

public class VarMapObject extends VarGenericArray<EntryObject[]> {
   
   public VarMapObject(String name, EntryObject[] defaultValue) {
      this(name, EntryObject[].class, defaultValue);
   }
   
   public VarMapObject(String name, Class<EntryObject[]> type, EntryObject[] defaultValue) {
      super(name, type, defaultValue);
   }
   
   @Override
   public VarEditor<EntryObject[]> createVarEditor() {
      System.out.println("create Var Editor");
      return super.createVarEditor();
   }
   
   @Override
   public VarEditor<EntryObject[]> createVarViewer() {
      System.out.println("CreateVarViewer()");
      return super.createVarViewer();
   }
}
