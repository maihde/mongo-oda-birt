/*
 *************************************************************************
 * Copyright (c) 2010 Pulak Bose
 *  
 *************************************************************************
 */

package org.eclipse.birt.report.data.oda.mongodb.impl;

import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.datatools.connectivity.oda.IResultSetMetaData;
import org.eclipse.datatools.connectivity.oda.OdaException;

import com.mongodb.DBObject;

/**
 * Implementation class of IResultSetMetaData for an ODA runtime driver.
 */
public class ResultSetMetaData implements IResultSetMetaData {
	private List<String> colHeaders = new ArrayList<String>();
	private List<Integer> colDataType = new ArrayList<Integer>();

	public ResultSetMetaData(DBObject metadataObject, List<String> selectColumns) {

		boolean allColumns = false;
		if (selectColumns.size() == 0) {
			allColumns = true; //all columns need to be selected
		}
		
		//Iterate the metadata object (first row in the collection) and determine column name, data type
		for (String keyField : metadataObject.keySet()) {
			if (allColumns || selectColumns.contains(keyField)) {
				colHeaders.add(keyField);
				Integer dataType = getDataType(metadataObject.get(keyField)
						.toString());
				colDataType.add(dataType);
			}
		}
	}

	private Integer getDataType(String sampleValue) {

		// TODO Uncomment the following if Date data type support is needed.
		/*
		 * try { DateFormat df = new SimpleDateFormat(); Date td =
		 * df.parse(sampleValue); return Types.DATE; } catch (ParseException e1)
		 * {// }
		 */

		// Not a date, check if it is a number
		if (sampleValue.indexOf('.') > 0) {
			try {
				@SuppressWarnings("unused")
				Double tDbl = new Double(sampleValue);
				return Types.DOUBLE;
			} catch (NumberFormatException e) { // do nothing it is not a number
			}
		}

		try {
			@SuppressWarnings("unused")
			Integer tInt = new Integer(sampleValue);
			return Types.INTEGER;
		} catch (NumberFormatException e) {
			// Wasn't an Integer must be a String
			return Types.CHAR;
		}
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
