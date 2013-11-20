package org.jbpm.ee.services.ejb.impl.interceptors;

import java.io.IOException;
import java.lang.reflect.Method;

import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import org.jbpm.ee.services.ejb.interceptors.InterceptorUtil;
import org.jbpm.ee.services.ejb.startup.BPMClassloaderService;
import org.jbpm.ee.services.model.LazyDeserializingMap;
import org.jbpm.ee.support.KieReleaseId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Interceptor
@ClassloaderBinding
public class ClassloaderInterceptor {
	private static final Logger LOG = LoggerFactory.getLogger(ClassloaderInterceptor.class);

	@Inject
	BPMClassloaderService classloaderService;
	
	@AroundInvoke
	public Object intercept(InvocationContext ctx) throws Exception {
		if(!InterceptorUtil.requiresClassloaderInterception(ctx.getMethod())) {
			LOG.info("Interceptor not required for method: "+ctx.getMethod().getName());
			return ctx.proceed();
		}
		
		setupClassloader(ctx.getMethod(), ctx.getParameters());
		
		try {
			lazyInitializeMaps(ctx.getParameters());
		}
		catch(IOException e) {
			throw new ClassloaderException("Exception deserializing object.", e); 
		}
		
		return ctx.proceed();
	}
	
	private void lazyInitializeMaps(Object[] parameters) throws IOException {
		for(int i=0, j=parameters.length; i<j; i++) {
			Object parameter = parameters[i];
			
			if(LazyDeserializingMap.class.isAssignableFrom(parameter.getClass())) {
				LazyDeserializingMap obj = (LazyDeserializingMap)parameter;
				obj.initializeLazy();
			}
		}
	}
	
	
	/**
	 * Determines the parameter that is going to be used to setup the classloader on the
	 * server side.
	 * 
	 * @param method
	 * @param parameters
	 */
	private void setupClassloader(Method method, Object[] parameters) {
		
		//setup the classloder..
		KieReleaseId releaseId = InterceptorUtil.extractReleaseId(method, parameters);
		if(releaseId != null) {
			classloaderService.bridgeClassloaderByReleaseId(releaseId);
			return;
		}
		
		Long processInstanceId = InterceptorUtil.extractProcessInstanceId(method, parameters);
		if(processInstanceId != null) {
			classloaderService.bridgeClassloaderByProcessInstanceId(processInstanceId);
			return;
		}
		
		Long taskId = InterceptorUtil.extractTaskId(method, parameters);
		if(taskId != null) {
			classloaderService.bridgeClassloaderByTaskId(taskId);
			return;
		}
		
		Long workItemId = InterceptorUtil.extractWorkItemId(method, parameters);
		if(workItemId != null) {
			classloaderService.bridgeClassloaderByWorkItemId(workItemId);
			return;
		}
			
		
		
	}
}
