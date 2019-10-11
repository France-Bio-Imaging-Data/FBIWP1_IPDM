/**
 * 
 */
package plugins.ofuica.metro.protocols;

import icy.plugin.abstract_.Plugin;
import icy.roi.ROI;
import plugins.adufour.blocks.lang.Block;
import plugins.adufour.blocks.util.VarList;
import plugins.adufour.vars.lang.VarEnum;
import plugins.adufour.vars.lang.VarInteger;
import plugins.adufour.vars.lang.VarROIArray;
import plugins.adufour.vars.lang.VarString;
import plugins.adufour.vars.util.VarException;
import plugins.ofuica.metro.Session;
import plugins.ofuica.metro.client.Client;
import plugins.ofuica.metro.client.ClientException;

/**
 * Allows you create and retrieves ROI (Region of interest) from the server side to
 * Icy environment.
 * 
 * @author osvaldo
 */

public final class MetroROI extends Plugin implements Block {
   
   public enum OverlayAction { CREATE, GET }
   private VarSession session_ = new VarSession("session", null);
   private VarLong id_ = new VarLong("id", 0);
   private VarInteger site_ = new VarInteger("site", 0);
   private VarString name_ = new VarString("name", null);
   private VarROIArray rois_ = new VarROIArray("rois");
   private VarEnum<OverlayAction> action_  = new VarEnum<>("action", OverlayAction.GET);
   
   @Override
   public void declareInput(VarList inputMap) {
      inputMap.add(session_.getName(), session_);
      inputMap.add(action_.getName(), action_);
      inputMap.add(id_.getName(), id_);
      inputMap.add(site_.getName(), site_);
      inputMap.add(name_.getName(), name_);
      inputMap.add(rois_.getName(), rois_);
      rois_.setOptional(true);
   }
   
   @Override
   public void declareOutput(VarList outputMap) {
      outputMap.add(rois_.getName(), rois_);
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
         
         if ( action_.getValue().equals(OverlayAction.CREATE) )
            client.addShapesToOverlay(id_.getValue(), site_.getValue(), name_.getValue(), rois_.getValue());
         
         else {
            ROI[] rois = client.getShapesFromOverlay(id_.getValue(), site_.getValue(), name_.getValue());
            rois_.setValue(rois);
         }
      } catch (ClientException e) {
         e.printStackTrace();
      }
   }
}
