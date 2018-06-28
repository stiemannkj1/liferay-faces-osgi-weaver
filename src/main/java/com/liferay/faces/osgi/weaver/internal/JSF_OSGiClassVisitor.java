/**
 * Copyright (c) 2000-2018 Liferay, Inc. All rights reserved.
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

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;


/**
 * @author  Kyle Stiemann
 */
/* package-private */ final class JSF_OSGiClassVisitor extends ClassVisitor {

	// Private Final Data Members
	private final ClassLoader bundleWiringClassLoader;
	private final String currentClassType;

	// Private Data Members
	private boolean classModified;

	/* package-private */ JSF_OSGiClassVisitor(OSGiClassWriter osgiClassWriter, String className) {

		super(Opcodes.ASM5, osgiClassWriter);
		this.bundleWiringClassLoader = osgiClassWriter.getBundleWiringClassLoader();
		this.currentClassType = JSF_OSGiMethodVisitor.getTypeString(className);
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {

		MethodVisitor methodVisitor = super.visitMethod(access, name, desc, signature, exceptions);

		// Since OSGiClassLoaderUtil relies on FacesContext.getInstance(), avoid calling OSGiClassLoaderUtil in
		// FacesContext initialization to avoid circular calls.
		if (!isFacesContextInit(name)) {
			methodVisitor = new JSF_OSGiMethodVisitor(this, methodVisitor, access, name, desc);
		}

		return methodVisitor;
	}

	/* package-private */ String getCurrentClassType() {
		return currentClassType;
	}

	/* package-private */ boolean isClassModified() {
		return classModified;
	}

	/* package-private */ void setClassModified(boolean classModified) {
		this.classModified = classModified;
	}

	private boolean isFacesContextInit(String methodName) {

		boolean isFacesContextInit = false;

		if ("<clinit>".equals(methodName) || "<init>".equals(methodName)) {

			IterableLazyTypeHierarchy iterableLazyTypeHierarchy = new IterableLazyTypeHierarchy(currentClassType,
					bundleWiringClassLoader);

			for (String type : iterableLazyTypeHierarchy) {

				if (JSF_OSGiMethodVisitor.FACES_CONTEXT_TYPE_STRING.equals(type)) {

					isFacesContextInit = true;

					break;
				}
			}
		}

		return isFacesContextInit;
	}
}
