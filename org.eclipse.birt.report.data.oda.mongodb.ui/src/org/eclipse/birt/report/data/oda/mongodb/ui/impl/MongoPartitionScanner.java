package org.eclipse.birt.report.data.oda.mongodb.ui.impl;

import java.util.ArrayList;

import org.eclipse.jface.text.rules.EndOfLineRule;
import org.eclipse.jface.text.rules.IPredicateRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.MultiLineRule;
import org.eclipse.jface.text.rules.RuleBasedPartitionScanner;
import org.eclipse.jface.text.rules.Token;

public class MongoPartitionScanner extends RuleBasedPartitionScanner {

	public static final String COMMENT = "mongo_comment"; //$NON-NLS-1$
	
	public static final String QUOTE_STRING = "mongo_quote_string1";
	
	/**
	 *  
	 */
	public MongoPartitionScanner( )
	{
		super( );
		IToken mongoComment = new Token( COMMENT );
		IToken mongoQuoteString = new Token( QUOTE_STRING );

		ArrayList rules = new ArrayList( );
		rules.add( new MultiLineRule( "\"", "\"", mongoQuoteString, '\\' ) ); //$NON-NLS-1$ //$NON-NLS-2$
		rules.add( new MultiLineRule( "\'", "\'", mongoQuoteString, '\\' ) ); //$NON-NLS-1$ //$NON-NLS-2$
		rules.add( new EndOfLineRule( "//", mongoComment ) ); //$NON-NLS-1$
		rules.add( new MultiLineRule( "/*", "*/", mongoComment ) ); //$NON-NLS-1$ //$NON-NLS-2$
		setPredicateRules( (IPredicateRule[]) rules.toArray( new IPredicateRule[rules.size( )] ) );

	}

}
