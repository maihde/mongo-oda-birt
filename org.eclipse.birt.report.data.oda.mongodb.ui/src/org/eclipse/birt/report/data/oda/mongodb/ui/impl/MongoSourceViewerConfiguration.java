package org.eclipse.birt.report.data.oda.mongodb.ui.impl;

import org.eclipse.birt.report.data.oda.jdbc.ui.editors.JdbcSQLContentAssistProcessor;
import org.eclipse.birt.report.data.oda.jdbc.ui.editors.NonRuleBasedDamagerRepairer;
import org.eclipse.birt.report.data.oda.jdbc.ui.editors.SQLKeywordScanner;
import org.eclipse.birt.report.data.oda.jdbc.ui.editors.SQLPartitionScanner;
import org.eclipse.birt.report.data.oda.jdbc.ui.util.ColorManager;
import org.eclipse.datatools.connectivity.oda.design.DataSourceDesign;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;

public class MongoSourceViewerConfiguration extends SourceViewerConfiguration {
	
	private static final TextAttribute quoteString = new TextAttribute( ColorManager.getColor(42, 0, 255) ) ;
	private static final TextAttribute comment = new TextAttribute( ColorManager.getColor(63, 127, 95) ) ;
	private static final TextAttribute collection = new TextAttribute( ColorManager.getColor(255, 0, 0) ) ;
	private static final TextAttribute find = new TextAttribute( ColorManager.getColor(0, 255, 0) ) ;
	private DataSourceDesign dsd;
	private long timeout;
	private boolean enableCodeAssist;
	
	public static final String[] CONFIGURED_TYPES = new String[] {
		MongoPartitionScanner.QUOTE_STRING,
		MongoPartitionScanner.COMMENT,
		IDocument.DEFAULT_CONTENT_TYPE 
	};
	/**
	 *  
	 */
	public MongoSourceViewerConfiguration( DataSourceDesign dsd, long timeout, boolean enableCodeAssist )
	{
		super( );
		this.dsd = dsd;
		this.timeout = timeout;
		this.enableCodeAssist = enableCodeAssist;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.text.source.SourceViewerConfiguration#getPresentationReconciler(org.eclipse.jface.text.source.ISourceViewer)
	 */
	public IPresentationReconciler getPresentationReconciler(
			ISourceViewer sourceViewer )
	{
		PresentationReconciler reconciler = new PresentationReconciler( );
		
		NonRuleBasedDamagerRepairer dr = new NonRuleBasedDamagerRepairer( quoteString );
		reconciler.setDamager( dr, MongoPartitionScanner.QUOTE_STRING );
		reconciler.setRepairer( dr, MongoPartitionScanner.QUOTE_STRING );
		
		
		dr = new NonRuleBasedDamagerRepairer( comment );
		reconciler.setDamager( dr, MongoPartitionScanner.COMMENT );
		reconciler.setRepairer( dr, MongoPartitionScanner.COMMENT );

		DefaultDamagerRepairer  ddr = new DefaultDamagerRepairer( new MongoKeywordScanner( ) );
		reconciler.setDamager( ddr, IDocument.DEFAULT_CONTENT_TYPE );
		reconciler.setRepairer( ddr, IDocument.DEFAULT_CONTENT_TYPE );

		return reconciler;
	}

	@Override
	public String[] getConfiguredContentTypes( ISourceViewer sourceViewer )
	{
		return CONFIGURED_TYPES;
	}
	

	public IContentAssistant getContentAssistant( ISourceViewer sourceViewer )
	{
		return null;
//		if ( !enableCodeAssist )
//		{
//			return null;
//		}
//		ContentAssistant assistant = new ContentAssistant( );
//		JdbcSQLContentAssistProcessor contentAssist = new JdbcSQLContentAssistProcessor( timeout );
//		contentAssist.setDataSourceHandle( dsd );
//		assistant.setContentAssistProcessor( contentAssist,
//				IDocument.DEFAULT_CONTENT_TYPE );
//		assistant.enableAutoActivation( true );
//		assistant.setAutoActivationDelay( 500 );
//		assistant.setProposalPopupOrientation( IContentAssistant.PROPOSAL_OVERLAY );
//		return assistant;
	}
}
