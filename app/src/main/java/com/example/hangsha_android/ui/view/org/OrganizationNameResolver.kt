package com.example.hangsha_android.ui.view.org

fun organizationLabel(
    orgId: Long?,
    organizationNames: Map<Long, String>,
    fallbackName: String? = null
): String {
    return orgId?.let(organizationNames::get)
        ?: fallbackName?.takeIf { it.isNotBlank() }
        ?: "-"
}