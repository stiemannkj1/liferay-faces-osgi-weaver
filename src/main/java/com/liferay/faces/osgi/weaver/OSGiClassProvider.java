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
package com.liferay.faces.osgi.weaver;

import java.net.URL;
import java.util.Set;

import javax.faces.context.FacesContext;

import org.osgi.framework.Bundle;
import org.osgi.framework.wiring.BundleWiring;


/**
 * @author  Kyle Stiemann
 */
public class OSGiClassProvider {

	public static Class<?> classForName(String name) throws ClassNotFoundException {
		return getClass(name, true, OSGiClassProvider.class.getClassLoader());
	}

	public static Class<?> classForName(String name, boolean initialize, ClassLoader suggestedClassLoader)
		throws ClassNotFoundException {
		return getClass(name, initialize, suggestedClassLoader);
	}

	public static Class<?> loadClass(String name, ClassLoader suggestedLoader) throws ClassNotFoundException {
		return getClass(name, null, suggestedLoader);
	}

	private static Class<?> getClass(String name, Boolean initialize, ClassLoader suggestedClassLoader)
		throws ClassNotFoundException {

		Class<?> clazz = null;
		FacesContext facesContext = FacesContext.getCurrentInstance();

		if ((facesContext != null) && !FacesBundleUtil.isCurrentBundleThickWab()) {

			Set<Bundle> facesBundles = FacesBundleUtil.getFacesBundles(facesContext);

			for (Bundle bundle : facesBundles) {

				if (!isClassFileInBundle(name, bundle)) {
					continue;
				}

				BundleWiring bundleWiring = bundle.adapt(BundleWiring.class);
				ClassLoader classLoader = bundleWiring.getClassLoader();

				try {

					if (initialize != null) {
						clazz = Class.forName(name, initialize, classLoader);
					}
					else {
						clazz = classLoader.loadClass(name);
					}

					break;
				}
				catch (ClassNotFoundException e) {
					// no-op
				}
			}
		}

		if (clazz == null) {

			if (initialize != null) {
				clazz = Class.forName(name, initialize, suggestedClassLoader);
			}
			else {
				clazz = suggestedClassLoader.loadClass(name);
			}
		}

		return clazz;
	}

	private static boolean isClassFileInBundle(String className, Bundle bundle) {

		String classFilePath = "/" + className.replace(".", "/") + ".class";
		URL classFileURL = bundle.getResource(classFilePath);

		return classFileURL != null;
	}
}
