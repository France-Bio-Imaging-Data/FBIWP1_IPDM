/**
 * 
 */
package plugins.ofuica.metro.protocols;

import org.w3c.dom.Node;

import plugins.adufour.vars.gui.VarEditor;
import plugins.adufour.vars.lang.Var;
import plugins.adufour.vars.util.VarListener;
import plugins.ofuica.metro.Metro;
import plugins.ofuica.metro.Session;
import plugins.ofuica.viewers.SessionChooser;
import plugins.ofuica.viewers.SessionViewer;

/**
 * @author osvaldo
 */

public final class VarSession extends Var<Session> {
   
   public static final String NO_SESSION = "no session";
   
   public VarSession(String name, Session defaultValue) throws NullPointerException {
      this(name, defaultValue, null);
   }
   
   public VarSession(String name, Session defaultValue, VarListener<Session> defaultListener)
         throws NullPointerException {
      super(name, Session.class, defaultValue, defaultListener);
   }
   
   @Override
   public VarEditor<Session> createVarEditor() {
      return new SessionChooser(this);
   }
   
   @Override
   public VarEditor<Session> createVarViewer() {
      return new SessionViewer(this);
   }
   
   @Override
   public String getValueAsString()
   {
       final Session s = getValue();
       
       if (s == null) return "no session";
       
       return s.getId();
   }
   
   @Override
   public Session parse(String text) {
      final Session session = Metro.getSession(this, text);
      return session;
   }
   
   @Override
   public void setValue(Session newValue) throws IllegalArgumentException {
      super.setValue(newValue);
   }
   
   @Override
   public boolean loadFromXML(Node node) {
      return super.loadFromXML(node);
   }
}
