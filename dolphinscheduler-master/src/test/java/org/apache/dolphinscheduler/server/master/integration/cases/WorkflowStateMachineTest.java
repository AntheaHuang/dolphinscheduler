/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

        // Test RUNNING_EXECUTION -> READY_PAUSE
        workflowOperator.pauseWorkflowInstance(workflowInstanceId);
        await().atMost(Duration.ofMinutes(1)).untilAsserted(() ->
                assertThat(repository.queryWorkflowInstance(workflowInstanceId).getState())
                        .isEqualTo(WorkflowExecutionStatus.READY_PAUSE));

        // Test READY_PAUSE -> PAUSE
        workflowOperator.pauseWorkflowInstance(workflowInstanceId);
        await().atMost(Duration.ofMinutes(1)).untilAsserted(() ->
                assertThat(repository.queryWorkflowInstance(workflowInstanceId).getState())
                        .isEqualTo(WorkflowExecutionStatus.PAUSE));

        // Test PAUSE -> RUNNING_EXECUTION
        workflowOperator.recoverSuspendWorkflowInstance(workflowInstanceId);
        await().atMost(Duration.ofMinutes(1)).untilAsserted(() ->
                assertThat(repository.queryWorkflowInstance(workflowInstanceId).getState())
                        .isEqualTo(WorkflowExecutionStatus.RUNNING_EXECUTION));

        // Test RUNNING_EXECUTION -> READY_STOP
        workflowOperator.stopWorkflowInstance(workflowInstanceId);
        await().atMost(Duration.ofMinutes(1)).untilAsserted(() ->
                assertThat(repository.queryWorkflowInstance(workflowInstanceId).getState())
                        .isEqualTo(WorkflowExecutionStatus.READY_STOP));

        // Test READY_STOP -> STOP
        workflowOperator.stopWorkflowInstance(workflowInstanceId);
        await().atMost(Duration.ofMinutes(1)).untilAsserted(() ->
                assertThat(repository.queryWorkflowInstance(workflowInstanceId).getState())
                        .isEqualTo(WorkflowExecutionStatus.STOP));
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
