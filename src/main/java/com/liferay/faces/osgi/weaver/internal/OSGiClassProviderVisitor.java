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
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;


/**
 * @author  Kyle Stiemann
 */
public class OSGiClassProviderVisitor extends ClassVisitor {

	// Private Data Members
	private boolean classModified;

	public OSGiClassProviderVisitor(ClassVisitor cv) {
		super(Opcodes.ASM5, cv);
	}

	private static String getOwnerString(Class<?> clazz) {
		return clazz.getName().replace(".", "/");
	}

	private static String getOwnerString(String className) {
		return className.replace(".", "/");
	}

	public boolean isClassModified() {
		return classModified;
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {

		return new OSGiClassProviderMethodVisitorImpl(this,
				super.visitMethod(access, name, desc, signature, exceptions), access, name, desc);
	}

	private void setClassModified(boolean modified) {
		this.classModified = modified;
	}

	private static final class OSGiClassProviderMethodVisitorImpl extends GeneratorAdapter {

		// Private Constants
		private static final String CLASS_LOADER_LOAD_CLASS_METHOD_DESCRIPTOR = Type.getMethodDescriptor(Type.getType(
					Class.class), Type.getType(String.class));
		private static final String CLASS_FOR_NAME_1_ARG_METHOD_DESCRIPTOR = Type.getMethodDescriptor(Type.getType(
					Class.class), Type.getType(String.class));
		private static final String CLASS_FOR_NAME_3_ARG_METHOD_DESCRIPTOR = Type.getMethodDescriptor(Type.getType(
					Class.class), Type.getType(String.class), Type.BOOLEAN_TYPE, Type.getType(ClassLoader.class));
		private static final String OSGI_CLASS_PROVIDER_LOAD_CLASS_METHOD_DESCRIPTOR = Type.getMethodDescriptor(Type
				.getType(Class.class), Type.getType(String.class), Type.getType(ClassLoader.class));
		private static final String OSGI_CLASS_PROVIDER_OWNER_STRING = getOwnerString(
				"com.liferay.faces.osgi.util.OSGiClassProvider");
		private static final String CLASS_OWNER_STRING = getOwnerString(Class.class);
		private static final String CLASS_LOADER_OWNER_STRING = getOwnerString(ClassLoader.class);

		// Private Final Data Members
		private final OSGiClassProviderVisitor osgiClassProviderVisitor;

		public OSGiClassProviderMethodVisitorImpl(OSGiClassProviderVisitor osgiClassProviderVisitor, MethodVisitor mv,
			int access, String name, String desc) {
			super(Opcodes.ASM5, mv, access, name, desc);
			this.osgiClassProviderVisitor = osgiClassProviderVisitor;
		}

		@Override
		public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {

			if ((opcode == Opcodes.INVOKEVIRTUAL) && owner.equals(CLASS_LOADER_OWNER_STRING) &&
					name.equals("loadClass") && desc.equals(CLASS_LOADER_LOAD_CLASS_METHOD_DESCRIPTOR)) {

				// Since the bytecode is prepared to call classLoader.loadClass(className), the top of the stack looks
				// like this:

				//J-
				// TOP OF STACK
				// classLoader
				// className
				// ...
				//J+

				// Since we need to call OSGiClassProvider.loadClass(className, classLoader), swap the top two elements
				// on the stack before calling OSGiClassProvider.loadClass(className, classLoader).
				super.visitInsn(Opcodes.SWAP);
				super.visitMethodInsn(Opcodes.INVOKESTATIC, OSGI_CLASS_PROVIDER_OWNER_STRING, "loadClass",
					OSGI_CLASS_PROVIDER_LOAD_CLASS_METHOD_DESCRIPTOR, false);
				osgiClassProviderVisitor.setClassModified(true);
			}
			else if ((opcode == Opcodes.INVOKESTATIC) && owner.equals(CLASS_OWNER_STRING) && name.equals("forName")) {

				String methodDescriptor = CLASS_FOR_NAME_1_ARG_METHOD_DESCRIPTOR;

				if (!desc.equals(methodDescriptor)) {
					methodDescriptor = CLASS_FOR_NAME_3_ARG_METHOD_DESCRIPTOR;
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
}
