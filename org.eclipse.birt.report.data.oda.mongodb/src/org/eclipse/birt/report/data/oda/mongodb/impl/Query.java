/*
 *************************************************************************
 * Copyright (c) 2010 Pulak Bose
 *  
 *************************************************************************
 */

package org.eclipse.birt.report.data.oda.mongodb.impl;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.birt.report.data.oda.mongodb.i18n.Messages;
import org.eclipse.datatools.connectivity.oda.IParameterMetaData;
import org.eclipse.datatools.connectivity.oda.IQuery;
import org.eclipse.datatools.connectivity.oda.IResultSet;
import org.eclipse.datatools.connectivity.oda.IResultSetMetaData;
import org.eclipse.datatools.connectivity.oda.OdaException;
import org.eclipse.datatools.connectivity.oda.SortSpec;
import org.eclipse.datatools.connectivity.oda.spec.QuerySpecification;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.util.JSON;

/**
 * Implementation class of IQuery for an ODA runtime driver.
 */
public class Query implements IQuery {
	
	private static Logger logger = Logger.getLogger( Query.class.getName( ) );
		
	private int m_maxRows;
	private String m_preparedText;
	private DB db = null;

	List<String> selectColumns = null;
	private String filterClause = null;
	private String sortClause = null;

	private DBCursor cursor = null;
	private DBObject metadataObject = null;
	private DBCollection collection = null;

	public Query(DB db) {
		this.db = db;
	}

	public void setFilterClause(String filterClause) {
		this.filterClause = filterClause;
	}

	public void setSortClause(String sortClause) {
		this.sortClause = sortClause;
	}

	/*
	 * @see
	 * org.eclipse.datatools.connectivity.oda.IQuery#prepare(java.lang.String)
	 */
	public void prepare(String queryText) throws OdaException {
		try {
			m_preparedText = queryText;
			if (queryText.trim().endsWith(")")) //Trim the end parenthesis if multiple select columns are specified
			{
				queryText = queryText.substring(0,queryText.length()-1);
			}
			String[] queryStringArray = queryText.split("\\("); //Split the queryText, take first token as collection name
			
			//Check explicitly whether the collection exists in the Mongo instance.
			Set<String> collections = db.getCollectionNames();
			if (collections.contains(queryStringArray[0]))
			{
				collection = db.getCollection(queryStringArray[0]);	
			}
			else
			{
				throw new OdaException(Messages.getString("query_INVALID_COLLECTION_NAME"));
			}

			metadataObject = collection.findOne(); //Get the first row to determine the metadata
			if (queryStringArray.length > 1) //Check whether further tokens exists in the query text
				selectColumns = Arrays.asList( queryStringArray[1].split(",")); //Split again to get the select column names
			else
				selectColumns = new ArrayList<String>();
			
			if (metadataObject == null)
				throw new OdaException(Messages.getString("query_INVALID_METADATA"));
			
		} catch (Exception e) {
			logger.logp(Level.FINER, Query.class.getName(), "prepare", e.getMessage());
			throw new OdaException(
					Messages.getString("query_COULD_NOT_PREPARE_QUERY_MONGO") + " " + e.getMessage());
		}
	}

	public void doQuery(DB db, DBCollection collection, String whereClause,String sortClause) throws OdaException 
	{
		try {
			BasicDBObject queryObject = (BasicDBObject) JSON.parse(whereClause);
			BasicDBObject sortObject = (BasicDBObject) JSON.parse(sortClause);
			cursor = collection.find(queryObject).sort(sortObject);
		} catch (Exception e) {
			logger.logp(Level.FINER, Query.class.getName(), "doQuery", e.getMessage());
			throw new OdaException(Messages.getString("query_INVALID_JSON_QUERY"));
		}
	}

	/*
	 * @see
	 * org.eclipse.datatools.connectivity.oda.IQuery#setAppContext(java.lang
	 * .Object)
	 */
	public void setAppContext(Object context) throws OdaException {
		// do nothing; assumes no support for pass-through context
	}

	/*
	 * @see org.eclipse.datatools.connectivity.oda.IQuery#close()
	 */
	public void close() throws OdaException {
		m_preparedText = null;
		this.db = null;
		this.collection = null;
		this.filterClause = null;
		this.sortClause = null;
	}

	/*
	 * @see org.eclipse.datatools.connectivity.oda.IQuery#getMetaData()
	 */
	public IResultSetMetaData getMetaData() throws OdaException {

		return new ResultSetMetaData(metadataObject,selectColumns);
	}

	/*
	 * @see org.eclipse.datatools.connectivity.oda.IQuery#executeQuery()
	 */
	public IResultSet executeQuery() throws OdaException {
		
		try
		{
			doQuery(this.db, this.collection, this.filterClause, this.sortClause);
			IResultSet resultSet = new ResultSet(this.cursor, this.metadataObject, this.selectColumns);
			resultSet.setMaxRows(getMaxRows());
			return resultSet;
		}
		catch(Exception e)
		{
			logger.logp(Level.FINER, Query.class.getName(), "executeQuery", e.getMessage());
			throw new OdaException(Messages.getString("query_COULD_NOT_RETRIEVE_RESULTSET"));
		}
	}

	/*
	 * @see
	 * org.eclipse.datatools.connectivity.oda.IQuery#setProperty(java.lang.String
	 * , java.lang.String)
	 */
	public void setProperty(String name, String value) throws OdaException {
		// do nothing; assumes no data set query property
	}

	/*
	 * @see org.eclipse.datatools.connectivity.oda.IQuery#setMaxRows(int)
	 */
	public void setMaxRows(int max) throws OdaException {
		m_maxRows = max;
	}

	/*
	 * @see org.eclipse.datatools.connectivity.oda.IQuery#getMaxRows()
	 */
	public int getMaxRows() throws OdaException {
		return m_maxRows;
	}

	/*
	 * @see org.eclipse.datatools.connectivity.oda.IQuery#clearInParameters()
	 */
	public void clearInParameters() throws OdaException {
		this.filterClause = "";
		this.sortClause = "";
	}

	/*
	 * @see
	 * org.eclipse.datatools.connectivity.oda.IQuery#setInt(java.lang.String,
	 * int)
	 */
	public void setInt(String parameterName, int value) throws OdaException {
		// TODO Auto-generated method stub
		// only applies to named input parameter
	}

	/*
	 * @see org.eclipse.datatools.connectivity.oda.IQuery#setInt(int, int)
	 */
	public void setInt(int parameterId, int value) throws OdaException {
		// TODO Auto-generated method stub
		// only applies to input parameter
	}

	/*
	 * @see
	 * org.eclipse.datatools.connectivity.oda.IQuery#setDouble(java.lang.String,
	 * double)
	 */
	public void setDouble(String parameterName, double value)
			throws OdaException {
		// TODO Auto-generated method stub
		// only applies to named input parameter
	}

	/*
	 * @see org.eclipse.datatools.connectivity.oda.IQuery#setDouble(int, double)
	 */
	public void setDouble(int parameterId, double value) throws OdaException {
		// TODO Auto-generated method stub
		// only applies to input parameter
	}

	/*
	 * @see
	 * org.eclipse.datatools.connectivity.oda.IQuery#setBigDecimal(java.lang
	 * .String, java.math.BigDecimal)
	 */
	public void setBigDecimal(String parameterName, BigDecimal value)
			throws OdaException {
		// TODO Auto-generated method stub
		// only applies to named input parameter
	}

	/*
	 * @see org.eclipse.datatools.connectivity.oda.IQuery#setBigDecimal(int,
	 * java.math.BigDecimal)
	 */
	public void setBigDecimal(int parameterId, BigDecimal value)
			throws OdaException {
		// TODO Auto-generated method stub
		// only applies to input parameter
	}

	/*
	 * @see
	 * org.eclipse.datatools.connectivity.oda.IQuery#setString(java.lang.String,
	 * java.lang.String)
	 */
	public void setString(String parameterName, String value)
			throws OdaException {
		// TODO Auto-generated method stub
		// only applies to named input parameter
	}

	/*
	 * @see org.eclipse.datatools.connectivity.oda.IQuery#setString(int,
	 * java.lang.String)
	 */
	public void setString(int parameterId, String value) throws OdaException {
		if (value == null)
			return;

		if (1 == parameterId) //Where clause parameter
			setFilterClause(value);

		if (2 == parameterId) //Sort clause parameter
			setSortClause(value);
	}

	/*
	 * @see
	 * org.eclipse.datatools.connectivity.oda.IQuery#setDate(java.lang.String,
	 * java.sql.Date)
	 */
	public void setDate(String parameterName, Date value) throws OdaException {
		// TODO Auto-generated method stub
		// only applies to named input parameter
	}

	/*
	 * @see org.eclipse.datatools.connectivity.oda.IQuery#setDate(int,
	 * java.sql.Date)
	 */
	public void setDate(int parameterId, Date value) throws OdaException {
		// TODO Auto-generated method stub
		// only applies to input parameter
	}

	/*
	 * @see
	 * org.eclipse.datatools.connectivity.oda.IQuery#setTime(java.lang.String,
	 * java.sql.Time)
	 */
	public void setTime(String parameterName, Time value) throws OdaException {
		// TODO Auto-generated method stub
		// only applies to named input parameter
	}

	/*
	 * @see org.eclipse.datatools.connectivity.oda.IQuery#setTime(int,
	 * java.sql.Time)
	 */
	public void setTime(int parameterId, Time value) throws OdaException {
		// TODO Auto-generated method stub
		// only applies to input parameter
	}

	/*
	 * @see
	 * org.eclipse.datatools.connectivity.oda.IQuery#setTimestamp(java.lang.
	 * String, java.sql.Timestamp)
	 */
	public void setTimestamp(String parameterName, Timestamp value)
			throws OdaException {
		// TODO Auto-generated method stub
		// only applies to named input parameter
	}

	/*
	 * @see org.eclipse.datatools.connectivity.oda.IQuery#setTimestamp(int,
	 * java.sql.Timestamp)
	 */
	public void setTimestamp(int parameterId, Timestamp value)
			throws OdaException {
		// TODO Auto-generated method stub
		// only applies to input parameter
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.datatools.connectivity.oda.IQuery#setBoolean(java.lang.String
	 * , boolean)
	 */
	public void setBoolean(String parameterName, boolean value)
			throws OdaException {
		// TODO Auto-generated method stub
		// only applies to named input parameter
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.datatools.connectivity.oda.IQuery#setBoolean(int,
	 * boolean)
	 */
	public void setBoolean(int parameterId, boolean value) throws OdaException {
		// TODO Auto-generated method stub
		// only applies to input parameter
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.datatools.connectivity.oda.IQuery#setObject(java.lang.String,
	 * java.lang.Object)
	 */
	public void setObject(String parameterName, Object value)
			throws OdaException {
		// TODO Auto-generated method stub
		// only applies to named input parameter
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.datatools.connectivity.oda.IQuery#setObject(int,
	 * java.lang.Object)
	 */
	public void setObject(int parameterId, Object value) throws OdaException {
		// TODO Auto-generated method stub
		// only applies to input parameter
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.datatools.connectivity.oda.IQuery#setNull(java.lang.String)
	 */
	public void setNull(String parameterName) throws OdaException {
		// TODO Auto-generated method stub
		// only applies to named input parameter
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.datatools.connectivity.oda.IQuery#setNull(int)
	 */
	public void setNull(int parameterId) throws OdaException {
		// TODO Auto-generated method stub
		// only applies to input parameter
	}

	/*
	 * @see
	 * org.eclipse.datatools.connectivity.oda.IQuery#findInParameter(java.lang
	 * .String)
	 */
	public int findInParameter(String parameterName) throws OdaException {
		// TODO Auto-generated method stub
		// only applies to named input parameter
		return 0;
	}

	/*
	 * @see org.eclipse.datatools.connectivity.oda.IQuery#getParameterMetaData()
	 */
	public IParameterMetaData getParameterMetaData() throws OdaException {
		/*
		 * TODO Auto-generated method stub Replace with implementation to return
		 * an instance based on this prepared query.
		 */
		return new ParameterMetaData();
	}

	/*
	 * @see
	 * org.eclipse.datatools.connectivity.oda.IQuery#setSortSpec(org.eclipse
	 * .datatools.connectivity.oda.SortSpec)
	 */
	public void setSortSpec(SortSpec sortBy) throws OdaException {
		// only applies to sorting, assumes not supported
		throw new UnsupportedOperationException();
	}

	/*
	 * @see org.eclipse.datatools.connectivity.oda.IQuery#getSortSpec()
	 */
	public SortSpec getSortSpec() throws OdaException {
		// only applies to sorting
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.datatools.connectivity.oda.IQuery#setSpecification(org.eclipse
	 * .datatools.connectivity.oda.spec.QuerySpecification)
	 */
	public void setSpecification(QuerySpecification querySpec)
			throws OdaException, UnsupportedOperationException {
		// assumes no support
		throw new UnsupportedOperationException();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.datatools.connectivity.oda.IQuery#getSpecification()
	 */
	public QuerySpecification getSpecification() {
		// assumes no support
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.datatools.connectivity.oda.IQuery#getEffectiveQueryText()
	 */
	public String getEffectiveQueryText() {
		// TODO Auto-generated method stub
		return m_preparedText;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.datatools.connectivity.oda.IQuery#cancel()
	 */
	public void cancel() throws OdaException, UnsupportedOperationException {
		// assumes unable to cancel while executing a query
		throw new UnsupportedOperationException();
	}

}
