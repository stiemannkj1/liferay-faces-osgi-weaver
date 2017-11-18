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

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.hooks.weaving.WeavingHook;

import com.liferay.faces.osgi.weaver.internal.JSF_OSGiWeavingHook;


/**
 * @author  Kyle Stiemann
 */
public class ThinWabActivator implements BundleActivator {

	// Private Data Members
	private ServiceRegistration weavingHookService;
//  private BundleTracker bundleTracker;

	public static boolean isCurrentBundleThickWab() {

		Bundle bundle = FrameworkUtil.getBundle(ThinWabActivator.class);

		return JSF_OSGiWeavingHook.isWab(bundle);
	}

	@Override
	public synchronized void start(BundleContext context) throws Exception {

		weavingHookService = context.registerService(WeavingHook.class.getName(), new JSF_OSGiWeavingHook(), null);
//      TODO consider alerting bundles that depend on us that they should shut down when we shut down.
//      bundleTracker = new BundleTracker(context,
//          Bundle.INSTALLED | Bundle.RESOLVED | Bundle.STARTING | Bundle.ACTIVE, null);
//      bundleTracker.open();
	}

	@Override
	public synchronized void stop(BundleContext context) throws Exception {

//      bundleTracker.close();
		weavingHookService.unregister();
	}
}
