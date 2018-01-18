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
/**
 * Copyright (c) 2000-2018 Liferay, Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.liferay.faces.osgi.weaver.internal;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.hooks.weaving.WeavingHook;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.log.LogService;


/**
 * This class exists to work around <a href="https://issues.apache.org/jira/browse/FELIX-5570">FELIX-5570</a>.
 *
 * @author  Kyle Stiemann
 */
@Component(immediate = true)
public final class JSF_OSGiWeaver {

	// Private Data Member
	@Reference
	private LogService logService;
	private ServiceRegistration weavingHookService;

	@Activate
	/* package-private */ synchronized void activate(BundleContext bundleContext) throws Exception {

		// Avoid using Declarative Services to register the weaving hook to work around
		// https://issues.apache.org/jira/browse/FELIX-5570.
		weavingHookService = bundleContext.registerService(WeavingHook.class, new JSF_OSGiWeavingHook(logService),
				null);
	}

	@Deactivate
	/* package-private */ synchronized void deactivate(BundleContext bundleContext) throws Exception {
		weavingHookService.unregister();
	}
}
