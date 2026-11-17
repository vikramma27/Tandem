package app.tandem.duo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import app.tandem.duo.ui.TandemViewModel
import app.tandem.duo.ui.screens.MainTandemScreen
import app.tandem.duo.ui.theme.TandemTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TandemTheme {
                val vm: TandemViewModel = viewModel()
                MainTandemScreen(viewModel = vm)
            }
        }
    }
}
