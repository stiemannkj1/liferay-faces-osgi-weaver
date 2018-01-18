/**
 * Copyright (c) 2000-2017 Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */
package com.liferay.faces.osgi.weaver.internal;

import java.util.Iterator;
import java.util.LinkedHashSet;

import org.objectweb.asm.ClassWriter;

import org.osgi.framework.Bundle;
import org.osgi.framework.wiring.BundleWiring;


/**
 * @author  Kyle Stiemann
 */
/* package-private */ final class OSGiClassWriter extends ClassWriter {

	// Package-Private Constants
	/* package-private */ static final String OBJECT_TYPE_STRING = JSF_OSGiMethodVisitor.getTypeString(Object.class);

	// Private Final Data Members
	private final String bundleSymbolicName;
	private final ClassLoader bundleWiringClassLoader;

	public OSGiClassWriter(int flags, BundleWiring bundleWiring) {

		super(flags);
		this.bundleWiringClassLoader = bundleWiring.getClassLoader();

		Bundle bundle = bundleWiring.getBundle();
		this.bundleSymbolicName = bundle.getSymbolicName();
	}

	/**
	 * This method is overridden in order to avoid loading classes when obtaining the common super class.
	 *
	 * @param  type1
	 * @param  type2
	 *
	 * @see    ClassWriter#getCommonSuperClass(java.lang.String, java.lang.String)
	 */
	@Override
	protected String getCommonSuperClass(String type1, String type2) {

		String commonSuperClass = null;

		if (type1.equals(type2)) {
			commonSuperClass = type1;
		}
		else if (type1.equals(OBJECT_TYPE_STRING) || type2.equals(OBJECT_TYPE_STRING)) {
			commonSuperClass = OBJECT_TYPE_STRING;
		}
		else {

			IterableLazyTypeHierarchy typeHierarchy1 = new IterableLazyTypeHierarchy(type1, bundleWiringClassLoader);
			IterableLazyTypeHierarchy typeHierarchy2 = new IterableLazyTypeHierarchy(type2, bundleWiringClassLoader);
			Iterator<String> typeHierarchy2Iterator = typeHierarchy2.iterator();
			LinkedHashSet<String> cachedTypeHierarchy2Values = new LinkedHashSet<String>();

			for (String typeFromHierarchy1 : typeHierarchy1) {

				if (cachedTypeHierarchy2Values.contains(typeFromHierarchy1)) {

					commonSuperClass = typeFromHierarchy1;

					break;
				}

				while (typeHierarchy2Iterator.hasNext()) {

					String typeFromHierarchy2 = typeHierarchy2Iterator.next();
					cachedTypeHierarchy2Values.add(typeFromHierarchy2);

					if (typeFromHierarchy2.equals(typeFromHierarchy1)) {

						commonSuperClass = typeFromHierarchy1;

						break;
					}
				}
			}

			if (commonSuperClass == null) {
				throw new CommonSuperClassNotFoundException(type1 + " and " + type2 +
					" have no common super class visible to " + bundleSymbolicName);
			}
		}

		return commonSuperClass;
	}
}
