/**
 * 
 */
package plugins.ofuica.metro.protocols;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import icy.plugin.abstract_.Plugin;
import icy.sequence.Sequence;
import plugins.adufour.blocks.lang.Block;
import plugins.adufour.blocks.util.VarList;
import plugins.adufour.vars.lang.Var;
import plugins.adufour.vars.lang.VarBoolean;
import plugins.adufour.vars.lang.VarEnum;
import plugins.adufour.vars.lang.VarMutable;
import plugins.adufour.vars.lang.VarString;
import plugins.adufour.vars.util.VarException;
import plugins.adufour.vars.util.VarListener;
import plugins.adufour.workbooks.Workbooks;
import plugins.ofuica.metro.Session;
import plugins.ofuica.metro.client.Client;
import plugins.ofuica.metro.client.ClientException;

/**
 * It block allows to attach an object with one image in the server side. 
 * The object could be an icy's sequence. 
 * 
 * @author osvaldo
 */

public final class MetroAttach extends Plugin implements Block {
   
   public enum AttachAction { CREATE, GET }
   private VarSession session_ = new VarSession("session", null);
   private VarLong id_ = new VarLong("id", 0);
   
   private VarMutable object_ = new VarMutable("object", null);
   private VarString name_ = new VarString("name", "");
   private VarString notes_ = new VarString("notes", "");
   private VarEnum<AttachAction> action_  = new VarEnum<>("action", AttachAction.CREATE);
   private VarBoolean workbook_ = new VarBoolean("workbook", false);
   
   private VarMutable output_ = new VarMutable("Output", File.class);
   private VarList outputsVars_ = null;
   
   private void removeOptionalInputs(VarList inputMap) {
      if ( inputMap.contains(workbook_)) inputMap.remove(workbook_);
      if ( inputMap.contains(object_)) inputMap.remove(object_);
      if ( inputMap.contains(name_)) inputMap.remove(name_);
      if ( inputMap.contains(notes_)) inputMap.remove(notes_);
   }
   
   private void declareInputCreate(VarList inputMap) {
      removeOptionalInputs(inputMap);
      inputMap.add(object_.getName(), object_);
      inputMap.add(name_.getName(), name_);
      inputMap.add(notes_.getName(), notes_);
      
      if ( outputsVars_ != null ) {
         if ( outputsVars_.contains(output_) ) 
            outputsVars_.remove(output_);
      }
   }
   
   private void declareInputGet(VarList inputMap) {
      removeOptionalInputs(inputMap);
      inputMap.add(workbook_.getName(), workbook_);
      inputMap.add(name_.getName(), name_);
      
      if ( outputsVars_ != null ) {
         if ( !outputsVars_.contains(output_) ) 
            outputsVars_.add(output_.getName(), output_);
      }
   }
   
   @Override
   public void declareInput(VarList inputMap) {
      inputMap.add(session_.getName(), session_);
      inputMap.add(action_.getName(), action_);
      inputMap.add(id_.getName(), id_);
      inputMap.add(object_.getName(), object_);
      inputMap.add(name_.getName(), name_);
      inputMap.add(notes_.getName(), notes_);
      
      action_.addListener(new VarListener<AttachAction>() {
         @Override
         public void valueChanged(Var<AttachAction> source, AttachAction oldValue, AttachAction newValue) {
            
            if ( newValue.equals(AttachAction.CREATE)) {
               declareInputCreate(inputMap);
            }
            else { // GET
               declareInputGet(inputMap);
            }
         }
         
         @Override
         public void referenceChanged(Var<AttachAction> source, Var<? extends AttachAction> oldReference,
               Var<? extends AttachAction> newReference) {
         }
      });
      
      final VarListener<Boolean> booleanListener = new VarListener<Boolean>() {
         
         @Override
         public void valueChanged(Var<Boolean> source, Boolean oldValue, Boolean newValue) {
            
            if ( output_.isReferenced() ) {
               return;
            }
            
            if ( action_.getValue().equals(AttachAction.GET)) {
               if ( newValue.equals(true)) 
                  output_.setType(Workbook.class);
               else
                  output_.setType(File.class);
            }
         }
         
         @Override
         public void referenceChanged(Var<Boolean> source, Var<? extends Boolean> oldReference,
               Var<? extends Boolean> newReference) {
         }
      };
      
      workbook_.addListener(booleanListener);
   }
   
   @Override
   public void declareOutput(VarList outputMap) {
      outputsVars_ = outputMap;
   }
   
   private void validateInputVariables() {
      if ( session_.getValue() == null )
         throw new VarException(session_, "You need select a Session. To create one use a Metro Login Block.");
      
   }
   
   @Override
   public void run() {
      validateInputVariables();
      
      try {
         final Session session = session_.getValue();
         final Client client = session.getClient();
         
         if ( action_.getValue() == AttachAction.CREATE ) {
            
            if ( object_.getValue() == null ) {
               throw new VarException(object_, "You should link with an object.");
            }
            
            if ( object_.getValue() instanceof Sequence ) {
               client.attachSequence(id_.getValue(), (Sequence) object_.getValue(), name_.getValue(), notes_.getValue());
            }
            
            else if ( object_.getValue() instanceof XSSFWorkbook ) {
               final XSSFWorkbook book = (XSSFWorkbook) object_.getValue();
               final File tmp = File.createTempFile("tmpattachment", ".xlsx");
               final FileOutputStream stream = new FileOutputStream(tmp);
               
               try {
                  book.write(stream);
               } catch (final IOException e) {
                  System.out.println(e.getMessage());
               }
               // book.close();
               stream.close();
               
               client.attachFile(id_.getValue(), tmp.getPath(), name_.getValue(), notes_.getValue());
               // FileUtil.delete(tmpFile, false);
            }
            
            else {
               throw new VarException(object_, "Object type not supported.");
            }
         }
         
         else {
            final File file = client.getAttachment(id_.getValue(), name_.getValue());
            
            if ( workbook_.getValue() ) {
               final Workbook book = Workbooks.openWorkbook(file);
               
               if ( book != null )
                  output_.setValue(book);
            }
            
            else {
               output_.setValue(file);
            }
         }
      } catch (ClientException e) {
         e.printStackTrace();
      } catch (FileNotFoundException e) {
         e.printStackTrace();
      } catch (IOException e) {
         e.printStackTrace();
      }
   }
}
