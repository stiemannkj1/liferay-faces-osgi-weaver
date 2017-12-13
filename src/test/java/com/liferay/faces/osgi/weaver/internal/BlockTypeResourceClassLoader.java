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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;


/**
 * @author  Kyle Stiemann
 */
public class BlockTypeResourceClassLoader extends ClassLoader {

	// Private Final Data Members
	private final String blockedType;

	public BlockTypeResourceClassLoader(String blockedType, ClassLoader parent) {

		super(parent);
		this.blockedType = blockedType;
	}

	@Override
	public URL getResource(String name) {

		URL url = null;

		if (!name.equals(blockedType + ".class")) {
			url = super.getResource(name);
		}

		return url;
	}

	@Override
	public InputStream getResourceAsStream(String name) {

		InputStream inputStream = null;

		if (!name.equals(blockedType + ".class")) {
			inputStream = super.getResourceAsStream(name);
		}

		return inputStream;
	}

	@Override
	public Enumeration<URL> getResources(String name) throws IOException {

		Enumeration<URL> enumeration = Collections.emptyEnumeration();

		if (!name.equals(blockedType + ".class")) {
			enumeration = super.getResources(name);
		}

		return enumeration;
	}
}
