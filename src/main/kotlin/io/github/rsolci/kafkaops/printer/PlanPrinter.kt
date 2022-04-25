package io.github.rsolci.kafkaops.printer

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
    print("Execution plan generated successfully.\n")
    if (!plan.topicPlans.any { it.action != PlanAction.DO_NOTHING }) {
        print("No changes detected. Cluster is up to date.")
        return
    }

    println("Actions performed will be indicated by this symbols:")
    green(" ${symbol[PlanAction.ADD]} create")
    yellow(" ${symbol[PlanAction.UPDATE]} update")
    red(" ${symbol[PlanAction.REMOVE]} remove")

    println("\nThe following changes are going to be performed:")

    plan.topicPlans.forEach { printTopicPlan(it) }
}

fun printTopicPlan(topicPlan: TopicPlan) {
    val topicPlanAction = topicPlan.action
    if (topicPlanAction == PlanAction.DO_NOTHING) {
        return
    }
    printAction(topicPlanAction, "${symbol[topicPlanAction]} [Topic] ${topicPlan.name}")

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
        printAction(topicPlanAction, "\t${symbol[topicPlanAction]} config:")
        changedConfigPlans.forEach { topicConfigPlan ->
            when (val configAction = topicConfigPlan.action) {
                PlanAction.ADD -> {
                    printAction(
                        configAction,
                        "\t\t${symbol[configAction]} ${topicConfigPlan.key}: ${topicConfigPlan.newValue}"
                    )
                }
                PlanAction.UPDATE -> {
                    printAction(
                        configAction,
                        "\t\t${symbol[configAction]} ${topicConfigPlan.key}: " +
                            "${topicConfigPlan.previousValue} -> ${topicConfigPlan.newValue}"
                    )
                }
                PlanAction.REMOVE -> {
                    printAction(configAction, "\t\t${symbol[configAction]} ${topicConfigPlan.key}")
                }
                else -> {}
            }
        }
    }
}

private fun printTopicDetailPlan(
    planAction: PlanAction,
    newValue: Int,
    previousValue: Int?,
    propertyName: String
) {
    if (planAction == PlanAction.ADD || planAction == PlanAction.REMOVE) {
        printAction(planAction, "\t${symbol[planAction]} $propertyName: $newValue")
    } else if (planAction == PlanAction.UPDATE) {
        printAction(
            planAction,
            "\t${symbol[planAction]} $propertyName: $previousValue -> $newValue"
        )
    }
}

fun preTopicApplyLog(topicPlan: TopicPlan) {
    if (topicPlan.action == PlanAction.DO_NOTHING) {
        return
    }
    printAction(topicPlan.action, "Applying ${topicPlan.action.toLogAction()}")
    printTopicPlan(topicPlan)
}

fun postTopicApplyLog(topicPlan: TopicPlan) {
    if (topicPlan.action == PlanAction.DO_NOTHING) {
        return
    }
    print("Successfully applied ${topicPlan.action.toLogAction()} to ${topicPlan.name}")
}

private fun PlanAction.toLogAction(): String {
    return when (this) {
        PlanAction.ADD -> "CREATE"
        PlanAction.UPDATE -> "UPDATE"
        PlanAction.REMOVE -> "DELETE"
        else -> ""
    }
}
