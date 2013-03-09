package org.eclipse.birt.report.data.oda.mongodb.ui.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.birt.report.data.oda.mongodb.impl.Connection;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.datatools.connectivity.oda.IDriver;
import org.eclipse.datatools.connectivity.oda.OdaException;
import org.eclipse.datatools.connectivity.oda.design.DataSetDesign;
import org.eclipse.datatools.connectivity.oda.design.DataSourceDesign;
import org.eclipse.datatools.connectivity.oda.design.ParameterDefinition;
import org.eclipse.datatools.connectivity.oda.design.ui.designsession.DesignSessionUtil;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.ui.progress.DeferredTreeContentManager;
import org.eclipse.ui.progress.IDeferredWorkbenchAdapter;
import org.eclipse.ui.progress.IElementCollector;
import org.eclipse.ui.progress.PendingUpdateAdapter;

import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.MapReduceCommand;
import com.mongodb.MapReduceOutput;

public class MongoDbCollectionContentProvider implements ITreeContentProvider {

	private DeferredTreeContentManager contentManager;
	
	public void dispose() {
		
	}

	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		contentManager = new DeferredTreeContentManager((AbstractTreeViewer) viewer);
	}

	public Object[] getElements(Object inputElement) {
		if (inputElement instanceof DataSetDesign) {
			return new Object[] { new DataSetDesignDeferredWorkbenchAdapter((DataSetDesign) inputElement) };
		} else {
		    return contentManager.getChildren(inputElement);
		}
	}

	public Object[] getChildren(Object parentElement) {
		return contentManager.getChildren(parentElement);
	}

	public Object getParent(Object element) {
		return null;
	}

	public boolean hasChildren(Object element) {
		return contentManager.mayHaveChildren(element);
	}

	public static class LabelProvider extends org.eclipse.jface.viewers.LabelProvider {

		@Override
		public String getText(Object element) {
			if (element instanceof PendingUpdateAdapter) {
				return "Loading...";
			} else if (element instanceof IAdaptable) {
				DataSourceDesign ds = (DataSourceDesign) ((IAdaptable) element).getAdapter(DataSourceDesign.class);
				if (ds != null) {
					return ds.getName();
				}
				
				DBCollection collection = (DBCollection)  ((IAdaptable) element).getAdapter(DBCollection.class);
				if (collection != null) {
					return collection.getName();
				}
			}
			return element.toString();
		}
		
	}
	
	class DataSetDesignDeferredWorkbenchAdapter implements IAdaptable, IDeferredWorkbenchAdapter {

		private Connection connection;
		private ArrayList<DBCollection> collections;
		private DataSetDesign dset;
		private DataSourceDesign ds;
		
		public DataSetDesignDeferredWorkbenchAdapter(DataSetDesign dset) {
			this.dset = dset;
			this.ds = dset.getDataSourceDesign();
			this.collections = new ArrayList<DBCollection>();
		};
		
		public Object[] getChildren(Object o) {
			if (o == ds) {
				this.collections.toArray();
			}
			return new Object[0];
		}

		public ImageDescriptor getImageDescriptor(Object object) {
			// TODO Auto-generated method stub
			return null;
		}

		public String getLabel(Object o) {
			if (o instanceof DataSourceDesign) {
				return ((DataSourceDesign) o).getName();
			} else if (o instanceof DBCollection) {
				return ((DBCollection) o).getName();
			}
			return o.toString();
		}

		public Object getParent(Object o) {
			if (o instanceof DBCollection) {
				return this.ds;
			}
			return null;
		}

		public void fetchDeferredChildren(Object object, IElementCollector collector, IProgressMonitor monitor) {
			SubMonitor progress = SubMonitor.convert(monitor);
			progress.setWorkRemaining(2);
			
			IDriver customDriver = new org.eclipse.birt.report.data.oda.mongodb.impl.OdaMongoDriver();
	        
	        // obtain and open a live connection
			try {
				connection = (Connection) customDriver.getConnection( null );
				java.util.Properties connProps = 
			            DesignSessionUtil.getEffectiveDataSourceProperties(ds);
				connection.open(connProps);
			} catch (OdaException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				connection = null;
			}
			progress.worked(1);
			
			Set<String> collectionNames = connection.getDB().getCollectionNames();
			progress.setWorkRemaining(collectionNames.size());
			for (String collection : collectionNames) {
				if (progress.isCanceled()) { break; }
				
				DBCollection dbcollection = connection.getDB().getCollection(collection);
				IDeferredWorkbenchAdapter dwa = new DBCollectionDeferredWorkbenchAdapter(dbcollection, schemaMode());
				collections.add(dbcollection);
				collector.add(dwa, progress.newChild(1));
			}
			collector.done();
		}

		public boolean isContainer() {
			return true;
		}

		public ISchedulingRule getRule(Object object) {
			// TODO Auto-generated method stub
			return null;
		}

		public Object getAdapter(Class adapter) {
			if (adapter == DataSourceDesign.class) {
				return ds;
			}
			return null;
		}
		
		private String schemaMode() {
			if (dset.getPrivateProperties() != null) {
				String defaultProjection = dset.getPrivateProperties().getProperty("DefaultProjection");
				if (defaultProjection != null) {
					return defaultProjection;
				}
			}
			return "first";
		}
	};
	
	class DBCollectionDeferredWorkbenchAdapter  implements IAdaptable,  IDeferredWorkbenchAdapter {

		private DBCollection collection;
		private Set<String> keys;
		private String schmeaMode;
		
		public DBCollectionDeferredWorkbenchAdapter(DBCollection collection, String schemaMode) {
			this.collection = collection;
			this.keys = new HashSet<String>();
			this.schmeaMode = schemaMode;
		};
		
		public Object[] getChildren(Object o) {
			if (o == collection) {
				this.keys.toArray();
			}
			return new Object[0];
		}

		public ImageDescriptor getImageDescriptor(Object object) {
			// TODO Auto-generated method stub
			return null;
		}

		public String getLabel(Object o) {
			if (o == collection) {
				return collection.getName();
			}
			return o.toString();
		}

		public Object getParent(Object o) {
			return collection;
		}

		public void fetchDeferredChildren(Object object, IElementCollector collector, IProgressMonitor monitor) {
			SubMonitor progress = SubMonitor.convert(monitor);
			progress.setWorkRemaining(1);
			if ("all".equals(this.schmeaMode)) {
				MapReduceOutput mro = collection.mapReduce(
						"function() { for (var key in this) { emit(key, null); } }",
	                    "function(key, stuff) { return null; }",
	                    null,
	                    MapReduceCommand.OutputType.INLINE,
	                    null);
				Set<String> keys = new HashSet<String>();
				for ( DBObject obj : mro.results() ) {
					if (progress.isCanceled()) { break; }
					keys.add(obj.get("_id").toString());
				}
				collector.add(keys.toArray(), progress.newChild(1));
				collector.done();
			} else {
				DBObject obj = collection.findOne();
				if (obj != null) {
					keys = obj.keySet();
					collector.add(keys.toArray(), progress.newChild(1));
					collector.done();
				}
			}
		}

		public boolean isContainer() {
			return true;
		}

		public ISchedulingRule getRule(Object object) {
			// TODO Auto-generated method stub
			return null;
		}

		public Object getAdapter(Class adapter) {
			if (adapter == DBCollection.class) {
				return collection;
			}
			return null;
		}
		
	};
}
