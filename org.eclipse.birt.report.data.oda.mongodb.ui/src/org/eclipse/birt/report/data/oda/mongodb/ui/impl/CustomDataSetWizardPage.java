/*
 *************************************************************************
 * Copyright (c) 2010 <<Your Company Name here>>
 *  
 *************************************************************************
 */

package org.eclipse.birt.report.data.oda.mongodb.ui.impl;

import java.lang.reflect.InvocationTargetException;
import java.util.Properties;
import java.util.Set;

import org.eclipse.birt.report.data.oda.jdbc.ui.editors.SQLPartitionScanner;
import org.eclipse.birt.report.data.oda.jdbc.ui.editors.SQLSourceViewerConfiguration;
import org.eclipse.birt.report.data.oda.mongodb.impl.Connection;
import org.eclipse.birt.report.data.oda.mongodb.ui.Activator;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.datatools.connectivity.oda.IConnection;
import org.eclipse.datatools.connectivity.oda.IDriver;
import org.eclipse.datatools.connectivity.oda.IParameterMetaData;
import org.eclipse.datatools.connectivity.oda.IQuery;
import org.eclipse.datatools.connectivity.oda.IResultSetMetaData;
import org.eclipse.datatools.connectivity.oda.OdaException;
import org.eclipse.datatools.connectivity.oda.design.DataSetDesign;
import org.eclipse.datatools.connectivity.oda.design.DataSetParameters;
import org.eclipse.datatools.connectivity.oda.design.DesignFactory;
import org.eclipse.datatools.connectivity.oda.design.ParameterDefinition;
import org.eclipse.datatools.connectivity.oda.design.ResultSetColumns;
import org.eclipse.datatools.connectivity.oda.design.ResultSetDefinition;
import org.eclipse.datatools.connectivity.oda.design.ui.designsession.DesignSessionUtil;
import org.eclipse.datatools.connectivity.oda.design.ui.wizards.DataSetWizardPage;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.rules.FastPartitioner;
import org.eclipse.jface.text.source.CompositeRuler;
import org.eclipse.jface.text.source.LineNumberChangeRulerColumn;
import org.eclipse.jface.text.source.LineNumberRulerColumn;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.progress.DeferredTreeContentManager;
import org.eclipse.ui.progress.UIJob;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Combo;

import com.mongodb.DBCollection;

/**
 * Auto-generated implementation of an ODA data set designer page
 * for an user to create or edit an ODA data set design instance.
 * This custom page provides a simple Query Text control for user input.  
 * It further extends the DTP design-time framework to update
 * an ODA data set design instance based on the query's derived meta-data.
 * <br>
 * A custom ODA designer is expected to change this exemplary implementation 
 * as appropriate. 
 */
public class CustomDataSetWizardPage extends DataSetWizardPage
{

    private static final String DEFAULT_QUERY_TEXT = "db.collection.find()";

	private static String DEFAULT_MESSAGE = "Define the MongoDB query for the data set";
    
	private Document doc;
    private transient SourceViewer m_queryTextField;
    private transient TreeViewer m_availableCollectionsTree;
    
    private String formerQueryTxt;
    private String formerDefaultProjection;
    
	private Combo m_defaultProjectionCombo;

    
	/**
     * Constructor
	 * @param pageName
	 * @wbp.parser.constructor
	 */
	public CustomDataSetWizardPage( String pageName )
	{
        super( pageName );
        setTitle( pageName );
        setMessage( DEFAULT_MESSAGE );
	}

	/**
     * Constructor
	 * @param pageName
	 * @param title
	 * @param titleImage
	 */
	public CustomDataSetWizardPage( String pageName, String title,
			ImageDescriptor titleImage )
	{
        super( pageName, title, titleImage );
        setMessage( DEFAULT_MESSAGE );
	}

	/* (non-Javadoc)
	 * @see org.eclipse.datatools.connectivity.oda.design.ui.wizards.DataSetWizardPage#createPageCustomControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createPageCustomControl( Composite parent )
	{
        setControl( createPageControl( parent ) );
        initializeControl();
	}
    
    /**
     * Creates custom control for user-defined query text.
     */
    private Control createPageControl( Composite parent )
    {
        Composite composite = new Composite( parent, SWT.NONE );
        composite.setLayout( new GridLayout( 2, false ) );
        GridData gridData = new GridData( GridData.HORIZONTAL_ALIGN_FILL
                | GridData.VERTICAL_ALIGN_FILL );

        composite.setLayoutData( gridData );
        
        Label lblAvailableCollections = new Label(composite, SWT.NONE);
        lblAvailableCollections.setText("Available Collections:");

        Label fieldLabel = new Label( composite, SWT.NONE );
        fieldLabel.setText( "&Query:" );
        
        m_availableCollectionsTree = new TreeViewer(composite, SWT.BORDER | SWT.VIRTUAL);
        Tree tree = m_availableCollectionsTree.getTree();
        tree.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true, 1, 1));
        m_availableCollectionsTree.setContentProvider(new MongoDbCollectionContentProvider());
        m_availableCollectionsTree.setLabelProvider(new MongoDbCollectionContentProvider.LabelProvider());
        m_availableCollectionsTree.addDoubleClickListener(new IDoubleClickListener() {
			
			public void doubleClick(DoubleClickEvent event) {
				IStructuredSelection selection = (IStructuredSelection) m_availableCollectionsTree.getSelection();
				if (selection.getFirstElement() instanceof IAdaptable) {
					IAdaptable adaptable = (IAdaptable) selection.getFirstElement();
					DBCollection collection = (DBCollection) adaptable.getAdapter(DBCollection.class);
					if (collection != null) {
						String queryText = doc.get().trim();
						if (queryText.startsWith("db.")) {
							int find_index = queryText.indexOf(".find(");
							if (find_index == -1) {
								try {
									doc.replace(3, 0, collection.getName());
								} catch (BadLocationException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							} else {
								try {
									doc.replace(3, find_index-3, collection.getName());
								} catch (BadLocationException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							}
						} else {
							 m_queryTextField.getTextWidget().insert(collection.getName());
						}
						return;
					}
				} else if (selection.getFirstElement() instanceof String) {
					 m_queryTextField.getTextWidget().insert((String) selection.getFirstElement());
					return;
				}
			}
		});
        
        CompositeRuler ruler = new CompositeRuler();
        LineNumberRulerColumn lineNumbers = new LineNumberRulerColumn();
        ruler.addDecorator( 0, lineNumbers);
       
		
        m_queryTextField = new SourceViewer( composite, ruler,
        		SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL );
        
        doc = new Document( );
        
        m_queryTextField.setDocument( doc );
        SourceViewerConfiguration svc = new MongoSourceViewerConfiguration( null, 5000, false);
        m_queryTextField.configure( svc );
        
        FastPartitioner partitioner = new FastPartitioner( new MongoPartitionScanner( ), svc.getConfiguredContentTypes(m_queryTextField) );
		partitioner.connect( doc );
		doc.setDocumentPartitioner( partitioner );
		
        GridData data = new GridData( GridData.FILL_HORIZONTAL );
        data.verticalAlignment = SWT.FILL;
        data.grabExcessVerticalSpace = true;
        data.heightHint = 100;
        m_queryTextField.getControl().setLayoutData( data );
        
        Label lblSchema = new Label(composite, SWT.NONE);
        lblSchema.setText("Schema Mode:");
        new Label(composite, SWT.NONE);
        
        m_defaultProjectionCombo = new Combo(composite, SWT.READ_ONLY);
        m_defaultProjectionCombo.setItems(new String[] {"First Document", "All Keys"});
        m_defaultProjectionCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        
        new Label(composite, SWT.NONE);
        m_queryTextField.getTextWidget().addModifyListener( new ModifyListener( ) 
        {
            public void modifyText( ModifyEvent e )
            {
                validateData();
            }
        } );
       
        
        setPageComplete( false );
        return composite;
    }

	/**
	 * Initializes the page control with the last edited data set design.
	 */
	private void initializeControl( )
	{
        /* 
         * To optionally restore the designer state of the previous design session, use
         *      getInitializationDesignerState(); 
         */

        // Restores the last saved data set design
        final DataSetDesign dataSetDesign = getInitializationDesign();
        if( dataSetDesign == null )
            return; // nothing to initialize

        this.formerQueryTxt = dataSetDesign.getQueryText();
        if( formerQueryTxt == null )
            return; // nothing to initialize

        // initialize control
        if ((formerQueryTxt != null) && (formerQueryTxt.trim().length() > 0)) {
        	 doc.set( formerQueryTxt );
        } else {
        	 doc.set( DEFAULT_QUERY_TEXT );
        }
        validateData();
        setMessage( DEFAULT_MESSAGE );

       
        if (dataSetDesign.getPrivateProperties() != null) {
        	formerDefaultProjection = dataSetDesign.getPrivateProperties().getProperty("DefaultProjection");
        } else {
        	updateDefaultProjection(dataSetDesign, "first");
        	formerDefaultProjection = "first";
        }

        if ("all".equals(formerDefaultProjection)) {
    		m_defaultProjectionCombo.select(1);
    	} else {
            m_defaultProjectionCombo.select(0);
    	}
        
        m_defaultProjectionCombo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (m_defaultProjectionCombo.getSelectionIndex() == 1) {
					updateDefaultProjection(dataSetDesign, "all");
				} else {
					updateDefaultProjection(dataSetDesign, "first");
				}
				m_availableCollectionsTree.setInput(dataSetDesign);
			}
		});

        /*
         * To optionally honor the request for an editable or
         * read-only design session, use
         *      isSessionEditable();
         */
        m_availableCollectionsTree.setInput(dataSetDesign);
	}

    /**
     * Obtains the user-defined query text of this data set from page control.
     * @return query text
     */
    private String getQueryText( )
    {
        return  m_queryTextField.getTextWidget().getText();
    }
    
    /**
     * Obtains the user-defined query text of this data set from page control.
     * @return query text
     */
    private String getDefaultProjection( )
    {
    	if (m_defaultProjectionCombo.getSelectionIndex() == 1) {
			return "all";
		} else {
			return "first";
		}
    }


    private String getDefaultProjection( DataSetDesign design ) {
    	if ((design.getPrivateProperties() == null) || (design.getPrivateProperties().getProperty("DefaultProjection") == null)) {
    		return "first";
    	} else {
    		return design.getPrivateProperties().getProperty("DefaultProjection");
    	}
    }
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.datatools.connectivity.oda.design.ui.wizards.DataSetWizardPage#collectDataSetDesign(org.eclipse.datatools.connectivity.oda.design.DataSetDesign)
	 */
	protected DataSetDesign collectDataSetDesign( DataSetDesign design )
	{
        if( getControl() == null )     // page control was never created
            return design;             // no editing was done
        if( ! hasValidData() )
            return null;    // to trigger a design session error status
        if ( !getQueryText().equals( formerQueryTxt ) || (!getDefaultProjection().equals( formerDefaultProjection )))
		{
        	savePage( design );
        	formerQueryTxt = design.getQueryText( );
        	formerDefaultProjection = getDefaultProjection( design );
		}
        return design;
	}

    /*
     * (non-Javadoc)
     * @see org.eclipse.datatools.connectivity.oda.design.ui.wizards.DataSetWizardPage#collectResponseState()
     */
	protected void collectResponseState( )
	{
		super.collectResponseState( );
		/*
		 * To optionally assign a custom response state, for inclusion in the ODA
		 * design session response, use 
         *      setResponseSessionStatus( SessionStatus status );
         *      setResponseDesignerState( DesignerState customState );
		 */
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.datatools.connectivity.oda.design.ui.wizards.DataSetWizardPage#canLeave()
	 */
	protected boolean canLeave( )
	{
        return isPageComplete();
	}

    /**
     * Validates the user-defined value in the page control exists
     * and not a blank text.
     * Set page message accordingly.
     */
	private void validateData( )
	{
        boolean isValid = ( m_queryTextField != null &&
            getQueryText() != null && getQueryText().trim().length() > 0 );

        if( isValid ) {
        	if ( getQueryText().startsWith("db.")) {
        		setMessage( DEFAULT_MESSAGE );
        	} else {
        		setMessage( "It is recommend that you provide a standard MongoDB query expression", WARNING );
        	}
        } else {
            setMessage( "Requires input value.", ERROR );
        }

		setPageComplete( isValid );
	}

	/**
	 * Indicates whether the custom page has valid data to proceed 
     * with defining a data set.
	 */
	private boolean hasValidData( )
	{
        validateData( );
        
		return canLeave();
	}

	private Connection openConnection() {
		IDriver customDriver = new org.eclipse.birt.report.data.oda.mongodb.impl.OdaMongoDriver();
        
        // obtain and open a live connection
		Connection customConn;
		try {
			customConn = (Connection) customDriver.getConnection( null );
			java.util.Properties connProps = 
		            DesignSessionUtil.getEffectiveDataSourceProperties( 
		                     getInitializationDesign().getDataSourceDesign() );
			customConn.open(connProps);
		} catch (OdaException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			customConn = null;
		}
        
        
        return customConn;
	}
	
	
	/**
     * Saves the user-defined value in this page, and updates the specified 
     * dataSetDesign with the latest design definition.
	 */
	private void savePage( final DataSetDesign dataSetDesign )
	{
        // save user-defined query text
        final String queryText = getQueryText();
        final String defaultProjection = getDefaultProjection();
        
        dataSetDesign.setQueryText( queryText );
        updateDefaultProjection(dataSetDesign, defaultProjection);

        final IRunnableWithProgress updateDataSet = new IRunnableWithProgress() {
			
			public void run(IProgressMonitor monitor) throws InvocationTargetException,
					InterruptedException {
				monitor.beginTask("Updating Data Set Design", IProgressMonitor.UNKNOWN);
				Connection customConn = openConnection();
				try {
					if (customConn != null) {
					    try {
							updateDesign(dataSetDesign, customConn, queryText);
						} catch (OdaException e) {
							throw new InvocationTargetException(e);
						}
					}
				} finally {
					monitor.done();
					if (customConn != null) {
						closeConnection(customConn);
					}
				}
			}
        };
        

        if (this.getOdaWizard().getContainer() != null) {
        	this.getOdaWizard().setNeedsProgressMonitor(true);
        	try {
        		this.getOdaWizard().getContainer().run(true, false, updateDataSet);
        	} catch (InvocationTargetException e) {
        		// not able to get current metadata, reset previous derived metadata
        		dataSetDesign.setResultSets( null );
        		e.printStackTrace();
        	} catch (InterruptedException e) {
        		dataSetDesign.setResultSets( null );
        		e.printStackTrace();
        	}
        } else {
        	Job updateDataSetJob = new Job("Update Data Set") {
        		@Override
        		protected IStatus run(IProgressMonitor monitor) {
        			try {
        				updateDataSet.run(monitor);
        			} catch (InvocationTargetException e) {
        				dataSetDesign.setResultSets( null );
        				return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Failed to update data-set", e);
        			} catch (InterruptedException e) {
        				dataSetDesign.setResultSets( null );
        				return Status.CANCEL_STATUS;
					}
        			return Status.OK_STATUS;
        		}

        	};
        	updateDataSetJob.setUser(true);
        	updateDataSetJob.setPriority(Job.INTERACTIVE);
        	updateDataSetJob.schedule();
        	try {
        	    updateDataSetJob.join();
        	} catch (InterruptedException e) {
        		dataSetDesign.setResultSets( null );
        		e.printStackTrace();
        	}
        }
		
	}

	private void updateDefaultProjection(DataSetDesign dataSetDesign, String value) {
		org.eclipse.datatools.connectivity.oda.design.Properties privateProperites = dataSetDesign.getPrivateProperties();
    	if (privateProperites == null) {
    	    privateProperites = DesignFactory.eINSTANCE.createProperties();
    	}
    	privateProperites.setProperty("DefaultProjection", value);
    	dataSetDesign.setPrivateProperties(privateProperites);
	}
	
	/**
     * Updates the given dataSetDesign with the queryText and its derived metadata
     * obtained from the ODA runtime connection.
     */
    private void updateDesign( DataSetDesign dataSetDesign,
                               IConnection conn, String queryText )
        throws OdaException
    {
    	IQuery query = conn.newQuery( null );
    	query.setProperty("DefaultProjection",  getDefaultProjection(dataSetDesign));
        query.prepare( queryText );
        
        // TODO a runtime driver might require a query to first execute before
        // its metadata is available
//      query.setMaxRows( 1 );
//      query.executeQuery();
        
        try
        {
            IResultSetMetaData md = query.getMetaData();
            updateResultSetDesign( md, dataSetDesign );
        }
        catch( OdaException e )
        {
            // no result set definition available, reset previous derived metadata
            dataSetDesign.setResultSets( null );
            e.printStackTrace();
        }
        
        // proceed to get parameter design definition
        try
        {
            IParameterMetaData paramMd = query.getParameterMetaData();
            updateParameterDesign( paramMd, dataSetDesign );
        }
        catch( OdaException ex )
        {
            // no parameter definition available, reset previous derived metadata
            dataSetDesign.setParameters( null );
            ex.printStackTrace();
        }
        
        /*
         * See DesignSessionUtil for more convenience methods
         * to define a data set design instance.  
         */     
    }

    /**
     * Updates the specified data set design's result set definition based on the
     * specified runtime metadata.
     * @param md    runtime result set metadata instance
     * @param dataSetDesign     data set design instance to update
     * @throws OdaException
     */
	private void updateResultSetDesign( IResultSetMetaData md,
            DataSetDesign dataSetDesign ) 
        throws OdaException
	{
        ResultSetColumns columns = DesignSessionUtil.toResultSetColumnsDesign( md );

        ResultSetDefinition resultSetDefn = DesignFactory.eINSTANCE
                .createResultSetDefinition();
        // resultSetDefn.setName( value );  // result set name
        resultSetDefn.setResultSetColumns( columns );

        // no exception in conversion; go ahead and assign to specified dataSetDesign
        dataSetDesign.setPrimaryResultSet( resultSetDefn );
        dataSetDesign.getResultSets().setDerivedMetaData( true );
	}

    /**
     * Updates the specified data set design's parameter definition based on the
     * specified runtime metadata.
     * @param paramMd   runtime parameter metadata instance
     * @param dataSetDesign     data set design instance to update
     * @throws OdaException
     */
    private void updateParameterDesign( IParameterMetaData paramMd,
            DataSetDesign dataSetDesign ) 
        throws OdaException
    {
        DataSetParameters paramDesign = 
            DesignSessionUtil.toDataSetParametersDesign( paramMd, 
                    DesignSessionUtil.toParameterModeDesign( IParameterMetaData.parameterModeIn ) );
        
        // no exception in conversion; go ahead and assign to specified dataSetDesign
        dataSetDesign.setParameters( paramDesign );        
        if( paramDesign == null )
            return;     // no parameter definitions; done with update
        
        paramDesign.setDerivedMetaData( true );

        for (ParameterDefinition paramDef :  paramDesign.getParameterDefinitions()) {
        	if (paramDef.getAttributes().getName().equals("SortCriteria")) {
        		if (paramDef.getDefaultValues() == null) {
        			paramDef.setDefaultScalarValue("{}");
        		}
        	} else if (paramDef.getAttributes().getName().equals("FilterCriteria")) {
        		if (paramDef.getDefaultValues() == null) {
        			paramDef.setDefaultScalarValue("{}");
        		}
        	}
        }
    }

    /**
     * Attempts to close given ODA connection.
     */
    private void closeConnection( IConnection conn )
    {
        try
        {
            if( conn != null && conn.isOpen() )
                conn.close();
        }
        catch ( OdaException e )
        {
            // ignore
            e.printStackTrace();
        }
    }

}
