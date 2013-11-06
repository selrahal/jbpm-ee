package org.jbpm.ee.service;

import javax.ejb.LocalBean;
import javax.ejb.Stateful;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;

import org.jbpm.ee.service.remote.ExecutorServiceRemote;
import org.kie.api.command.Command;
import org.kie.api.runtime.CommandExecutor;

@Stateful
@LocalBean
@SessionScoped
public class ExecutorServiceBean implements ExecutorServiceRemote {

	@Inject
	private RuntimeServiceBean runtimeService;
	
	private CommandExecutor commandExecutorDelegate;
	
	private void delegateCheck() {
		if (commandExecutorDelegate == null) {
			commandExecutorDelegate = runtimeService.getKnowledgeSession();
		}
	}
	
	@Override
	public <T> T execute(Command<T> command) {
		delegateCheck();
		return commandExecutorDelegate.execute(command);
	}

}