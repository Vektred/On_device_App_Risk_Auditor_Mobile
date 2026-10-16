package com.riskauditor.core.analyzer

import com.riskauditor.core.model.AppFacts
import com.riskauditor.core.model.Finding

/**
 * Interface implemented by every rule in the risk engine.
 * Guarantees that score points and explainable human reasons come from a single source of truth.
 */
interface Rule {
    val ruleId: String
    fun evaluate(app: AppFacts): Finding?
}

