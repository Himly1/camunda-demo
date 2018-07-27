package org.camunda.bpm.getstarted.loanapproval.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.ws.rs.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Api
@RestController
@RequestMapping("/projects")
public class ProjectController {

    private final Logger LOGGER = LoggerFactory.getLogger(ProjectController.class);
    private final TaskService taskService;
    private final RuntimeService runtimeService;

    private static class ProjectParticipateRequestRecord {
        Long studentId;

        Long projectParticipateId;

        String taskId;

        public Long getProjectParticipateId() {
            return projectParticipateId;
        }

        public Long getStudentId() {
            return studentId;
        }

        public void setProjectParticipateId(Long projectParticipateId) {
            this.projectParticipateId = projectParticipateId;
        }

        public void setStudentId(Long studentId) {
            this.studentId = studentId;
        }

        public String getTaskId() {
            return taskId;
        }

        public void setTaskId(String taskId) {
            this.taskId = taskId;
        }
    }

    private static class UploadExtraInfoRecord {
        private String taskId;

        private String theUploadUrlOfExtraInfo;

        public String getTaskId() {
            return taskId;
        }

        public String getTheUploadUrlOfExtraInfo() {
            return theUploadUrlOfExtraInfo;
        }

        public void setTaskId(String taskId) {
            this.taskId = taskId;
        }

        public void setTheUploadUrlOfExtraInfo(String theUploadUrlOfExtraInfo) {
            this.theUploadUrlOfExtraInfo = theUploadUrlOfExtraInfo;
        }
    }

    public ProjectController(TaskService taskService, RuntimeService runtimeService) {
        this.taskService = taskService;
        this.runtimeService = runtimeService;
    }

    @ApiOperation(value = "项目报名")
    @PostMapping(value = "/{projectId}/users{userId}")
    public boolean ParticipatingProject(@PathVariable Long projectId, @PathVariable Long userId) {
        //ignore argument verify

        //save the record to db
        Long savedRecordId = 3L;

        //start a new instance of the process
        Map<String, Object> variables = new HashMap<String, Object>();
        variables.put("school", "上海交通大学");
        variables.put("student", String.valueOf(userId));
        variables.put("recordId", savedRecordId);

        ProcessInstance instance = runtimeService.startProcessInstanceByKey("project", variables);
        if (instance == null) {
            return false;
        }else {
            return true;
        }
    }

    @ApiOperation(value = "获取需要审批的项目申请列表")
    @GetMapping(value = "/project/approve/list")
    public @ResponseBody List<ProjectParticipateRequestRecord> getAllProjectParticipateRequest() {

        //get the department id that the current user has joined
        Long joinedDepartmentId = 3L;
        String joinedDepartmentName = "上海交通大学";

        //get the taskList
        List<Task> tasks = taskService.createTaskQuery().taskCandidateGroup(joinedDepartmentName).list();
        List<ProjectParticipateRequestRecord> records = new ArrayList<ProjectParticipateRequestRecord>(tasks.size());
        tasks.forEach( task -> {
            ProjectParticipateRequestRecord record = new ProjectParticipateRequestRecord();
            String taskId = task.getId();
            Map<String, Object> variables = taskService.getVariables(taskId);

            Long studentId = Long.valueOf ( (String)variables.get("student") );
            Long recordId = (Long) variables.get("recordId");
            record.setStudentId(studentId);
            record.setProjectParticipateId(recordId);
            record.setTaskId(taskId);

            records.add(record);
        });

        return records;
    }

    @ApiOperation(value = "审批项目申请")
    @PutMapping(value = "/project/participateRequests/{taskId}")
    public boolean approveProjectParticipateRequest(@PathVariable String taskId, boolean needExtraInfo, boolean passed) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            LOGGER.error("The task not found, task id is {}", taskId);
            return false;
        }else {

            //business logic here

            //Into next step
            LOGGER.info("The taskId is {}", taskId);
            Map<String, Object> variables = new HashMap<>();
            variables.put("extra_info_1",  needExtraInfo);
            variables.put("approved_1", passed);
            taskService.complete(task.getId(), variables);

            return true;
        }
    }

    @ApiOperation(value = "获取学生需要上传额外材料的记录")
    @GetMapping(value = "/users/{userId}/extraInfo/list")
    public List<UploadExtraInfoRecord> getUploadExtraTask(Long userId) {
        List<Task> uploadExtraInfoTask =
                taskService.createTaskQuery().taskAssignee(String.valueOf(userId)).taskName("上传额外材料").list();

        List<UploadExtraInfoRecord> records = new ArrayList<>(uploadExtraInfoTask.size());
        uploadExtraInfoTask.forEach( task -> {
            UploadExtraInfoRecord record = new UploadExtraInfoRecord();
            record.setTaskId(task.getId());

            //the upload url of extra info is up to the variable
            record.setTheUploadUrlOfExtraInfo("www.google.com");

            records.add(record);
        });

        return records;
    }
}
