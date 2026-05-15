package com.example.hangsha_android.ui.view.org

fun organizationLabel(
    orgId: Long,
    organizationNames: Map<Long, String>,
    fallbackName: String? = null
): String {
    return organizationNames[orgId]
        ?: fallbackName?.takeIf { it.isNotBlank() }
        ?: "orgID: $orgId"
}
