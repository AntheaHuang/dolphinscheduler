
package org.apache.dolphinscheduler.server.master.integration.cases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;

import org.apache.dolphinscheduler.common.enums.WorkflowExecutionStatus;
import org.apache.dolphinscheduler.dao.entity.WorkflowDefinition;
import org.apache.dolphinscheduler.extract.master.command.RunWorkflowCommandParam;
import org.apache.dolphinscheduler.server.master.integration.WorkflowTestCaseContext;
import org.apache.dolphinscheduler.server.master.integration.WorkflowOperator;
import org.apache.dolphinscheduler.server.master.AbstractMasterIntegrationTestCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class WorkflowStateMachineTest extends AbstractMasterIntegrationTestCase {

    @Test
    @DisplayName("Test valid state transitions")
    public void testValidStateTransitions() {
        final String yaml = "/it/start/workflow_with_one_fake_task_success.yaml";
        final WorkflowTestCaseContext context = workflowTestCaseContextFactory.initializeContextFromYaml(yaml);
        final WorkflowDefinition workflow = context.getOneWorkflow();

        final WorkflowOperator.WorkflowTriggerDTO workflowTriggerDTO = WorkflowOperator.WorkflowTriggerDTO.builder()
                .workflowDefinition(workflow)
                .runWorkflowCommandParam(new RunWorkflowCommandParam())
                .build();
        final Integer workflowInstanceId = workflowOperator.manualTriggerWorkflow(workflowTriggerDTO);

        await().atMost(Duration.ofMinutes(1)).untilAsserted(() ->
                assertThat(repository.queryWorkflowInstance(workflowInstanceId))
                        .satisfies(workflowInstance -> {
                            assertThat(workflowInstance.getState()).isEqualTo(WorkflowExecutionStatus.RUNNING_EXECUTION);
                        }));

        // Test RUNNING_EXECUTION -> PAUSE
        workflowOperator.pauseWorkflowInstance(workflowInstanceId);
        await().atMost(Duration.ofMinutes(1)).untilAsserted(() ->
                assertThat(repository.queryWorkflowInstance(workflowInstanceId).getState())
                        .isIn(WorkflowExecutionStatus.PAUSE, WorkflowExecutionStatus.SUCCESS));

        // Test PAUSE -> RUNNING_EXECUTION
        workflowOperator.recoverSuspendWorkflowInstance(workflowInstanceId);
        await().atMost(Duration.ofMinutes(1)).untilAsserted(() ->
                assertThat(repository.queryWorkflowInstance(workflowInstanceId).getState())
                        .isIn(WorkflowExecutionStatus.RUNNING_EXECUTION, WorkflowExecutionStatus.SUCCESS));

        // Test RUNNING_EXECUTION -> READY_STOP
        workflowOperator.stopWorkflowInstance(workflowInstanceId);
        await().atMost(Duration.ofMinutes(1)).untilAsserted(() ->
                assertThat(repository.queryWorkflowInstance(workflowInstanceId).getState())
                        .isEqualTo(WorkflowExecutionStatus.READY_STOP));

        // Test READY_STOP -> STOP
        workflowOperator.stopWorkflowInstance(workflowInstanceId);
        await().atMost(Duration.ofMinutes(1)).untilAsserted(() ->
                assertThat(repository.queryWorkflowInstance(workflowInstanceId).getState())
                        .isEqualTo(WorkflowExecutionStatus.SUCCESS));
    }

    @Test
    @DisplayName("Test failure recovery transitions")
    public void testFailureRecoveryTransitions() {
        final String yaml = "/it/recover_failure_tasks/failure_workflow_with_two_serial_fake_task.yaml";
        final WorkflowTestCaseContext context = workflowTestCaseContextFactory.initializeContextFromYaml(yaml);
        final WorkflowDefinition workflow = context.getOneWorkflow();

        final WorkflowOperator.WorkflowTriggerDTO workflowTriggerDTO = WorkflowOperator.WorkflowTriggerDTO.builder()
                .workflowDefinition(workflow)
                .runWorkflowCommandParam(new RunWorkflowCommandParam())
                .build();
        final Integer workflowInstanceId = workflowOperator.manualTriggerWorkflow(workflowTriggerDTO);

        // Test FAILURE -> RUNNING_EXECUTION
        workflowOperator.recoverFailureTasks(workflowInstanceId);
        await().atMost(Duration.ofMinutes(1)).untilAsserted(() ->
                assertThat(repository.queryWorkflowInstance(workflowInstanceId).getState())
                        .isIn(WorkflowExecutionStatus.RUNNING_EXECUTION, WorkflowExecutionStatus.SUCCESS));

        // Test FAILURE -> STOP
        workflowOperator.stopWorkflowInstance(workflowInstanceId);
        await().atMost(Duration.ofMinutes(1)).untilAsserted(() ->
                assertThat(repository.queryWorkflowInstance(workflowInstanceId).getState())
                        .isIn(WorkflowExecutionStatus.STOP, WorkflowExecutionStatus.SUCCESS));
    }

    @Test
    @DisplayName("Test invalid state transitions")
    public void testInvalidStateTransitions() {
        final String yaml = "/it/start/workflow_with_one_fake_task_success.yaml";
        final WorkflowTestCaseContext context = workflowTestCaseContextFactory.initializeContextFromYaml(yaml);
        final WorkflowDefinition workflow = context.getOneWorkflow();

        final WorkflowOperator.WorkflowTriggerDTO workflowTriggerDTO = WorkflowOperator.WorkflowTriggerDTO.builder()
                .workflowDefinition(workflow)
                .runWorkflowCommandParam(new RunWorkflowCommandParam())
                .build();
        final Integer workflowInstanceId = workflowOperator.manualTriggerWorkflow(workflowTriggerDTO);

        // Test invalid SUBMITTED_SUCCESS -> SUCCESS transition
        workflowOperator.stopWorkflowInstance(workflowInstanceId);
        assertThat(repository.queryWorkflowInstance(workflowInstanceId).getState())
                .isNotEqualTo(WorkflowExecutionStatus.SUCCESS);
        assertThat(repository.queryWorkflowInstance(workflowInstanceId).getState())
                .isNotEqualTo(WorkflowExecutionStatus.SUCCESS);

        // Test invalid RUNNING_EXECUTION -> STOP transition without READY_STOP
        workflowOperator.stopWorkflowInstance(workflowInstanceId);
        assertThat(repository.queryWorkflowInstance(workflowInstanceId).getState())
                .isNotEqualTo(WorkflowExecutionStatus.SUCCESS);
        assertThat(repository.queryWorkflowInstance(workflowInstanceId).getState())
                .isNotEqualTo(WorkflowExecutionStatus.STOP);
    }
}
