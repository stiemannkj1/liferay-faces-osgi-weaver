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

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.util.CheckClassAdapter;


/**
 * @author  Kyle Stiemann
 */
public abstract class TestCheckClassAdapter extends CheckClassAdapter {

	public TestCheckClassAdapter(ClassVisitor cv) {
		super(cv);
	}

	public TestCheckClassAdapter(ClassVisitor cv, boolean checkDataFlow) {
		super(cv, checkDataFlow);
	}

	public TestCheckClassAdapter(int api, ClassVisitor cv, boolean checkDataFlow) {
		super(api, cv, checkDataFlow);
	}
}
