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

import org.apache.aries.spifly.dynamic.OSGiFriendlyClassWriter;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import org.osgi.framework.Bundle;
import org.osgi.framework.hooks.weaving.WeavingHook;
import org.osgi.framework.hooks.weaving.WovenClass;
import org.osgi.framework.wiring.BundleWiring;


/**
 * @author  Kyle Stiemann
 */
public class JSF_OSGiWeavingHook implements WeavingHook {

	// Private Constants
	private static final List<String> HANDLED_BUNDLE_SYMBOLIC_NAMES = Collections.unmodifiableList(Arrays.asList(
				"org.glassfish.javax.faces", "com.liferay.faces.util", "com.liferay.faces.bridge.api",
				"com.liferay.faces.bridge.impl", "com.liferay.faces.bridge.ext"));

	/**
	 * For more details on Java class format/target versions see here: <a
	 * href="https://blogs.oracle.com/darcy/source%2c-target%2c-class-file-version-decoder-ring">
	 * https://blogs.oracle.com/darcy/source%2c-target%2c-class-file-version-decoder-ring</a>
	 */
	private static final short JAVA_1_6_MAJOR_VERSION = 50;
	private static final int CLASS_MAJOR_VERSION_BYTE_OFFSET = 6;
	private static final int CLASS_MAJOR_VERSION_BYTE_SIZE = 2;
	private static final String OSGI_CLASS_PROVIDER_PACKAGE_NAME = "com.liferay.faces.osgi.util";
	private static final String OSGI_CLASS_PROVIDER_DYNAMIC_IMPORT = OSGI_CLASS_PROVIDER_PACKAGE_NAME +
		";bundle-symbolic-name=" + OSGI_CLASS_PROVIDER_PACKAGE_NAME;

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
		// Byte Offset: 0  1  2	 3	4  5  6	 7
		// Bytes:	   CA FE BA BE 00 00 00 33
		//J+
		ByteBuffer buffer = ByteBuffer.wrap(classBytes, CLASS_MAJOR_VERSION_BYTE_OFFSET, CLASS_MAJOR_VERSION_BYTE_SIZE);
		short majorVersion = buffer.getShort();

		return majorVersion >= JAVA_1_6_MAJOR_VERSION;
	}

	private static boolean isFacesWab(Bundle bundle) {

		Dictionary<String, String> headers = bundle.getHeaders();
		String importPackageHeader = headers.get("Import-Package");

		return isWab(bundle) && importPackageHeader.contains("javax.faces");
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
		String bundleSymbolicName = bundle.getSymbolicName();

		if (className.startsWith("com.liferay.faces.osgi") ||
				className.startsWith("com.liferay.faces.bridge.ext.mojarra.spi") ||
				(!HANDLED_BUNDLE_SYMBOLIC_NAMES.contains(bundleSymbolicName) && !isFacesWab(bundle))) {
			return;
		}

		byte[] bytes = wovenClass.getBytes();

		// ASM cannot handle classes compiled with Java 1.5 or lower.
		if (!isCompiledWithJava_1_6_OrGreater(bytes)) {
			return;
		}

		ClassReader classReader = new ClassReader(bytes);
		ClassLoader bundleClassLoader = bundleWiring.getClassLoader();
		ClassWriter classWriter = new OSGiFriendlyClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES,
				bundleClassLoader);
		OSGiClassProviderVisitor osgiClassProviderVisitor = new OSGiClassProviderVisitor(classWriter, className);
		classReader.accept(osgiClassProviderVisitor, ClassReader.SKIP_FRAMES);

		if (osgiClassProviderVisitor.isClassModified()) {

			wovenClass.setBytes(classWriter.toByteArray());

			List<String> dynamicImports = wovenClass.getDynamicImports();
			dynamicImports.add(OSGI_CLASS_PROVIDER_DYNAMIC_IMPORT);
		}
	}
}
