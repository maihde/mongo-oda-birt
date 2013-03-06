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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;

import org.eclipse.datatools.connectivity.oda.IBlob;
import org.eclipse.datatools.connectivity.oda.IClob;
import org.eclipse.datatools.connectivity.oda.IResultSet;
import org.eclipse.datatools.connectivity.oda.IResultSetMetaData;
import org.eclipse.datatools.connectivity.oda.OdaException;

import com.mongodb.DBCursor;
import com.mongodb.DBObject;

/**
 * Implementation class of IResultSet for an ODA runtime driver.
 */
public class ResultSet implements IResultSet
{
	private int m_maxRows;
    private int m_currentRowId;
    private IResultSetMetaData resultSetMetaData;
    private DBCursor cursor;
    private DBObject curRow;
    private boolean wasNull;
    
    public ResultSet(DBCursor cursor, IResultSetMetaData resultSetMetaData)
    {	
    	this.cursor = cursor;
    	this.resultSetMetaData = resultSetMetaData;
    	this.wasNull = false;
    }
    
	/*
	 * @see org.eclipse.datatools.connectivity.oda.IResultSet#getMetaData()
	 */
	public IResultSetMetaData getMetaData() throws OdaException
	{
		return resultSetMetaData;
	}

	/*
	 * @see org.eclipse.datatools.connectivity.oda.IResultSet#setMaxRows(int)
	 */
	public void setMaxRows( int max ) throws OdaException
	{
		m_maxRows = max;
		if (m_maxRows > 0) {
		    this.cursor = this.cursor.limit(m_maxRows);
		}
	}
	
	/**
	 * Returns the maximum number of rows that can be fetched from this result set.
	 * @return the maximum number of rows to fetch.
	 */
	protected int getMaxRows()
	{
		return m_maxRows;
	}

	/*
	 * @see org.eclipse.datatools.connectivity.oda.IResultSet#next()
	 */
	public boolean next() throws OdaException
	{
        int maxRows = getMaxRows();
        if (maxRows > 0 && m_currentRowId > maxRows){
            return false;
        }
        
        while(cursor.hasNext()) {
        	curRow = cursor.next();
        	m_currentRowId++;
        	return true;
        }      
        
        return false;        
	}

	/*
	 * @see org.eclipse.datatools.connectivity.oda.IResultSet#close()
	 */
	public void close() throws OdaException
	{
        // TODO Auto-generated method stub       
        m_currentRowId = 0;     // reset row counter
	}

	/*
	 * @see org.eclipse.datatools.connectivity.oda.IResultSet#getRow()
	 */
	public int getRow() throws OdaException
	{
		return m_currentRowId;
	}

	/*
	 * @see org.eclipse.datatools.connectivity.oda.IResultSet#getString(int)
	 */
	public String getString( int index ) throws OdaException
	{
		String colName = this.resultSetMetaData.getColumnName(index);
        return getString(colName);
	}

	/*
	 * @see org.eclipse.datatools.connectivity.oda.IResultSet#getString(java.lang.String)
	 */
	public String getString( String columnName ) throws OdaException
	{
		Object value = curRow.get(columnName); 
		wasNull = (value == null);
        return (value==null)? "" : value.toString();
	}

	/*
	 * @see org.eclipse.datatools.connectivity.oda.IResultSet#getInt(int)
	 */
	public int getInt( int index ) throws OdaException
	{
		String colName = this.resultSetMetaData.getColumnName(index);
		return getInt(colName);
	}

	/*
	 * @see org.eclipse.datatools.connectivity.oda.IResultSet#getInt(java.lang.String)
	 */
	public int getInt( String columnName ) throws OdaException
	{
		Object value = curRow.get(columnName);
		wasNull = (value == null);
		try
		{
			return (value==null)? 0 : new Integer(value.toString());
		}
		catch(NumberFormatException ne)
		{
			//Second level check since Mongo is schema free and can accomodate different data types in the same column
			return 0;
		}
	}

	/*
	 * @see org.eclipse.datatools.connectivity.oda.IResultSet#getDouble(int)
	 */
	public double getDouble( int index ) throws OdaException
	{
		String colName = this.resultSetMetaData.getColumnName(index);
		return getDouble(colName);
	}

	/*
	 * @see org.eclipse.datatools.connectivity.oda.IResultSet#getDouble(java.lang.String)
	 */
	public double getDouble( String columnName ) throws OdaException
	{
		Object value = curRow.get(columnName);
		wasNull = (value == null);
        try
        {
		return (value==null)? 0.0 : new Double(value.toString());
        }
        catch(NumberFormatException ne)
        {
        	//Second level check since Mongo DB is schema free and can accommodate different data types in the same column
			return 0.0;
        }        
	}

	/*
	 * @see org.eclipse.datatools.connectivity.oda.IResultSet#getBigDecimal(int)
	 */
	public BigDecimal getBigDecimal( int index ) throws OdaException
	{
		throw new UnsupportedOperationException();
	}

	/*
	 * @see org.eclipse.datatools.connectivity.oda.IResultSet#getBigDecimal(java.lang.String)
	 */
	public BigDecimal getBigDecimal( String columnName ) throws OdaException
	{
		throw new UnsupportedOperationException();
	}

	/*
	 * @see org.eclipse.datatools.connectivity.oda.IResultSet#getDate(int)
	 */
	public Date getDate( int index ) throws OdaException
	{
		String colName = this.resultSetMetaData.getColumnName(index);
		return getDate(colName);
	}

	/*
	 * @see org.eclipse.datatools.connectivity.oda.IResultSet#getDate(java.lang.String)
	 */
	public Date getDate( String columnName ) throws OdaException
	{
		if (curRow.get(columnName) == null) {
			wasNull = true;
			return null;
		}

		try {
			
			if (curRow.get(columnName) instanceof java.util.Date) {
				return new java.sql.Date(((java.util.Date) curRow.get(columnName)).getTime());
			} else {
			    return new java.sql.Date(new SimpleDateFormat().parse(curRow.get(columnName).toString()).getTime());
			}
		} catch (ParseException e) {
			throw new UnsupportedOperationException();
		}
	}

	/*
	 * @see org.eclipse.datatools.connectivity.oda.IResultSet#getTime(int)
	 */
	public Time getTime( int index ) throws OdaException
	{
        // TODO Auto-generated method stub
		String colName = this.resultSetMetaData.getColumnName(index);
		return getTime(colName);
	}

	/*
	 * @see org.eclipse.datatools.connectivity.oda.IResultSet#getTime(java.lang.String)
	 */
	public Time getTime( String columnName ) throws OdaException
	{
		if (curRow.get(columnName) == null) {
			wasNull = true;
			return null;
		}

		try {
			
			if (curRow.get(columnName) instanceof java.util.Date) {
				return new java.sql.Time(((java.util.Date) curRow.get(columnName)).getTime());
			} else {
			    return new java.sql.Time(new SimpleDateFormat().parse(curRow.get(columnName).toString()).getTime());
			}
		} catch (ParseException e) {
			throw new UnsupportedOperationException();
		}
	}

	/*
	 * @see org.eclipse.datatools.connectivity.oda.IResultSet#getTimestamp(int)
	 */
	public Timestamp getTimestamp( int index ) throws OdaException
	{
        // TODO Auto-generated method stub
		String colName = this.resultSetMetaData.getColumnName(index);
		return getTimestamp(colName);
	}

	/*
	 * @see org.eclipse.datatools.connectivity.oda.IResultSet#getTimestamp(java.lang.String)
	 */
	public Timestamp getTimestamp( String columnName ) throws OdaException
	{
		if (curRow.get(columnName) == null) {
			wasNull = true;
			return null;
		}

		try {
			
			if (curRow.get(columnName) instanceof java.util.Date) {
				return new java.sql.Timestamp(((java.util.Date) curRow.get(columnName)).getTime());
			} else {
			    return new java.sql.Timestamp(new SimpleDateFormat().parse(curRow.get(columnName).toString()).getTime());
			}
		} catch (ParseException e) {
			throw new UnsupportedOperationException();
		}
	}

    /* 
     * @see org.eclipse.datatools.connectivity.oda.IResultSet#getBlob(int)
     */
    public IBlob getBlob( int index ) throws OdaException
    {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    /* 
     * @see org.eclipse.datatools.connectivity.oda.IResultSet#getBlob(java.lang.String)
     */
    public IBlob getBlob( String columnName ) throws OdaException
    {
    	throw new UnsupportedOperationException();
    }

    /* 
     * @see org.eclipse.datatools.connectivity.oda.IResultSet#getClob(int)
     */
    public IClob getClob( int index ) throws OdaException
    {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    /* 
     * @see org.eclipse.datatools.connectivity.oda.IResultSet#getClob(java.lang.String)
     */
    public IClob getClob( String columnName ) throws OdaException
    {
    	throw new UnsupportedOperationException();
    }

    /* (non-Javadoc)
     * @see org.eclipse.datatools.connectivity.oda.IResultSet#getBoolean(int)
     */
    public boolean getBoolean( int index ) throws OdaException
    {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    /* (non-Javadoc)
     * @see org.eclipse.datatools.connectivity.oda.IResultSet#getBoolean(java.lang.String)
     */
    public boolean getBoolean( String columnName ) throws OdaException
    {
    	throw new UnsupportedOperationException();
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.datatools.connectivity.oda.IResultSet#getObject(int)
     */
    public Object getObject( int index ) throws OdaException
    {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    /* (non-Javadoc)
     * @see org.eclipse.datatools.connectivity.oda.IResultSet#getObject(java.lang.String)
     */
    public Object getObject( String columnName ) throws OdaException
    {
    	throw new UnsupportedOperationException();
    }

    /*
     * @see org.eclipse.datatools.connectivity.oda.IResultSet#wasNull()
     */
    public boolean wasNull() throws OdaException
    {
        return wasNull;
    }

    /*
     * @see org.eclipse.datatools.connectivity.oda.IResultSet#findColumn(java.lang.String)
     */
    public int findColumn( String columnName ) throws OdaException
    {
        // TODO replace with data source specific implementation
        
        // hard-coded for demo purpose
        int columnId = 1;   // dummy column id
        if( columnName == null || columnName.length() == 0 )
            return columnId;
        String lastChar = columnName.substring( columnName.length()-1, 1 );
        try
        {
            columnId = Integer.parseInt( lastChar );
        }
        catch( NumberFormatException e )
        {
            // ignore, use dummy column id
        }
        return columnId;
    }
    
}
