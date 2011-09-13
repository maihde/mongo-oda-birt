/*
 *************************************************************************
 * Copyright (c) 2010 Pulak Bose
 *  
 *************************************************************************
 */

package org.eclipse.birt.report.data.oda.mongodb.impl;

import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.birt.report.data.oda.mongodb.i18n.Messages;
import org.eclipse.birt.report.data.oda.mongodb.util.MongoDBUtil;
import org.eclipse.datatools.connectivity.oda.IConnection;
import org.eclipse.datatools.connectivity.oda.IDataSetMetaData;
import org.eclipse.datatools.connectivity.oda.IQuery;
import org.eclipse.datatools.connectivity.oda.OdaException;

import com.ibm.icu.util.ULocale;
import com.mongodb.DB;

/**
 * Implementation class of IConnection for an ODA runtime driver.
 */
public class Connection implements IConnection {
	
	/** logger */
	private static Logger logger = Logger.getLogger( Connection.class.getName( ) );

	private boolean m_isOpen = false;
	private DB db = null;
	
	/*
	 * @see
	 * org.eclipse.datatools.connectivity.oda.IConnection#open(java.util.Properties
	 * )
	 */
	public void open(Properties connProperties) throws OdaException {

		String hostName = connProperties.getProperty(CommonConstants.CONN_HOSTNAME_PROP);
		String port = connProperties.getProperty(CommonConstants.CONN_PORT_PROP);
		String dbName = connProperties.getProperty(CommonConstants.CONN_DBNAME_PROP);
		String userName = connProperties.getProperty(CommonConstants.CONN_USERNAME_PROP);
		String password = connProperties.getProperty(CommonConstants.CONN_PASSWORD_PROP);

		int portNumber = 0;

		if (port==null || port.trim().equals(""))
		{
			portNumber = CommonConstants.CONN_DEFAULT_PORT;
		}
		else
		{
			try {
				portNumber = Integer.parseInt(port);
			} catch (NumberFormatException ne) {
				throw new OdaException(
						Messages.getString("connection_INVALID_PORT_NUMBER"));
			}
		}
		
		if (this.db == null) //Do not reconnect if db already exists
		{
			try {
				this.db = MongoDBUtil.connectDb(hostName, portNumber, dbName, userName,
						password);
			} catch (OdaException e) {
				logger.logp(Level.FINER, Connection.class.getName(), "open", e.getMessage(),e);
				throw new OdaException(Messages.getString("connection_COULD_NOT_CONNECT_TO_MONGO") + " " + e.getMessage());
			}
		}
		logger.log(Level.FINER, "Connection Established");
		m_isOpen = true;
	}

	/*
	 * @see
	 * org.eclipse.datatools.connectivity.oda.IConnection#setAppContext(java
	 * .lang.Object)
	 */
	public void setAppContext(Object context) throws OdaException {
		// do nothing; assumes no support for pass-through context
	}

	/*
	 * @see org.eclipse.datatools.connectivity.oda.IConnection#close()
	 */
	public void close() throws OdaException {
		this.db.getMongo().close();
		this.db=null;
		m_isOpen = false;
	}

	/*
	 * @see org.eclipse.datatools.connectivity.oda.IConnection#isOpen()
	 */
	public boolean isOpen() throws OdaException {
		// TODO Auto-generated method stub
		return m_isOpen;
	}

	/*
	 * @see
	 * org.eclipse.datatools.connectivity.oda.IConnection#getMetaData(java.lang
	 * .String)
	 */
	public IDataSetMetaData getMetaData(String dataSetType) throws OdaException {
		// assumes that this driver supports only one type of data set,
		// ignores the specified dataSetType
		return new DataSetMetaData(this);
	}

	/*
	 * @see
	 * org.eclipse.datatools.connectivity.oda.IConnection#newQuery(java.lang
	 * .String)
	 */
	public IQuery newQuery(String dataSetType) throws OdaException {
		// assumes that this driver supports only one type of data set,
		// ignores the specified dataSetType
		return new Query(db);
	}

	/*
	 * @see org.eclipse.datatools.connectivity.oda.IConnection#getMaxQueries()
	 */
	public int getMaxQueries() throws OdaException {
		return 0; // no limit
	}

	/*
	 * @see org.eclipse.datatools.connectivity.oda.IConnection#commit()
	 */
	public void commit() throws OdaException {
		// do nothing; assumes no transaction support needed
	}

	/*
	 * @see org.eclipse.datatools.connectivity.oda.IConnection#rollback()
	 */
	public void rollback() throws OdaException {
		// do nothing; assumes no transaction support needed
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.datatools.connectivity.oda.IConnection#setLocale(com.ibm.
	 * icu.util.ULocale)
	 */
	public void setLocale(ULocale locale) throws OdaException {
		// do nothing; assumes no locale support
	}

}
