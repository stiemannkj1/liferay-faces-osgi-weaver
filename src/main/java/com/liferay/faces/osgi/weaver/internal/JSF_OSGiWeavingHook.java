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

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.List;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import org.osgi.framework.Bundle;
import org.osgi.framework.hooks.weaving.WeavingHook;
import org.osgi.framework.hooks.weaving.WovenClass;
import org.osgi.framework.wiring.BundleWiring;

import org.osgi.service.log.LogService;

import org.osgi.util.tracker.ServiceTracker;


/**
 * @author  Kyle Stiemann
 */
public class JSF_OSGiWeavingHook implements WeavingHook {

	// Private Constants
	private static final String LIFERAY_FACES_UTIL_BUNDLE_SYMBOLIC_NAME = "com.liferay.faces.util";
	private static final List<String> HANDLED_BUNDLE_SYMBOLIC_NAMES = Collections.unmodifiableList(Arrays.asList(
				"org.glassfish.javax.faces", LIFERAY_FACES_UTIL_BUNDLE_SYMBOLIC_NAME, "com.liferay.faces.bridge.api",
				"com.liferay.faces.bridge.impl", "com.liferay.faces.bridge.ext"));

	/**
	 * For more details on Java class format/target versions see here: <a
	 * href="https://blogs.oracle.com/darcy/source%2c-target%2c-class-file-version-decoder-ring">
	 * https://blogs.oracle.com/darcy/source%2c-target%2c-class-file-version-decoder-ring</a>
	 */
	private static final short JAVA_1_6_MAJOR_VERSION = 50;
	private static final int CLASS_MAJOR_VERSION_BYTE_OFFSET = 6;
	private static final int CLASS_MAJOR_VERSION_BYTE_SIZE = 2;
	private static final String OSGI_CLASS_LOADER_PACKAGE_NAME = "com.liferay.faces.util.osgi";
	private static final String OSGI_CLASS_LOADER_DYNAMIC_IMPORT = OSGI_CLASS_LOADER_PACKAGE_NAME +
		";bundle-symbolic-name=" + LIFERAY_FACES_UTIL_BUNDLE_SYMBOLIC_NAME;

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

	private static boolean isFacesBundle(Bundle bundle) {

		String bundleSymbolicName = bundle.getSymbolicName();

		return HANDLED_BUNDLE_SYMBOLIC_NAMES.contains(bundleSymbolicName) || isFacesWab(bundle);
	}

	private static boolean isFacesWab(Bundle bundle) {

		Dictionary<String, String> headers = bundle.getHeaders();
		String importPackageHeader = headers.get("Import-Package");

		return isWab(bundle) && importPackageHeader.contains("javax.faces");
	}

	private static boolean isLiferayFacesOSGiClass(String className) {
		return className.startsWith("com.liferay.faces.util.osgi");
	}

	private static boolean isMojarraSPIClass(String className) {
		return className.startsWith("com.sun.faces.spi") || className.startsWith("com.sun.faces.config.configprovider");
	}

	private static boolean isWab(Bundle bundle) {

		Dictionary<String, String> headers = bundle.getHeaders();
		String webContextPathHeader = headers.get("Web-ContextPath");

		return webContextPathHeader != null;
	}

	@Override
	public void weave(WovenClass wovenClass) {

		String className = wovenClass.getClassName();
		BundleWiring bundleWiring = wovenClass.getBundleWiring();
		Bundle bundle = bundleWiring.getBundle();

		// Don't weave Liferay Faces OSGi classes becuase they are already designed to be used in an OSGi environement
		// with OSGi's limited class loaders.
		if (!isMojarraSPIClass(className) && !isLiferayFacesOSGiClass(className) && isFacesBundle(bundle)) {

			byte[] bytes = wovenClass.getBytes();

			// ASM cannot handle classes compiled with Java 1.5 or lower.
			if (isCompiledWithJava_1_6_OrGreater(bytes)) {

				ClassReader classReader = new ClassReader(bytes);
				ClassWriter classWriter = new OSGiClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES,
						bundleWiring);

				try {

					JSF_OSGiClassVisitor osgiClassLoaderVisitor = new JSF_OSGiClassVisitor(classWriter, className);
					classReader.accept(osgiClassLoaderVisitor, ClassReader.SKIP_FRAMES);

					if (osgiClassLoaderVisitor.isClassModified()) {

						wovenClass.setBytes(classWriter.toByteArray());

						List<String> dynamicImports = wovenClass.getDynamicImports();
						dynamicImports.add(OSGI_CLASS_LOADER_DYNAMIC_IMPORT);
					}
				}
				catch (CommonSuperClassNotFoundException e) {
					logService.log(LogService.LOG_WARNING,
						"Unable to weave " + className +
						" for use with OSGi. Unexpected class loading errors may occur when using this class. Weaving failed due to the following error(s):",
						e);
				}
			}
		}
	}
}
