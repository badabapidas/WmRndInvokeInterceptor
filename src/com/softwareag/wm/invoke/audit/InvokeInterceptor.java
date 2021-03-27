package com.softwareag.wm.invoke.audit;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Stack;

import com.softwareag.uhm.E2ETracerObject;
import com.softwareag.uhm.constants.UhmTransactionStatus;
import com.wm.app.b2b.server.BaseService;
import com.wm.app.b2b.server.InvokeException;
import com.wm.app.b2b.server.InvokeState;
import com.wm.app.b2b.server.Service;
import com.wm.app.b2b.server.ServiceException;
import com.wm.app.b2b.server.invoke.InvokeChainProcessor;
import com.wm.app.b2b.server.invoke.ServiceStatus;
import com.wm.data.IData;
import com.wm.lang.ns.AuditSettings;
import com.wm.lang.ns.NSService;
import com.wm.net.HttpHeader;
import com.wm.util.ServerException;

/**
 * Implements the webMethods InvokeChainProcessor interface to allow services to
 * be intercepted.
 * 
 * Don't forget to update the
 * <SAG_HOME>/IntegrationServer/config/invokemananger.cnf file and add the class
 * of the interceptor class. Otherwise webMethods will not be aware of your
 * interceptor. In addition the classes must be loaded at startup so ensure that
 * the class is packaged in a static jar
 * 
 * Ideally would want to modify the class com/wm/app/b2b/server/AuditLogManager
 * to include this code as it would ensure that the start audit would also have
 * the correct root context id.
 * 
 * @author John Carter (john.carter@softwareag.com)
 */
public class InvokeInterceptor implements InvokeChainProcessor {

	public static final String WM_ENDTOEND_TRANSACTION_ID = "wm-etoe-transaction-id";

	/**
	 * Used to identify the webMethods root context id based in
	 * runtime-attribute array returned by InvokeState. Attention this will have
	 * to be tested for each webMethods version as this is not official.
	 */
	public static final int WM_ROOT_CONTEXT_ID_INDEX = 0;
	// private E2ETracerObject e2einstance;

	private static InvokeInterceptor _default;

	public InvokeInterceptor() {

		System.out.println("Instantiating com.softwareag.wm.invoke.agent.EndToEndAgent");
		_default = this;

		_default.startup();
	}

	/**
	 * Ensures that the default interceptor is properly configured and that all
	 * context map are instantiated and hooked up to the backing store
	 * 
	 * @throws com.wm.app.b2b.server.ServiceException
	 */
	public static void restart() throws ServiceException {
		if (_default != null)
			_default.startup();
	}

	/**
	 * Configures the InvokeInterceptor and ensures the persistence store for
	 * storing suspended services is instantiated and working properly
	 * 
	 * @throws com.wm.app.b2b.server.ServiceException
	 */
	private void startup() {
		// TODO:
	}

	public void process(@SuppressWarnings("rawtypes") Iterator chain, BaseService baseService, IData pipeline,
			ServiceStatus status) throws ServiceException {

		// Will force root context id to header value identified by
		// WM_ENDTOEND_TRANSACTION_ID
		String contextId = determineRootContextId(status);
		String serviceName = getServiceName(baseService);

		try {
			if (chain.hasNext()) {

				if (chainPreProcessor(contextId, serviceName, baseService, pipeline, status)) {
					// TODO: Do we want to make audit destination one of file,
					// db or external monitoring or should it be a complement

					// we logged via etoe, so disable internal logging, don't
					// want to do it twice
					baseService.setAuditSettings(new AuditSettings());
				}

				((InvokeChainProcessor) chain.next()).process(chain, baseService, pipeline, status);

				// service success
				chainPostProcessor(contextId, serviceName, baseService, pipeline, status);
			}
		} catch (InvokeException error) {
			// service exceptions arrive here
			chainPostProcessor(contextId, serviceName, baseService, pipeline, error);

			throw new ServiceException(error);
		} catch (ServerException error) {
			throw new ServiceException(error);
		}
	}

	/**
	 * Either uses wm context id or that provided by end to end monitoring (in
	 * which case it updates the invoke status to use it This occurs after the
	 * start step of auditing so the IS audit will have it's own unique id,
	 * rather than the etoe id, end and error steps will be okay as well as any
	 * invoked services.
	 * 
	 * @param status
	 *            references current runtime info (but not audit settings
	 *            strangely)
	 * @return the current context id of the service
	 */
	protected String determineRootContextId(ServiceStatus status) {

		if (status.isTopService() && getEtoETransactionId() != null) {
			System.out.println("This is top service");
			String id = getEtoETransactionId();
			setRootContextIdForThread(id);
			return id;
		} else {
			return Service.getCurrentActivationID();
		}
	}

	protected String getServiceName(BaseService baseService) {
		return baseService.getNSName().getFullName();
	}

	protected String getEtoETransactionId() {
		// TODO: pull from header

		HttpHeader header = Service.getHttpRequestHeader();
		return Service.getHttpHeaderField(WM_ENDTOEND_TRANSACTION_ID, header);
	}

	/**
	 * Entry point for end to end monitoring to allow service start to be logged
	 * if required
	 * 
	 * @param status
	 * 
	 * @return true if end to end monitoring is activated, false if not
	 */
	protected boolean chainPreProcessor(String integrationId, String serviceName, BaseService baseService,
			IData pipeline, ServiceStatus status) {

		// base auditing on requirements from audit sub-system
		if (baseService.getAuditSettings().isStartAuditEnabled()) {

			System.out.println("Processing start " + serviceName + " / " + integrationId);
			String[][] businessDataKeys = baseService.getInputAuditFields();
			Map<String, Object> businessData = extractDataFromPipeline(businessDataKeys, pipeline);
			System.out.println("no of keys: " + businessData.size());

			// Fetch out the tenant id and stage informations
			String tenantId = "badawmio"; // InvokeState.getCurrentState().getTenantID();
			String stage = "devStage"; // InvokeState.getCurrentState().getStageID();

			// if this is top service then create the entry span or else create
			// local span
			if (status.isTopService()) {
				System.out.println("This is top service: " + serviceName);
				E2ETracerObject.startEntrySpan(tenantId, serviceName, stage, null);
			} else {
				// if there is any active span then only create the local span;
				if (E2ETracerObject.isActiveSpan()) {
					E2ETracerObject.startLocalSpan(serviceName);
				}
			}
		}
		// if we are sending true here its not going to chainpost proces; have
		// to check with john about the behavior
		// return E2ETracerObject.isE2EConfigured();
		return false;
	}

	/**
	 *
	 */
	protected void chainPostProcessor(String integrationId, String serviceName, BaseService baseService, IData pipeline,
			ServiceStatus status) {

		if (baseService.getAuditSettings().isCompleteAuditEnabled()) {
			// TODO: report success
			System.out.println("Processing completion " + serviceName + " / " + integrationId);

			String[][] businessDataKeys = baseService.getOutputAuditFields();
			Map<String, Object> businessData = extractDataFromPipeline(businessDataKeys, pipeline);
			System.out.println("no of keys: " + businessData.size());

			// TODO: Added by BADA
			if (E2ETracerObject.isActiveSpan()) {
				E2ETracerObject.updateSuccessStatus(UhmTransactionStatus.PASS);
				E2ETracerObject.stopSpan(serviceName);
			}
		}
	}

	/**
	*
	*/
	protected void chainPostProcessor(String integrationId, String serviceName, BaseService baseService, IData pipeline,
			InvokeException e) {

		// TODO: report here!

		if (baseService.getAuditSettings().isErrorAuditEnabled()) {
			System.out.println("Processing error " + serviceName + " / " + integrationId);
			if (E2ETracerObject.isActiveSpan()) {
				E2ETracerObject.updateErrorStatus(UhmTransactionStatus.FAIL, e.getMessage());
			}
		}
	}

	/**
	 * Extract given keys from pipeline
	 */

	protected Map<String, Object> extractDataFromPipeline(String[][] keys, IData pipeline) {

		Map<String, Object> vals = new HashMap<String, Object>();

		if (keys != null) {
			for (String[] k : keys) {
				Object obj = _extractDataFromPipeline(k[0], pipeline);

				if (obj != null) {
					vals.put(k[1], obj); // USED simple name, risky because it
											// might not be be unique, perhaps
											// should use xpath ?
				}
			}
		}

		return vals;
	}

	private Object _extractDataFromPipeline(String xpath, IData doc) {
		XPathExpression xpe = new XPathExpression(xpath);

		try {
			return xpe.getObject(doc);
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Return the package name associated with the context of the calling
	 * service.
	 *
	 * @return package name associated with the calling context
	 */
	public static String getPackageForCaller() {
		return getPackageForCaller(false);
	}

	public static String getPackageForCaller(boolean ifNotExistGetCurrent) {

		String packageName = null;

		try {
			NSService caller = Service.getCallingService();
			if (caller == null && ifNotExistGetCurrent) {
				packageName = Service.getPackageName();
			} else {
				packageName = caller.getPackage().getName();
			}
		} catch (Exception e) {
			// throw new RuntimeException("Cannot determine package name");
		}

		return packageName;
	}

	protected static String[] getContextIDsForService() {

		String[] contextIDs = { null, null, null };

		try {
			InvokeState currentInvokeState = InvokeState.getCurrentState();
			Stack<?> servicesStack = currentInvokeState.getCallStack();
			String contextIDStack[] = currentInvokeState.getAuditRuntime().getContextStack();

			String contextId = null;
			String parentContextId = null;
			String rootContextId = null;

			int contextId_index = contextIDStack.length - 1;

			contextId = contextIDStack[contextId_index];
			if (contextId_index > 0) {
				parentContextId = contextIDStack[contextId_index - 1];
			}
			rootContextId = contextIDStack[0];

			contextIDs[0] = contextId;
			contextIDs[1] = parentContextId;
			contextIDs[2] = rootContextId;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		return contextIDs;
	}

	/**
	 * Forces root context ID to given value Unfortunately audit start event
	 * occurs before the class in the invocation chain and will record wm value
	 * 
	 * activationId must be a valid UUID v1 String e.g.
	 * f10f14ac-8297-11eb-8dcd-0242ac130003
	 */
	private static void setRootContextIdForThread(String activationId) {

		if (activationId == null) {
			return;
		}

		InvokeState is = InvokeState.getCurrentState();

		if (is != null) {
			String[] args = null;

			if (is.getAuditRuntime() != null) {

				args = is.getAuditRuntime().getContextStack();

				System.out.println("alternatively it is " + args[WM_ROOT_CONTEXT_ID_INDEX]);

				if (args.length <= WM_ROOT_CONTEXT_ID_INDEX)
					args = new String[WM_ROOT_CONTEXT_ID_INDEX + 1];

				args[WM_ROOT_CONTEXT_ID_INDEX] = activationId;

				InvokeState.getCurrentState().getAuditRuntime().setContextStack(args);
			}
		}
	}
}
