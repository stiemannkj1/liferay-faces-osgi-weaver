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

import java.nio.ByteBuffer;
import java.util.List;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import org.osgi.framework.Bundle;
import org.osgi.framework.hooks.weaving.WeavingHook;
import org.osgi.framework.hooks.weaving.WovenClass;
import org.osgi.framework.wiring.BundleWiring;

import org.osgi.service.log.LogService;


/**
 * Due to <a href="https://issues.apache.org/jira/browse/FELIX-5570">FELIX-5570</a>, this class cannot be annotated with
 * {@link org.osgi.service.component.annotations.Component}.
 *
 * @see     JSF_OSGiWeaver
 * @author  Kyle Stiemann
 */
/* package-private */ final class JSF_OSGiWeavingHook implements WeavingHook {

	// Private Constants
	private static final int CLASS_MAJOR_VERSION_BYTE_OFFSET = 6;
	private static final int CLASS_MAJOR_VERSION_BYTE_SIZE = 2;

	/**
	 * For more details on Java class format/target versions see here: <a
	 * href="https://blogs.oracle.com/darcy/source%2c-target%2c-class-file-version-decoder-ring">
	 * https://blogs.oracle.com/darcy/source%2c-target%2c-class-file-version-decoder-ring</a>
	 */
	private static final short JAVA_1_6_MAJOR_VERSION = 50;
	private static final String OSGI_CLASS_LOADER_PACKAGE_NAME = "com.liferay.faces.util.osgi";
	private static final String OSGI_CLASS_LOADER_DYNAMIC_IMPORT = OSGI_CLASS_LOADER_PACKAGE_NAME +
		";bundle-symbolic-name=" + JSF_OSGiWeaver.LIFERAY_FACES_UTIL_BUNDLE_SYMBOLIC_NAME;

	// Private Data Members
	private LogService logService;

	public JSF_OSGiWeavingHook(LogService logService) {
		this.logService = logService;
	}

	/**
	 * Returns true if the class was compiled with a Java 1.6 compiler or target compiler version. The first 4 bytes of
	 * a Java class file are the magic bytes 0xCAFEBABE. The next 4 bytes specify the class format version (for more
	 * details, see: <a href="https://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html#jvms-4.1">
	 * https://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html#jvms-4.1</a>). The first 2 bytes are the minor
	 * version (which can be safely ignored in this case). The second 2 bytes are the major version (see {@link
	 * #JAVA_1_6_MAJOR_VERSION}). To obtain the major target compiler version, extract the 2 bytes (see {@link
	 * #CLASS_MAJOR_VERSION_BYTE_SIZE) starting at offset 6 (see {@link #CLASS_MAJOR_VERSION_BYTE_OFFSET}) as a short
	 * int. If the obtained version is greater than or equal to {@link #JAVA_1_6_MAJOR_VERSION}, then return true.
	 *
	 * @see  #JAVA_1_6_MAJOR_VERSION
	 * @see  #CLASS_MAJOR_VERSION_BYTE_OFFSET
	 * @see  #CLASS_MAJOR_VERSION_BYTE_SIZE
	 */
	private static boolean isCompiledWithJava_1_6_OrGreater(byte[] classBytes) {

		// Example hexadecimal data for a Java class file compiled with a Java 1.7 compiler (or target version):

		//J-
		// Byte Offset: 00 01 02 03 04 05 06 07
		// Bytes:		CA FE BA BE 00 00 00 33
		//J+
		ByteBuffer buffer = ByteBuffer.wrap(classBytes, CLASS_MAJOR_VERSION_BYTE_OFFSET, CLASS_MAJOR_VERSION_BYTE_SIZE);
		short majorVersion = buffer.getShort();

		return majorVersion >= JAVA_1_6_MAJOR_VERSION;
	}

	private static boolean isHandledBundle(BundleWiring bundleWiring) {

		Bundle bundle = bundleWiring.getBundle();
		String bundleSymbolicName = bundle.getSymbolicName();

		return JSF_OSGiWeaver.HANDLED_BUNDLE_SYMBOLIC_NAMES.contains(bundleSymbolicName);
	}

	private static boolean isLiferayFacesOSGiClass(String className) {
		return className.startsWith("com.liferay.faces.util.osgi");
	}

	private static boolean isLiferayFacesOSGiClassDependency(String className) {
		return className.startsWith("com.liferay.faces.util.logging");
	}

	private static boolean isMojarraSPIClass(String className) {
		return className.startsWith("com.sun.faces.spi") || className.startsWith("com.sun.faces.config.configprovider");
	}

	@Override
	public void weave(WovenClass wovenClass) {

		String className = wovenClass.getClassName();
		BundleWiring bundleWiring = wovenClass.getBundleWiring();

		// Don't weave Liferay Faces OSGi classes (or the classes that they depend on) becuase they are already designed
		// to be used in an OSGi environement with OSGi's limited class loaders.
		if (!isMojarraSPIClass(className) && !isLiferayFacesOSGiClassDependency(className) &&
				!isLiferayFacesOSGiClass(className) && isHandledBundle(bundleWiring)) {

			byte[] bytes = wovenClass.getBytes();

			// ASM cannot handle classes compiled with Java 1.5 or lower.
			if (isCompiledWithJava_1_6_OrGreater(bytes)) {

				ClassReader classReader = new ClassReader(bytes);
				OSGiClassWriter osgiClassWriter = new OSGiClassWriter(ClassWriter.COMPUTE_MAXS |
						ClassWriter.COMPUTE_FRAMES, bundleWiring);

				try {

					JSF_OSGiClassVisitor jsfOSGiClassVisitor = new JSF_OSGiClassVisitor(osgiClassWriter, className);
					classReader.accept(jsfOSGiClassVisitor, ClassReader.SKIP_FRAMES);

					if (jsfOSGiClassVisitor.isClassModified()) {

						wovenClass.setBytes(osgiClassWriter.toByteArray());

						List<String> dynamicImports = wovenClass.getDynamicImports();
						dynamicImports.add(OSGI_CLASS_LOADER_DYNAMIC_IMPORT);
					}
				}
				catch (CommonSuperClassNotFoundException e) {

					logService.log(LogService.LOG_WARNING,
						"Unable to weave " + className +
						" for use with OSGi. Unexpected class loading errors may occur when using this class.");
					logService.log(LogService.LOG_DEBUG,
						"Unable to weave " + className + " due to the following error(s):", e);
				}
			}
		}
	}
}
