package org.eclipse.birt.report.data.oda.mongodb.ui.impl;

public interface IMongoSyntax {
	public static final String[] reservedwords = {
		"db", //$NON-NLS-1$
	};

	public static final String[] types = {
	};

	public static final String[] predicates = {
		"$in", //$NON-NLS-1$
		"$elemMatch", //$NON-NLS-1$
		"$or", //$NON-NLS-1$
		"$and", //$NON-NLS-1$
		"$slice", //$NON-NLS-1$
	};

	public static final String[] functions = {
		"find", //$NON-NLS-1$
		"sort", //$NON-NLS-1$
		"limit", //$NON-NLS-1$
		"findOne", //$NON-NLS-1$
		"skip", //$NON-NLS-1$
	};
	
	public static final String[] constants = {

	};
}
