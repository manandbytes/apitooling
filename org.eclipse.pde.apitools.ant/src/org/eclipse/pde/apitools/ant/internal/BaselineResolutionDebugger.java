/*******************************************************************************
 * Copyright (c) 2013 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.apitools.ant.internal;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;


import org.eclipse.core.runtime.CoreException;
import org.eclipse.osgi.service.resolver.ResolverError;
import org.eclipse.pde.api.tools.internal.model.BundleComponent;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.apitools.ant.util.BaselineUtils;
import org.eclipse.pde.apitools.ant.util.IMemento;
import org.eclipse.pde.apitools.ant.util.StringUtils;
import org.eclipse.pde.apitools.ant.util.XMLMemento;

public class BaselineResolutionDebugger {
	
	public static final String TEMPORARY_BASELINE = "temporaryBaseline";
	
	public RootReport debugBaseline(String file, String name, String[] bundlesToVerify) {
		IApiBaseline referenceBaseline = BaselineUtils.createBaseline(
				name, file, null);
		return debugBaseline(referenceBaseline, bundlesToVerify);
	}
	
	public RootReport debugBaseline(IApiBaseline baseline, String[] bundlesToVerify) {
		IApiComponent[] allComponents = baseline.getApiComponents();
		RootResolutionReport main = new RootResolutionReport();
		for( int i = 0; i < bundlesToVerify.length; i++ ) {
			ResolutionReport report = debugBundleResolution(baseline, allComponents, bundlesToVerify[i]);
			main.addChildReport(report);
		}
		return main;
	}

	public RootReport debugBaseline(IApiBaseline baseline, IApiComponent[] bundlesToVerify) {
		IApiComponent[] allComponents = baseline.getApiComponents();
		RootResolutionReport main = new RootResolutionReport();
		for( int i = 0; i < bundlesToVerify.length; i++ ) {
			ResolutionReport report = debugBundleResolution(baseline, allComponents, bundlesToVerify[i].getSymbolicName());
			main.addChildReport(report);
		}
		return main;
	}

	protected ResolutionReport debugBundleResolution(IApiBaseline baseline, IApiComponent[] allComponents, String bundleName) {
		
		ArrayList<IApiComponent> matching = findMatchingComponents(allComponents, bundleName);
		if( matching.size() == 0 ) {
			return new MissingBundleReport(bundleName);
		}
		ResolutionReport main = new ResolutionReport(bundleName);
		Iterator<IApiComponent> i = matching.iterator();
		while(i.hasNext()) {
			IApiComponent current = i.next();
			
			ResolverError[] errors = null;
			boolean isResolved = false;
			try {
				isResolved = isResolved(current);
				if( !isResolved) {
					errors = getErrors(current);
				}
			} catch(CoreException ce ) {
				ResolutionReport rep = new FrameworkResolutionFailedReport(bundleName, current, ce);
				if( matching.size() == 1 )
					return rep;
				main.addChildReport(rep);
				continue;
			}
			
			if( isResolved ) {
				ResolutionReport rep = new BundleResolvedReport(bundleName, current);
				if( matching.size() == 1 )
					return rep;
				main.addChildReport(rep);
				continue;
			}
			
			for( int j = 0; j < errors.length; j++ ) {
				if( errors[j].getType() == ResolverError.MISSING_REQUIRE_BUNDLE) {
					MissingRequirementReport rep = new MissingRequirementReport(bundleName, current, errors[j]);
					
					ResolutionReport rep2 = debugBundleResolution(baseline, allComponents, rep.getRequiredPlugin());
					rep.addChildReport(rep2);
					
					if( matching.size() == 1 ) 
						return rep;
					main.addChildReport(rep);
				} else {
					ResolutionReport rep = new ResolverErrorReport(bundleName, current, errors[j]); 
					if( matching.size() == 1 )
						return rep;
					main.addChildReport(rep);
				}
			}
		}
		return main;
	} 
	
	protected ArrayList<IApiComponent> findMatchingComponents(IApiComponent[] allComponents, String bundleId) {
		ArrayList<IApiComponent> matching = new ArrayList<IApiComponent>();
		for( int i = 0; i < allComponents.length; i++ ) {
			String name = allComponents[i].getSymbolicName();
			if( name.equals(bundleId)) {
				matching.add(allComponents[i]);
			}
		}
		return matching;
	}
	
	protected boolean isResolved(IApiComponent comp) throws CoreException {
		if( !(comp instanceof BundleComponent)) {
			System.out.println("Break");
			return true;
		}
		boolean resolved = (((BundleComponent)comp).getBundleDescription().isResolved());
		return resolved;
	}

	protected ResolverError[] getErrors(IApiComponent comp) throws CoreException {
		return ((BundleComponent)comp).getErrors();
	}

	
	private void outputMemento(XMLMemento mem) {
		try {
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			mem.save(os);
			System.out.println(new String(os.toByteArray()));
		} catch( IOException ioe) {
			ioe.printStackTrace();
		}
	}
	
	public static class RootResolutionReport extends RootReport {
		public RootResolutionReport() {
			super("debugBaseline");
		}
	}
	
	public static class ResolutionReport extends AbstractReport {
		protected String id;
		protected ArrayList<ResolutionReport> children;
		public ResolutionReport(String bundleId) {
			super();
			this.id = bundleId;
		}
		protected void fillMemento(IMemento parentContext) {
			IMemento mem = parentContext.createChild("resolvingBundle");
			mem.putString("id", id);
			fillChildren(mem);
		}
	}
	
	public static class FrameworkResolutionFailedReport extends ResolutionReport {
		private IApiComponent comp;
		private CoreException ce;
		public FrameworkResolutionFailedReport(String id, IApiComponent component, CoreException ce) {
			super(id);
			this.comp = component;
			this.ce = ce;
		}
		protected void fillMemento(IMemento parentContext) {
			IMemento mem = parentContext.createChild("bundleFailure");
			mem.putString("id", id);
			mem.putString("version", comp.getVersion());
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			PrintStream ps = new PrintStream(os);
			ce.printStackTrace(ps);
			((XMLMemento)mem).putTextData(new String(os.toByteArray()));
			fillChildren(mem);
		}
	}

	/*
	 * A successfully resolved bundle
	 */
	public static class BundleResolvedReport extends ResolutionReport {
		private IApiComponent comp;
		public BundleResolvedReport(String id, IApiComponent component) {
			super(id);
			this.comp = component;
		}
		protected void fillMemento(IMemento parentContext) {
			IMemento mem = parentContext.createChild("bundleResolved");
			mem.putString("id",  id);
			mem.putString("version", comp.getVersion());
			fillChildren(mem);
		}
	}

	/*
	 * A successfully resolved bundle
	 */
	public static class MissingBundleReport extends ResolutionReport {
		public MissingBundleReport(String id) {
			super(id);
		}
		protected void fillMemento(IMemento parentContext) {
			IMemento mem = parentContext.createChild("bundleMissing");
			mem.putString("id", id);
			fillChildren(mem);
		}
	}

	/*
	 * For an error which we do not handle specifically
	 */
	public static class ResolverErrorReport extends ResolutionReport {
		private IApiComponent comp;
		private ResolverError error;
		public ResolverErrorReport(String id, IApiComponent component, ResolverError error) {
			super(id);
			this.comp = component;
			this.error = error;
		}
		protected void fillMemento(IMemento parentContext) {
			IMemento mem = parentContext.createChild("bundleResolveFailed");
			mem.putString("id", id);
			mem.putString("version", comp.getVersion());
			mem.putString("msg", error.toString());
			fillChildren(mem);
		}
	}


	public static class MissingRequirementReport extends ResolutionReport {
		private IApiComponent comp;
		private String requiredPlugin;
		private ResolverError error;
		public MissingRequirementReport(String id, IApiComponent root, ResolverError error) {
			super(id);
			this.comp = root;
			this.error = error;
			String data = error.getData();
			requiredPlugin = StringUtils.getRequiredBundleName(data);
		}
		
		public String getRequiredPlugin() {
			return requiredPlugin;
		}
		protected void fillMemento(IMemento parentContext) {
			IMemento mem = parentContext.createChild("missingDependency");
			mem.putString("bundleid", id);
			mem.putString("bundleversion", comp.getVersion());
			mem.putString("requiredid", requiredPlugin);
			mem.putString("message", error.toString());
			fillChildren(mem);
		}
	}
}
