/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.flowable.dmn.test.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.flowable.cmmn.api.CmmnRuntimeService;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnRule;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.DefaultTenantProvider;
import org.flowable.common.engine.impl.interceptor.EngineConfigurationConstants;
import org.flowable.dmn.api.DmnHistoricDecisionExecution;
import org.flowable.dmn.engine.DmnEngineConfiguration;
import org.flowable.dmn.engine.DmnEngines;
import org.flowable.task.api.Task;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * @author martin.grofcik
 * @author Filip Hrisafov
 */
public class DecisionTaskTest {

    @Rule
    public FlowableCmmnRule cmmnRule = new FlowableCmmnRule("org/flowable/cmmn/test/runtime/DecisionTaskTest.cfg.xml");
    
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    @CmmnDeployment(
            resources = {"org/flowable/cmmn/test/runtime/DecisionTaskTest.testDecisionServiceTask.cmmn",
                    "org/flowable/cmmn/test/runtime/DecisionTaskTest.testDecisionServiceTask.dmn"
            }
    )
    public void testDecisionServiceTask() {
        CaseInstance caseInstance = cmmnRule.getCmmnRuntimeService().createCaseInstanceBuilder()
                .caseDefinitionKey("myCase")
                .variable("testVar", "test2")
                .start();

        assertResultVariable(caseInstance);
    }

    @Test
    @CmmnDeployment(
            resources = { "org/flowable/cmmn/test/runtime/DecisionTaskTest.testDecisionServiceTask.cmmn",
                          "org/flowable/cmmn/test/runtime/DecisionTaskTest.testDecisionServiceTask.dmn"
            }
    )
    public void testUnknowPropertyUsedInDmn() {
        this.expectedException.expect(FlowableException.class);
        this.expectedException.expectMessage("DMN decision table with key decisionTable execution failed. Cause: Unknown property used in expression: #{testVar == \"test2\"}");
        
        CaseInstance caseInstance = cmmnRule.getCmmnRuntimeService().createCaseInstanceBuilder()
                .caseDefinitionKey("myCase")
                .start();

        assertResultVariable(caseInstance);
    }

    @Test
    @CmmnDeployment(
            resources = { "org/flowable/cmmn/test/runtime/DecisionTaskTest.testDecisionServiceTask.cmmn",
                          "org/flowable/cmmn/test/runtime/DecisionTaskTest.testDecisionServiceTask.dmn"
            }
    )
    public void testDecisionServiceTaskWithoutHitDefaultBehavior() {
        CaseInstance caseInstance = cmmnRule.getCmmnRuntimeService().createCaseInstanceBuilder()
                .caseDefinitionKey("myCase")
                .variable("testVar", "noHit")
                .start();

        assertNoResultVariable(caseInstance);
    }

    @Test
    @CmmnDeployment(
            resources = { "org/flowable/cmmn/test/runtime/DecisionTaskTest.testHitFlag.cmmn",
                          "org/flowable/cmmn/test/runtime/DecisionTaskTest.testDecisionServiceTask.dmn"
            }
    )
    public void testDoNotThrowErrorOnNoHitWithHit() {
        CaseInstance caseInstance = cmmnRule.getCmmnRuntimeService().createCaseInstanceBuilder()
                .variable("throwErrorOnNoHits", false)
                .variable("testVar", "test2")
                .caseDefinitionKey("myCase")
                .start();

        assertResultVariable(caseInstance);
    }

    @Test
    @CmmnDeployment(
            resources = { "org/flowable/cmmn/test/runtime/DecisionTaskTest.testHitFlag.cmmn",
                          "org/flowable/cmmn/test/runtime/DecisionTaskTest.testDecisionServiceTask.dmn"
            }
    )
    public void testThrowErrorOnNoHitBooleanExpression() {
        CaseInstance caseInstance = cmmnRule.getCmmnRuntimeService().createCaseInstanceBuilder()
                .variable("throwErrorOnNoHits", Boolean.FALSE)
                .variable("testVar", "test2")
                .caseDefinitionKey("myCase")
                .start();

        assertResultVariable(caseInstance);
    }

    @Test
    @CmmnDeployment(
            resources = { "org/flowable/cmmn/test/runtime/DecisionTaskTest.testHitFlag.cmmn",
                          "org/flowable/cmmn/test/runtime/DecisionTaskTest.testDecisionServiceTask.dmn"
            }
    )
    public void testThrowErrorOnNoHitWithHit() {
        CaseInstance caseInstance = cmmnRule.getCmmnRuntimeService().createCaseInstanceBuilder()
                .variable("throwErrorOnNoHits", true)
                .variable("testVar", "test2")
                .caseDefinitionKey("myCase")
                .start();

        assertResultVariable(caseInstance);
    }

    @Test
    @CmmnDeployment(
            resources = { "org/flowable/cmmn/test/runtime/DecisionTaskTest.testHitFlag.cmmn",
                          "org/flowable/cmmn/test/runtime/DecisionTaskTest.testDecisionServiceTask.dmn"
            }
    )
    public void testThrowErrorOnNoHit() {
        this.expectedException.expect(FlowableException.class);
        this.expectedException.expectMessage("DMN decision table with key decisionTable did not hit any rules for the provided input.");

        cmmnRule.getCmmnRuntimeService().createCaseInstanceBuilder()
                .variable("throwErrorOnNoHits", true)
                .variable("testVar", "noHit")
                .caseDefinitionKey("myCase")
                .start();
    }

    @Test
    @CmmnDeployment(
            resources = {"org/flowable/cmmn/test/runtime/DecisionTaskTest.testHitFlag.cmmn",
                    "org/flowable/cmmn/test/runtime/DecisionTaskTest.testDecisionServiceTask.dmn"
            }
    )
    public void testDoNotThrowErrorOnNoHit() {
        CaseInstance caseInstance = cmmnRule.getCmmnRuntimeService().createCaseInstanceBuilder()
                .variable("throwErrorOnNoHits", false)
                .variable("testVar", "noHitValue")
                .caseDefinitionKey("myCase")
                .start();

        assertNoResultVariable(caseInstance);
    }

    @Test
    @CmmnDeployment(
            resources = {"org/flowable/cmmn/test/runtime/DecisionTaskTest.testExpressionReferenceKey.cmmn",
                    "org/flowable/cmmn/test/runtime/DecisionTaskTest.testDecisionServiceTask.dmn"
            }
    )
    public void testExpressionReferenceKey() {
        CaseInstance caseInstance = cmmnRule.getCmmnRuntimeService().createCaseInstanceBuilder()
                .caseDefinitionKey("myCase")
                .variable("testVar", "test2")
                .variable("referenceKey", "decisionTable")
                .start();

        assertResultVariable(caseInstance);
    }

    @Test
    @CmmnDeployment(
            resources = {"org/flowable/cmmn/test/runtime/DecisionTaskTest.testExpressionReferenceKey.cmmn"}
    )
    public void testNullReferenceKey() {
        this.expectedException.expect(FlowableException.class);
        this.expectedException.expectMessage("Could not execute decision: no externalRef defined");

        cmmnRule.getCmmnRuntimeService().createCaseInstanceBuilder()
                .caseDefinitionKey("myCase")
                .variable("testVar", "test2")
                .variable("referenceKey", null)
                .start();
    }

    @Test
    @CmmnDeployment(
            resources = {"org/flowable/cmmn/test/runtime/DecisionTaskTest.testExpressionReferenceKey.cmmn"}
    )
    public void testNonStringReferenceKey() {
        this.expectedException.expect(FlowableException.class);
        this.expectedException.expectMessage("No decision found for key: 1 and parent deployment id");

        cmmnRule.getCmmnRuntimeService().createCaseInstanceBuilder()
                .caseDefinitionKey("myCase")
                .variable("testVar", "test2")
                .variable("referenceKey", 1)
                .start();
    }

    @Test
    @CmmnDeployment(
            resources = {"org/flowable/cmmn/test/runtime/DecisionTaskTest.testExpressionReferenceKey.cmmn"}
    )
    public void testNonExistingReferenceKey() {
        this.expectedException.expect(FlowableException.class);
        this.expectedException.expectMessage("No decision found for key: NonExistingReferenceKey and parent deployment id");

        cmmnRule.getCmmnRuntimeService().createCaseInstanceBuilder()
                .caseDefinitionKey("myCase")
                .variable("testVar", "test2")
                .variable("referenceKey", "NonExistingReferenceKey")
                .start();
    }

    @Test
    @CmmnDeployment(
            resources = {
                    "org/flowable/cmmn/test/runtime/DecisionTaskTest.testBlocking.cmmn",
                    "org/flowable/cmmn/test/runtime/DecisionTaskTest.testDecisionServiceTask.dmn"
            }
    )
    public void testBlocking() {
        // is blocking is not taken into the execution
        CaseInstance caseInstance = cmmnRule.getCmmnRuntimeService().createCaseInstanceBuilder()
                .caseDefinitionKey("myCase")
                .variable("testVar", "test2")
                .variable("referenceKey", "decisionTable")
                .start();

        assertResultVariable(caseInstance);
    }

    @Test
    @CmmnDeployment(
        resources = {
            "org/flowable/cmmn/test/runtime/DecisionTaskTest.testUseDmnOutputInEntryCriteria.cmmn",
            "org/flowable/cmmn/test/runtime/DecisionTaskTest.testUseDmnOutputInEntryCriteria.dmn"
        }
    )
    public void testUseDmnOutputInEntryCriteria() {
        CaseInstance caseInstance = cmmnRule.getCmmnRuntimeService().createCaseInstanceBuilder()
            .caseDefinitionKey("testRules")
            .variable("first", "11")
            .variable("second", "11")
            .start();

        // The entry sentry on the first stage uses the output of the DMN table
        List<Task> tasks = cmmnRule.getCmmnTaskService().createTaskQuery().caseInstanceId(caseInstance.getId()).orderByTaskName().asc().list();
        assertEquals("Human task", tasks.get(0).getName());
        assertEquals("Task One", tasks.get(1).getName());
    }

    @Test
    @CmmnDeployment(
        resources = {"org/flowable/cmmn/test/runtime/DecisionTaskTest.testDecisionServiceTaskFallBackToDefaultTenant.cmmn"},
        tenantId = "flowable"
    )
    public void testDecisionServiceTaskWithFallback() {
        deployDmnTableAssertCaseStarted();
    }
    
    @Test
    @CmmnDeployment(
        resources = {"org/flowable/cmmn/test/runtime/DecisionTaskTest.testDecisionServiceTaskFallBackToDefaultTenantFalse.cmmn"},
        tenantId = "flowable"
    )
    public void testDecisionServiceTaskWithFallbackFalse() {
        this.expectedException.expect(FlowableException.class);
        this.expectedException.expectMessage("and tenant id: flowable. There was also no fall back decision table found without parent deployment id.");

        deployDmnTableAssertCaseStarted();
    }
    
    @Test
    @CmmnDeployment(
        resources = {"org/flowable/cmmn/test/runtime/DecisionTaskTest.testDecisionServiceTask.cmmn"},
        tenantId = "flowable"
    )
    public void testDecisionServiceTaskWithGlobalTenantFallback() {
        deployDmnTableWithGlobalTenantFallback("defaultFlowable");
    }
    
    @Test
    @CmmnDeployment(
        resources = {"org/flowable/cmmn/test/runtime/DecisionTaskTest.testDecisionServiceTaskFallBackToDefaultTenantFalse.cmmn"},
        tenantId = "flowable"
    )
    public void testDecisionServiceTaskWithGlobalTenantFallbackNoDefinition() {
        this.expectedException.expect(FlowableException.class);
        this.expectedException.expectMessage("There was also no fall back decision table found for default tenant defaultFlowable");

        deployDmnTableWithGlobalTenantFallback("otherTenant");
    }

    @Test
    @CmmnDeployment(resources = {
            "org/flowable/cmmn/test/runtime/DecisionTaskTest.testDecisionServiceTaskSameDeployment.cmmn",
            "org/flowable/cmmn/test/runtime/DecisionTaskTest.testDecisionServiceTask.dmn"
    })
    public void testDecisionTaskExecutionWithSameDeployment() {
        CmmnRuntimeService cmmnRuntimeService = cmmnRule.getCmmnRuntimeService();
        CaseInstance caseInstance = cmmnRuntimeService
                .createCaseInstanceBuilder()
                .caseDefinitionKey("myCase")
                .variable("testVar", "test2")
                .start();

        assertThat(cmmnRuntimeService.getVariable(caseInstance.getId(), "resultVar"))
                .isEqualTo("executed");

        try {
            // Same deployment decision should be used from parent deployment
            DmnEngines.getDefaultDmnEngine()
                    .getDmnRepositoryService()
                    .createDeployment()
                    .addClasspathResource("org/flowable/cmmn/test/runtime/DecisionTaskTest.testDecisionServiceTaskV2.dmn")
                    .deploy();

            caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                    .caseDefinitionKey("myCase")
                    .variable("testVar", "test2")
                    .start();

            assertThat(cmmnRuntimeService.getVariable(caseInstance.getId(), "resultVar"))
                    .isEqualTo("executed");

        } finally {
            deleteAllDmnDeployments();
        }
    }

    @Test
    @CmmnDeployment(resources = {
            "org/flowable/cmmn/test/runtime/DecisionTaskTest.testDecisionServiceTaskSameDeployment.cmmn",
            "org/flowable/cmmn/test/runtime/DecisionTaskTest.testDecisionServiceTask.dmn"
    }, tenantId = "flowable")
    public void testDecisionTaskExecutionWithSameDeploymentInTenant() {
        CmmnRuntimeService cmmnRuntimeService = cmmnRule.getCmmnRuntimeService();
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("myCase")
                .variable("testVar", "test2")
                .tenantId("flowable")
                .start();

        assertThat(cmmnRuntimeService.getVariable(caseInstance.getId(), "resultVar"))
                .isEqualTo("executed");

        try {
            // Same deployment decision should be used from parent deployment
            DmnEngines.getDefaultDmnEngine()
                    .getDmnRepositoryService()
                    .createDeployment()
                    .addClasspathResource("org/flowable/cmmn/test/runtime/DecisionTaskTest.testDecisionServiceTaskV2.dmn")
                    .tenantId("flowable")
                    .deploy();

            caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                    .caseDefinitionKey("myCase")
                    .variable("testVar", "test2")
                    .tenantId("flowable")
                    .start();

            assertThat(cmmnRuntimeService.getVariable(caseInstance.getId(), "resultVar"))
                    .isEqualTo("executed");

        } finally {
            deleteAllDmnDeployments();
        }
    }

    @Test
    @CmmnDeployment(resources = {
            "org/flowable/cmmn/test/runtime/DecisionTaskTest.testDecisionServiceTask.cmmn",
            "org/flowable/cmmn/test/runtime/DecisionTaskTest.testDecisionServiceTask.dmn"
    })
    public void testDecisionTaskExecutionWithSameDeploymentGlobal() {
        CmmnRuntimeService cmmnRuntimeService = cmmnRule.getCmmnRuntimeService();
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("myCase")
                .variable("testVar", "test2")
                .start();

        assertThat(cmmnRuntimeService.getVariable(caseInstance.getId(), "resultVar"))
                .isEqualTo("executed");

        try {
            // Same deployment decision should be used from parent deployment
            DmnEngines.getDefaultDmnEngine()
                    .getDmnRepositoryService()
                    .createDeployment()
                    .addClasspathResource("org/flowable/cmmn/test/runtime/DecisionTaskTest.testDecisionServiceTaskV2.dmn")
                    .deploy();

            caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                    .caseDefinitionKey("myCase")
                    .variable("testVar", "test2")
                    .start();

            assertThat(cmmnRuntimeService.getVariable(caseInstance.getId(), "resultVar"))
                    .isEqualTo("executed");

        } finally {
            deleteAllDmnDeployments();
        }
    }

    @Test
    @CmmnDeployment(resources = {
            "org/flowable/cmmn/test/runtime/DecisionTaskTest.testDecisionServiceTaskSameDeploymentFalse.cmmn",
            "org/flowable/cmmn/test/runtime/DecisionTaskTest.testDecisionServiceTask.dmn"
    })
    public void testDecisionTaskExecutionWithSameDeploymentFalse() {
        CmmnRuntimeService cmmnRuntimeService = cmmnRule.getCmmnRuntimeService();
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("myCase")
                .variable("testVar", "test2")
                .start();

        assertThat(cmmnRuntimeService.getVariable(caseInstance.getId(), "resultVar"))
                .isEqualTo("executed");

        try {
            // Latest decision should be used
            DmnEngines.getDefaultDmnEngine()
                    .getDmnRepositoryService()
                    .createDeployment()
                    .addClasspathResource("org/flowable/cmmn/test/runtime/DecisionTaskTest.testDecisionServiceTaskV2.dmn")
                    .deploy();

            caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                    .caseDefinitionKey("myCase")
                    .variable("testVar", "test2")
                    .start();

            assertThat(cmmnRuntimeService.getVariable(caseInstance.getId(), "resultVar"))
                    .isEqualTo("executedV2");

        } finally {
            deleteAllDmnDeployments();
        }
    }

    @Test
    @CmmnDeployment(resources = {
            "org/flowable/cmmn/test/runtime/DecisionTaskTest.testDecisionServiceTaskSameDeploymentFalse.cmmn",
            "org/flowable/cmmn/test/runtime/DecisionTaskTest.testDecisionServiceTask.dmn"
    }, tenantId = "flowable")
    public void testDecisionTaskExecutionWithSameDeploymentFalseInTenant() {
        CmmnRuntimeService cmmnRuntimeService = cmmnRule.getCmmnRuntimeService();
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("myCase")
                .variable("testVar", "test2")
                .tenantId("flowable")
                .start();

        assertThat(cmmnRuntimeService.getVariable(caseInstance.getId(), "resultVar"))
                .isEqualTo("executed");

        try {
            // Latest decision should be used
            DmnEngines.getDefaultDmnEngine()
                    .getDmnRepositoryService()
                    .createDeployment()
                    .addClasspathResource("org/flowable/cmmn/test/runtime/DecisionTaskTest.testDecisionServiceTaskV2.dmn")
                    .tenantId("flowable")
                    .deploy();

            caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                    .caseDefinitionKey("myCase")
                    .variable("testVar", "test2")
                    .tenantId("flowable")
                    .start();

            assertThat(cmmnRuntimeService.getVariable(caseInstance.getId(), "resultVar"))
                    .isEqualTo("executedV2");

        } finally {
            deleteAllDmnDeployments();
        }
    }

    // Helper methodes

    protected void deployDmnTableAssertCaseStarted() {
        org.flowable.cmmn.api.repository.CmmnDeployment cmmnDeployment = cmmnRule.getCmmnRepositoryService().createDeployment().
            addClasspathResource("org/flowable/cmmn/test/runtime/DecisionTaskTest.testDecisionServiceTask.dmn").
            tenantId(CmmnEngineConfiguration.NO_TENANT_ID).
            deploy();

        try {
            CaseInstance caseInstance = cmmnRule.getCmmnRuntimeService().createCaseInstanceBuilder()
                .caseDefinitionKey("myCase")
                .variable("testVar", "test2")
                .tenantId("flowable")
                .start();

            assertResultVariable(caseInstance);
            
            CmmnEngineConfiguration cmmnEngineConfiguration = cmmnRule.getCmmnEngineConfiguration();
            DmnEngineConfiguration dmnEngineConfiguration = (DmnEngineConfiguration) cmmnEngineConfiguration.getEngineConfigurations().get(
                            EngineConfigurationConstants.KEY_DMN_ENGINE_CONFIG);
            
            DmnHistoricDecisionExecution decisionExecution = dmnEngineConfiguration.getDmnHistoryService()
                            .createHistoricDecisionExecutionQuery()
                            .instanceId(caseInstance.getId())
                            .singleResult();
            
            assertNotNull(decisionExecution);
            assertEquals("flowable", decisionExecution.getTenantId());
            
        } finally {
            cmmnRule.getCmmnRepositoryService().deleteDeployment(cmmnDeployment.getId(), true);
        }
    }
    
    protected void deployDmnTableWithGlobalTenantFallback(String tenantId) {
        CmmnEngineConfiguration cmmnEngineConfiguration = cmmnRule.getCmmnEngineConfiguration();
        DmnEngineConfiguration dmnEngineConfiguration = (DmnEngineConfiguration) cmmnEngineConfiguration.getEngineConfigurations().get(
                        EngineConfigurationConstants.KEY_DMN_ENGINE_CONFIG);
        
        DefaultTenantProvider originalDefaultTenantProvider = dmnEngineConfiguration.getDefaultTenantProvider();
        dmnEngineConfiguration.setFallbackToDefaultTenant(true);
        dmnEngineConfiguration.setDefaultTenantValue("defaultFlowable");
        
        org.flowable.cmmn.api.repository.CmmnDeployment cmmnDeployment = cmmnRule.getCmmnRepositoryService().createDeployment().
            addClasspathResource("org/flowable/cmmn/test/runtime/DecisionTaskTest.testDecisionServiceTask.dmn").
            tenantId(tenantId).
            deploy();

        try {
            CaseInstance caseInstance = cmmnRule.getCmmnRuntimeService().createCaseInstanceBuilder()
                .caseDefinitionKey("myCase")
                .variable("testVar", "test2")
                .tenantId("flowable")
                .start();

            assertResultVariable(caseInstance);
            
            DmnHistoricDecisionExecution decisionExecution = dmnEngineConfiguration.getDmnHistoryService()
                            .createHistoricDecisionExecutionQuery()
                            .instanceId(caseInstance.getId())
                            .singleResult();
            
            assertNotNull(decisionExecution);
            assertEquals("flowable", decisionExecution.getTenantId());
            
        } finally {
            dmnEngineConfiguration.setFallbackToDefaultTenant(false);
            dmnEngineConfiguration.setDefaultTenantProvider(originalDefaultTenantProvider);
            cmmnRule.getCmmnRepositoryService().deleteDeployment(cmmnDeployment.getId(), true);
        }
    }

    protected void assertResultVariable(CaseInstance caseInstance) {
        assertNotNull(caseInstance);

        PlanItemInstance planItemInstance = cmmnRule.getCmmnRuntimeService().createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .singleResult();
        assertNotNull(planItemInstance);

        Assert.assertEquals("executed", cmmnRule.getCmmnRuntimeService().getVariable(caseInstance.getId(), "resultVar"));

        // Triggering the task should end the case instance
        cmmnRule.getCmmnRuntimeService().triggerPlanItemInstance(planItemInstance.getId());
        Assert.assertEquals(0, cmmnRule.getCmmnRuntimeService().createCaseInstanceQuery().count());

        Assert.assertEquals("executed", cmmnRule.getCmmnHistoryService().createHistoricVariableInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .variableName("resultVar")
                .singleResult().getValue());
    }

    protected void assertNoResultVariable(CaseInstance caseInstance) {
        assertNotNull(caseInstance);

        PlanItemInstance planItemInstance = cmmnRule.getCmmnRuntimeService().createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .singleResult();
        assertNotNull(planItemInstance);

        Assert.assertNull(cmmnRule.getCmmnRuntimeService().getVariable(caseInstance.getId(), "resultVar"));

        // Triggering the task should end the case instance
        cmmnRule.getCmmnRuntimeService().triggerPlanItemInstance(planItemInstance.getId());
        Assert.assertEquals(0, cmmnRule.getCmmnRuntimeService().createCaseInstanceQuery().count());
    }

    protected void deleteAllDmnDeployments() {
        DmnEngineConfiguration dmnEngineConfiguration = (DmnEngineConfiguration) cmmnRule.getCmmnEngine()
                .getCmmnEngineConfiguration()
                .getEngineConfigurations()
                .get(EngineConfigurationConstants.KEY_DMN_ENGINE_CONFIG);
        dmnEngineConfiguration.getDmnRepositoryService().createDeploymentQuery().list().stream()
                .forEach(
                        deployment -> dmnEngineConfiguration.getDmnRepositoryService().deleteDeployment(deployment.getId())
                );
    }

}
