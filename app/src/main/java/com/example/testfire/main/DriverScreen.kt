package com.example.testfire

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.example.finalproject.R
import com.example.testfire.data.BusStop
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.delay
import kotlin.math.*

private const val TAG = "DriverScreen"

@Composable
fun DriverScreen(navController: NavController, vm: FinalViewModel) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    var hasStopRequest by remember { mutableStateOf(false) }
    var hasBlindRequest by remember { mutableStateOf(false) }
    var hasDeafRequest by remember { mutableStateOf(false) }
    var hasElderlyRequest by remember { mutableStateOf(false) }
    var hasWheelchairRequest by remember { mutableStateOf(false) }
    var isBus823 by remember { mutableStateOf(false) }
    var isBus851 by remember { mutableStateOf(false) }
    var isBus201 by remember { mutableStateOf(false) }
    var closestBusStop by remember { mutableStateOf<String?>(null) }
    val fusedLocationClient: FusedLocationProviderClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    var currentLocation by remember { mutableStateOf<Location?>(null) }
    var listenerRegistration: ListenerRegistration? by remember { mutableStateOf<ListenerRegistration?>(null) }
    var isPermissionGranted by remember { mutableStateOf(false) }
    var busStops by remember { mutableStateOf<List<BusStop>>(emptyList()) }

    LaunchedEffect(Unit) {
        // 위치 권한 확인
        when {
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED -> {
                // 권한이 허용된 경우 위치를 가져옴
                isPermissionGranted = true
                fetchCurrentLocation(fusedLocationClient) { location ->
                    currentLocation = location
                    Log.d(TAG, "Current location: $location")
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

    DisposableEffect(context) {
        val callback = ActivityCompat.OnRequestPermissionsResultCallback { _, permissions, grantResults ->
            if (permissions.isNotEmpty() && permissions[0] == Manifest.permission.ACCESS_FINE_LOCATION) {
                isPermissionGranted = grantResults[0] == PackageManager.PERMISSION_GRANTED
                if (isPermissionGranted) {
                    fetchCurrentLocation(fusedLocationClient) { location ->
                        currentLocation = location
                        Log.d(TAG, "Current location after permission granted: $location")
                    }
                }
            }
        }

        onDispose {
            listenerRegistration?.remove()
        }
    }

    LaunchedEffect(currentLocation) {
        while (true) {
            fetchCurrentLocation(fusedLocationClient) { location ->
                currentLocation = location
                Log.d(TAG, "Updated location: $location")

                currentLocation?.let { loc ->
                    db.collection("bus_stops").get()
                        .addOnSuccessListener { snapshots ->
                            busStops = snapshots.documents.mapNotNull { it.toObject(BusStop::class.java) }
                            val closest = vm.getNearestStop(loc, busStops)
                            closestBusStop = closest
                            Log.d(TAG, "Closest bus stop: $closestBusStop")

                            // 토스트 메시지로 새로고침 알림
                            Toast.makeText(context, "새로고침 되었습니다.", Toast.LENGTH_SHORT).show()

                            closest?.let { busStop ->
                                listenerRegistration?.remove()
                                listenerRegistration = db.collection("rides").document(busStop)
                                    .addSnapshotListener { snapshot, e ->
                                        if (e != null || snapshot == null || !snapshot.exists()) {
                                            hasStopRequest = false
                                            hasBlindRequest = false
                                            hasDeafRequest = false
                                            hasElderlyRequest = false
                                            hasWheelchairRequest = false
                                            isBus823 = false
                                            isBus851 = false
                                            isBus201 = false
                                            Log.d(TAG, "Snapshot error or not exists: $e")
                                        } else {
                                            hasStopRequest = snapshot.getString("status") == "하차합니다"
                                            hasBlindRequest = snapshot.getBoolean("blind") == true
                                            hasDeafRequest = snapshot.getBoolean("deaf") == true
                                            hasElderlyRequest = snapshot.getBoolean("elderly") == true
                                            hasWheelchairRequest = snapshot.getBoolean("wheelchair") == true
                                            isBus823 = snapshot.getBoolean("bus823") == true
                                            isBus851 = snapshot.getBoolean("bus851") == true
                                            isBus201 = snapshot.getBoolean("bus201") == true
                                            Log.d(TAG, "Snapshot data: status=$hasStopRequest, blind=$hasBlindRequest, deaf=$hasDeafRequest, elderly=$hasElderlyRequest, wheelchair=$hasWheelchairRequest, bus823=$isBus823, bus851=$isBus851, bus201=$isBus201")
                                        }
                                    }
                            }
                        }
                }
            }
            delay(5000) // 5초마다 갱신
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        closestBusStop?.let {
            Text(
                text = "이번 정류장: $it",
                fontSize = 20.sp
            )
        } ?: run {
            Text(
                text = "가장 가까운 정류장을 찾는 중...",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        CommonDivider()

        // 버스 번호 버튼 Row (상단)
        Row(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val busNumbers = listOf(
                Triple("bus823", isBus823, R.drawable.a823_on to R.drawable.a823_off),
                Triple("bus851", isBus851, R.drawable.a851_on to R.drawable.a851_off),
                Triple("bus201", isBus201, R.drawable.a201_on to R.drawable.a201_off)
            )
            busNumbers.forEach { (busField, isBusSelected, images) ->
                Image(
                    painter = painterResource(if (isBusSelected) images.first else images.second),
                    contentDescription = busField,
                    modifier = Modifier
                        .size(120.dp) // 이미지 크기 증가
                        .padding(horizontal = 16.dp) // 좌우 간격을 넓힘
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        CommonDivider()

        // Stop 버튼 (중단)
        Image(
            painter = painterResource(if (hasStopRequest) R.drawable.stop else R.drawable.nonstop),
            contentDescription = null,
            modifier = Modifier
                .size(150.dp) // Stop 버튼 크기 증가
                .clickable {
                    if (hasStopRequest) {
                        closestBusStop?.let { busStop ->
                            db.collection("rides").document(busStop).delete()
                        }
                    } else {
                        closestBusStop?.let { busStop ->
                            val rideData = hashMapOf("status" to "하차합니다")
                            db.collection("rides").document(busStop).set(rideData, SetOptions.merge())
                        }
                    }
                    hasStopRequest = !hasStopRequest
                }
        )

        Spacer(modifier = Modifier.height(16.dp))

        CommonDivider()

        // 불편 정보 이미지 2줄로 배치 (하단)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(if (hasBlindRequest) R.drawable.eye_on else R.drawable.eye_off),
                    contentDescription = null,
                    modifier = Modifier.size(100.dp)
                )
                Image(
                    painter = painterResource(if (hasDeafRequest) R.drawable.ear_on else R.drawable.ear_off),
                    contentDescription = null,
                    modifier = Modifier.size(100.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(if (hasElderlyRequest) R.drawable.elderly_on else R.drawable.elderly_off),
                    contentDescription = null,
                    modifier = Modifier.size(100.dp)
                )
                Image(
                    painter = painterResource(if (hasWheelchairRequest) R.drawable.wheel_on else R.drawable.wheel_off),
                    contentDescription = null,
                    modifier = Modifier.size(100.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        CommonDivider()

        // 초기화 버튼
        Button(
            onClick = {
                closestBusStop?.let { busStop ->
                    val rideData = hashMapOf(
                        "status" to "비활성화",
                        "blind" to false,
                        "deaf" to false,
                        "elderly" to false,
                        "wheelchair" to false,
                        "bus823" to false,
                        "bus851" to false,
                        "bus201" to false
                    )
                    db.collection("rides").document(busStop).set(rideData, SetOptions.merge())
                    hasStopRequest = false
                    hasBlindRequest = false
                    hasDeafRequest = false
                    hasElderlyRequest = false
                    hasWheelchairRequest = false
                    isBus823 = false
                    isBus851 = false
                    isBus201 = false
                }
            },
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(0.9f)
                .height(60.dp),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = Color(0xFF388E3C), // 초록색
                contentColor = Color.White
            )
        ) {
            Text(text = "불편정보 확인")
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
