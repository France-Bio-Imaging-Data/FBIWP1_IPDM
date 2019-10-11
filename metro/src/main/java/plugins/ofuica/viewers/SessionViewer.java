/**
 * 
 */
package plugins.ofuica.viewers;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JComponent;

import plugins.adufour.vars.gui.swing.SwingVarEditor;
import plugins.adufour.vars.lang.Var;
import plugins.ofuica.metro.Session;

/**
 * @author osvaldo
 */

public final class SessionViewer extends SwingVarEditor<Session> {
   
   private final MouseAdapter mouseAdapter_ = new MouseAdapter()
   {
       public void mouseEntered(MouseEvent e)
       {
          getEditorComponent().repaint();
       };
       
       public void mouseExited(MouseEvent e)
       {
          getEditorComponent().repaint();
       };
   };
   
   public SessionViewer(Var<Session> variable) {
      super(variable);
   }
   
   @Override
   protected JComponent createEditorComponent() {
      return null;
   }
   
   @Override
   protected void activateListeners() {
      getEditorComponent().addMouseListener(mouseAdapter_);
   }
   
   @Override
   protected void deactivateListeners() {
      getEditorComponent().removeMouseListener(mouseAdapter_);
   }
   
   @Override
   protected void updateInterfaceValue() {
   }
}
