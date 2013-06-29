package org.eclipse.birt.report.data.oda.mongodb.ui.impl;

import java.util.ArrayList;

import org.eclipse.birt.report.data.oda.jdbc.ui.editors.SQLKeywordRule;
import org.eclipse.birt.report.data.oda.jdbc.ui.util.ColorManager;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.IWhitespaceDetector;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.rules.WhitespaceRule;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;

public class MongoKeywordScanner extends RuleBasedScanner implements IMongoSyntax {

	/**
	 * @param args
	 */
	public MongoKeywordScanner( ) {
		super( );
		IToken mongoKeywordsToken = new Token( new TextAttribute( ColorManager.getColor(127, 0, 85), null, SWT.BOLD ) );
		ArrayList rules = new ArrayList( );
		rules.add( new MongoKeywordRule( mongoKeywordsToken, reservedwords ) );
		rules.add( new MongoKeywordRule( mongoKeywordsToken, types ) );
		rules.add( new MongoKeywordRule( mongoKeywordsToken, constants ) );
		rules.add( new MongoKeywordRule( mongoKeywordsToken, functions ) );
		rules.add( new MongoKeywordRule( mongoKeywordsToken, predicates ) );
		
		// Add generic whitespace rule.
		rules.add( new WhitespaceRule( new IWhitespaceDetector( ) {

			public boolean isWhitespace( char c )
			{
				return Character.isWhitespace( c );
			}
		} ) );

		setRules( (IRule[]) rules.toArray( new IRule[rules.size( )] ) );
		this.setDefaultReturnToken( new Token( new TextAttribute( Display.getDefault( ).getSystemColor( SWT.COLOR_LIST_FOREGROUND ))));
	}

	
}
