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

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;


/**
 * @author  Kyle Stiemann
 */
/* package-private */ class OSGiClassProviderMethodVisitor extends GeneratorAdapter {

	// Private Constants
	private static final String CLASS_LOADER_LOAD_CLASS_METHOD_DESCRIPTOR = Type.getMethodDescriptor(Type.getType(
				Class.class), Type.getType(String.class));
	private static final String CLASS_FOR_NAME_1_ARG_METHOD_DESCRIPTOR = Type.getMethodDescriptor(Type.getType(
				Class.class), Type.getType(String.class));
	private static final String CLASS_FOR_NAME_3_ARG_METHOD_DESCRIPTOR = Type.getMethodDescriptor(Type.getType(
				Class.class), Type.getType(String.class), Type.BOOLEAN_TYPE, Type.getType(ClassLoader.class));
	private static final String OSGI_CLASS_PROVIDER_CLASS_FOR_NAME_2_ARG_METHOD_DESCRIPTOR = Type.getMethodDescriptor(
			Type.getType(Class.class), Type.getType(String.class), Type.getType(Object.class));
	private static final String OSGI_CLASS_PROVIDER_LOAD_CLASS_METHOD_DESCRIPTOR = Type.getMethodDescriptor(Type
			.getType(Class.class), Type.getType(String.class), Type.getType(ClassLoader.class));
	private static final String OSGI_CLASS_PROVIDER_OWNER_STRING = getTypeString(
			"com.liferay.faces.osgi.util.OSGiClassProviderUtil");
	private static final String CLASS_OWNER_STRING = getTypeString(Class.class);
	private static final String CLASS_LOADER_OWNER_STRING = getTypeString(ClassLoader.class);

	// Private Final Data Members
	private final boolean visitingStaticMethod;
	private final OSGiClassProviderVisitor osgiClassProviderVisitor;

	/* package-private */ OSGiClassProviderMethodVisitor(OSGiClassProviderVisitor osgiClassProviderVisitor, MethodVisitor mv, int access,
		String name, String desc) {
		super(Opcodes.ASM5, mv, access, name, desc);
		this.visitingStaticMethod = (access & Opcodes.ACC_STATIC) > 0;
		this.osgiClassProviderVisitor = osgiClassProviderVisitor;
	}

	/* package-private */ static String getTypeString(String className) {
		return className.replace(".", "/");
	}

	private static String getTypeString(Class<?> clazz) {
		return clazz.getName().replace(".", "/");
	}

	@Override
	public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {

		if ((opcode == Opcodes.INVOKEVIRTUAL) && owner.equals(CLASS_LOADER_OWNER_STRING) && name.equals("loadClass") &&
				desc.equals(CLASS_LOADER_LOAD_CLASS_METHOD_DESCRIPTOR)) {

			// Since the bytecode is prepared to call classLoader.loadClass(className), the top of the stack looks
			// like this:

			//J-
			// TOP OF STACK
			// classLoader
			// className
			// ...
			//J+

			// Since we need to call OSGiClassProviderUtil.loadClass(className, classLoader), swap the top two
			// elements on the stack before calling OSGiClassProviderUtil.loadClass(className, classLoader).
			swap();
			super.visitMethodInsn(Opcodes.INVOKESTATIC, OSGI_CLASS_PROVIDER_OWNER_STRING, "loadClass",
				OSGI_CLASS_PROVIDER_LOAD_CLASS_METHOD_DESCRIPTOR, false);
			osgiClassProviderVisitor.setClassModified(true);
		}
		else if ((opcode == Opcodes.INVOKESTATIC) && owner.equals(CLASS_OWNER_STRING) && name.equals("forName") &&
				(desc.equals(CLASS_FOR_NAME_1_ARG_METHOD_DESCRIPTOR) ||
					desc.equals(CLASS_FOR_NAME_3_ARG_METHOD_DESCRIPTOR))) {

			String methodDescriptor = CLASS_FOR_NAME_3_ARG_METHOD_DESCRIPTOR;

			// If the 1-arg version of Class.forName() is used, provide the current class (in a static method) or
			// "this" (the calling object) to the OSGiClassProviderUtil's 2-arg classForName() method so that it can
			// obtain the correct ClassLoader. For more information, see the
			// OSGiClassProviderUtil.classForName(java.lang.String, java.lang.Object) JavaDoc and the
			// Class.forName(java.lang.String) JavaDoc
			// (https://docs.oracle.com/javase/8/docs/api/java/lang/Class.html#forName-java.lang.String-).
			if (desc.equals(CLASS_FOR_NAME_1_ARG_METHOD_DESCRIPTOR)) {

				methodDescriptor = OSGI_CLASS_PROVIDER_CLASS_FOR_NAME_2_ARG_METHOD_DESCRIPTOR;

				if (visitingStaticMethod) {

					// Push the current class to the top of the stack.
					visitLdcInsn(osgiClassProviderVisitor.getClassType());
				}
				else {

					// Push "this" object to the top of the stack.
					loadThis();
				}
			}

			super.visitMethodInsn(Opcodes.INVOKESTATIC, OSGI_CLASS_PROVIDER_OWNER_STRING, "classForName",
				methodDescriptor, false);
			osgiClassProviderVisitor.setClassModified(true);
		}
		else {
			super.visitMethodInsn(opcode, owner, name, desc, itf);
		}
	}
}
