package com.riskauditor.core.model

enum class ProtectionLevel(val label: String) {
    NORMAL("Normal"),
    DANGEROUS("Dangerous"),
    SIGNATURE("Signature"),
    SPECIAL("Special"),
    CUSTOM("Custom / Unknown")
}

