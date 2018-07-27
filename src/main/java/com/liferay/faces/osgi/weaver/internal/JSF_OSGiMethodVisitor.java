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

import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Locale;
import java.util.ResourceBundle;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;


/**
 * @author  Kyle Stiemann
 */
/* package-private */ final class JSF_OSGiMethodVisitor extends GeneratorAdapter {

	// Package-Private
	/* package-private */ static final String FACES_CONTEXT_CLASS_NAME = "javax.faces.context.FacesContext";
	/* package-private */ static final String FACES_CONTEXT_TYPE_STRING = getTypeString("javax.faces.context.FacesContext");

	// Private Constants
	private static final Type CLASS_TYPE = Type.getType(Class.class);
	private static final String CLASS_FOR_NAME_1_ARG_METHOD_DESCRIPTOR = Type.getMethodDescriptor(CLASS_TYPE,
			Type.getType(String.class));
	private static final String CLASS_FOR_NAME_3_ARG_METHOD_DESCRIPTOR = Type.getMethodDescriptor(CLASS_TYPE,
			Type.getType(String.class), Type.BOOLEAN_TYPE, Type.getType(ClassLoader.class));
	private static final String CLASS_LOADER_OWNER_STRING = getTypeString(ClassLoader.class);
	private static final String CLASS_OWNER_STRING = getTypeString(Class.class);
	private static final String GET_BUNDLE_3_ARG_METHOD_DESCRIPTOR = Type.getMethodDescriptor(Type.getType(
				ResourceBundle.class), Type.getType(String.class), Type.getType(Locale.class),
			Type.getType(ClassLoader.class));
	private static final String GET_BUNDLE_4_ARG_METHOD_DESCRIPTOR = Type.getMethodDescriptor(Type.getType(
				ResourceBundle.class), Type.getType(String.class), Type.getType(Locale.class),
			Type.getType(ClassLoader.class), Type.getType(ResourceBundle.Control.class));
	private static final String GET_RESOURCES_METHOD_DESCRIPTOR = Type.getMethodDescriptor(Type.getType(
				Enumeration.class), Type.getType(String.class));
	private static final String GET_RESOURCE_AS_STREAM_METHOD_DESCRIPTOR = Type.getMethodDescriptor(Type.getType(
				InputStream.class), Type.getType(String.class));
	private static final String GET_RESOURCE_METHOD_DESCRIPTOR = Type.getMethodDescriptor(Type.getType(URL.class),
			Type.getType(String.class));
	private static final String LOAD_CLASS_METHOD_DESCRIPTOR = Type.getMethodDescriptor(CLASS_TYPE,
			Type.getType(String.class));
	private static final Type FACES_CONTEXT_TYPE = Type.getObjectType(FACES_CONTEXT_TYPE_STRING);
	private static final String OSGI_CLASS_LOADER_UTIL_OWNER_STRING = getTypeString(
			"com.liferay.faces.util.osgi.OSGiClassLoaderUtil");
	private static final String REPLACEMENT_CLASS_FOR_NAME_1_ARG_METHOD_DESCRIPTOR = Type.getMethodDescriptor(
			CLASS_TYPE, Type.getType(String.class), FACES_CONTEXT_TYPE, CLASS_TYPE);
	private static final String REPLACEMENT_CLASS_FOR_NAME_3_ARG_METHOD_DESCRIPTOR = Type.getMethodDescriptor(
			CLASS_TYPE, Type.getType(String.class), Type.BOOLEAN_TYPE, FACES_CONTEXT_TYPE,
			Type.getType(ClassLoader.class));
	private static final String REPLACEMENT_GET_RESOURCES_METHOD_DESCRIPTOR = Type.getMethodDescriptor(Type.getType(
				Enumeration.class), Type.getType(String.class), FACES_CONTEXT_TYPE, Type.getType(ClassLoader.class));
	private static final String REPLACEMENT_GET_RESOURCE_METHOD_DESCRIPTOR = Type.getMethodDescriptor(Type.getType(
				URL.class), Type.getType(String.class), FACES_CONTEXT_TYPE, Type.getType(ClassLoader.class));
	private static final String REPLACEMENT_GET_RESOURCE_AS_STREAM_METHOD_DESCRIPTOR = Type.getMethodDescriptor(Type
			.getType(InputStream.class), Type.getType(String.class), FACES_CONTEXT_TYPE,
			Type.getType(ClassLoader.class));
	private static final String REPLACEMENT_LOAD_CLASS_METHOD_DESCRIPTOR = Type.getMethodDescriptor(CLASS_TYPE,
			Type.getType(String.class), FACES_CONTEXT_TYPE, Type.getType(ClassLoader.class));
	private static final String RESOURCE_BUNDLE_OWNER_STRING = getTypeString(ResourceBundle.class);

	// Private Final Data Members
	private final JSF_OSGiClassVisitor osgiClassLoaderVisitor;
	private final boolean visitingStaticMethod;

	/* package-private */ JSF_OSGiMethodVisitor(JSF_OSGiClassVisitor osgiClassLoaderVisitor, MethodVisitor mv, int access, String name,
		String desc) {
		super(Opcodes.ASM5, mv, access, name, desc);
		this.visitingStaticMethod = (access & Opcodes.ACC_STATIC) > 0;
		this.osgiClassLoaderVisitor = osgiClassLoaderVisitor;
	}

	/* package-private */ static String getTypeString(Class<?> clazz) {
		return getTypeString(clazz.getName());
	}

	/* package-private */ static String getTypeString(String className) {
		return className.replace(".", "/");
	}

	/**
	 * Converts a {@link java.util.ResourceBundle}<code>.getBundle()</code> method descriptor into a
	 * com.liferay.faces.util.osgi.OSGiClassLoaderUtil.getResourceBundle() method descriptor by adding an argument of
	 * type {@link Class} to the list of argument types.
	 */
	private static String toGetResourceBundleMethodDescriptor(String getBundleMethodDescriptor) {

		Type returnType = Type.getReturnType(getBundleMethodDescriptor);
		Type[] argumentTypes = Type.getArgumentTypes(getBundleMethodDescriptor);
		argumentTypes = Arrays.copyOf(argumentTypes, argumentTypes.length + 1);
		argumentTypes[argumentTypes.length - 1] = CLASS_TYPE;

		return Type.getMethodDescriptor(returnType, argumentTypes);
	}

	@Override
	public void visitMethodInsn(int opcode, String owner, String name, String methodDescriptor, boolean itf) {

		boolean weaveClassLoadingCalls = osgiClassLoaderVisitor.isWeaveClassLoadingCalls();

		if (weaveClassLoadingCalls && (opcode == Opcodes.INVOKEVIRTUAL) && owner.equals(CLASS_LOADER_OWNER_STRING) &&
				name.equals("loadClass") && methodDescriptor.equals(LOAD_CLASS_METHOD_DESCRIPTOR)) {

			// The stack has been prepared so that classLoader.loadClass(className) can be called next:

			//J-
			// TOP OF STACK
			// className
			// classLoader
			// ...
			//J+

			// However, since OSGiClassLoaderUtil.loadClass(className, facesContext, classLoader) will be called
			// instead, the stack must be reordered and include the facesContext:

			//J-
			// TOP OF STACK needed to call OSGiClassLoaderUtil.loadClass() 3-arg
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

			loadCurrentFacesContext();

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

			super.visitMethodInsn(Opcodes.INVOKESTATIC, OSGI_CLASS_LOADER_UTIL_OWNER_STRING, "loadClass",
				REPLACEMENT_LOAD_CLASS_METHOD_DESCRIPTOR, false);
			osgiClassLoaderVisitor.setClassModified(true);
		}
		else if (weaveClassLoadingCalls && (opcode == Opcodes.INVOKESTATIC) && owner.equals(CLASS_OWNER_STRING) &&
				name.equals("forName") &&
				(methodDescriptor.equals(CLASS_FOR_NAME_1_ARG_METHOD_DESCRIPTOR) ||
					methodDescriptor.equals(CLASS_FOR_NAME_3_ARG_METHOD_DESCRIPTOR))) {

			String osgiClassLoaderMethodDescriptor = REPLACEMENT_CLASS_FOR_NAME_3_ARG_METHOD_DESCRIPTOR;

			// If the 1-arg version of Class.forName() is used, provide the current class (in a static method) or
			// this.getClass() (in a non-static method) to the OSGiClassLoaderUtil's 3-arg classForName() method so
			// that it can obtain the correct ClassLoader. For more information, see the
			// OSGiClassLoaderUtil.classForName(java.lang.String, javax.faces.context.FacesContext, java.lang.Class)
			// JavaDoc and the Class.forName(java.lang.String) JavaDoc
			// (https://docs.oracle.com/javase/8/docs/api/java/lang/Class.html#forName-java.lang.String-).
			if (methodDescriptor.equals(CLASS_FOR_NAME_1_ARG_METHOD_DESCRIPTOR)) {

				osgiClassLoaderMethodDescriptor = REPLACEMENT_CLASS_FOR_NAME_1_ARG_METHOD_DESCRIPTOR;
				loadCurrentFacesContext();
				loadCurrentClass();
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

				// However, since OSGiClassLoaderUtil.classForName(className, initialize, facesContext, classLoader)
				// will be called instead, the stack must be reordered and include the facesContext:

				//J-
				// TOP OF STACK needed to call OSGiClassLoaderUtil.classForName() (4-arg)
				// classLoader
				// facesContext
				// initialize
				// className
				// ...
				//J+

				loadCurrentFacesContext();

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

			super.visitMethodInsn(Opcodes.INVOKESTATIC, OSGI_CLASS_LOADER_UTIL_OWNER_STRING, "classForName",
				osgiClassLoaderMethodDescriptor, false);
			osgiClassLoaderVisitor.setClassModified(true);
		}
		else if (weaveClassLoadingCalls && (opcode == Opcodes.INVOKEVIRTUAL) &&
				owner.equals(CLASS_LOADER_OWNER_STRING) && name.startsWith("getResource")) {

			// The stack has been prepared so that classLoader.getResource*(name) can be called next:

			//J-
			// TOP OF STACK
			// name
			// classLoader
			// ...
			//J+

			// However, since OSGiClassLoaderUtil.getResource*(name, facesContext, classLoader) will be called
			// instead, the stack must be reordered and include the facesContext:

			//J-
			// TOP OF STACK needed to call OSGiClassLoaderUtil.getResource*() (3-arg)
			// classLoader
			// facesContext
			// name
			// ...
			//J+

			swap();

			//J-
			// TOP OF STACK
			// classLoader
			// name
			// ...
			//J+

			loadCurrentFacesContext();

			//J-
			// TOP OF STACK
			// facesContext
			// classLoader
			// name
			// ...
			//J+

			swap();

			//J-
			// TOP OF STACK
			// classLoader
			// facesContext
			// name
			// ...
			//J+

			String osgiClassLoaderMethodDescriptor = null;

			if (methodDescriptor.equals(GET_RESOURCE_METHOD_DESCRIPTOR)) {
				osgiClassLoaderMethodDescriptor = REPLACEMENT_GET_RESOURCE_METHOD_DESCRIPTOR;
			}
			else if (methodDescriptor.equals(GET_RESOURCES_METHOD_DESCRIPTOR)) {
				osgiClassLoaderMethodDescriptor = REPLACEMENT_GET_RESOURCES_METHOD_DESCRIPTOR;
			}
			else if (methodDescriptor.equals(GET_RESOURCE_AS_STREAM_METHOD_DESCRIPTOR)) {
				osgiClassLoaderMethodDescriptor = REPLACEMENT_GET_RESOURCE_AS_STREAM_METHOD_DESCRIPTOR;
			}

			if (osgiClassLoaderMethodDescriptor != null) {

				super.visitMethodInsn(Opcodes.INVOKESTATIC, OSGI_CLASS_LOADER_UTIL_OWNER_STRING, name,
					osgiClassLoaderMethodDescriptor, false);
				osgiClassLoaderVisitor.setClassModified(true);
			}
			else {
				super.visitMethodInsn(opcode, owner, name, methodDescriptor, itf);
			}
		}
		else if ((opcode == Opcodes.INVOKESTATIC) && owner.equals(RESOURCE_BUNDLE_OWNER_STRING) &&
				name.equals("getBundle") &&
				(methodDescriptor.equals(GET_BUNDLE_3_ARG_METHOD_DESCRIPTOR) ||
					methodDescriptor.equals(GET_BUNDLE_4_ARG_METHOD_DESCRIPTOR))) {

			loadCurrentClass();

			// Call OSGiClassLoaderUtil.getResourceBundle() with the same arguments as ResourceBundle.getBundle(), but
			// additionally pass the calling class.
			String getResourceBundleMethodDescriptor = toGetResourceBundleMethodDescriptor(methodDescriptor);
			super.visitMethodInsn(Opcodes.INVOKESTATIC, OSGI_CLASS_LOADER_UTIL_OWNER_STRING, "getResourceBundle",
				getResourceBundleMethodDescriptor, false);
			osgiClassLoaderVisitor.setClassModified(true);
		}
		else {
			super.visitMethodInsn(opcode, owner, name, methodDescriptor, itf);
		}
	}

	private void loadCurrentClass() {

		String currentClassType = osgiClassLoaderVisitor.getCurrentClassType();

		if (visitingStaticMethod) {

			// Push the current class (for example MyClass.class) to the top of the stack.
			Type currentClassObjectType = Type.getObjectType(currentClassType);
			visitLdcInsn(currentClassObjectType);
		}
		else {

			// Push the current class (which is the return value of "this.getClass()") to the top of the stack.
			loadThis();
			super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, currentClassType, "getClass",
				Type.getMethodDescriptor(CLASS_TYPE), false);
		}
	}

	private void loadCurrentFacesContext() {

		// Push the current FacesContext to the top of the stack.
		super.visitMethodInsn(Opcodes.INVOKESTATIC, FACES_CONTEXT_TYPE_STRING, "getCurrentInstance",
			Type.getMethodDescriptor(FACES_CONTEXT_TYPE), false);
	}
}
