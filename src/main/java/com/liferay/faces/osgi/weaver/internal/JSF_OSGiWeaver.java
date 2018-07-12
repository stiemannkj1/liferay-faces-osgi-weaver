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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.List;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.hooks.weaving.WeavingHook;
import org.osgi.framework.wiring.FrameworkWiring;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.log.LogService;


/**
 * This class exists to work around <a href="https://issues.apache.org/jira/browse/FELIX-5570">FELIX-5570</a> and
 * refresh all Faces bundles to ensure that bytecode weaving occurs even on bundles deployed before the weaver was
 * activated.
 *
 * @author  Kyle Stiemann
 */
@Component(immediate = true)
public final class JSF_OSGiWeaver {

	//J-
	// Package-Private Constants
	/* package-private */ static final List<String> HANDLED_BUNDLE_SYMBOLIC_NAMES =
	Collections.unmodifiableList(Arrays.asList(
		"org.glassfish.javax.faces",
		"com.liferay.faces.util",
		"com.liferay.faces.bridge.impl",
		"org.primefaces"
	));
	//J+

	// Private Data Members
	@Reference
	private LogService logService;
	private ServiceRegistration weavingHookService;

	private static boolean isFacesWab(Bundle bundle) {

		Dictionary<String, String> headers = bundle.getHeaders();
		String importPackageHeader = headers.get("Import-Package");

		return isWab(bundle) && (importPackageHeader != null) && importPackageHeader.contains("javax.faces");
	}

	private static boolean isWab(Bundle bundle) {

		Dictionary<String, String> headers = bundle.getHeaders();
		String webContextPathHeader = headers.get("Web-ContextPath");

		return webContextPathHeader != null;
	}

	private static void restartFacesWabs(List<Bundle> facesWabs, LogService logService) {

		for (Bundle facesWab : facesWabs) {

			try {
				facesWab.start();
			}
			catch (BundleException e) {
				logService.log(LogService.LOG_ERROR,
					facesWab.getSymbolicName() + " failed to start due to the following error(s):", e);
			}
		}
	}

	@Activate
	/* package-private */ synchronized void activate(BundleContext bundleContext) throws BundleException {

		// Avoid using Declarative Services to register the weaving hook to work around
		// https://issues.apache.org/jira/browse/FELIX-5570.
		weavingHookService = bundleContext.registerService(WeavingHook.class, new JSF_OSGiWeavingHook(logService),
				null);

		// Refresh deployed Faces bundles to ensure that bytecode weaving occurs even on bundles deployed before the
		// weaver was activated.
		Bundle systemBundle = bundleContext.getBundle(0);
		FrameworkWiring frameworkWiring = systemBundle.adapt(FrameworkWiring.class);
		List<Bundle> facesBundles = new ArrayList<Bundle>();
		List<Bundle> facesWabs = new ArrayList<Bundle>();
		Bundle[] bundles = bundleContext.getBundles();

		for (Bundle bundle : bundles) {

			String bundleSymbolicName = bundle.getSymbolicName();

			if (HANDLED_BUNDLE_SYMBOLIC_NAMES.contains(bundleSymbolicName)) {
				facesBundles.add(bundle);
			}
			else if (isFacesWab(bundle)) {

				int facesWabState = bundle.getState();

				if ((facesWabState == Bundle.STARTING) || (facesWabState == Bundle.ACTIVE)) {

					bundle.stop();
					facesWabs.add(bundle);
				}
			}
		}

		if (!facesBundles.isEmpty()) {
			frameworkWiring.refreshBundles(facesBundles, new FacesBundlesRefreshListener(facesWabs, logService));
		}
		else if (!facesWabs.isEmpty()) {
			restartFacesWabs(Collections.unmodifiableList(facesWabs), logService);
		}
	}

	@Deactivate
	/* package-private */ synchronized void deactivate(BundleContext bundleContext) {
		weavingHookService.unregister();
	}

	private static final class FacesBundlesRefreshListener implements FrameworkListener {

		// Private Final Data Members
		private final List<Bundle> facesWabs;
		private final LogService logService;

		public FacesBundlesRefreshListener(List<Bundle> facesWabs, LogService logService) {

			this.facesWabs = Collections.unmodifiableList(facesWabs);
			this.logService = logService;
		}

		@Override
		public void frameworkEvent(FrameworkEvent frameworkEvent) {

			int eventType = frameworkEvent.getType();

			if (eventType == FrameworkEvent.PACKAGES_REFRESHED) {
				restartFacesWabs(facesWabs, logService);
			}
		}
	}
}
