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

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.objectweb.asm.ClassReader;


/**
 * This class is not thread safe.
 *
 * @author  Kyle Stiemann
 */
/* package-private */ final class IterableLazyTypeHierarchy implements Iterable<String> {

	// Private Final Data Members
	private final String initialClassType;
	private final ClassLoader classLoader;

	/* package-private */ IterableLazyTypeHierarchy(String initialClassType, ClassLoader classLoader) {

		this.initialClassType = initialClassType;
		this.classLoader = classLoader;
	}

	@Override
	public Iterator<String> iterator() {
		return new LazyTypeHierarchyIterator(initialClassType, classLoader);
	}

	private static final class LazyTypeHierarchyIterator implements Iterator<String> {

		// Private Final Data Members
		private final ClassLoader classLoader;

		// Private Data Members
		private InputStream nextTypeInputStream;
		private String previousType;
		private boolean firstIteration;
		private Boolean hasNext;

		private LazyTypeHierarchyIterator(String initialClassType, ClassLoader classLoader) {

			this.firstIteration = true;
			this.hasNext = firstIteration;
			this.previousType = initialClassType;
			this.classLoader = classLoader;
		}

		@Override
		public boolean hasNext() {

			if (hasNext == null) {

				if (!previousType.equals(OSGiClassWriter.OBJECT_TYPE_STRING)) {

					nextTypeInputStream = classLoader.getResourceAsStream(previousType + ".class");

					hasNext = (nextTypeInputStream != null);
				}
				else {
					hasNext = false;
				}
			}

			return hasNext;
		}

		@Override
		public String next() {

			if (!hasNext()) {
				throw new NoSuchElementException();
			}

			hasNext = null;

			String type;

			if (firstIteration) {

				type = previousType;
				firstIteration = false;
			}
			else {

				ClassReader typeClassReader;

				try {
					typeClassReader = new ClassReader(nextTypeInputStream);
				}
				catch (IOException e) {
					throw new RuntimeException(e);
				}
				finally {

					try {
						nextTypeInputStream.close();
					}
					catch (IOException e) {
						// do nothing.
					}
				}

				type = typeClassReader.getSuperName();

				if (type == null) {
					type = OSGiClassWriter.OBJECT_TYPE_STRING;
				}

				previousType = type;
			}

			return type;
		}
	}
}
