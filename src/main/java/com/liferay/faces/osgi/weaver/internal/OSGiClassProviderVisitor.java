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

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;


/**
 * @author  Kyle Stiemann
 */
/* package-private */ class OSGiClassProviderVisitor extends ClassVisitor {

	// Private Final Data Members
	private final String currentClassName;

	// Private Data Members
	private boolean classModified;

	/* package-private */ OSGiClassProviderVisitor(ClassVisitor cv, String className) {

		super(Opcodes.ASM5, cv);
		this.currentClassName = className;
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {

		return new OSGiClassProviderMethodVisitor(this, super.visitMethod(access, name, desc, signature, exceptions),
				access, name, desc);
	}

	/* package-private */ String getCurrentClassName() {
		return currentClassName;
	}

	/* package-private */ boolean isClassModified() {
		return classModified;
	}

	/* package-private */ void setClassModified(boolean classModified) {
		this.classModified = classModified;
	}
}
