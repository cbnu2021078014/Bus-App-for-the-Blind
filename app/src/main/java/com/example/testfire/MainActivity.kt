package com.example.testfire

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.finalproject.auth.LoginScreen
import com.example.finalproject.auth.SignupScreen
import com.example.testfire.main.SelectBusStopScreen
import com.example.testfire.ui.theme.TestFireTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TestFireTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),

                    ) {
                    FinalProjectApp()
                }
            }
        }
    }
}

sealed class DestinationScreen(val route: String) {
    object Signup : DestinationScreen("signup")
    object Login : DestinationScreen("login")
    object Passenger : DestinationScreen("passenger")
    object Driver : DestinationScreen("driver")
    object SelectBusStop : DestinationScreen("select_bus_stop")
}

@Composable
fun FinalProjectApp() {
    val vm = hiltViewModel<FinalViewModel>()
    val navController = rememberNavController()
    vm.navController = navController

    NotificationMessage(vm = vm)

    //여기 수정 처음에 어디로 화면 보여줄지
    NavHost(navController = navController, startDestination = DestinationScreen.Login.route) {
        composable(DestinationScreen.Signup.route) {
            SignupScreen(navController = navController, vm = vm)
        }
        composable(DestinationScreen.Login.route) {
            LoginScreen(navController = navController, vm = vm)
        }
        composable(DestinationScreen.Passenger.route) {
            PassengerScreen(navController = navController, vm = vm)
        }
        composable(DestinationScreen.Driver.route) {
            DriverScreen(navController = navController, vm = vm)
        }
        composable(DestinationScreen.SelectBusStop.route) {
            SelectBusStopScreen(navController = navController, vm = vm)
        }
    }
}
