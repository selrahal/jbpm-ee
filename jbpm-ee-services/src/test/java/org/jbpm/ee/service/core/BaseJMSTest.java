package org.jbpm.ee.service.core;

import static org.jbpm.ee.test.util.KJarUtil.createKieJar;
import static org.jbpm.ee.test.util.KJarUtil.getPom;
import static org.junit.Assert.assertEquals;
import static org.kie.scanner.MavenRepository.getMavenRepository;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.EJB;

import org.drools.compiler.kie.builder.impl.InternalKieModule;
import org.drools.compiler.kproject.ReleaseIdImpl;
import org.drools.core.command.runtime.process.StartProcessCommand;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.transaction.api.annotation.TransactionMode;
import org.jboss.arquillian.transaction.api.annotation.Transactional;
import org.jbpm.ee.services.ejb.local.AsyncCommandExecutorLocal;
import org.jbpm.ee.support.KieReleaseId;
import org.jbpm.services.task.commands.GetTaskAssignedAsPotentialOwnerCommand;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kie.api.KieServices;
import org.kie.api.runtime.process.ProcessInstance;
import org.kie.api.task.model.TaskSummary;
import org.kie.scanner.MavenRepository;

@RunWith(Arquillian.class)
public class BaseJMSTest extends BaseJBPMServiceTest {

	private static final KieReleaseId kri = new KieReleaseId("com.redhat.demo", "testProj", "1.0-SNAPSHOT");
	
	@EJB
	AsyncCommandExecutorLocal cmdExecutor;
	
	@Before
    public void prepare() {
		KieServices ks = KieServices.Factory.get();
        List<String> processes = new ArrayList<String>();
        processes.add("src/test/resources/kjar/testProcess.bpmn2");
        ReleaseIdImpl releaseID = new ReleaseIdImpl(kri.getGroupId(), kri.getArtifactId(), kri.getVersion());
        InternalKieModule kjar = createKieJar(ks, releaseID, processes);
        File pom = new File("target/kmodule", "pom.xml");
        pom.getParentFile().mkdir();
        try {
            FileOutputStream fs = new FileOutputStream(pom);
            fs.write(getPom(kri).getBytes());
            fs.close();
        } catch (Exception e) {
            
        }
        MavenRepository repository = getMavenRepository();
        repository.deployArtifact(releaseID, kjar, pom);
    }
	
	@Test
	@Transactional(value=TransactionMode.DEFAULT)
	public void testSimpleProcess() throws Exception {
		final String processString = "testProj.testProcess";
		final String variableKey = "processString";
		
		Map<String, Object> processVariables = new HashMap<String, Object>();
		processVariables.put(variableKey, "Initial");
		
		StartProcessCommand startProcess = new StartProcessCommand(processString, processVariables);
		String correlationId = cmdExecutor.execute(kri, startProcess);
		
		ProcessInstance processInstance = null;
		int count = 0;
		while (processInstance == null && count < 1) {
			processInstance = (ProcessInstance) cmdExecutor.pollResponse(correlationId);
			count += 1;
		}
		
		assertEquals(1, processInstance.getState());
		
		GetTaskAssignedAsPotentialOwnerCommand getTasks = new GetTaskAssignedAsPotentialOwnerCommand("abaxter", "en-UK");
		correlationId = cmdExecutor.execute(kri, getTasks);
		
		List<TaskSummary> taskSummaries = null;
		count = 0;
		while (taskSummaries == null && count < 1) {
			taskSummaries = (List<TaskSummary>) cmdExecutor.pollResponse(correlationId);
			count += 1;
		}
	}
}
