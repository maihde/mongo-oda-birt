/*
 *************************************************************************
 * Copyright (c) 2010 Pulak Bose
 *  
 *************************************************************************
 */

package org.eclipse.birt.report.data.oda.mongodb.impl;

import java.sql.Types;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.eclipse.datatools.connectivity.oda.IResultSetMetaData;
import org.eclipse.datatools.connectivity.oda.OdaException;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

/**
 * Implementation class of IResultSetMetaData for an ODA runtime driver.
 */
public class ResultSetMetaData implements IResultSetMetaData {
	private List<String> colHeaders = new ArrayList<String>();
	private List<Integer> colDataType = new ArrayList<Integer>();

	public ResultSetMetaData(DBObject metadataObject, Set<String> selectColumns) {

		if ((selectColumns == null) || (selectColumns.size() == 0)) {
			selectColumns = metadataObject.keySet();
		}
		
		//Iterate the metadata object (first row in the collection) and determine column name, data type
		for (String keyField : selectColumns) {
			colHeaders.add(keyField);
			Integer dataType = getDataType(metadataObject.get(keyField));
			colDataType.add(dataType);
		}
	}

	private Integer getDataType(Object sampleValue) {
		if (sampleValue == null) {
			return Types.CHAR; // assume string, the user can always override it
		}
		
		if (sampleValue instanceof Date) {
			return Types.DATE;
		} else if (sampleValue instanceof Double) {
			return Types.DOUBLE;
		} else if (sampleValue instanceof Integer) {
			return Types.INTEGER;
		}
		return Types.CHAR;
	}

	/*
	 * @see
	 * org.eclipse.datatools.connectivity.oda.IResultSetMetaData#getColumnCount
	 * ()
	 */
	public int getColumnCount() throws OdaException {
		return colHeaders.size();
	}

	/*
	 * @see
	 * org.eclipse.datatools.connectivity.oda.IResultSetMetaData#getColumnName
	 * (int)
	 */
	public String getColumnName(int index) throws OdaException {
		return colHeaders.get(index - 1);
	}

	/*
	 * @see
	 * org.eclipse.datatools.connectivity.oda.IResultSetMetaData#getColumnLabel
	 * (int)
	 */
	public String getColumnLabel(int index) throws OdaException {
		return getColumnName(index); // default
	}

	/*
	 * @see
	 * org.eclipse.datatools.connectivity.oda.IResultSetMetaData#getColumnType
	 * (int)
	 */
	public int getColumnType(int index) throws OdaException {
		// return java.sql.Types.CHAR;
		return colDataType.get(index - 1);
	}

	/*
	 * @see
	 * org.eclipse.datatools.connectivity.oda.IResultSetMetaData#getColumnTypeName
	 * (int)
	 */
	public String getColumnTypeName(int index) throws OdaException {
		int nativeTypeCode = getColumnType(index);
		return OdaMongoDriver.getNativeDataTypeName(nativeTypeCode);
	}

	/*
	 * @see org.eclipse.datatools.connectivity.oda.IResultSetMetaData#
	 * getColumnDisplayLength(int)
	 */
	public int getColumnDisplayLength(int index) throws OdaException {
		return 8;
	}

	/*
	 * @see
	 * org.eclipse.datatools.connectivity.oda.IResultSetMetaData#getPrecision
	 * (int)
	 */
	public int getPrecision(int index) throws OdaException {
		// TODO Auto-generated method stub
		return -1;
	}

	/*
	 * @see
	 * org.eclipse.datatools.connectivity.oda.IResultSetMetaData#getScale(int)
	 */
	public int getScale(int index) throws OdaException {
		// TODO Auto-generated method stub
		return -1;
	}

	/*
	 * @see
	 * org.eclipse.datatools.connectivity.oda.IResultSetMetaData#isNullable(int)
	 */
	public int isNullable(int index) throws OdaException {
		// TODO Auto-generated method stub
		return IResultSetMetaData.columnNullableUnknown;
	}

}
