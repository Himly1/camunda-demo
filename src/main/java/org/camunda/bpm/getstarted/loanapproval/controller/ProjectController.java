package org.camunda.bpm.getstarted.loanapproval.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.camunda.bpm.getstarted.loanapproval.camunda.process.constant.ProjectProcessConstant;
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
        variables.put(ProjectProcessConstant.VAR_NAME_SCHOOL, "上海交通大学");
        variables.put(ProjectProcessConstant.VAR_NAME_STUDENT, String.valueOf(userId));
        variables.put(ProjectProcessConstant.FORM_RECORD_ID, savedRecordId);

        ProcessInstance instance = runtimeService.
                startProcessInstanceByKey(ProjectProcessConstant.PROCESS_ID, variables);
        if (instance == null) {
            return false;
        }else {
            return true;
        }
    }

    @ApiOperation(value = "获取需要审批的项目申请列表")
    @GetMapping(value = "/project/approve/list")
    public @ResponseBody List<ProjectParticipateRequestRecord> getAllProjectParticipateRequest(String schoolName, Integer reviewLevel) {

        LOGGER.info("The school name is {}", schoolName);
        //get the taskList
        List<Task> tasks;
        if (reviewLevel.equals(1)) {
             tasks = taskService.createTaskQuery().
                taskName(ProjectProcessConstant.TASK_NAME_FIRST_LEVEL_REVIEW).
                            taskCandidateGroup(schoolName).
                            list();
        }else {
            tasks = taskService.createTaskQuery().
                    taskName(ProjectProcessConstant.TASK_NAME_SECOND_LEVEL_REVIEW).
                    taskCandidateGroup(schoolName).
                    list();
        }

        List<ProjectParticipateRequestRecord> records = new ArrayList<ProjectParticipateRequestRecord>(tasks.size());
        tasks.forEach( task -> {
            ProjectParticipateRequestRecord record = new ProjectParticipateRequestRecord();
            String taskId = task.getId();
            Map<String, Object> variables = taskService.getVariables(taskId);

            Long studentId = Long.valueOf ( (String)variables.get(ProjectProcessConstant.VAR_NAME_STUDENT) );
            Long recordId = (Long) variables.get(ProjectProcessConstant.FORM_RECORD_ID);
            record.setStudentId(studentId);
            record.setProjectParticipateId(recordId);
            record.setTaskId(taskId);

            records.add(record);
        });

        return records;
    }

    @ApiOperation(value = "审批项目申请")
    @PutMapping(value = "/project/participateRequests/{taskId}")
    public boolean approveProjectParticipateRequest(@PathVariable String taskId, boolean needExtraInfo, boolean passed, String schoolName) {
        Task task = taskService.createTaskQuery().
                taskCandidateGroup(schoolName).taskId(taskId).singleResult();
        if (task == null) {
            LOGGER.error("The task not found, task id is {}", taskId);
            return false;
        }else {
            //business logic here

            //Into next step
            LOGGER.info("The taskId is {}", taskId);
            Map<String, Object> variables = new HashMap<>();
            variables.put(ProjectProcessConstant.FORM_EXTRA_INFO_1,  needExtraInfo);
            variables.put(ProjectProcessConstant.FORM_APPROVED_1, passed);
            taskService.complete(task.getId(), variables);
            return true;
        }
    }

    @ApiOperation(value = "获取学生需要上传额外材料的记录")
    @GetMapping(value = "/users/{userId}/extraInfo/list")
    public List<UploadExtraInfoRecord> getUploadExtraTask(Long userId) {
        List<Task> uploadExtraInfoTask =
                taskService.createTaskQuery().
                        taskAssignee(String.valueOf(userId)).
                        taskName(ProjectProcessConstant.TASK_NAME_UPLOAD_EXTRA_INFO).
                        list();

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

    @ApiOperation(value = "上传指定项目所需的额外资料")
    @PostMapping(value = "/{projectId}/users/{userId}/extraInfo")
    public boolean  uploadExtraInfo(@PathVariable Long projectId, @PathVariable Long userId,  String extraInfo, String taskId) {
        //must verify the task of the taskId pointing is belong the current user.
        Task task = taskService.createTaskQuery().
                taskAssignee(String.valueOf(userId)).
                taskName(ProjectProcessConstant.TASK_NAME_UPLOAD_EXTRA_INFO).
                taskId(taskId).
                singleResult();
        if (task == null) {
            LOGGER.error("The task not found.");
            LOGGER.error("the assignee is {}, taskName is {}, taskId is {}.", userId, ProjectProcessConstant.TASK_NAME_UPLOAD_EXTRA_INFO, taskId);
            return false;
        }else {
            //upload extra info to db.

            //business logic here

            //into next step
            taskService.complete(task.getId());
            return true;
        }
    }
}
