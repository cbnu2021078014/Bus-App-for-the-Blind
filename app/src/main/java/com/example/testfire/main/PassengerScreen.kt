package com.example.testfire

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.finalproject.R
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeech.OnInitListener

@Composable
fun PassengerScreen(navController: NavController, vm: FinalViewModel) {
    val db = FirebaseFirestore.getInstance()
    val userId = vm.auth.currentUser?.uid ?: return
    var isStopping by remember { mutableStateOf(false) }
    var isBlind by remember { mutableStateOf(false) }
    var isDeaf by remember { mutableStateOf(false) }
    var isElderly by remember { mutableStateOf(false) }
    var isWheelchair by remember { mutableStateOf(false) }
    var isBus823 by remember { mutableStateOf(false) }
    var isBus851 by remember { mutableStateOf(false) }
    var isBus201 by remember { mutableStateOf(false) }
    var selectedBusStop by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }

    // Initialize TextToSpeech
    LaunchedEffect(Unit) {
        tts = TextToSpeech(context, OnInitListener { status ->
            if (status == TextToSpeech.SUCCESS) {
                val guidanceText = """
                    화면 안내를 시작합니다.
                    상단에 버스 번호 버튼이 있습니다.
                    왼쪽부터 823번, 851번, 201번 버스 버튼이 있습니다.
                    중앙에 하차 버튼이 있습니다.
                    하단에 장애인 모드 버튼이 있습니다.
                    왼쪽부터 시각 장애인, 청각 장애인, 노인, 휠체어 모드 버튼이 있습니다.
                """.trimIndent()
                tts?.speak(guidanceText, TextToSpeech.QUEUE_FLUSH, null, null)
            }
        })
    }

    // Get selected bus stop from Firestore
    LaunchedEffect(Unit) {
        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    selectedBusStop = document.getString("busStop")
                }
            }
    }

    // Check current state from Firestore
    LaunchedEffect(selectedBusStop) {
        selectedBusStop?.let { busStop ->
            db.collection("rides").document(busStop)
                .addSnapshotListener { snapshot, e ->
                    if (e != null || snapshot == null || !snapshot.exists()) {
                        isStopping = false
                        isBlind = false
                        isDeaf = false
                        isElderly = false
                        isWheelchair = false
                        isBus823 = false
                        isBus851 = false
                        isBus201 = false
                    } else {
                        isStopping = snapshot.getString("status") == "하차합니다"
                        isBlind = snapshot.getBoolean("blind") ?: false
                        isDeaf = snapshot.getBoolean("deaf") ?: false
                        isElderly = snapshot.getBoolean("elderly") ?: false
                        isWheelchair = snapshot.getBoolean("wheelchair") ?: false
                        isBus823 = snapshot.getBoolean("bus823") ?: false
                        isBus851 = snapshot.getBoolean("bus851") ?: false
                        isBus201 = snapshot.getBoolean("bus201") ?: false
                    }
                }
        }
    }

    // Clean up TextToSpeech resources
    DisposableEffect(Unit) {
        onDispose {
            tts?.stop()
            tts?.shutdown()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "승객 화면",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))

        selectedBusStop?.let {
            Text(
                text = it,
                fontSize = 36.sp,
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
                        .clickable {
                            selectedBusStop?.let { busStop ->
                                val newValue = !isBusSelected
                                val rideData = hashMapOf(busField to newValue)
                                db.collection("rides").document(busStop).set(rideData, SetOptions.merge())
                                tts?.speak("${busField.removePrefix("bus")} 번 버스가 ${if (newValue) "활성화" else "비활성화"} 되었습니다.", TextToSpeech.QUEUE_FLUSH, null, null)
                                when (busField) {
                                    "bus823" -> isBus823 = newValue
                                    "bus851" -> isBus851 = newValue
                                    "bus201" -> isBus201 = newValue
                                }
                            }
                        }
                        .padding(horizontal = 16.dp) // 좌우 간격을 넓힘
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        CommonDivider()

        // Stop 버튼 (중단)
        Image(
            painter = painterResource(if (isStopping) R.drawable.stop else R.drawable.nonstop),
            contentDescription = null,
            modifier = Modifier
                .size(150.dp) // Stop 버튼 크기 증가
                .clickable {
                    selectedBusStop?.let { busStop ->
                        if (isStopping) {
                            db.collection("rides").document(busStop).update("status", "비활성화")
                            tts?.speak("하차가 비활성화 되었습니다.", TextToSpeech.QUEUE_FLUSH, null, null)
                        } else {
                            db.collection("rides").document(busStop).update("status", "하차합니다")
                            tts?.speak("하차가 활성화 되었습니다.", TextToSpeech.QUEUE_FLUSH, null, null)
                        }
                        isStopping = !isStopping
                    }
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
                    painter = painterResource(if (isBlind) R.drawable.eye_on else R.drawable.eye_off),
                    contentDescription = null,
                    modifier = Modifier
                        .size(100.dp)
                        .clickable {
                            selectedBusStop?.let { busStop ->
                                val rideData = hashMapOf("blind" to !isBlind)
                                db.collection("rides").document(busStop).set(rideData, SetOptions.merge())
                                if (isBlind) {
                                    tts?.speak("시각장애인 모드가 비활성화 되었습니다.", TextToSpeech.QUEUE_FLUSH, null, null)
                                } else {
                                    tts?.speak("시각장애인 모드가 활성화 되었습니다.", TextToSpeech.QUEUE_FLUSH, null, null)
                                }
                            }
                            isBlind = !isBlind
                        }
                )
                Image(
                    painter = painterResource(if (isDeaf) R.drawable.ear_on else R.drawable.ear_off),
                    contentDescription = null,
                    modifier = Modifier
                        .size(100.dp)
                        .clickable {
                            selectedBusStop?.let { busStop ->
                                val rideData = hashMapOf("deaf" to !isDeaf)
                                db.collection("rides").document(busStop).set(rideData, SetOptions.merge())
                                if (isDeaf) {
                                    tts?.speak("청각장애인 모드가 비활성화 되었습니다.", TextToSpeech.QUEUE_FLUSH, null, null)
                                } else {
                                    tts?.speak("청각장애인 모드가 활성화 되었습니다.", TextToSpeech.QUEUE_FLUSH, null, null)
                                }
                            }
                            isDeaf = !isDeaf
                        }
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(if (isElderly) R.drawable.elderly_on else R.drawable.elderly_off),
                    contentDescription = null,
                    modifier = Modifier
                        .size(100.dp)
                        .clickable {
                            selectedBusStop?.let { busStop ->
                                val rideData = hashMapOf("elderly" to !isElderly)
                                db.collection("rides").document(busStop).set(rideData, SetOptions.merge())
                                if (isElderly) {
                                    tts?.speak("노인 모드가 비활성화 되었습니다.", TextToSpeech.QUEUE_FLUSH, null, null)
                                } else {
                                    tts?.speak("노인 모드가 활성화 되었습니다.", TextToSpeech.QUEUE_FLUSH, null, null)
                                }
                            }
                            isElderly = !isElderly
                        }
                )
                Image(
                    painter = painterResource(if (isWheelchair) R.drawable.wheel_on else R.drawable.wheel_off),
                    contentDescription = null,
                    modifier = Modifier
                        .size(100.dp)
                        .clickable {
                            selectedBusStop?.let { busStop ->
                                val rideData = hashMapOf("wheelchair" to !isWheelchair)
                                db.collection("rides").document(busStop).set(rideData, SetOptions.merge())
                                if (isWheelchair) {
                                    tts?.speak("휠체어 모드가 비활성화 되었습니다.", TextToSpeech.QUEUE_FLUSH, null, null)
                                } else {
                                    tts?.speak("휠체어 모드가 활성화 되었습니다.", TextToSpeech.QUEUE_FLUSH, null, null)
                                }
                            }
                            isWheelchair = !isWheelchair
                        }
                )
            }
        }
    }
}


