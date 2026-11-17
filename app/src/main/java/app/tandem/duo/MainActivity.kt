package app.tandem.duo

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.runtime.collectAsState
import app.tandem.duo.data.remote.TandemSupabase
import app.tandem.duo.ui.TandemViewModel
import app.tandem.duo.ui.screens.MainTandemScreen
import app.tandem.duo.ui.screens.TandemAuthScreen
import app.tandem.duo.ui.theme.TandemTheme
import app.tandem.duo.travel.TravelSyncWorker
import io.github.jan.supabase.auth.handleDeeplinks

class MainActivity : ComponentActivity() {
    private lateinit var tandemViewModel: TandemViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        TandemSupabase.client.handleDeeplinks(intent)
        TravelSyncWorker.schedule(this)
        tandemViewModel = ViewModelProvider(this)[TandemViewModel::class.java]
        setContent {
            TandemTheme {
                val vm: TandemViewModel = viewModel()
                val user = vm.authUser.collectAsState()
                val checking = vm.authChecking.collectAsState()
                if (checking.value) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (user.value == null) {
                    TandemAuthScreen(viewModel = vm)
                } else {
                    MainTandemScreen(viewModel = vm)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        TandemSupabase.client.handleDeeplinks(intent)
        if (::tandemViewModel.isInitialized) tandemViewModel.refreshSignedInUser()
    }
}
