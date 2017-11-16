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

import java.util.Collections;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.ServletContext;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;


/**
 * @author  Kyle Stiemann
 */
public final class FacesBundleUtil {

	// Logger
	private static final Log logger = LogFactoryUtil.getLog(FacesBundleUtil.class);

	// Private Constants
	private static final boolean FRAMEWORK_UTIL_DETECTED;

	static {

		boolean frameworkUtilDetected = false;

		try {

			Class.forName("org.osgi.framework.FrameworkUtil");
			frameworkUtilDetected = true;
		}
		catch (Throwable t) {

			if (!((t instanceof NoClassDefFoundError) || (t instanceof ClassNotFoundException))) {

				logger.error("An unexpected error occurred when attempting to detect OSGi:");
				logger.error(t);
			}
		}

		FRAMEWORK_UTIL_DETECTED = frameworkUtilDetected;
	}

	private FacesBundleUtil() {
		throw new AssertionError();
	}

	public static Bundle getCurrentFacesWab(Object context) {

		BundleContext bundleContext = (BundleContext) getServletContextAttribute(context, "osgi-bundlecontext");

		return bundleContext.getBundle();
	}

	public static Set<Bundle> getFacesBundles(Object context) {

		Set<Bundle> facesBundles;

		if (FRAMEWORK_UTIL_DETECTED) {

			facesBundles = (Set<Bundle>) getServletContextAttribute(context, FacesBundleUtil.class.getName());

			if (facesBundles == null) {

				facesBundles = new HashSet<Bundle>();

				Bundle wabBundle = getCurrentFacesWab(context);
				facesBundles.add(wabBundle);

				// If the WAB's dependencies are not contained in the WAB's WEB-INF/lib, find all the WAB's
				// dependencies and return them as well.
				if (!FacesBundleUtil.isCurrentBundleThickWab()) {

					addRequiredBundlesRecurse(facesBundles, wabBundle);
					addBridgeImplBundles(facesBundles);
				}

				facesBundles = Collections.unmodifiableSet(facesBundles);
				setServletContextAttribute(context, FacesBundleUtil.class.getName(), facesBundles);
			}
		}
		else {
			facesBundles = Collections.emptySet();
		}

		return facesBundles;
	}

	public static boolean isCurrentBundleThickWab() {

		Bundle bundle = FrameworkUtil.getBundle(FacesBundleUtil.class);

		return isWab(bundle);
	}

	public static boolean isWab(Bundle bundle) {

		Dictionary<String, String> headers = bundle.getHeaders();
		String webContextPathHeader = headers.get("Web-ContextPath");

		return webContextPathHeader != null;
	}

	private static void addBridgeImplBundles(Set<Bundle> facesBundles) {

		for (Bundle bundle : facesBundles) {

			if (isBridgeBundle(bundle, "api")) {

				BundleWiring bundleWiring = bundle.adapt(BundleWiring.class);
				List<BundleWire> bundleWires = bundleWiring.getProvidedWires(BundleRevision.PACKAGE_NAMESPACE);
				boolean addedBridgeImplBundle = false;
				boolean addedBridgeExtBundle = false;

				for (BundleWire bundleWire : bundleWires) {

					Bundle bundleDependingOnBridgeAPI = bundleWire.getRequirer().getBundle();

					if (isBridgeBundle(bundleDependingOnBridgeAPI, "impl")) {

						facesBundles.add(bundleDependingOnBridgeAPI);
						addRequiredBundlesRecurse(facesBundles, bundleDependingOnBridgeAPI);
						addedBridgeImplBundle = true;
					}
					else if (isBridgeBundle(bundleDependingOnBridgeAPI, "ext")) {

						facesBundles.add(bundleDependingOnBridgeAPI);
						addRequiredBundlesRecurse(facesBundles, bundleDependingOnBridgeAPI);
						addedBridgeExtBundle = true;
					}

					if (addedBridgeImplBundle && addedBridgeExtBundle) {
						break;
					}
				}

				break;
			}
		}
	}

	private static void addRequiredBundlesRecurse(Set<Bundle> facesBundles, Bundle bundle) {

		BundleWiring bundleWiring = bundle.adapt(BundleWiring.class);
		List<BundleWire> bundleWires = bundleWiring.getRequiredWires(BundleRevision.PACKAGE_NAMESPACE);

		for (BundleWire bundleWire : bundleWires) {

			bundle = bundleWire.getProvider().getBundle();

			if (!((bundle.getBundleId() == 0) || facesBundles.contains(bundle))) {

				facesBundles.add(bundle);
				addRequiredBundlesRecurse(facesBundles, bundle);
			}
		}
	}

	private static Object getServletContextAttribute(Object context, String servletContextAttributeName) {

		Object servletContextAttributeValue;
		boolean isFacesContext = context instanceof FacesContext;

		if (isFacesContext || (context instanceof ExternalContext)) {

			ExternalContext externalContext;

			if (isFacesContext) {

				FacesContext facesContext = (FacesContext) context;
				externalContext = facesContext.getExternalContext();
			}
			else {
				externalContext = (ExternalContext) context;
			}

			Map<String, Object> applicationMap = externalContext.getApplicationMap();
			servletContextAttributeValue = applicationMap.get(servletContextAttributeName);
		}
		else if (context instanceof ServletContext) {

			ServletContext servletContext = (ServletContext) context;
			servletContextAttributeValue = servletContext.getAttribute(servletContextAttributeName);
		}
		else {
			throw new IllegalArgumentException("context [" + context.getClass().getName() + "] is not an instanceof " +
				FacesContext.class.getName() + " or " + ExternalContext.class.getName() + " or " +
				ServletContext.class.getName());
		}

		return servletContextAttributeValue;
	}

	private static boolean isBridgeBundle(Bundle bundle, String bundleSymbolicNameSuffix) {

		String bundleSymbolicName = "com.liferay.faces.bridge." + bundleSymbolicNameSuffix;

		return bundleSymbolicName.equals(bundle.getHeaders().get("Bundle-SymbolicName"));
	}

	private static void setServletContextAttribute(Object context, String servletContextAttributeName,
		Object servletContextAttributeValue) {

		boolean isFacesContext = context instanceof FacesContext;

		if (isFacesContext || (context instanceof ExternalContext)) {

			ExternalContext externalContext;

			if (isFacesContext) {

				FacesContext facesContext = (FacesContext) context;
				externalContext = facesContext.getExternalContext();
			}
			else {
				externalContext = (ExternalContext) context;
			}

			Map<String, Object> applicationMap = externalContext.getApplicationMap();
			applicationMap.put(servletContextAttributeName, servletContextAttributeValue);
		}
		else if (context instanceof ServletContext) {

			ServletContext servletContext = (ServletContext) context;
			servletContext.setAttribute(servletContextAttributeName, servletContextAttributeValue);
		}
		else {
			throw new IllegalArgumentException("context [" + context.getClass().getName() + "] is not an instanceof " +
				FacesContext.class.getName() + " or " + ExternalContext.class.getName() + " or " +
				ServletContext.class.getName());
		}
	}
}
