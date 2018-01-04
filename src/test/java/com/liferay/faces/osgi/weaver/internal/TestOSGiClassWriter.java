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

import org.junit.Assert;
import org.junit.Test;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.util.CheckClassAdapter;

import static com.liferay.faces.osgi.weaver.internal.JSF_OSGiMethodVisitor.getTypeString;


/**
 * @author  Kyle Stiemann
 */
public class TestOSGiClassWriter {

	// Private Constants
	private static final String OBJECT_TYPE_STRING = getTypeString(Object.class);
	private static final String CLASS_READER_TYPE_STRING = getTypeString(ClassReader.class);
	private static final String CLASS_VISITOR_TYPE_STRING = getTypeString(ClassVisitor.class);
	private static final String OSGI_CLASS_WRITER_TYPE_STRING = getTypeString(OSGiClassWriter.class);
	private static final String CLASS_WRITER_TYPE_STRING = getTypeString(ClassWriter.class);
	private static final String CHECK_CLASS_ADAPTER_TYPE_STRING = getTypeString(CheckClassAdapter.class);
	private static final String TEST_CHECK_CLASS_ADAPTER_TYPE_STRING = getTypeString(TestCheckClassAdapter.class);

	@Test
	public void testGetCommonSuperClass() {

		Class<? extends TestOSGiClassWriter> clazz = this.getClass();
		ClassLoader classLoader = clazz.getClassLoader();

		OSGiClassWriter osgiClassWriter = new OSGiClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES,
				new BundleWiringMockImpl(classLoader));
		String actualCommonSuperClass = osgiClassWriter.getCommonSuperClass(OBJECT_TYPE_STRING, OBJECT_TYPE_STRING);
		Assert.assertEquals(OBJECT_TYPE_STRING, actualCommonSuperClass);
		actualCommonSuperClass = osgiClassWriter.getCommonSuperClass(OSGI_CLASS_WRITER_TYPE_STRING, OBJECT_TYPE_STRING);
		Assert.assertEquals(OBJECT_TYPE_STRING, actualCommonSuperClass);
		actualCommonSuperClass = osgiClassWriter.getCommonSuperClass(OSGI_CLASS_WRITER_TYPE_STRING,
				OSGI_CLASS_WRITER_TYPE_STRING);
		Assert.assertEquals(OSGI_CLASS_WRITER_TYPE_STRING, actualCommonSuperClass);

		// Test obtaining common ancestor.
		actualCommonSuperClass = osgiClassWriter.getCommonSuperClass(OSGI_CLASS_WRITER_TYPE_STRING,
				CLASS_WRITER_TYPE_STRING);
		Assert.assertEquals(CLASS_WRITER_TYPE_STRING, actualCommonSuperClass);

		// Test obtiaining common ancestor where one class is the common ancestor of the other.
		actualCommonSuperClass = osgiClassWriter.getCommonSuperClass(OSGI_CLASS_WRITER_TYPE_STRING,
				CLASS_WRITER_TYPE_STRING);
		Assert.assertEquals(CLASS_WRITER_TYPE_STRING, actualCommonSuperClass);

		// Test obtiaining common ancestor where classes are differing distances from the ancestor.
		actualCommonSuperClass = osgiClassWriter.getCommonSuperClass(OSGI_CLASS_WRITER_TYPE_STRING,
				TEST_CHECK_CLASS_ADAPTER_TYPE_STRING);
		Assert.assertEquals(CLASS_VISITOR_TYPE_STRING, actualCommonSuperClass);

		// Test obtaining common ancestor when Object is the only common ancestor.
		actualCommonSuperClass = osgiClassWriter.getCommonSuperClass(CLASS_READER_TYPE_STRING,
				TEST_CHECK_CLASS_ADAPTER_TYPE_STRING);
		Assert.assertEquals(OBJECT_TYPE_STRING, actualCommonSuperClass);

		// Test obtaining common ancestor when elements of the class hierarchy are unreachable.
		BlockTypeResourceClassLoader blockClassVisitorTypeResourceClassLoader = new BlockTypeResourceClassLoader(
				CLASS_VISITOR_TYPE_STRING, classLoader);
		Assert.assertNull(blockClassVisitorTypeResourceClassLoader.getResource(CLASS_VISITOR_TYPE_STRING + ".class"));
		osgiClassWriter = new OSGiClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES,
				new BundleWiringMockImpl(blockClassVisitorTypeResourceClassLoader));
		actualCommonSuperClass = osgiClassWriter.getCommonSuperClass(TEST_CHECK_CLASS_ADAPTER_TYPE_STRING,
				CHECK_CLASS_ADAPTER_TYPE_STRING);
		Assert.assertEquals(CHECK_CLASS_ADAPTER_TYPE_STRING, actualCommonSuperClass);
		actualCommonSuperClass = osgiClassWriter.getCommonSuperClass(TEST_CHECK_CLASS_ADAPTER_TYPE_STRING,
				CLASS_VISITOR_TYPE_STRING);
		Assert.assertEquals(CLASS_VISITOR_TYPE_STRING, actualCommonSuperClass);
	}

	@Test
	public void testGetCommonSuperClassExceptions() {

		Class<? extends TestOSGiClassWriter> clazz = this.getClass();
		ClassLoader classLoader = clazz.getClassLoader();
		BlockTypeResourceClassLoader blockClassVisitorTypeResourceClassLoader = new BlockTypeResourceClassLoader(
				CHECK_CLASS_ADAPTER_TYPE_STRING, classLoader);
		Assert.assertNull(blockClassVisitorTypeResourceClassLoader.getResource(
				CHECK_CLASS_ADAPTER_TYPE_STRING + ".class"));

		OSGiClassWriter osgiClassWriter = new OSGiClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES,
				new BundleWiringMockImpl(blockClassVisitorTypeResourceClassLoader));

		try {

			String actualCommonSuperClass = osgiClassWriter.getCommonSuperClass(OSGI_CLASS_WRITER_TYPE_STRING,
					TEST_CHECK_CLASS_ADAPTER_TYPE_STRING);
			Assert.fail("OsgiClassWriter.getCommonSuperClass() failed to throw a " +
				CommonSuperClassNotFoundException.class.getName() +
				" when necessary elements of the class hierarchy were inaccessible. The following incorrect common super class was obtained: " +
				actualCommonSuperClass);
		}
		catch (CommonSuperClassNotFoundException e) {
			// Test passed.
		}
		catch (AssertionError e) {
			throw e;
		}
		catch (Throwable t) {
			Assert.fail("OsgiClassWriter.getCommonSuperClass() threw exception " + t.getClass().getName() +
				" instead of " + CommonSuperClassNotFoundException.class.getName());
		}
	}
}
