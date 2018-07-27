package org.camunda.bpm.getstarted.loanapproval.camunda.tasks.service;

import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class SmsServiceTask implements JavaDelegate {
    private static final Logger log = LoggerFactory.getLogger(SmsServiceTask.class);

    private final TaskService taskService;

    public SmsServiceTask(TaskService taskService) {
        this.taskService = taskService;
    }

    public void execute(DelegateExecution delegateExecution) throws Exception {
        Map<String, Object> variables = delegateExecution.getVariables();
        log.info("variables is {}", variables);

        String studentId = (String)variables.get("student");
        log.info("success send sms message to the student {}", studentId);
    }
}
