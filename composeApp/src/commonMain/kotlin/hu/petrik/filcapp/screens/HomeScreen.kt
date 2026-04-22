package hu.petrik.filcapp.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import hu.petrik.filcapp.api.APIResult
import hu.petrik.filcapp.api.ClassroomApi
import hu.petrik.filcapp.api.CohortApi
import hu.petrik.filcapp.api.LessonApi
import hu.petrik.filcapp.api.MovedLessonApi
import hu.petrik.filcapp.api.NewsAnnouncementsApi
import hu.petrik.filcapp.api.NewsSystemMessagesApi
import hu.petrik.filcapp.api.SubstitutionApi
import hu.petrik.filcapp.api.TeacherApi
import hu.petrik.filcapp.api.client.APIClient
import hu.petrik.filcapp.components.DateView
import hu.petrik.filcapp.components.NoticesBanner
import hu.petrik.filcapp.components.NoticeState
import hu.petrik.filcapp.components.UpcomingClasses
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeScreenModel(
    private val announcementsApi: NewsAnnouncementsApi,
    private val systemMessagesApi: NewsSystemMessagesApi,
) : ScreenModel {
    fun loadNotices() {
        if (NoticeState.loaded) return
        screenModelScope.launch(Dispatchers.Default) {
            val annResult = announcementsApi.getNewsAnnouncements()
            val sysResult = systemMessagesApi.getNewsSystemMessages()
            withContext(Dispatchers.Main) {
                if (annResult is APIResult.Success) NoticeState.announcements = annResult.data
                if (sysResult is APIResult.Success) NoticeState.systemMessages = sysResult.data
                NoticeState.loaded = true
            }
        }
    }
}

@Composable
fun HomeScreen() {
    val cohortId = TimetableState.selectedCohortId

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(top = 16.dp),
    ) {
        DateView()
        Spacer(Modifier.height(16.dp))
        NoticesBanner(cohortId = cohortId)
        UpcomingClasses()
    }
}

object HomeTab : Tab {
    override val options: TabOptions
        @Composable
        get() {
            val title = "Home"
            val icon = rememberVectorPainter(Icons.Default.Home)
            return remember {
                TabOptions(
                    index = 0u,
                    title = title,
                    icon = icon,
                )
            }
        }

    @Composable
    override fun Content() {
        val timetableModel = rememberScreenModel {
            TimetableScreenModel(
                LessonApi(APIClient),
                CohortApi(APIClient),
                TeacherApi(APIClient),
                SubstitutionApi(APIClient),
                MovedLessonApi(APIClient),
                ClassroomApi(APIClient),
            )
        }
        val homeModel = rememberScreenModel {
            HomeScreenModel(
                NewsAnnouncementsApi(APIClient),
                NewsSystemMessagesApi(APIClient),
            )
        }
        LaunchedEffect(Unit) {
            timetableModel.loadCohorts()
            homeModel.loadNotices()
        }
        LaunchedEffect(TimetableState.selectedCohortId) {
            val id = TimetableState.selectedCohortId ?: return@LaunchedEffect
            timetableModel.loadTimetable(id)
        }
        HomeScreen()
    }
}
