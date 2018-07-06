package LocatorServer.dblayer;

import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.context.internal.ManagedSessionContext;

public class sessionclass {
	public static Session thisSession = null;
	public static Session getSession()
	{
		//if(thisSession == null)
		//{
			thisSession = createNewSessionAndTransaction();
			
		//}
		return thisSession;
	}
	
	 protected static org.hibernate.Session createNewSession() {
	      org.hibernate.Session session = HibernateUtil.getSessionFactory().openSession();
	      session.setFlushMode(FlushMode.MANUAL);
	      ManagedSessionContext.bind(session);
	      return (org.hibernate.Session) session;
	   }
	 /**
	    * Start a new Transaction in the given session
	    * @param session The session to create the transaction in
	    */
	   protected static void startNewTransaction(Session session) {
	      session.beginTransaction();
	   }

	   /**
	    * Shortcut method that creates a new session and begins a transaction in it
	    * @return A new session with a transaction started
	    */
	   protected static org.hibernate.Session createNewSessionAndTransaction() {
	      Session session = createNewSession();
	      startNewTransaction(session);
	      return session;
	   }

	   /**
	    * Commit the transaction within the given session. This method unbinds
	    * the session from the session context (ManagedSessionContext), flushes
	    * the session, commmits the session and then closes the session
	    * @param session The session with the transaction to commit
	    */
	   protected static void commitTransaction(Session session) {
	      ManagedSessionContext.unbind(HibernateUtil.getSessionFactory());
	      session.flush();
	      session.getTransaction().commit();
	     // session.close();
	   }
}
