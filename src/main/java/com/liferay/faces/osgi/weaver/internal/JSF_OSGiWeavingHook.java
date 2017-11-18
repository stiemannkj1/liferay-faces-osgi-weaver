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
	private static final short JAVA_1_6_MAJOR_VERSION = 50;
	private static final List<String> HANDLED_BUNDLE_SYMBOLIC_NAMES = Collections.unmodifiableList(Arrays.asList(
				"org.glassfish.javax.faces", "com.liferay.faces.util", "com.liferay.faces.bridge.api",
				"com.liferay.faces.bridge.impl", "com.liferay.faces.bridge.ext"));
	private static final String OSGI_CLASS_PROVIDER_PACKAGE_NAME = "com.liferay.faces.osgi.util";
	private static final String OSGI_CLASS_PROVIDER_DYNAMIC_IMPORT = OSGI_CLASS_PROVIDER_PACKAGE_NAME +
		";bundle-symbolic-name=" + OSGI_CLASS_PROVIDER_PACKAGE_NAME;

	public static boolean isWab(Bundle bundle) {

		Dictionary<String, String> headers = bundle.getHeaders();
		String webContextPathHeader = headers.get("Web-ContextPath");

		return webContextPathHeader != null;
	}

	private static boolean isCompiledWithJava_1_6_OrGreater(byte[] classBytes) {

		ByteBuffer buffer = ByteBuffer.wrap(classBytes, 6, 2);
		short majorVersion = buffer.getShort();

		return majorVersion >= JAVA_1_6_MAJOR_VERSION;
	}

	private static boolean isFacesWab(Bundle bundle) {

		Dictionary<String, String> headers = bundle.getHeaders();
		String importPackageHeader = headers.get("Import-Package");

		return isWab(bundle) && importPackageHeader.contains("javax.faces");
	}

	@Override
	public void weave(WovenClass wovenClass) {

		String className = wovenClass.getClassName();
		BundleWiring bundleWiring = wovenClass.getBundleWiring();
		Bundle bundle = bundleWiring.getBundle();
		String bundleSymbolicName = bundle.getSymbolicName();

		if (className.startsWith("com.liferay.faces.osgi") ||
				className.startsWith("com.liferay.faces.bridge.ext.mojarra") ||
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
		OSGiClassProviderVisitor osgiClassProvider = new OSGiClassProviderVisitor(classWriter);
		classReader.accept(osgiClassProvider, ClassReader.SKIP_FRAMES);

		if (osgiClassProvider.isClassModified()) {

			wovenClass.setBytes(classWriter.toByteArray());

			List<String> dynamicImports = wovenClass.getDynamicImports();
			dynamicImports.add(OSGI_CLASS_PROVIDER_DYNAMIC_IMPORT);
		}
	}
}
