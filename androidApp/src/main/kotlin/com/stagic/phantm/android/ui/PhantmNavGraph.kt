package com.stagic.phantm.android.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.stagic.phantm.android.ui.contact.AddContactScreen
import com.stagic.phantm.android.ui.contact.ContactVerificationScreen
import com.stagic.phantm.android.ui.conversation.ConversationListScreen
import com.stagic.phantm.android.ui.message.MessageThreadScreen
import com.stagic.phantm.android.ui.onboarding.OnboardingScreen
import com.stagic.phantm.android.ui.onboarding.SplashScreen
import com.stagic.phantm.android.ui.panic.PanicWipeScreen
import com.stagic.phantm.android.ui.qr.QrIdentityScreen
import com.stagic.phantm.android.ui.settings.SettingsScreen

private object Route {
    const val SPLASH = "splash"
    const val ONBOARDING = "onboarding"
    const val CONVERSATION_LIST = "conversations"
    const val MESSAGE_THREAD = "thread/{contactId}"
    const val ADD_CONTACT = "add-contact"
    const val CONTACT_VERIFY = "verify/{contactId}"
    const val QR_IDENTITY = "qr-identity"
    const val SETTINGS = "settings"
    const val PANIC_WIPE = "panic-wipe"

    fun thread(contactId: String) = "thread/$contactId"
    fun verify(contactId: String) = "verify/$contactId"
}

@Composable
fun PhantmNavGraph() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Route.SPLASH) {

        composable(Route.SPLASH) {
            SplashScreen(onReady = {
                navController.navigate(Route.CONVERSATION_LIST) {
                    popUpTo(Route.SPLASH) { inclusive = true }
                }
            })
        }

        composable(Route.ONBOARDING) {
            OnboardingScreen(onComplete = {
                navController.navigate(Route.CONVERSATION_LIST) {
                    popUpTo(Route.ONBOARDING) { inclusive = true }
                }
            })
        }

        composable(Route.CONVERSATION_LIST) {
            ConversationListScreen(
                onConversationClick = { contactId -> navController.navigate(Route.thread(contactId)) },
                onNewChat = { navController.navigate(Route.ADD_CONTACT) },
                onSettingsClick = { navController.navigate(Route.SETTINGS) },
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

        composable(Route.ADD_CONTACT) {
            AddContactScreen(
                onBack = { navController.popBackStack() },
                onContactSelected = { name ->
                    navController.navigate(Route.thread(name.lowercase().replace(" ", "-")))
                },
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

        composable(Route.QR_IDENTITY) {
            QrIdentityScreen(onBack = { navController.popBackStack() })
        }

        composable(Route.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onPanicWipe = { navController.navigate(Route.PANIC_WIPE) },
            )
        }

        composable(Route.PANIC_WIPE) {
            PanicWipeScreen(
                onCancel = { navController.popBackStack() },
                onWipe = {
                    navController.navigate(Route.ONBOARDING) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }
    }
}
