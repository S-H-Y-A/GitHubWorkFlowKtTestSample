#!/usr/bin/env kotlin
// Usage: $ .github/workflows/refreshVersions-755.main.kts

@file:DependsOn("it.krzeminski:github-actions-kotlin-dsl:0.33.0")

// Find latest version at https://github.com/krzema12/github-actions-kotlin-dsl/releases

import it.krzeminski.githubactions.actions.Action
import it.krzeminski.githubactions.actions.CustomAction
import it.krzeminski.githubactions.actions.actions.CheckoutV3
import it.krzeminski.githubactions.actions.actions.SetupJavaV3
import it.krzeminski.githubactions.actions.reposync.PullRequestV2
import it.krzeminski.githubactions.domain.RunnerType
import it.krzeminski.githubactions.domain.Workflow
import it.krzeminski.githubactions.domain.triggers.Cron
import it.krzeminski.githubactions.domain.triggers.Schedule
import it.krzeminski.githubactions.domain.triggers.WorkflowDispatch
import it.krzeminski.githubactions.dsl.expressions.expr
import it.krzeminski.githubactions.dsl.workflow
import it.krzeminski.githubactions.yaml.writeToFile

private val everyMondayAt7am = Cron(minute = "0", hour = "7", dayWeek = "1")

val masterBranch = "master"
val newBranch = "dependency-update"
val commitMessage = "Refresh libs.versions.toml and versions.properties"
val prTitle = "Upgrade gradle dependencies"
val prBody = "[refreshVersions](https://github.com/jmfayard/refreshVersions) has found those library updates!"
val javaSetup = SetupJavaV3(
    javaVersion = "17",
    distribution = SetupJavaV3.Distribution.Adopt,
    cache = SetupJavaV3.BuildPlatform.Gradle
)

val workflowRefreshVersions: Workflow = workflow(
    name = "RefreshVersions-755",
    on = listOf(
        Schedule(listOf(everyMondayAt7am)),
        WorkflowDispatch(),
    ),
    sourceFile = __FILE__.toPath(),
) {
    job(
        id = "Refresh-Versions-755",
        runsOn = RunnerType.UbuntuLatest,
    ) {
        uses(
            name = "check-out",
            action = CheckoutV3(ref = masterBranch),
        )
        uses(
            name = "setup-java",
            action = javaSetup,
        )
        uses(
            name = "create-branch",
            action = ActionCreateBranchV2(newBranch),
            env = linkedMapOf(
                "GITHUB_TOKEN" to expr { secrets.GITHUB_TOKEN },
            ),
        )
        run(
            name = "Grant permission gradlew",
            command = "chmod +x gradlew"
        )
        run(
            name = "gradle refreshVersions",
            command = "./gradlew refreshVersions"
        )
        uses(
            name = "Commit",
            action = CustomAction(
                actionOwner = "actions-js",
                actionName = "push",
                actionVersion = "v1.4",
                inputs = linkedMapOf(
                    "github_token" to expr { secrets.GITHUB_TOKEN },
                    "message" to commitMessage,
                    "branch" to newBranch,
                    "empty" to false.toString(),
                    "force" to true.toString()
                )
            )
        )
        uses(
            name = "Pull Request",
            action = PullRequestV2(
                sourceBranch = newBranch,
                destinationBranch = masterBranch,
                prTitle = prTitle,
                prBody = prBody,
                prDraft = false,
                prAllowEmpty = false,
                githubToken = expr { secrets.GITHUB_TOKEN },
            ),
        )
    }
}
println("Updating ${workflowRefreshVersions.targetFileName}")
workflowRefreshVersions.writeToFile()

class ActionCreateBranchV2(private val branch: String) : Action("peterjgrainger", "action-create-branch", "v2.3.0") {
    override fun toYamlArguments() = linkedMapOf(
        "branch" to branch,
    )
}
