package LocatorServer.dblayer;

import java.sql.SQLException;
import java.util.List;

import LocatorServer.MainClass;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.exception.ConstraintViolationException;


import LocatorServer.dblayer.tables.Profile;
import LocatorServer.dblayer.tables.Registrations;


public class dbManager {

    static Session session = sessionclass.getSession();

	public int createRegistration(String name ,String contact , String mobilestamp)
	{
		int retVal = 1;
		try
		{
		    if(session == null || session.isOpen() == false) {
                session = sessionclass.createNewSession();

            }
            session = sessionclass.getSession();

            //  session.beginTransaction();
		  Registrations reg = new Registrations();
		  reg.setContact(contact);
		  reg.setMobilestamp(mobilestamp);

		 session.saveOrUpdate(reg);
//		 Profile pro = new Profile();
//		 pro.setName(name);
//		 pro.setNumber(contact);
//		 session.save(pro);
            MainClass.MYLOGGER.info("Registering a new user. contact = "+contact+" mobilestamp = "+mobilestamp);
		 sessionclass.commitTransaction(session);
		}
		catch(ConstraintViolationException  ex)
		{
			int err_code = ex.getErrorCode();
			//int errCode = ex.getErrorCode();
			String msg = ex.getLocalizedMessage();
			retVal = 0;
            MainClass.MYLOGGER.info("exception : "+msg+" while Registering a new user. contact = "+contact+" mobilestamp = "+mobilestamp);

            System.out.println(retVal);
		}

		
		finally
		{
//		    if(session != null)
//		        session.close();
			//HibernateUtil.getSessionFactory().close();
			int y = 100;
			System.out.println(y);
			
		}
		return retVal;
		
	}

	public String updateMobileContact(String contact, String mobileStamp)
    {
      //  Session session = null;
        try {
            String tempmobileStamp = "";
            if(session == null || session.isOpen() == false) {
                session = sessionclass.createNewSession();

            }
            session = sessionclass.getSession();

            //  session.beginTransaction();
            String hql = "update Registrations set contact = '" + contact + "' where mobilestamp = '" + mobileStamp + "'";
            session.createQuery(hql).executeUpdate();
            //  session.getTransaction().commit();
            sessionclass.commitTransaction(session);
           // session.close();

        }
        catch(Exception ex)
        {
            mobileStamp = null;
        }
        finally
        {
//            if(session != null)
//                session.close();
        }
        return mobileStamp;
    }

    public List<Object[]> getContactAndMobileNumbers()
    {
        if(session == null || session.isOpen() == false) {
            session = sessionclass.createNewSession();

        }
        session = sessionclass.getSession();
        Query q = session.createQuery("select contact, mobilestamp from Registrations");
        List<Object[]> details= (List<Object[]>)q.list();
        return details;
    }

	public String updateMobileStamp(String contact, String mobileStamp)
	{
        String hql = "";
	   // Session session = null;
		try {
			String tempmobileStamp = "";
            if(session == null || session.isOpen() == false) {
                session = sessionclass.createNewSession();

            }
            session = sessionclass.getSession();

            //  session.beginTransaction();
			 hql = "update Registrations set mobilestamp = '" + mobileStamp + "' where contact = '" + contact + "'";
            MainClass.MYLOGGER.info("in updateMobileStamp. query : "+hql);
			session.createQuery(hql).executeUpdate();
			//  session.getTransaction().commit();
			sessionclass.commitTransaction(session);
			session.close();

		}
		catch(Exception ex)
			{
			    MainClass.MYLOGGER.info("exception in updateMobileStamp fo query "+ hql);
				mobileStamp = null;
			}
        finally
        {
//            if(session != null)
//                session.close();
        }
		return mobileStamp;
	}
	public String getContact(String mobileStamp)
    {
        Session session = null;
        String contact = "";
        try {

            //    session = sessionclass.getSession();
            //  session.beginTransaction();


            if(session == null || session.isOpen() == false) {
                session = sessionclass.createNewSession();

            }
            session = sessionclass.getSession();


            String hql = "select contact  from Registrations where mobilestamp = '" + mobileStamp + "'";
            List result = session.createQuery(hql).list();
            //  session.getTransaction().commit();
            sessionclass.commitTransaction(session);

            // HibernateUtil.getSessionFactory().close();
            if (result != null && result.size() > 0)
                contact = result.get(0).toString();
        }
        catch(Exception ex)
        {

        }
        finally
        {
//            if(session != null)
//                session.close();
        }

        return contact;
    }

    public boolean isContactRegistered(String contact)
    {
        Session session = null;
        String mobileStamp = "";
        String hql = "";
        try {

            //    session = sessionclass.getSession();
            //  session.beginTransaction();


            if(session == null || session.isOpen() == false) {
                session = sessionclass.createNewSession();

            }
            session = sessionclass.getSession();


            hql = "select contact from Registrations where contact = '" + contact + "'";
            MainClass.MYLOGGER.info("In isContactRegistered . executing query : "+hql);
            List result = session.createQuery(hql).list();
            //  session.getTransaction().commit();
            sessionclass.commitTransaction(session);

            // HibernateUtil.getSessionFactory().close();
            if (result != null && result.size() > 0)
                return true;
        }
        catch(Exception ex)
        {
            MainClass.MYLOGGER.info("In isContactRegistered . exception occured for executing query : "+hql);

        }
        finally
        {
//            if(session != null)
//                session.close();
        }

        return false;
    }
	public String getMobileStamp(String contact)
	{
        Session session = null;
        String mobileStamp = "";
        String hql = "";
	    try {

         //    session = sessionclass.getSession();
            //  session.beginTransaction();


            if(session == null || session.isOpen() == false) {
                session = sessionclass.createNewSession();

            }
            session = sessionclass.getSession();


             hql = "select mobilestamp from Registrations where contact = '" + contact + "'";
             MainClass.MYLOGGER.info("In getMobileStamp . executing query : "+hql);
            List result = session.createQuery(hql).list();
            //  session.getTransaction().commit();
            sessionclass.commitTransaction(session);

            // HibernateUtil.getSessionFactory().close();
            if (result != null && result.size() > 0)
                mobileStamp = result.get(0).toString();
        }
        catch(Exception ex)
        {
            MainClass.MYLOGGER.info("In getMobileStamp . exception occured for executing query : "+hql);

        }
        finally
        {
//            if(session != null)
//                session.close();
        }

        return mobileStamp;
	}
};
