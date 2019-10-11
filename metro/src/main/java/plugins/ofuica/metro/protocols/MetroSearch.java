/**
 * 
 */
package plugins.ofuica.metro.protocols;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import icy.plugin.abstract_.Plugin;
import plugins.adufour.blocks.lang.Block;
import plugins.adufour.blocks.util.VarList;
import plugins.adufour.vars.lang.Var;
import plugins.adufour.vars.lang.VarString;
import plugins.adufour.vars.lang.VarTrigger;
import plugins.adufour.vars.lang.VarTrigger.TriggerListener;
import plugins.adufour.vars.util.VarException;
import plugins.adufour.vars.util.VarReferencingPolicy;
import plugins.ofuica.metro.Session;
import plugins.ofuica.metro.client.Client;
import plugins.ofuica.metro.client.ClientException;
import plugins.ofuica.metro.client.SearchCondition;

/**
 * Finds the image's guids based in a generic criteria input.
 * 
 * @author osvaldo
 */

public final class MetroSearch extends Plugin implements Block {
   
   private VarSession session_ = new VarSession("session", null);
   private final VarLongArrayNative guids_ = new VarLongArrayNative("guids", null);
   private final List<VarString> searchConditionInputs_ = new ArrayList<>();
   
   private VarTrigger addConditionTrigger_;
   private VarTrigger delConditionTrigger_;
   
   public MetroSearch() {
      super();
   }
   
   protected void initializeConditionTriggers(VarList inputMap) {
      addConditionTrigger_ = new VarTrigger("Add Condition");
      delConditionTrigger_ = new VarTrigger("Delete Condition");
      
      addConditionTrigger_.setReferencingPolicy(VarReferencingPolicy.NONE);
      delConditionTrigger_.setReferencingPolicy(VarReferencingPolicy.NONE);
      
      final TriggerListener addConditionListener = new TriggerListener() {
         @Override
         public void valueChanged(Var<Integer> source, Integer oldValue, Integer newValue) {
            final int conditionsToAdd = newValue - oldValue;
            
            for ( int i = 0 ; i < conditionsToAdd ; i++ ) {
               final VarString stringInput = new VarString("condition #" + (searchConditionInputs_.size()+1), null);
               stringInput.setReferencingPolicy(VarReferencingPolicy.NONE);
               searchConditionInputs_.add(stringInput);
               inputMap.add(stringInput.getName(), stringInput);
            }
         }
         
         @Override
         public void referenceChanged(Var<Integer> source, Var<? extends Integer> oldReference,
               Var<? extends Integer> newReference) {
            System.out.println("referenceChanged");
         }
         
         @Override
         public void triggered(VarTrigger source) {
         }
      };
      
      final TriggerListener delConditionListener = new TriggerListener() {
         
         @Override
         public void valueChanged(Var<Integer> source, Integer oldValue, Integer newValue) {
         }
         
         @Override
         public void referenceChanged(Var<Integer> source, Var<? extends Integer> oldReference,
               Var<? extends Integer> newReference) {
         }
         
         @Override
         public void triggered(VarTrigger source) {
            if ( searchConditionInputs_.size() == 0)
               return;
            
            
            final VarString obj = searchConditionInputs_.get(searchConditionInputs_.size() - 1);
            inputMap.remove(obj);
            searchConditionInputs_.remove(obj);
         }
      };
      
      addConditionTrigger_.addListener(addConditionListener);
      delConditionTrigger_.addListener(delConditionListener);
   }
   
   @Override
   public void declareInput(VarList inputMap) {
      inputMap.add(session_.getName(), session_);
      initializeConditionTriggers(inputMap);
      inputMap.add(addConditionTrigger_.getName(), addConditionTrigger_);
      inputMap.add(delConditionTrigger_.getName(), delConditionTrigger_);
   }
   
   @Override
   public void declareOutput(VarList outputMap) {
      outputMap.add(guids_.getName(), guids_);
   }
   
   protected Set<SearchCondition> parseSearchConditions() {
      final Set<SearchCondition> ret = new HashSet<>();
      
      for ( VarString varString : searchConditionInputs_ ) {
          ret.add(SearchCondition.parse(varString.getValue()));
      }
      
      return ret;
   }
   
   private void validateInputVariables() {
      if ( session_.getValue() == null ) {
         throw new VarException(session_, "You need select a Session. To create one use a Metro Login Block.");
      }
   }
   
   @Override
   public void run() {
      validateInputVariables();
      
      try {
         final Session session = session_.getValue();
         final Client client = session.getClient();
         guids_.setValue(client.searchAll(parseSearchConditions()));
      } catch (ClientException e) {
         e.printStackTrace();
      }
   }
}
