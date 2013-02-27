package org.eclipse.pde.apitools.ant.internal;

import java.io.ByteArrayInputStream;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.api.tools.internal.provisional.comparator.IDelta;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.apitools.ant.tasks.AbstractFilterListDeltaVisitor;
import org.eclipse.pde.apitools.ant.tasks.FilterListDeltaVisitor;
import org.eclipse.pde.apitools.ant.util.XMLMemento;

public class DeltaReport extends RootReport {
	public static final int DEPRECATION = AbstractFilterListDeltaVisitor.CHECK_DEPRECATION;
	public static final int OTHER = AbstractFilterListDeltaVisitor.CHECK_OTHER;
	public static final int ALL = AbstractFilterListDeltaVisitor.CHECK_ALL;

	
	private IDelta delta;
	private IApiComponent[] included;
	private int flags;
	public DeltaReport(IDelta delta, IApiComponent[] included, int flags) {
		super("deltas");
		this.delta = delta;
		this.included = included;
		this.flags = flags;
	}
	
	public XMLMemento generateMemento() {
		try {
			String xml = getXML();
			XMLMemento mem = XMLMemento.createReadRoot(new ByteArrayInputStream(xml.getBytes()));
			addStylesheetInstructions(mem);
			return mem;
		} catch(CoreException ce) {
			System.out.println("Error Generating report");
			ce.printStackTrace();
		}
		return null;
	}
	
	protected String getXML() throws CoreException {
		AbstractFilterListDeltaVisitor visitor = new AbstractFilterListDeltaVisitor(flags) {
			@Override
			protected boolean isExcluded(IDelta delta) {
				String symName = null;
				for( int i = 0; i < included.length; i++ ) {
					symName = included[i].getSymbolicName();
					if( symName != null && symName.equals(delta.getComponentId())) {
						return false;
					}
				}
				return true;
			}
		};
		delta.accept(visitor);
		return visitor.getXML();
	}
}
