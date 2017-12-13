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

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import org.osgi.framework.Bundle;
import org.osgi.framework.wiring.BundleWiring;


/**
 * @author  Kyle Stiemann
 */
/* package-private */ class OSGiClassWriter extends ClassWriter {

	// Private Constants
	private static final String OBJECT_TYPE_STRING = OSGiClassProviderMethodVisitor.getTypeString(Object.class);

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

			Set<String> typeSet1 = getTypeAndAncestors(type1);
			Set<String> typeSet2 = getTypeAndAncestors(type2);

			for (String typeFromTypeSet1 : typeSet1) {

				if (typeSet2.contains(typeFromTypeSet1)) {

					commonSuperClass = typeFromTypeSet1;

					break;
				}
			}

			if (commonSuperClass == null) {
				throw new CommonSuperClassNotFoundException(type1 + " and " + type2 +
					" have no common super class visible to " + bundleSymbolicName);
			}
		}

		return commonSuperClass;
	}

	private Set<String> getTypeAndAncestors(String initialType) {

		Set<String> ancestors = new LinkedHashSet<String>();
		ancestors.add(initialType);

		String type = initialType;

		while (type != null) {

			InputStream inputStream = bundleWiringClassLoader.getResourceAsStream(type + ".class");

			if (inputStream == null) {
				break;
			}

			try {

				ClassReader typeClassReader = new ClassReader(inputStream);
				type = typeClassReader.getSuperName();

				if ((type != null) && !type.equals(OBJECT_TYPE_STRING)) {
					ancestors.add(type);
				}
				else {

					ancestors.add(OBJECT_TYPE_STRING);
					type = null;
				}
			}
			catch (IOException e) {
				throw new RuntimeException(e);
			}
			finally {

				try {
					inputStream.close();
				}
				catch (IOException e) {
					// do nothing.
				}
			}
		}

		return Collections.unmodifiableSet(ancestors);
	}
}
