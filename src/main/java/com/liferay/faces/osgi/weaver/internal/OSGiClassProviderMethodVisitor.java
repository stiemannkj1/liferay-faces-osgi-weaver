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
	private static final String FACES_CONTEXT_TYPE_STRING = getTypeString("javax.faces.context.FacesContext");
	private static final Type FACES_CONTEXT_TYPE = Type.getObjectType(FACES_CONTEXT_TYPE_STRING);
	private static final String OSGI_CLASS_PROVIDER_CLASS_FOR_NAME_3_ARG_METHOD_DESCRIPTOR = Type.getMethodDescriptor(
			Type.getType(Class.class), Type.getType(String.class), FACES_CONTEXT_TYPE, Type.getType(Class.class));
	private static final String OSGI_CLASS_PROVIDER_CLASS_FOR_NAME_4_ARG_METHOD_DESCRIPTOR = Type.getMethodDescriptor(
			Type.getType(Class.class), Type.getType(String.class), Type.BOOLEAN_TYPE, FACES_CONTEXT_TYPE,
			Type.getType(ClassLoader.class));
	private static final String OSGI_CLASS_PROVIDER_LOAD_CLASS_METHOD_DESCRIPTOR = Type.getMethodDescriptor(Type
			.getType(Class.class), Type.getType(String.class), FACES_CONTEXT_TYPE, Type.getType(ClassLoader.class));
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

	private static String getTypeString(String className) {
		return className.replace(".", "/");
	}

	private static String getTypeString(Class<?> clazz) {
		return getTypeString(clazz.getName());
	}

	@Override
	public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {

		if ((opcode == Opcodes.INVOKEVIRTUAL) && owner.equals(CLASS_LOADER_OWNER_STRING) && name.equals("loadClass") &&
				desc.equals(CLASS_LOADER_LOAD_CLASS_METHOD_DESCRIPTOR)) {

			// The stack has been prepared so that classLoader.loadClass(className) can be called next:

			//J-
			// TOP OF STACK
			// className
			// classLoader
			// ...
			//J+

			// However, since OSGiClassProviderUtil.loadClass(className, facesContext, classLoader) will be called
			// instead, the stack must be reordered and include the facesContext:

			//J-
			// TOP OF STACK needed to call OSGiClassProviderUtil.loadClass() 3-arg
			// classLoader
			// facesContext
			// className
			// ...
			//J+

			swap();

			//J-
			// TOP OF STACK
			// classLoader
			// className
			// ...
			//J+

			invokeFacesContextGetCurrentInstance();

			//J-
			// TOP OF STACK
			// facesContext
			// classLoader
			// className
			// ...
			//J+

			swap();

			//J-
			// TOP OF STACK
			// classLoader
			// facesContext
			// className
			// ...
			//J+

			super.visitMethodInsn(Opcodes.INVOKESTATIC, OSGI_CLASS_PROVIDER_OWNER_STRING, "loadClass",
				OSGI_CLASS_PROVIDER_LOAD_CLASS_METHOD_DESCRIPTOR, false);
			osgiClassProviderVisitor.setClassModified(true);
		}
		else if ((opcode == Opcodes.INVOKESTATIC) && owner.equals(CLASS_OWNER_STRING) && name.equals("forName") &&
				(desc.equals(CLASS_FOR_NAME_1_ARG_METHOD_DESCRIPTOR) ||
					desc.equals(CLASS_FOR_NAME_3_ARG_METHOD_DESCRIPTOR))) {

			String methodDescriptor = OSGI_CLASS_PROVIDER_CLASS_FOR_NAME_4_ARG_METHOD_DESCRIPTOR;

			// If the 1-arg version of Class.forName() is used, provide the current class (in a static method) or
			// this.getClass() (in a non-static method) to the OSGiClassProviderUtil's 3-arg classForName() method so
			// that it can obtain the correct ClassLoader. For more information, see the
			// OSGiClassProviderUtil.classForName(java.lang.String, javax.faces.context.FacesContext, java.lang.Class)
			// JavaDoc and the Class.forName(java.lang.String) JavaDoc
			// (https://docs.oracle.com/javase/8/docs/api/java/lang/Class.html#forName-java.lang.String-).
			if (desc.equals(CLASS_FOR_NAME_1_ARG_METHOD_DESCRIPTOR)) {

				invokeFacesContextGetCurrentInstance();

				String currentClassName = osgiClassProviderVisitor.getCurrentClassName();
				String currentClassType = getTypeString(currentClassName);
				methodDescriptor = OSGI_CLASS_PROVIDER_CLASS_FOR_NAME_3_ARG_METHOD_DESCRIPTOR;

				if (visitingStaticMethod) {

					// Push the current class (for example MyClass.class) to the top of the stack.
					Type currentClassObjectType = Type.getObjectType(currentClassType);
					visitLdcInsn(currentClassObjectType);
				}
				else {

					// Push the current class (which is the return value of "this.getClass()") to the top of the stack.
					loadThis();
					super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, currentClassType, "getClass",
						Type.getMethodDescriptor(Type.getType(Class.class)), false);
				}
			}
			else {

				// The stack has been prepared so that Class.forName(className, initialize, classLoader) can be called
				// next:

				//J-
				// TOP OF STACK
				// classLoader
				// initialize
				// className
				// ...
				//J+

				// However, since OSGiClassProviderUtil.classForName(className, initialize, facesContext, classLoader)
				// will be called instead, the stack must be reordered and include the facesContext:

				//J-
				// TOP OF STACK needed to call OSGiClassProviderUtil.classForName() (4-arg)
				// classLoader
				// facesContext
				// initialize
				// className
				// ...
				//J+

				invokeFacesContextGetCurrentInstance();

				//J-
				// TOP OF STACK
				// facesContext
				// classLoader
				// initialize
				// className
				// ...
				//J+

				swap();

				//J-
				// TOP OF STACK
				// classLoader
				// facesContext
				// initialize
				// className
				// ...
				//J+
			}

			super.visitMethodInsn(Opcodes.INVOKESTATIC, OSGI_CLASS_PROVIDER_OWNER_STRING, "classForName",
				methodDescriptor, false);
			osgiClassProviderVisitor.setClassModified(true);
		}
		else {
			super.visitMethodInsn(opcode, owner, name, desc, itf);
		}
	}

	private void invokeFacesContextGetCurrentInstance() {

		// Push the current FacesContext to the top of the stack.
		super.visitMethodInsn(Opcodes.INVOKESTATIC, FACES_CONTEXT_TYPE_STRING, "getCurrentInstance",
			Type.getMethodDescriptor(FACES_CONTEXT_TYPE), false);
	}
}
