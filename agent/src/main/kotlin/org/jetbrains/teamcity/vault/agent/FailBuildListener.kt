/*
 * Copyright 2000-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetbrains.teamcity.vault.agent

import jetbrains.buildServer.agent.AgentLifeCycleAdapter
import jetbrains.buildServer.agent.AgentRunningBuild
import org.jetbrains.teamcity.vault.VaultConstants

class FailBuildListener : AgentLifeCycleAdapter() {
    override fun buildStarted(runningBuild: AgentRunningBuild) {
        val parameters = runningBuild.sharedConfigParameters
        parameters.keys.filter { it.startsWith(VaultConstants.PARAMETER_PREFIX) && it.endsWith(VaultConstants.URL_PROPERTY_SUFFIX) }
                .forEach {

                    val url = parameters[it]

                    if (url == null || url.isNullOrBlank()) return

                    runningBuild.stopBuild("HashiCorp Vault is not supported on this agent. Please add agent requirement for '${VaultConstants.FEATURE_SUPPORTED_AGENT_PARAMETER}' parameter or run agent using Java 1.8")
                }
    }
}