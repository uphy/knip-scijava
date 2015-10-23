package org.knime.scijava.core;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.osgi.util.ManifestElement;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;

/**
 * ResourceAwareClassLoader
 * 
 * Class loader which is aware of bundle resources.
 * 
 * @author Christian Dietz (University of Konstanz)
 * @author Jonathan Hale (University of Konstanz)
 *
 */
public class ResourceAwareClassLoader extends ClassLoader {

	/**
	 * Resources which need to be treated in a special way.
	 */
	// TODO make an extension point to add resources as needed (later)
	private final String[] RESOURCES = new String[] {
			"META-INF/json/org.scijava.plugin.Plugin",
			"META-INF/services/javax.script.ScriptEngineFactory" };

	private final Map<String, Set<URL>> urls = new HashMap<String, Set<URL>>();
	private final Set<URL> bundleUrls = new HashSet<URL>();

	/**
	 * Constructor.
	 * 
	 * Parses current bundle resources of required bundles of
	 * org.knime.knip.scijava.core and caches their urls.
	 * 
	 * @param parent
	 *            The parent class loader
	 * @deprecated use {@link #ResourceAwareClassLoader(ClassLoader, Class)}
	 *             instead.
	 */
	public ResourceAwareClassLoader(final ClassLoader parent) {
		this(parent, null);
	}

	/**
	 * Constructor.
	 * 
	 * Parses current bundle resources of required bundles of c's bundle and
	 * caches their urls.
	 * 
	 * @param parent
	 *            The parent class loader
	 * @param clazz
	 *            Class whose bundles requirements will be parsed
	 */
	public ResourceAwareClassLoader(final ClassLoader parent, Class<?> clazz) {
		super(parent);

		if (clazz == null) {
			clazz = getClass();
		}

		// initialize urls map
		for (final String res : RESOURCES) {
			urls.put(res, new HashSet<URL>());
		}

		final String requireBundle = (String) FrameworkUtil.getBundle(clazz)
				.getHeaders().get(Constants.REQUIRE_BUNDLE);
		try {
			final ManifestElement[] elements = ManifestElement.parseHeader(
					Constants.BUNDLE_CLASSPATH, requireBundle);
			for (final ManifestElement manifestElement : elements) {
				final Bundle bundle = org.eclipse.core.runtime.Platform
						.getBundle(manifestElement.getValue());

				try {
					// get file url for this bundle
					bundleUrls.add(new URL("file://"
							+ FileLocator.getBundleFile(bundle)
									.getAbsolutePath()));
				} catch (MalformedURLException e1) {
					e1.printStackTrace();
				} catch (IOException e1) {
					e1.printStackTrace();
				}

				for (final String res : RESOURCES) {
					Enumeration<URL> resources;
					try {
						resources = bundle.getResources(res);
					} catch (IOException e) {
						continue;
					}

					if (resources == null) {
						continue;
					}

					while (resources.hasMoreElements()) {
						final URL resource = resources.nextElement();
						// we want to avoid transitive resolving of dependencies
						final String host = resource.getHost();
						if (bundle.getBundleId() == Long.valueOf(host
								.substring(0, host.indexOf(".")))) {
							safeAdd(urls.get(res), resource);
						}
					}
				}
			}
		} catch (BundleException e) {
			e.printStackTrace();
		}
	}

	@Override
	public Enumeration<URL> getResources(final String name) throws IOException {
		final Set<URL> urlList = urls.get(name);
		if (urlList == null) {
			// nothing special to do here
			return super.getResources(name);
		}

		for(final URL url : Collections.list(super.getResources(name))){
			safeAdd(urlList, url);
		}
		
		return Collections.enumeration(urlList);
	}
	
	/**
	 * Get a set of file URLs to the bundles dependency bundles.
	 * @return set of dependency bundle file urls
	 */
	public Set<URL> getBundleUrls() {
		return bundleUrls;
	}
	
	/*
	 * Add url to urls while making sure, that the resulting file urls are
	 * always unique.
	 * @param urls Set to add the url to
	 * @param urlToAdd Url to add to the set
	 * @see FileLocator
	 */
	private static void safeAdd(final Set<URL> urls, final URL urlToAdd) {
		// make sure the resulting file url is not in urls already
		try {
			final URL fileToAdd = FileLocator.resolve(urlToAdd);

			for (final URL url : urls) {
				if (fileToAdd.equals(FileLocator.resolve(url))) {
					// we found a duplicate, do not add.
					return;
				}
			}
		} catch (IOException e) {
			// ignore
		}

		// no duplicate found, we can safely add this url.
		urls.add(urlToAdd);
	}
	
}