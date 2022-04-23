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

    echo("Actions will be indicated by this symbols:")

    val groupedPlans = plan.topicPlans.groupBy { it.action }
    if (groupedPlans.contains(PlanAction.ADD)) {
        echo(" ${symbol[PlanAction.ADD]} create")
    }
    if (groupedPlans.contains(PlanAction.UPDATE)) {
        echo(" $${symbol[PlanAction.UPDATE]} update")
    }
    if (groupedPlans.contains(PlanAction.REMOVE)) {
        echo(" $${symbol[PlanAction.REMOVE]} remove")
    }

    echo("The following changes are going to be performed:")

    plan.topicPlans.forEach { printTopicPlan(it) }
}

fun printTopicPlan(topicPlan: TopicPlan) {
    val topicPlanAction = topicPlan.action
    echo("${symbol[topicPlanAction]} [Topic] ${topicPlan.name}")
    echo("\t${symbol[topicPlanAction]} partitions: ${topicPlan.partitionPlan.newValue}")
    echo("\t${symbol[topicPlanAction]} replication: ${topicPlan.replicationPlan.newValue}")
    if (topicPlan.topicConfigPlans.isNotEmpty()) {
        echo("\t${symbol[topicPlanAction]} config:")
        topicPlan.topicConfigPlans.forEach { topicConfigPlan ->
            echo("\t\t${symbol[topicPlanAction]} ${topicConfigPlan.key}: ${topicConfigPlan.newValue}")
        }
    }
}

fun preTopicApplyLog(topicPlan: TopicPlan) {
    echo("Applying ${topicPlan.action.toLogAction()}")
    printTopicPlan(topicPlan)
}

fun postTopicApplyLog(topicPlan: TopicPlan) {
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
