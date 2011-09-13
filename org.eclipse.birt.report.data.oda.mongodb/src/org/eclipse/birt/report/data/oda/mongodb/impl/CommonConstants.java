package org.eclipse.birt.report.data.oda.mongodb.impl;

/**
 * The class that defines package-wide static constants.
 */

public final class CommonConstants
{

	public static final String CONN_HOSTNAME_PROP = "hostName"; //$NON-NLS-1$
	public static final String CONN_PORT_PROP = "port"; //$NON-NLS-1$
	public static final String CONN_DBNAME_PROP = "dbName"; //$NON-NLS-1$
	public static final String CONN_USERNAME_PROP = "userName"; //$NON-NLS-1$
	public static final String CONN_PASSWORD_PROP = "password"; //$NON-NLS-1$
	
	public static final int CONN_DEFAULT_PORT = 27017; //$NON-NLS-1$
	
	
	/**
	 * Private constructor to ensure that the class cannot be instantiated.
	 */
	private CommonConstants( )
	{
	}	
}
