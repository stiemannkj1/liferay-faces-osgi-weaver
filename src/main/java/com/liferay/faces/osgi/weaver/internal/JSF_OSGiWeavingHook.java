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
import org.osgi.framework.Version;
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
	private static final String MOJARRA_BUNDLE_SYMBOLIC_NAME = "org.glassfish.javax.faces";
	private static final String PRIMEFACES_BUNDLE_SYMBOLIC_NAME = "org.primefaces";

	/**
	 * For more details on Java class format/target versions see here: <a
	 * href="https://blogs.oracle.com/darcy/source%2c-target%2c-class-file-version-decoder-ring">
	 * https://blogs.oracle.com/darcy/source%2c-target%2c-class-file-version-decoder-ring</a>
	 */
	private static final short JAVA_1_6_MAJOR_VERSION = 50;
	private static final String OSGI_CLASS_LOADER_DYNAMIC_IMPORT =
		"com.liferay.faces.util.osgi;version=\"[1.0.0,2.0.0)\"";

	// Private Data Members
	private LogService logService;

	public JSF_OSGiWeavingHook(LogService logService) {
		this.logService = logService;
	}

	/* package-private */ static boolean isWeaveBundle(Bundle bundle) {

		String bundleSymbolicName = bundle.getSymbolicName();

		return MOJARRA_BUNDLE_SYMBOLIC_NAME.equals(bundleSymbolicName) || isPrimeFaces_6_2_OrLower(bundle);
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
		ByteBuffer byteBuffer = ByteBuffer.wrap(classBytes, CLASS_MAJOR_VERSION_BYTE_OFFSET,
				CLASS_MAJOR_VERSION_BYTE_SIZE);
		short majorVersion = byteBuffer.getShort();

		return majorVersion >= JAVA_1_6_MAJOR_VERSION;
	}

	private static boolean isMojarraSPIClass(String className) {
		return className.startsWith("com.sun.faces.spi") || className.startsWith("com.sun.faces.config.configprovider");
	}

	private static boolean isPrimeFaces_6_2_OrLower(Bundle bundle) {

		boolean primeFaces_6_2_OrLower = false;
		String bundleSymbolicName = bundle.getSymbolicName();

		if (PRIMEFACES_BUNDLE_SYMBOLIC_NAME.equals(bundleSymbolicName)) {

			Version version = bundle.getVersion();
			primeFaces_6_2_OrLower = ((version.getMajor() == 6) && (version.getMinor() <= 2)) ||
				(version.getMajor() < 6);
		}

		return primeFaces_6_2_OrLower;
	}

	@Override
	public void weave(WovenClass wovenClass) {

		String className = wovenClass.getClassName();
		BundleWiring bundleWiring = wovenClass.getBundleWiring();
		Bundle bundle = bundleWiring.getBundle();

		if (!isMojarraSPIClass(className) && isWeaveBundle(bundle)) {

			byte[] bytes = wovenClass.getBytes();

			// ASM cannot handle classes compiled with Java 1.5 or lower without using JSRInlinerAdapter (TODO use
			// JSRInlinerAdapter to support classes compiled with target 1.5 and below in the future). For more
			// information, see: https://stackoverflow.com/questions/37013761/status-of-jsr-ret-in-jvm-spec,
			// https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html#jvms-4.10.2.5, and
			// https://asm.ow2.io/javadoc/org/objectweb/asm/commons/JSRInlinerAdapter.html.
			if (isCompiledWithJava_1_6_OrGreater(bytes)) {

				ClassReader classReader = new ClassReader(bytes);
				OSGiClassWriter osgiClassWriter = new OSGiClassWriter(ClassWriter.COMPUTE_MAXS |
						ClassWriter.COMPUTE_FRAMES, bundleWiring);

				try {

					JSF_OSGiClassVisitor jsfOSGiClassVisitor = new JSF_OSGiClassVisitor(!isPrimeFaces_6_2_OrLower(
								bundle), osgiClassWriter, className);
					classReader.accept(jsfOSGiClassVisitor, ClassReader.SKIP_FRAMES);

					if (jsfOSGiClassVisitor.isClassModified()) {

						wovenClass.setBytes(osgiClassWriter.toByteArray());

						List<String> dynamicImports = wovenClass.getDynamicImports();
						dynamicImports.add(OSGI_CLASS_LOADER_DYNAMIC_IMPORT);
					}
				}
				catch (CommonSuperClassNotFoundException e) {
					logService.log(LogService.LOG_DEBUG,
						"Unable to weave " + className + " due to the following error(s):", e);
				}
			}
			else {
				logService.log(LogService.LOG_DEBUG,
					"Unable to weave " + className +
					" since it is not compiled with Java (target) 1.6+. Classes compiled for Java 1.5 and below may contain jsr and ret bytecode instructions which cannot be handled by this bytecode weaver.");
			}
		}
	}
}
