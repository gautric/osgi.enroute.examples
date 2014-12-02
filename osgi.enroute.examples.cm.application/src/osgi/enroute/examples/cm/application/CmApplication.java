package osgi.enroute.examples.cm.application;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import org.osgi.dto.DTO;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.cm.ConfigurationListener;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

import osgi.enroute.capabilities.AngularWebResource;
import osgi.enroute.capabilities.BootstrapWebResource;
import osgi.enroute.capabilities.ConfigurerExtender;
import osgi.enroute.capabilities.EventAdminSSEEndpoint;
import osgi.enroute.capabilities.WebServerExtender;
import osgi.enroute.dto.api.DTOs;

/**
 * CM Application.
 * 
 * This is the CM Example application main class. It is mainly there to require
 * the different components that are needed to run this app.
 * 
 * This application consists of the following classes:
 * <ul>
 * <li>{@link CmFacade} – Provides the facade to the application from the
 * outside world.
 * <li>{@link Configuration2EventAdmin} – Forwards events about CM to the Event
 * Admin (which forwards it to the browser).
 * <li>{@link ConfigurationListenerComponent} – An example listener.
 * <li>{@link ConfigurationPluginComponent} – An example plugin. It adds a new
 * key to each configuration before it is delivered. The new key contains the
 * service id of the receiving Managed Service (Factory).
 * <li>{@link ManagedServiceComponent} – An example Managed Service, just prints
 * out the configuration.
 * <li>{@link ManagedServiceFactoryComponent} – An example Managed Service
 * factory, just prints out the configurations it gets.
 * </ul>
 */

@AngularWebResource.Require
@BootstrapWebResource.Require
@WebServerExtender.Require
@ConfigurerExtender.Require
@EventAdminSSEEndpoint.Require
@Component(name = "osgi.enroute.examples.cm", service = { CmApplication.class,
		ConfigurationListener.class })
public class CmApplication implements ConfigurationListener {
	private static final String TOPIC = "osgi/enroute/examples/cm";
	private EventAdmin ea;
	private ConfigurationAdmin cm;
	private DTOs dtos;

	/*
	 * A utility function to convert a dictonary to a map.
	 */
	<K, V> Map<K, V> toMap(Dictionary<K, V> properties, Map<K, V> map) {
		if (properties != null) {
			for (Enumeration<K> e = properties.keys(); e.hasMoreElements();) {
				K key = e.nextElement();
				map.put(key, properties.get(key));
			}
		}
		return map;
	}

	/*
	 * A utility function to convert a dictonary to a map.
	 */
	Map<String, Object> toMap(Dictionary<String, Object> properties) {
		return toMap(properties, new HashMap<>());
	}

	public static class ConfigurationEventProperties extends DTO {
		public String pid;
		public String factoryPid;
		public Map<String, Object> properties;
		public String location;
	}

	@Override
	public void configurationEvent(ConfigurationEvent event) {
		try {
			ConfigurationEventProperties cep = new ConfigurationEventProperties();
			cep.factoryPid = event.getFactoryPid();
			cep.pid = event.getPid();
			if ( ConfigurationEvent.CM_DELETED  != event.getType()) {
				Configuration configuration = cm.getConfiguration(event.getPid());
				cep.location =configuration.getBundleLocation();
				Dictionary<String, Object> properties = configuration.getProperties();
				if ( properties == null) {
					cep.properties = new HashMap<>();
				} else
					cep.properties = toMap(properties);
			}
			ea.postEvent(new Event(TOPIC, dtos.asMap(cep)));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Reference
	void setEventAdmin(EventAdmin ea) {
		this.ea = ea;
	}

	@Reference
	void setCm(ConfigurationAdmin cm) {
		this.cm = cm;
	}

	@Reference
	void setDTOs(DTOs dtos) {
		this.dtos = dtos;
	}

}
