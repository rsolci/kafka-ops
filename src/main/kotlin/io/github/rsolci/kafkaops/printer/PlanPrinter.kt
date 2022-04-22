package io.github.rsolci.kafkaops.printer

import com.github.ajalt.clikt.output.TermUi.echo
import io.github.rsolci.kafkaops.models.plan.ClusterPlan
import io.github.rsolci.kafkaops.models.plan.PlanAction

private const val ADD_SYMBOL = "+"
private const val UPDATE_SYMBOL = "~"
private const val DELETE_SYMBOL = "-"

fun printPlan(plan: ClusterPlan) {
    echo("Execution plan generated successfully.")
    echo("Actions will be indicated by this symbols:")

    val groupedPlans = plan.topicPlans.groupBy { it.action }
    if (groupedPlans.contains(PlanAction.ADD)) {
        echo(" $ADD_SYMBOL create")
    }
    if (groupedPlans.contains(PlanAction.UPDATE)) {
        echo(" $UPDATE_SYMBOL update")
    }
    if (groupedPlans.contains(PlanAction.REMOVE)) {
        echo(" $DELETE_SYMBOL delete")
    }

    echo("The following changes are going to be performed:")

    val toAddTopics = groupedPlans[PlanAction.ADD] ?: emptyList()
    toAddTopics.forEach { topicPlan ->
        echo("$ADD_SYMBOL [Topic] ${topicPlan.name}")
        echo("\t$ADD_SYMBOL partitions: ${topicPlan.partitionPlan.newValue}")
        echo("\t$ADD_SYMBOL replication: ${topicPlan.replicationPlan.newValue}")
        if (topicPlan.topicConfigPlans.isNotEmpty()) {
            echo("\t$ADD_SYMBOL config:")
            topicPlan.topicConfigPlans.forEach { topicConfigPlan ->
                echo("\t\t$ADD_SYMBOL ${topicConfigPlan.key}: ${topicConfigPlan.newValue}")
            }
        }
    }
}
