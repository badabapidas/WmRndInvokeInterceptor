/**
 * Sample class which should be responsible for e2e monitoring trace
 * 
 * @author bada
 */
package com.softwareag.uhm;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.skywalking.apm.agent.core.conf.Config;
import org.apache.skywalking.apm.agent.core.context.CarrierItem;
import org.apache.skywalking.apm.agent.core.context.ContextCarrier;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.SW6CarrierItem;
import org.apache.skywalking.apm.agent.core.context.tag.Tags;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;

import com.wm.net.HttpHeader;

public class E2ETracerObject {
	private static boolean IS_E2E_AGENT_LOADED = false;
	private static List<String> RCP_SERVICE_LISTS = new ArrayList<>();

	public static void startEntrySpan(String tenantId, String operationName, String stage, String injectKey) {
		if (isE2EConfigured()) {
			try {
				System.out.println("sw6 found from parent: " + injectKey);
				ContextCarrier contextCarrier = new ContextCarrier();
				CarrierItem contextCarrierItem = contextCarrier.items();
				while (contextCarrierItem.hasNext()) {
					contextCarrierItem = contextCarrierItem.next();
					contextCarrierItem.setHeadValue(injectKey);
				}
				AbstractSpan span = ContextManager.createEntrySpan("/integration/" + operationName, contextCarrier);
				populateSystemData(span, tenantId, operationName, stage);

				System.out.println("Created the Entry span..." + operationName);
			} catch (Exception e) {
				System.out.println("Error while creating span for " + operationName + ", error:" + e.getMessage());
			}
		}
	}

	private static void updateRcpServices() {
		RCP_SERVICE_LISTS.add("pub.client:http");
	}

	public static void startLocalSpan(String operationName) {
		if (isE2EConfigured()) {
			try {
				AbstractSpan span = ContextManager.createLocalSpan("/integration/" + operationName);
				Tags.UHM.OPERATION_NAME.set(span, operationName);
				Tags.UHM.TENANT_ID.set(span, "localSpanTenent");
				System.out.println("-> Created the local Entry span..." + operationName);
			} catch (Exception e) {
				System.out
						.println("Error while creating local span for " + operationName + ", error:" + e.getMessage());
			}
		}
	}

	public static boolean isAValidRcpServiceCall(String serviceName) {
		if (RCP_SERVICE_LISTS.contains(serviceName)) {
			return true;
		}
		return false;

	}

	public static HttpHeader startExitSpan(String opName, HttpHeader header) {
		if (isE2EConfigured()) {
			try {
				ContextCarrier contextCarrier = new ContextCarrier();
				ContextManager.createExitSpan("HttpReqLeavingIS", contextCarrier, "localhost:5555");
				CarrierItem next = contextCarrier.items();
				while (next.hasNext()) {
					next = next.next();
					String key = next.getHeadKey();
					String value = next.getHeadValue();
					header.addField(SW6CarrierItem.HEADER_NAME, value);
				}
				System.out.println("Created the Exit span..." + opName);
				ContextManager.stopSpan();
			} catch (Exception e) {
				System.out.println("Error while creating exit span for " + opName + ", error:" + e.getMessage());
			}
		}
		return header;
	}

	public static boolean isActiveSpan() {
		return ContextManager.isActive();
	}

	private static void populateSystemData(AbstractSpan span, String tenantId, String operationName, String stage) {

		if (isE2EConfigured()) {
			// decorate the span with tags
			Tags.UHM.OPERATION_NAME.set(span, operationName);
			Tags.UHM.FULLY_QUALIFIED_NAME.set(span, operationName);
			Tags.UHM.STAGE.set(span, stage);
			Tags.UHM.TENANT_ID.set(span, tenantId);
			if (Config.Agent.SERVICE_NAME != null) {
				Tags.UHM.COMPONENT.set(span, Config.Agent.SERVICE_NAME.trim());
			} else {
				Tags.UHM.COMPONENT.set(span, "");
			}
		}
	}

	@SuppressWarnings("deprecation")
	public static void populateBuisnessData(Map<String, Object> businessData) {
		AbstractSpan span = ContextManager.activeSpan();
		for (Map.Entry m : businessData.entrySet()) {
			System.out.println("->" + m.getKey().toString() + " " + (String) m.getValue());
			span.tag(m.getKey().toString(), (String) m.getValue());
		}
	}

	public static void updateErrorStatus(String status, String error) {
		if (isE2EConfigured()) {
			if (ContextManager.isActive()) {
				AbstractSpan span = ContextManager.activeSpan();
				Tags.UHM.TRANSACTION_STATUS.set(span, status);
				Tags.UHM.ERROR_MSG.set(span, error);
			}
		}
	}

	public static void updateSuccessStatus(String status) {
		if (isE2EConfigured()) {
			if (ContextManager.isActive()) {
				AbstractSpan span = ContextManager.activeSpan();
				Tags.UHM.TRANSACTION_STATUS.set(span, status);
			}
		}
	}

	public static void stopSpan(String operationName) {
		if (isE2EConfigured()) {
			try {
				if (ContextManager.isActive()) {
					ContextManager.stopSpan();
					System.out.println("Stopped the span..." + operationName);
				}
			} catch (Exception e) {
				System.out.println("Error while clossing span for " + operationName + ", error:" + e.getMessage());
			}
		}
	}

	public static void discardEntrySpan(String operationName) {
		if (isE2EConfigured()) {
			try {
				if (ContextManager.isActive()) {
					ContextManager.discardActiveSpan();
					System.out.println("Discarded the span..." + operationName);
				}
			} catch (Exception e) {
				System.out.println("Error while discarding span for " + operationName + ", error:" + e.getMessage());
			}
		}
	}

	public static boolean isE2EConfigured() {
		if (!IS_E2E_AGENT_LOADED) {
			try {
				Class cls = Class.forName("org.apache.skywalking.apm.agent.core.conf.Config");
				if (cls != null) {
					System.out.println("E2E agent is configured");
					IS_E2E_AGENT_LOADED = true;
					updateRcpServices();
				}
			} catch (Exception e) {
				System.out.println("E2E agent not configured");
				IS_E2E_AGENT_LOADED = false;
			}
		}
		return IS_E2E_AGENT_LOADED;
	}

	// public static boolean isE2EConfigured() {
	// return IS_E2E_AGENT_LOADED;
	// }

}
