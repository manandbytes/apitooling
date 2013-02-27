/*******************************************************************************
 * Copyright (c) 2008, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.apitools.ant.tasks;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.Flags;
import org.eclipse.pde.api.tools.internal.comparator.DeltaXmlVisitor;
import org.eclipse.pde.api.tools.internal.provisional.RestrictionModifiers;
import org.eclipse.pde.api.tools.internal.provisional.comparator.DeltaProcessor;
import org.eclipse.pde.api.tools.internal.provisional.comparator.IDelta;
import org.eclipse.pde.api.tools.internal.util.Util;

/**
 * This class is used to exclude some deltas from the generated report.
 */
public abstract class AbstractFilterListDeltaVisitor extends DeltaXmlVisitor {
	public static final int CHECK_DEPRECATION = 0x01;
	public static final int CHECK_OTHER = 0x02;
	public static final int CHECK_ALL = CHECK_DEPRECATION | CHECK_OTHER;

	private int flags;
	
	public AbstractFilterListDeltaVisitor(int flags) throws CoreException {
		super();
		this.flags = flags;
	}
	
	/* Not quite sure what this does, but tried to preserve it cleanly */
	protected String generateListKey(IDelta delta) {
		String typeName = delta.getTypeName();
		StringBuffer buffer = new StringBuffer();
		String componentId = delta.getComponentId();
		if (componentId != null) {
			buffer.append(componentId).append(':');
		}
		if (typeName != null) {
			buffer.append(typeName);
		}
		int flags = delta.getFlags();
		switch(flags) {
			case IDelta.TYPE_MEMBER :
				buffer.append('.').append(delta.getKey());
				break;
			case IDelta.API_METHOD :
			case IDelta.API_CONSTRUCTOR :
			case IDelta.API_ENUM_CONSTANT :
			case IDelta.API_FIELD :
			case IDelta.API_METHOD_WITH_DEFAULT_VALUE :
			case IDelta.API_METHOD_WITHOUT_DEFAULT_VALUE :
			case IDelta.METHOD :
			case IDelta.CONSTRUCTOR :
			case IDelta.ENUM_CONSTANT :
			case IDelta.METHOD_WITH_DEFAULT_VALUE :
			case IDelta.METHOD_WITHOUT_DEFAULT_VALUE :
			case IDelta.FIELD :
			case IDelta.REEXPORTED_API_TYPE :
			case IDelta.REEXPORTED_TYPE :
			case IDelta.DEPRECATION :
				buffer.append('#').append(delta.getKey());
				break;
			case IDelta.MAJOR_VERSION :
			case IDelta.MINOR_VERSION :
				buffer
					.append(Util.getDeltaFlagsName(flags))
					.append('_')
					.append(Util.getDeltaKindName(delta.getKind()));
				break;
			case IDelta.API_COMPONENT :
				buffer.append(Util.getDeltaKindName(delta.getKind())).append('#').append(delta.getKey());
		}

		String listKey = String.valueOf(buffer);
		return listKey;
	}
	
	protected abstract boolean isExcluded(IDelta delta);
	
	protected boolean isDeprecationDelta(IDelta delta) {
		if( delta.getFlags() == IDelta.DEPRECATION)
			return true;
		return false;
	}
	
	protected boolean shouldProcessDeprecations() {
		return (this.flags & CHECK_DEPRECATION) != 0;
	}
	
	protected boolean shouldProcessNonDeprecations() {
		return (this.flags & CHECK_OTHER) != 0;
	}
	
	protected void processLeafDelta(IDelta delta) {
		if( !isExcluded(delta))
			return;
		
		// IF we're handling deprecations, and this is a dep. delta, process it
		if (shouldProcessDeprecations() && isDeprecationDelta(delta)) {
			super.processLeafDelta(delta);
		}
		
		// We're not processing other events, so exit early
		if( !shouldProcessNonDeprecations()) 
			return;
		
		
		if (DeltaProcessor.isCompatible(delta)) {
			// Compatible changes worth noting
			switch(delta.getKind()) {
				case IDelta.ADDED :
					if (Flags.isPublic(delta.getNewModifiers()) && shouldProcessPublicCompatibleAddition(delta)) {
						// public compatible additions
						super.processLeafDelta(delta);
					} else if (Flags.isProtected(delta.getNewModifiers()) && !RestrictionModifiers.isExtendRestriction(delta.getCurrentRestrictions())) {
						// Protected compatible additions
						if( shouldProcessProtectedCompatibleAddition(delta))
							super.processLeafDelta(delta);
					}
					if (delta.getElementType() == IDelta.API_BASELINE_ELEMENT_TYPE) {
						switch(delta.getKind()) {
							case IDelta.ADDED :
								if (delta.getFlags() == IDelta.API_COMPONENT) {
									super.processLeafDelta(delta);
								}
						}
					}
					break;
				case IDelta.CHANGED :
						switch(delta.getFlags()) {
							case IDelta.MAJOR_VERSION :
							case IDelta.MINOR_VERSION :
								super.processLeafDelta(delta);
						}
					break;
			}
		} else {
			// Incompatible changes
			switch(delta.getKind()) {
				case IDelta.ADDED :
					if( shouldProcessIncompatibleAddition(delta))
						super.processLeafDelta(delta);
					break;
				case IDelta.REMOVED :
					if( shouldProcessIncompatibleRemoval(delta))
						super.processLeafDelta(delta);
				break;
			}
		}
	}
	
	private boolean shouldProcessPublicCompatibleAddition(IDelta delta) {
		switch(delta.getFlags()) {
			case IDelta.TYPE_MEMBER :
			case IDelta.METHOD :
			case IDelta.CONSTRUCTOR :
			case IDelta.ENUM_CONSTANT :
			case IDelta.METHOD_WITH_DEFAULT_VALUE :
			case IDelta.METHOD_WITHOUT_DEFAULT_VALUE :
			case IDelta.FIELD :
			case IDelta.TYPE :
			case IDelta.API_TYPE :
			case IDelta.API_METHOD :
			case IDelta.API_FIELD :
			case IDelta.API_CONSTRUCTOR :
			case IDelta.API_ENUM_CONSTANT :
			case IDelta.REEXPORTED_TYPE :
				return true;
		}
		return false;
	}

	private boolean shouldProcessProtectedCompatibleAddition(IDelta delta) {
		switch(delta.getFlags()) {
			case IDelta.TYPE_MEMBER :
			case IDelta.METHOD :
			case IDelta.CONSTRUCTOR :
			case IDelta.ENUM_CONSTANT :
			case IDelta.FIELD :
			case IDelta.TYPE :
			case IDelta.API_TYPE :
			case IDelta.API_METHOD :
			case IDelta.API_FIELD :
			case IDelta.API_CONSTRUCTOR :
			case IDelta.API_ENUM_CONSTANT :
			case IDelta.REEXPORTED_TYPE :
				return true;
		}
		return false;
	}

	private boolean shouldProcessIncompatibleAddition(IDelta delta) {
		switch(delta.getFlags()) {
			case IDelta.TYPE_MEMBER :
			case IDelta.METHOD :
			case IDelta.CONSTRUCTOR :
			case IDelta.ENUM_CONSTANT :
			case IDelta.METHOD_WITH_DEFAULT_VALUE :
			case IDelta.METHOD_WITHOUT_DEFAULT_VALUE :
			case IDelta.FIELD :
			case IDelta.TYPE :
			case IDelta.API_TYPE :
			case IDelta.API_METHOD :
			case IDelta.API_FIELD :
			case IDelta.API_CONSTRUCTOR :
			case IDelta.API_ENUM_CONSTANT :
			case IDelta.REEXPORTED_TYPE :
				if (Util.isVisible(delta.getNewModifiers())) {
					return true;
				}
		}
		return false;
	}
	
	private boolean shouldProcessIncompatibleRemoval(IDelta delta) {
		switch(delta.getFlags()) {
			case IDelta.TYPE_MEMBER :
			case IDelta.METHOD :
			case IDelta.CONSTRUCTOR :
			case IDelta.ENUM_CONSTANT :
			case IDelta.METHOD_WITH_DEFAULT_VALUE :
			case IDelta.METHOD_WITHOUT_DEFAULT_VALUE :
			case IDelta.FIELD :
			case IDelta.TYPE :
			case IDelta.API_TYPE :
			case IDelta.API_METHOD :
			case IDelta.API_FIELD :
			case IDelta.API_CONSTRUCTOR :
			case IDelta.API_ENUM_CONSTANT :
			case IDelta.REEXPORTED_API_TYPE :
			case IDelta.REEXPORTED_TYPE :
				if (Util.isVisible(delta.getOldModifiers())) {
					return true;
				}
				break;
			case IDelta.API_COMPONENT :
				return true;
		}
		return false;
	}
}