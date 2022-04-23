package io.github.rsolci.kafkaops.printer

import com.github.ajalt.clikt.output.TermUi.echo
import io.github.rsolci.kafkaops.models.plan.ClusterPlan
import io.github.rsolci.kafkaops.models.plan.PlanAction
import io.github.rsolci.kafkaops.models.plan.TopicPlan

private val symbol = mapOf(
    PlanAction.ADD to "+",
    PlanAction.UPDATE to "~",
    PlanAction.REMOVE to "-",
    PlanAction.DO_NOTHING to "",
)

fun printPlan(plan: ClusterPlan) {
    echo("Execution plan generated successfully.\n")
    if (!plan.topicPlans.any { it.action != PlanAction.DO_NOTHING }) {
        echo("No changes detected. Cluster is up to date.")
        return
    }

    echo("Actions performed will be indicated by this symbols:")
    echo(" ${symbol[PlanAction.ADD]} create")
    echo(" ${symbol[PlanAction.UPDATE]} update")
    echo(" ${symbol[PlanAction.REMOVE]} remove")

    echo("\nThe following changes are going to be performed:")

    plan.topicPlans.forEach { printTopicPlan(it) }
}

fun printTopicPlan(topicPlan: TopicPlan) {
    val topicPlanAction = topicPlan.action
    if (topicPlanAction == PlanAction.DO_NOTHING) {
        return
    }
    echo("${symbol[topicPlanAction]} [Topic] ${topicPlan.name}")

    printTopicDetailPlan(
        topicPlan.partitionPlan.action,
        topicPlan.partitionPlan.newValue,
        topicPlan.partitionPlan.previousValue,
        "partitions"
    )

    printTopicDetailPlan(
        topicPlan.replicationPlan.action,
        topicPlan.replicationPlan.newValue,
        topicPlan.replicationPlan.previousValue,
        "replication"
    )

    val changedConfigPlans = topicPlan.topicConfigPlans.filter { it.action != PlanAction.DO_NOTHING }
    if (changedConfigPlans.isNotEmpty()) {
        echo("\t${symbol[topicPlanAction]} config:")
        changedConfigPlans.forEach { topicConfigPlan ->
            when (val configAction = topicConfigPlan.action) {
                PlanAction.ADD -> {
                    echo("\t\t${symbol[configAction]} ${topicConfigPlan.key}: ${topicConfigPlan.newValue}")
                }
                PlanAction.UPDATE -> {
                    echo(
                        "\t\t${symbol[configAction]} ${topicConfigPlan.key}: " +
                            "${topicConfigPlan.previousValue} -> ${topicConfigPlan.newValue}"
                    )
                }
                PlanAction.REMOVE -> {
                    echo("\t\t${symbol[configAction]} ${topicConfigPlan.key}")
                }
                else -> {}
            }
        }
    }
}

private fun printTopicDetailPlan(
    replicationPlanAction: PlanAction,
    newValue: Int,
    previousValue: Int?,
    propertyName: String
) {
    if (replicationPlanAction == PlanAction.ADD || replicationPlanAction == PlanAction.REMOVE) {
        echo("\t${symbol[replicationPlanAction]} $propertyName: $newValue")
    } else if (replicationPlanAction == PlanAction.UPDATE) {
        echo(
            "\t${symbol[replicationPlanAction]} $propertyName: $previousValue -> $newValue"
        )
    }
}

fun preTopicApplyLog(topicPlan: TopicPlan) {
    if (topicPlan.action == PlanAction.DO_NOTHING) {
        return
    }
    echo("Applying ${topicPlan.action.toLogAction()}")
    printTopicPlan(topicPlan)
}

fun postTopicApplyLog(topicPlan: TopicPlan) {
    if (topicPlan.action == PlanAction.DO_NOTHING) {
        return
    }
    echo("Successfully applied ${topicPlan.action.toLogAction()} to ${topicPlan.name}")
}

private fun PlanAction.toLogAction(): String {
    return when (this) {
        PlanAction.ADD -> "CREATE"
        PlanAction.UPDATE -> "UPDATE"
        PlanAction.REMOVE -> "DELETE"
        else -> ""
    }
}
