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
import java.util.HashSet;
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
import com.mongodb.MapReduceOutput;
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
	
	private DBCursor cursor = null;
	private DBObject metadataObject = null;
	private DBCollection collection = null;

	private boolean legacyMode = false;
	
	private DBObject defaultFilterClause;
	private DBObject defaultSortClause;
	
	private DBObject filterClause;
	private DBObject projectionClause;
	private DBObject sortClause;

	private QuerySpecification querySpec;
	
	public Query(DB db) {
		this.db = db;
	}

	public void setFilterClause(String filterClause) {
		this.filterClause = (DBObject) JSON.parse(filterClause);
	}

	public void setSortClause(String sortClause) {
		this.sortClause = (DBObject) JSON.parse(sortClause);
	}

	/*
	 * @see
	 * org.eclipse.datatools.connectivity.oda.IQuery#prepare(java.lang.String)
	 */
	public void prepare(String queryText) throws OdaException {
		try {
			m_preparedText = queryText.trim();
			selectColumns = new ArrayList<String>();
			
			if (m_preparedText.startsWith("db.")) {
				int find_index = m_preparedText.indexOf(".find(");
				if (find_index == -1) {
					throw new OdaException(Messages.getString("query_INVALID_FIND_STATEMENT"));
				}
				
				if (!m_preparedText.endsWith(")")) {
					throw new OdaException(Messages.getString("query_INVALID_FIND_STATEMENT"));
				}
				
				String collection_name = m_preparedText.substring(3, find_index);
				Set<String> collections = db.getCollectionNames();
				if (collections.contains(collection_name))
				{
					collection = db.getCollection(collection_name);	
				}
				else
				{
					throw new OdaException(Messages.getString("query_INVALID_COLLECTION_NAME"));
				}
				
				int query_end = m_preparedText.indexOf(")", find_index+6);
				String find_statement =  m_preparedText.substring(find_index+6, query_end);
				try {
				    List<DBObject> fo = (List<DBObject>) JSON.parse("[" + find_statement + "]");
				    if (fo.size() > 2) {
						throw new OdaException(Messages.getString("query_INVALID_FIND_QUERY"));
					}
					
					if (fo.size() >= 1) {
						defaultFilterClause = fo.get(0);
					}
					if (fo.size() == 2) {
						projectionClause = fo.get(1);
					}
				} catch (RuntimeException e) {
					throw new OdaException(Messages.getString("query_INVALID_FIND_QUERY"));
				}
				
				if (projectionClause == null) {
				    metadataObject = collection.findOne(defaultFilterClause); //Get the first row to help determine the data types
				} else {
					 metadataObject = collection.findOne(defaultFilterClause, projectionClause); //Get the first row to help determine the data types
				}
				// If the query didn't find anything at all,just grab anything
				if (metadataObject == null) {
					metadataObject = collection.findOne(null, projectionClause);
				}
				
				// If the query is still empty, so is the database
				if (metadataObject == null) {
					throw new OdaException(Messages.getString("query_INVALID_METADATA"));
				}
				
				// If the user didn't provide a projection-clause, then grab all columns
				if (projectionClause == null) {
					// Get the list of all columns in the database
					MapReduceOutput mro = collection.mapReduce("function() { for (var key in this) { emit(key, null); } }",
							                                   "function(key, stuff) { return null; }",
							                                   "{out: {inline: 1}}",
							                                   null);
					selectColumns = mro.getOutputCollection().distinct("_id");
				}
			} else {
				legacyPrepare(queryText);
			}
			
		} catch (Exception e) {
			logger.logp(Level.FINER, Query.class.getName(), "prepare", e.getMessage());
			throw new OdaException(
					Messages.getString("query_COULD_NOT_PREPARE_QUERY_MONGO") + " " + e.getMessage());
		}
	}

	protected void legacyPrepare(String queryText) throws OdaException {
		// Fall back to legacy behavior
		legacyMode = true;
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

		metadataObject = collection.findOne(); //Get the first row to help determine the data types
		if (queryStringArray.length > 1) { //Check whether further tokens exists in the query text
			selectColumns = Arrays.asList( queryStringArray[1].split(",")); //Split again to get the select column names
		} else {
			// Get the list of all columns in the database
			MapReduceOutput mro = collection.mapReduce("function() { for (var key in this) { emit(key, null); } }",
					                                   "function(key, stuff) { return null; }",
					                                   "{out: {inline: 1}}",
					                                   null);
			selectColumns = mro.getOutputCollection().distinct("_id");
		}
		
		if (metadataObject == null)
			throw new OdaException(Messages.getString("query_INVALID_METADATA"));
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
		this.projectionClause = null;
	}

	/*
	 * @see org.eclipse.datatools.connectivity.oda.IQuery#getMetaData()
	 */
	public IResultSetMetaData getMetaData() throws OdaException {
        return new ResultSetMetaData(metadataObject, new HashSet(selectColumns));
	}

	/*
	 * @see org.eclipse.datatools.connectivity.oda.IQuery#executeQuery()
	 */
	public IResultSet executeQuery() throws OdaException {
		
		if (filterClause == null) filterClause = defaultFilterClause;
		if (sortClause == null) sortClause = defaultSortClause;
		
		try
		{
			if (projectionClause == null) {
				cursor = collection.find(filterClause);
			} else {
				cursor = collection.find(filterClause, projectionClause); //Get the first row to help determine the data types
			}
			if (sortClause != null) {
				cursor = cursor.sort(sortClause);
			}
			
			IResultSet resultSet = new ResultSet(this.cursor, getMetaData());
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
		if (this.legacyMode) {
			this.filterClause = defaultFilterClause;
			this.sortClause = defaultSortClause;
		}
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
		if (value == null)
			return;

        if (legacyMode) {
			if (parameterName.equals("FilterCriteria")) //Where clause parameter
				setFilterClause(value);
	
			if (parameterName.equals("SortCriteria")) //Sort clause parameter
				setSortClause(value);
        }
	}

	/*
	 * @see org.eclipse.datatools.connectivity.oda.IQuery#setString(int,
	 * java.lang.String)
	 */
	public void setString(int parameterId, String value) throws OdaException {
		if (value == null)
			return;

        if (legacyMode) {
			if (1 == parameterId) //Where clause parameter
				setFilterClause(value);
	
			if (2 == parameterId) //Sort clause parameter
				setSortClause(value);
        }
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
		return new ParameterMetaData(legacyMode);
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
		this.querySpec = querySpec;
		
		if ((querySpec.getProperty("FilterCriteria") != null) && (querySpec.getProperty("FilterCriteria").toString().trim().length() > 0)) {
			String filterCriteria = querySpec.getProperty("FilterCriteria").toString().trim();
			try {
				if (filterCriteria.startsWith("(") && filterCriteria.endsWith(")")) {
					filterCriteria = filterCriteria.substring(1, filterCriteria.length()-1);
				}
				filterClause = (DBObject) JSON.parse(filterCriteria);
			} catch (RuntimeException e) {
				throw new OdaException(Messages.getString("query_INVALID_FILTERCRITERIA"));
			}
		} else {
			filterClause = null;
		}
		
		if ((querySpec.getProperty("SortCriteria") != null) && (querySpec.getProperty("SortCriteria").toString().trim().length() > 0)) {
			String sortCriteria = querySpec.getProperty("SortCriteria").toString();
			try {
				if (sortCriteria.startsWith("(") && sortCriteria.endsWith(")")) {
					sortCriteria = sortCriteria.substring(1, sortCriteria.length()-1);
				}
				sortClause = (DBObject) JSON.parse(sortCriteria);
			} catch (RuntimeException e) {
				throw new OdaException(Messages.getString("query_INVALID_SORTCRITERIA"));
			}
		} else {
			sortClause = defaultSortClause;
		}
		
		if (legacyMode) {
			if (querySpec.getParameterValue(1) != null) {
				String filterCriteria = querySpec.getParameterValue(1).toString().trim();
				try {
				    filterClause = (DBObject) JSON.parse(filterCriteria);
				} catch (RuntimeException e) {
					throw new OdaException(Messages.getString("query_INVALID_FILTERCRITERIA"));
				}  
			}
			if (querySpec.getParameterValue(2) != null) {
				String sortCriteria = querySpec.getParameterValue(2).toString();
				try {
				    filterClause = (DBObject) JSON.parse(sortCriteria);
				} catch (RuntimeException e) {
					throw new OdaException(Messages.getString("query_INVALID_SORTCRITERIA"));
				}  
			}
		}
		
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.datatools.connectivity.oda.IQuery#getSpecification()
	 */
	public QuerySpecification getSpecification() {
		return this.querySpec;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.datatools.connectivity.oda.IQuery#getEffectiveQueryText()
	 */
	public String getEffectiveQueryText() {
		// TODO Auto-generated method stub
		if (legacyMode) {
		    return m_preparedText;
		} else {
			StringBuilder sb = new StringBuilder();
			sb.append("db.");
			sb.append(this.collection.getName());
			sb.append(".find(");
			if (this.filterClause != null) {
				sb.append(this.filterClause.toString());
			} else {
				sb.append("{ }");
			}
			if (this.projectionClause != null) {
				sb.append(", ");
				sb.append(this.projectionClause.toString());
			}
			sb.append(")");
			if (this.sortClause != null) {
				sb.append(".sort(");
				sb.append(this.sortClause.toString());
				sb.append(")");
			}
			return sb.toString();
		}
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
