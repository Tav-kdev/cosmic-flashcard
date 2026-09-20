package com.cosmic.flashcards

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.cosmic.flashcards.data.dao.CardState
import com.cosmic.flashcards.ui.components.CosmicBackdrop
import com.cosmic.flashcards.ui.nav.CosmicBottomBar
import com.cosmic.flashcards.ui.nav.CosmicTopBar
import com.cosmic.flashcards.ui.nav.Routes
import com.cosmic.flashcards.ui.screens.CardEditScreen
import com.cosmic.flashcards.ui.screens.CardsScreen
import com.cosmic.flashcards.ui.screens.DashboardScreen
import com.cosmic.flashcards.ui.screens.DeckEditScreen
import com.cosmic.flashcards.ui.screens.DecksScreen
import com.cosmic.flashcards.ui.screens.ExamSetupScreen
import com.cosmic.flashcards.ui.screens.ImportScreen
import com.cosmic.flashcards.ui.screens.SessionDoneScreen
import com.cosmic.flashcards.ui.screens.StudyHomeScreen
import com.cosmic.flashcards.ui.screens.StudySessionScreen
import com.cosmic.flashcards.ui.screens.TagEditScreen
import com.cosmic.flashcards.ui.screens.TagsScreen
import com.cosmic.flashcards.ui.theme.CosmicTheme
import com.cosmic.flashcards.ui.theme.TextPrimary
import com.cosmic.flashcards.vm.CardEditViewModel
import com.cosmic.flashcards.vm.CardsViewModel
import com.cosmic.flashcards.vm.CosmicViewModelFactory
import com.cosmic.flashcards.vm.DashboardViewModel
import com.cosmic.flashcards.vm.DecksViewModel
import com.cosmic.flashcards.vm.ImportViewModel
import com.cosmic.flashcards.vm.StudyViewModel
import com.cosmic.flashcards.vm.TagsViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            CosmicTheme {
                CosmicAppRoot()
            }
        }
    }
}

/** Routes that show the bottom bar. Editors and the study session are full-screen. */
private val BAR_ROUTES = setOf(
    Routes.DASHBOARD, Routes.CARDS, Routes.DECKS,
    Routes.TAGS, Routes.IMPORT, Routes.STUDY_HOME,
)

@Composable
private fun CosmicAppRoot() {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val current = backStack?.destination?.route

    // One study VM for the whole app, so a session survives navigation.
    val studyVm: StudyViewModel = viewModel(factory = CosmicViewModelFactory.Factory)
    val due by studyVm.dueCount.collectAsStateWithLifecycle()

    val showBar = current in BAR_ROUTES

    CosmicBackdrop {
        Scaffold(
            containerColor = Color.Transparent,
            contentColor = TextPrimary,
            topBar = {
                if (showBar) {
                    Box(Modifier.statusBarsPadding()) { CosmicTopBar() }
                } else {
                    Box(Modifier.statusBarsPadding())
                }
            },
            bottomBar = {
                if (showBar) {
                    CosmicBottomBar(
                        currentRoute = current,
                        dueCount = due,
                        onNavigate = { route -> nav.navigateTab(route) },
                        onStudy = { nav.navigateTab(Routes.STUDY_HOME) },
                    )
                }
            },
        ) { padding ->
            CosmicNavHost(
                nav = nav,
                studyVm = studyVm,
                modifier = Modifier.padding(padding),
            )
        }
    }
}

/** Tab navigation: single-top, popping back to the dashboard. */
private fun NavHostController.navigateTab(route: String) {
    navigate(route) {
        popUpTo(Routes.DASHBOARD) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun CosmicNavHost(
    nav: NavHostController,
    studyVm: StudyViewModel,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = nav,
        startDestination = Routes.DASHBOARD,
        modifier = modifier.fillMaxSize(),
    ) {

        composable(Routes.DASHBOARD) {
            val vm: DashboardViewModel = viewModel(factory = CosmicViewModelFactory.Factory)
            DashboardScreen(
                vm = vm,
                onOpenCards = { state ->
                    pendingCardState = state
                    nav.navigate(Routes.CARDS)
                },
                onOpenDeck = { id -> pendingDeckId = id; nav.navigate(Routes.CARDS) },
                onOpenTag = { id -> pendingTagId = id; nav.navigate(Routes.CARDS) },
                onAllDecks = { nav.navigate(Routes.DECKS) },
                onAllTags = { nav.navigate(Routes.TAGS) },
                onStudyNormal = {
                    studyVm.startNormal()
                    nav.navigate(Routes.STUDY_SESSION)
                },
                onExam = { nav.navigate(Routes.EXAM_SETUP) },
                onCreateCard = { nav.navigate(Routes.cardEdit(0)) },
                onImport = { nav.navigate(Routes.IMPORT) },
                onCreateDeck = { nav.navigate(Routes.deckEdit(0)) },
            )
        }

        composable(Routes.CARDS) {
            val vm: CardsViewModel = viewModel(factory = CosmicViewModelFactory.Factory)
            // Apply any filter requested by the screen that navigated here.
            androidx.compose.runtime.LaunchedEffect(Unit) {
                pendingCardState?.let { vm.setState(it); pendingCardState = null }
                pendingDeckId?.let { vm.setDeck(it); pendingDeckId = null }
                pendingTagId?.let { vm.setTag(it); pendingTagId = null }
                if (pendingUnsorted) { vm.setDeck(null, true); pendingUnsorted = false }
            }
            CardsScreen(
                vm = vm,
                onCreate = { nav.navigate(Routes.cardEdit(0)) },
                onEdit = { id -> nav.navigate(Routes.cardEdit(id)) },
                onImport = { nav.navigate(Routes.IMPORT) },
            )
        }

        composable(
            route = "${Routes.CARD_EDIT}/{cardId}",
            arguments = listOf(navArgument("cardId") { type = NavType.LongType }),
        ) { entry ->
            val vm: CardEditViewModel = viewModel(factory = CosmicViewModelFactory.Factory)
            CardEditScreen(
                vm = vm,
                cardId = entry.arguments?.getLong("cardId") ?: 0L,
                onDone = { nav.popBackStack() },
            )
        }

        composable(Routes.DECKS) {
            val vm: DecksViewModel = viewModel(factory = CosmicViewModelFactory.Factory)
            DecksScreen(
                vm = vm,
                onCreate = { nav.navigate(Routes.deckEdit(0)) },
                onEdit = { id -> nav.navigate(Routes.deckEdit(id)) },
                onOpenCards = { id -> pendingDeckId = id; nav.navigate(Routes.CARDS) },
                onOpenUnsorted = { pendingUnsorted = true; nav.navigate(Routes.CARDS) },
                onStudyDeck = { id, name ->
                    studyVm.startNormal(deckId = id, label = name)
                    nav.navigate(Routes.STUDY_SESSION)
                },
            )
        }

        composable(
            route = "${Routes.DECK_EDIT}/{deckId}",
            arguments = listOf(navArgument("deckId") { type = NavType.LongType }),
        ) { entry ->
            val vm: DecksViewModel = viewModel(factory = CosmicViewModelFactory.Factory)
            DeckEditScreen(
                vm = vm,
                deckId = entry.arguments?.getLong("deckId") ?: 0L,
                onDone = { nav.popBackStack() },
            )
        }

        composable(Routes.TAGS) {
            val vm: TagsViewModel = viewModel(factory = CosmicViewModelFactory.Factory)
            TagsScreen(
                vm = vm,
                onCreate = { nav.navigate(Routes.tagEdit(0)) },
                onEdit = { id -> nav.navigate(Routes.tagEdit(id)) },
                onOpenCards = { id -> pendingTagId = id; nav.navigate(Routes.CARDS) },
                onStudyTag = { id, name ->
                    studyVm.startNormal(tagId = id, label = name)
                    nav.navigate(Routes.STUDY_SESSION)
                },
            )
        }

        composable(
            route = "${Routes.TAG_EDIT}/{tagId}",
            arguments = listOf(navArgument("tagId") { type = NavType.LongType }),
        ) { entry ->
            val vm: TagsViewModel = viewModel(factory = CosmicViewModelFactory.Factory)
            TagEditScreen(
                vm = vm,
                tagId = entry.arguments?.getLong("tagId") ?: 0L,
                onDone = { nav.popBackStack() },
            )
        }

        composable(Routes.IMPORT) {
            val vm: ImportViewModel = viewModel(factory = CosmicViewModelFactory.Factory)
            ImportScreen(
                vm = vm,
                onImported = { nav.navigate(Routes.CARDS) },
            )
        }

        composable(Routes.STUDY_HOME) {
            StudyHomeScreen(
                vm = studyVm,
                onStartNormal = {
                    studyVm.startNormal()
                    nav.navigate(Routes.STUDY_SESSION)
                },
                onExam = { nav.navigate(Routes.EXAM_SETUP) },
                onStartDeck = { id, name ->
                    studyVm.startNormal(deckId = id, label = name)
                    nav.navigate(Routes.STUDY_SESSION)
                },
                onCreateCard = { nav.navigate(Routes.cardEdit(0)) },
                onImport = { nav.navigate(Routes.IMPORT) },
            )
        }

        composable(Routes.EXAM_SETUP) {
            ExamSetupScreen(
                vm = studyVm,
                onStarted = {
                    nav.navigate(Routes.STUDY_SESSION) {
                        popUpTo(Routes.EXAM_SETUP) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.STUDY_SESSION) {
            StudySessionScreen(
                vm = studyVm,
                onFinished = {
                    nav.navigate(Routes.STUDY_DONE) {
                        popUpTo(Routes.STUDY_SESSION) { inclusive = true }
                    }
                },
                onExit = { studyVm.endSession() },
            )
        }

        composable(Routes.STUDY_DONE) {
            SessionDoneScreen(
                vm = studyVm,
                onKeepGoing = {
                    studyVm.startNormal()
                    nav.navigate(Routes.STUDY_SESSION) {
                        popUpTo(Routes.STUDY_DONE) { inclusive = true }
                    }
                },
                onHome = {
                    studyVm.reset()
                    nav.navigate(Routes.DASHBOARD) {
                        popUpTo(Routes.DASHBOARD) { inclusive = true }
                    }
                },
            )
        }
    }
}

// Filter hand-off between screens. Process-scoped and short-lived: set right
// before navigating to the card list, consumed once on arrival.
private var pendingCardState: Int? = null
private var pendingDeckId: Long? = null
private var pendingTagId: Long? = null
private var pendingUnsorted: Boolean = false
