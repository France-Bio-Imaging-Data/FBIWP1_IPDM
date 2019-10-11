/**
 * 
 */
package plugins.ofuica.metro.protocols;

import java.util.ArrayList;
import java.util.List;

import icy.plugin.abstract_.Plugin;
import plugins.adufour.blocks.tools.ToolsBlock;
import plugins.adufour.blocks.util.VarList;
import plugins.adufour.vars.lang.Var;
import plugins.adufour.vars.lang.VarGenericArray;
import plugins.adufour.vars.lang.VarMutable;
import plugins.adufour.vars.lang.VarString;
import plugins.adufour.vars.util.VarException;
import plugins.adufour.vars.util.VarListener;
import plugins.ofuica.metro.Session;

/**
 * List to be append in the loop or batch block.
 * 
 * @author osvaldo
 */

public final class ListAppend extends Plugin implements ToolsBlock {
   private VarSession session_ = new VarSession("session", null);
   private VarMutable element_ = new VarMutable("element", null);
   private VarGenericArray<Object[]> array_ = new VarGenericArray<>("array", Object[].class, null);
   private List<Object> objects_ = new ArrayList<>();
   
   private final VarListener<String> listener_ = new VarListener<String> () {
      @Override
      public void valueChanged(Var<String> source, String oldValue, String newValue) {
         if ( newValue.equals("connecting")) {
            objects_.clear();
         }
      }
      
      @Override
      public void referenceChanged(Var<String> source, Var<? extends String> oldReference,
            Var<? extends String> newReference) {
      }
   };
   
   public ListAppend() {
      super();
      
      session_.addListener(new VarListener<Session>() {
         @Override
         public void valueChanged(Var<Session> source, Session oldValue, Session newValue) {
            
            if ( newValue != null ) {
               final VarString newValueStatus = newValue.getStatus();
               newValueStatus.addListener(listener_);
            }
            
            if ( oldValue != null ) {
               final VarString oldValueStatus = oldValue.getStatus();
               oldValueStatus.removeListener(listener_);
            }
         }
         
         @Override
         public void referenceChanged(Var<Session> source, Var<? extends Session> oldReference,
               Var<? extends Session> newReference) {
            newReference.getValue().getStatus().addListener(listener_);
            oldReference.getValue().getStatus().removeListener(listener_);
         }
      });
   }
   
   @Override
   public void declareInput(VarList inputMap) {
      inputMap.add(session_.getName(), session_);
      inputMap.add(element_.getName(), element_);
   }
   
   @Override
   public void declareOutput(VarList outputMap) {
      outputMap.add(array_.getName(), array_);
   }
   
   private void validateInputVariables() {
      if ( session_.getValue() == null ) 
         throw new VarException(session_, "session variable is not assigned.");
      
      if ( element_.getValue() == null )
         throw new VarException(element_, "element variable is not assigned.");
   }
   
   @Override
   public void run() {
      validateInputVariables();
      objects_.add(element_.getValue());
      array_.setValue(objects_.toArray(new Object[objects_.size()]));
   }
}
