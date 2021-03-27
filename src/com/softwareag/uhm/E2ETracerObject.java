/**
 * Sample class which should be responsible for e2e monitoring trace
 * 
 * @author bada
 */
package com.softwareag.uhm;

import java.util.HashMap;
import java.util.Map;

import org.apache.skywalking.apm.agent.core.conf.Config;
import org.apache.skywalking.apm.agent.core.context.CarrierItem;
import org.apache.skywalking.apm.agent.core.context.ContextCarrier;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.tag.Tags;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;

public class E2ETracerObject {
	// private static String tenantId;
	// private static String operationName;
	// private static String stage;
	// private Map<String, Object> payloads = new HashMap<>();
	// private E2ETracerObject instance;
	private static boolean IS_E2E_AGENT_LOADED = false;

	// public E2ETracerObject createInstance(String tenantId, String
	// operationName, String stage) {
	// instance = new E2ETracerObject(tenantId, operationName, stage);
	// System.out.println("[E2E] trace object created ");
	// return instance;
	// }

	// public E2ETracerObject(String tenantId, String operationName, String
	// stage) {
	// this.tenantId = tenantId;
	// this.operationName = operationName;
	// this.stage = stage;
	// System.out.println("[E2E] trace object created ");
	// }

	public static void startEntrySpan(String tenantId, String operationName, String stage, String injectKey) {
		if (isE2EConfigured()) {
			try {
				System.out.println("[E2E] start entry span ");
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

	public static void startLocalSpan(String operationName) {
		if (isE2EConfigured()) {
			try {
				System.out.println("[E2E] start local entry span ");
				AbstractSpan span = ContextManager.createLocalSpan("/integration/" + operationName);
				Tags.UHM.OPERATION_NAME.set(span, operationName);
				Tags.UHM.TENANT_ID.set(span, "localSpanTenent");
				System.out.println("Created the local Entry span..." + operationName);
			} catch (Exception e) {
				System.out
						.println("Error while creating local span for " + operationName + ", error:" + e.getMessage());
			}
		}
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
				}
			} catch (Exception e) {
				System.out.println("E2E agent not configured");
			}
		}
		return IS_E2E_AGENT_LOADED;
	}

	// public static boolean isE2EConfigured() {
	// return IS_E2E_AGENT_LOADED;
	// }

}
