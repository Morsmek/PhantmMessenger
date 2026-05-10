package com.stagic.phantm.android.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.stagic.phantm.android.ui.contact.ContactVerificationScreen
import com.stagic.phantm.android.ui.conversation.ConversationListScreen
import com.stagic.phantm.android.ui.message.MessageThreadScreen
import com.stagic.phantm.android.ui.settings.SettingsScreen

private object Route {
    const val CONVERSATION_LIST = "conversations"
    const val MESSAGE_THREAD = "thread/{contactId}"
    const val CONTACT_VERIFY = "verify/{contactId}"
    const val SETTINGS = "settings"

    fun thread(contactId: String) = "thread/$contactId"
    fun verify(contactId: String) = "verify/$contactId"
}

@Composable
fun PhantmNavGraph() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Route.CONVERSATION_LIST) {

        composable(Route.CONVERSATION_LIST) {
            ConversationListScreen(
                onConversationClick = { contactId -> navController.navigate(Route.thread(contactId)) },
            )
        }

        composable(
            route = Route.MESSAGE_THREAD,
            arguments = listOf(navArgument("contactId") { type = NavType.StringType }),
        ) { backStack ->
            val contactId = backStack.arguments?.getString("contactId") ?: return@composable
            MessageThreadScreen(
                contactId = contactId,
                onBack = { navController.popBackStack() },
            )
        }

        composable(
            route = Route.CONTACT_VERIFY,
            arguments = listOf(navArgument("contactId") { type = NavType.StringType }),
        ) { backStack ->
            val contactId = backStack.arguments?.getString("contactId") ?: return@composable
            ContactVerificationScreen(
                contactId = contactId,
                onBack = { navController.popBackStack() },
            )
        }

        composable(Route.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
