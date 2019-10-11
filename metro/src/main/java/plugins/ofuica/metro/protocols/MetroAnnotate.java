/**
 * 
 */
package plugins.ofuica.metro.protocols;

import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import icy.plugin.abstract_.Plugin;
import plugins.adufour.blocks.lang.Block;
import plugins.adufour.blocks.util.VarList;
import plugins.adufour.vars.lang.VarEnum;
import plugins.adufour.vars.lang.VarMutable;
import plugins.adufour.vars.lang.VarString;
import plugins.adufour.vars.util.VarException;
import plugins.ofuica.metro.Session;
import plugins.ofuica.metro.client.Client;
import plugins.ofuica.metro.client.ClientException;
import plugins.ofuica.metro.misc.EntryObject;
import plugins.ofuica.metro.misc.VarMapObject;

/**
 * This block allows to retrieve (get/get_all) and to define (put) an annotation.
 * The annotation has two components a key and a value.
 * The key is always a string type and the value could vary (mutable).
 * 
 * It needs a valid client's context to work.
 * 
 * @author osvaldo
 */

public final class MetroAnnotate extends Plugin implements Block {
   
   public enum AnnotateAction { GET, PUT, GET_ALL }
   
   private VarSession session_ = new VarSession("session", null);
   private VarLong id_ = new VarLong("id", 0);
   private VarString key_ = new VarString("key", "");
   private VarMutable value_ = new VarMutable("value", Object.class);
   private VarEnum<AnnotateAction> action_  = new VarEnum<>("action", AnnotateAction.GET);
   
   public MetroAnnotate() {
      super();
   }
   
   @Override
   public void declareInput(VarList inputMap) {
      inputMap.add("session", session_);
      inputMap.add(action_.getName(), action_);
      inputMap.add(id_.getName(), id_);
      inputMap.add(key_.getName(), key_);
      inputMap.add(value_.getName(), value_);
   }
   
   @Override
   public void declareOutput(VarList outputMap) {
      outputMap.add(value_.getName(), value_);
   }
   
   private void validateInputVariables() {
      if ( session_.getValue() == null ) {
         throw new VarException(session_, "You need select a Session. To create one use a Metro Login Block.");
      }
      
      if (id_.getValue() <= 0 ) {
         throw new VarException(id_, "id image should be major than zero");
      }
      
      if ( key_.getValue().isEmpty() ) {
         throw new VarException(key_, "key should not be empty");
      }
   }
   
   @Override
   public void run() {
      
      validateInputVariables();
      
      try {
         
         final Session session = session_.getValue();
         
         if ( action_.getValue().equals(AnnotateAction.PUT) ) {
            final Client client = session.getClient();
            client.annotateImage(id_.getValue(true), key_.getValue(true), value_.getValue(true).toString());
         }
         
         else if ( action_.getValue().equals(AnnotateAction.GET) ) {
            final Client client = session.getClient();
            final Object value = client.getAnnotationValue(id_.getValue(true), key_.getValue(true));
            
            if ( value instanceof GregorianCalendar ) {
               final GregorianCalendar c = (GregorianCalendar) value;
               value_.setValue(c.getTime());
            }
            
            else {
               value_.setValue(value);
            }
         }
         
         else if ( action_.getValue().equals(AnnotateAction.GET_ALL) ) {
            final Client client = session_.getValue().getClient();
            final Map<String, Object> annotations = client.getAllAnnotations(id_.getValue(true));
            
            final EntryObject [] obj = new EntryObject[annotations.size()];
            final Iterator<Entry<String, Object>> entry = annotations.entrySet().iterator();
            
            int i = 0;
            while(entry.hasNext()) {
               Entry<String, Object> e = entry.next();
               obj[i++] = new EntryObject(e);
            }
            
            final VarMapObject map = new VarMapObject("mapTest", obj);
            value_.setValue(map.getValue());
         }
         
         else {
            throw new VarException(action_, "Invalid action selected.");
         }
      } catch (ClientException e) {
         e.printStackTrace();
      }
   }
}
