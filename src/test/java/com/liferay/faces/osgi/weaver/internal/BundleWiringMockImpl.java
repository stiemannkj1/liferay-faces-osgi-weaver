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

import java.net.URL;
import java.util.Collection;
import java.util.List;

import org.osgi.framework.Bundle;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;

import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Wire;


/**
 * @author  Kyle Stiemann
 */
public class BundleWiringMockImpl implements BundleWiring {

	// Private Constants
	private static final Bundle BUNDLE = new BundleMockImpl();

	// Private Final Data Members
	private final ClassLoader classLoader;

	public BundleWiringMockImpl(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	@Override
	public List<URL> findEntries(String path, String filePattern, int options) {
		throw new UnsupportedOperationException("");
	}

	@Override
	public Bundle getBundle() {
		return BUNDLE;
	}

	@Override
	public List<BundleCapability> getCapabilities(String namespace) {
		throw new UnsupportedOperationException("");
	}

	@Override
	public ClassLoader getClassLoader() {
		return classLoader;
	}

	@Override
	public List<Wire> getProvidedResourceWires(String namespace) {
		throw new UnsupportedOperationException("");
	}

	@Override
	public List<BundleWire> getProvidedWires(String namespace) {
		throw new UnsupportedOperationException("");
	}

	@Override
	public List<Wire> getRequiredResourceWires(String namespace) {
		throw new UnsupportedOperationException("");
	}

	@Override
	public List<BundleWire> getRequiredWires(String namespace) {
		throw new UnsupportedOperationException("");
	}

	@Override
	public List<BundleRequirement> getRequirements(String namespace) {
		throw new UnsupportedOperationException("");
	}

	@Override
	public BundleRevision getResource() {
		throw new UnsupportedOperationException("");
	}

	@Override
	public List<Capability> getResourceCapabilities(String namespace) {
		throw new UnsupportedOperationException("");
	}

	@Override
	public List<Requirement> getResourceRequirements(String namespace) {
		throw new UnsupportedOperationException("");
	}

	@Override
	public BundleRevision getRevision() {
		throw new UnsupportedOperationException("");
	}

	@Override
	public boolean isCurrent() {
		throw new UnsupportedOperationException("");
	}

	@Override
	public boolean isInUse() {
		throw new UnsupportedOperationException("");
	}

	@Override
	public Collection<String> listResources(String path, String filePattern, int options) {
		throw new UnsupportedOperationException("");
	}
}
