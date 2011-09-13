package org.eclipse.birt.report.data.oda.mongodb.util;

import org.eclipse.birt.report.data.oda.mongodb.i18n.Messages;
import org.eclipse.datatools.connectivity.oda.OdaException;

import com.mongodb.DB;
import com.mongodb.Mongo;

public class MongoDBUtil {

	public static DB connectDb(String hostName, int port, String dbName,
			String userName, String password) throws OdaException {
		Mongo m;
		DB localdb = null;
		boolean authRequired;
		boolean authSuccess = false;

		if (userName==null || password==null || userName.trim().equals("") || password.trim().equals(""))
			authRequired = false;
		else
			authRequired = true;

		try 
		{
			m = new Mongo(hostName, port);
			localdb = m.getDB(dbName);

			if (authRequired) {
				authSuccess = localdb.authenticate(userName,
						password.toCharArray());
			}	
			if (localdb.getCollectionNames().size()==0) throw new OdaException(Messages.getString("util_NO_COLLECTION_RETURNED")) ;
			
		} catch (Exception e) {
			throw new OdaException(e.getMessage());
		}

		if (authRequired && !authSuccess)
		{
			throw new OdaException(
					Messages.getString("util_MONGO_AUTHENTICATION_FAILED"));
		}
		
		return localdb;
	}
}
