package com.example.testfire.main

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeech.OnInitListener
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.opencsv.CSVReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStreamReader
import kotlin.math.*
import android.util.Log
import com.example.testfire.DestinationScreen
import com.example.testfire.FinalViewModel
import com.example.testfire.data.BusStop
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

@Composable
fun SelectBusStopScreen(navController: NavController, vm: FinalViewModel) {
    val context = LocalContext.current
    val fusedLocationClient: FusedLocationProviderClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    var currentLocation by remember { mutableStateOf<Location?>(null) }
    var isPermissionGranted by remember { mutableStateOf(false) }
    var searchTriggered by remember { mutableStateOf(false) }
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    val db = FirebaseFirestore.getInstance()
    val userId = vm.auth.currentUser?.uid ?: return

    // CSV 파일 읽기 및 Firestore에 추가
    val busStops = remember { mutableStateListOf<BusStop>() }
    LaunchedEffect(Unit) {
        tts = TextToSpeech(context, OnInitListener { status ->
            if (status == TextToSpeech.SUCCESS) {
                val guidanceText = "가까운 정류장 탐색 버튼이 화면 중앙에 있습니다."
                tts?.speak(guidanceText, TextToSpeech.QUEUE_FLUSH, null, null)
            }
        })
    }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            context.assets.open("bus_stops.csv").use { inputStream ->
                val reader = CSVReader(InputStreamReader(inputStream))
                val batch = db.batch()
                reader.readAll().forEach { line ->
                    val name = line[0]
                    val latitude = line[1].toDouble()
                    val longitude = line[2].toDouble()
                    busStops.add(BusStop(name, latitude, longitude))

                    // Firestore에 데이터 추가
                    val busStopRef = db.collection("bus_stops").document(name)
                    batch.set(busStopRef, BusStop(name, latitude, longitude))

                    // 디버깅용 로그 출력
                    Log.d("PassengerScreen", "Added bus stop: $name ($latitude, $longitude)")
                }
                batch.commit().addOnCompleteListener {
                    Log.d("PassengerScreen", "Bus stops successfully added to Firestore")
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        // 위치 권한 확인 및 요청
        when {
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED -> {
                // 권한이 허용된 경우 위치 가져오기
                isPermissionGranted = true
                fetchCurrentLocation(fusedLocationClient) { location ->
                    currentLocation = location
                }
            }
            else -> {
                // 권한 요청
                ActivityCompat.requestPermissions(
                    context as ComponentActivity,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    LOCATION_PERMISSION_REQUEST_CODE
                )
            }
        }
    }

    // 위치 권한 요청 결과 처리
    DisposableEffect(context) {
        val callback = ActivityCompat.OnRequestPermissionsResultCallback { _, permissions, grantResults ->
            if (permissions.isNotEmpty() && permissions[0] == Manifest.permission.ACCESS_FINE_LOCATION) {
                isPermissionGranted = grantResults[0] == PackageManager.PERMISSION_GRANTED
                if (isPermissionGranted) {
                    fetchCurrentLocation(fusedLocationClient) { location ->
                        currentLocation = location
                    }
                }
            }
        }

        onDispose {
            // DisposableEffect에서 리소스 해제
            tts?.stop()
            tts?.shutdown()
        }
    }

    // UI 구성
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (!searchTriggered) {
            Button(
                onClick = {
                    searchTriggered = true
                    // 디버깅용 로그 출력
                    busStops.forEach { busStop ->
                        Log.d("PassengerScreen", "Bus stop: ${busStop.name} (${busStop.latitude}, ${busStop.longitude})")
                    }
                    // 가까운 정류장 음성 출력
                    currentLocation?.let { location ->
                        val sortedBusStops = busStops.sortedBy { busStop ->
                            haversine(location.latitude, location.longitude, busStop.latitude, busStop.longitude)
                        }.take(3)
                        val busStopNames = sortedBusStops.joinToString(", ") { it.name }
                        val ttsText = "현재 가까운 정류장순서는 $busStopNames 정류장입니다. 안내되는 정류장 순서를 들으시고 순서에 맞게 상단, 중단, 하단을 터치해주세요"
                        tts?.speak(ttsText, TextToSpeech.QUEUE_FLUSH, null, null)
                    }
                },
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF388E3C)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp) // 화면의 1/3 크기
            ) {
                Text(text = "가까운 정류장 탐색", fontSize = 28.sp, fontWeight = FontWeight.Bold)
            }
        } else {
            Text(
                text = "현재 위치 기반 버스 정류장",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(modifier = Modifier.height(16.dp))
            if (isPermissionGranted) {
                currentLocation?.let { location ->
                    Text(
                        text = "현재 위치: (${location.latitude}, ${location.longitude})",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    // 가까운 버스 정류장 계산 및 표시
                    val sortedBusStops = busStops.sortedBy { busStop ->
                        haversine(location.latitude, location.longitude, busStop.latitude, busStop.longitude)
                    }.take(3)
                    sortedBusStops.forEach { busStop ->
                        Button(
                            onClick = {
                                val userBusStopData = hashMapOf("busStop" to busStop.name)
                                db.collection("users").document(userId).set(userBusStopData, SetOptions.merge())
                                navController.navigate(DestinationScreen.Passenger.route)
                                // 선택된 정류장 음성 출력
                                val ttsText = "${busStop.name}정류장을 선택하셨습니다."
                                tts?.speak(ttsText, TextToSpeech.QUEUE_FLUSH, null, null)
                            },
                            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF388E3C)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp)
                                .weight(1f) // 버튼이 화면을 채우도록 설정
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = busStop.name,
                                    fontSize = 28.sp, // 정류장 이름 크게 설정
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "(${busStop.latitude}, ${busStop.longitude})",
                                    fontSize = 18.sp
                                )
                            }
                        }
                    }
                } ?: run {
                    Text(
                        text = "위치 정보를 가져오는 중입니다...",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            } else {
                Text(
                    text = "위치 권한이 필요합니다.",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}

private const val LOCATION_PERMISSION_REQUEST_CODE = 100

private fun fetchCurrentLocation(
    fusedLocationClient: FusedLocationProviderClient,
    callback: (Location?) -> Unit
) {
    fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
        callback(location)
    }
}

private fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val R = 6372.8 // 지구의 반지름 (단위: km)
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2).pow(2.0) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2.0)
    val c = 2 * asin(sqrt(a))
    return R * c
}
