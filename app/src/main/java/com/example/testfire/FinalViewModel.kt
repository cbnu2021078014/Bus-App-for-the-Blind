package com.example.testfire

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.finalproject.data.Event
import com.example.finalproject.data.UserData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.navigation.NavController
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import android.location.Location
import com.example.testfire.data.BusStop

const val USERS = "users"
const val RIDES = "rides"

@HiltViewModel
class FinalViewModel @Inject constructor(
    val auth: FirebaseAuth,
    val db: FirebaseFirestore,
    val storage: FirebaseStorage
) : ViewModel() {

    val signedIn = mutableStateOf(false)
    val inProgress = mutableStateOf(false)
    val userData = mutableStateOf<UserData?>(null)
    val popupNotification = mutableStateOf<Event<String>?>(null)
    var navController: NavController? = null

    init {
        onLogout()
        val currentUser = auth.currentUser
        signedIn.value = currentUser != null
        currentUser?.uid?.let { uid ->
            getUserData(uid)
        }
    }

    fun onSignup(username: String, email: String, pass: String, isPassenger: Boolean) {
        if (username.isEmpty() or email.isEmpty() or pass.isEmpty()) {
            handleException(customMessage = "모든 칸을 채워주세요")
            return
        }
        inProgress.value = true

        db.collection(USERS).whereEqualTo("username", username).get()
            .addOnSuccessListener { documents ->
                if (documents.size() > 0) {
                    handleException(customMessage = "사용자 이름이 이미 존재합니다.")
                    inProgress.value = false
                } else {
                    auth.createUserWithEmailAndPassword(email, pass)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                signedIn.value = true
                                auth.currentUser?.uid?.let { uid ->
                                    createOrUpdateProfile(uid, username, isPassenger)
                                }
                            } else {
                                handleException(task.exception, "회원가입 실패")
                            }
                            inProgress.value = false
                        }
                }
            }
            .addOnFailureListener { exc ->
                handleException(exc, "회원가입 실패")
                inProgress.value = false
            }
    }

    private fun createOrUpdateProfile(uid: String, username: String, passenger: Boolean) {
        val userData = UserData(
            userId = uid,
            username = username,
            passenger = passenger,
            driver = !passenger
        )

        inProgress.value = true
        db.collection(USERS).document(uid).get()
            .addOnSuccessListener {
                if (it.exists()) {
                    it.reference.update(userData.toMap())
                        .addOnSuccessListener {
                            this.userData.value = userData
                            navigateToCorrectScreen(userData)
                            inProgress.value = false
                        }
                        .addOnFailureListener {
                            handleException(it, "사용자를 업데이트할 수 없음")
                            inProgress.value = false
                        }
                } else {
                    db.collection(USERS).document(uid).set(userData)
                        .addOnSuccessListener {
                            getUserData(uid)
                            navigateToCorrectScreen(userData)
                            inProgress.value = false
                        }
                        .addOnFailureListener { exc ->
                            handleException(exc, "사용자를 생성할 수 없음")
                            inProgress.value = false
                        }
                }
            }
            .addOnFailureListener { exc ->
                handleException(exc, "사용자 정보를 가져올 수 없음")
                inProgress.value = false
            }
    }

    private fun navigateToCorrectScreen(userData: UserData) {
        if (userData.passenger == true) {
            navigateToSelectBusStopScreen()
        } else {
            navigateToDriverScreen()
        }
    }

    fun onLogin(email: String, pass: String) {
        if (email.isEmpty() or pass.isEmpty()) {
            handleException(customMessage = "모든 칸을 채워주세요")
            return
        }
        inProgress.value = true
        auth.signInWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    signedIn.value = true
                    auth.currentUser?.uid?.let { uid ->
                        handleException(customMessage = "로그인 성공")
                        getUserData(uid, onLogin = true)
                    }
                } else {
                    handleException(task.exception, "로그인 실패")
                    inProgress.value = false
                }
            }
            .addOnFailureListener { exc ->
                handleException(exc, "로그인 실패")
                inProgress.value = false
            }
    }

    private fun getUserData(uid: String, onLogin: Boolean = false) {
        inProgress.value = true
        db.collection(USERS).document(uid).get()
            .addOnSuccessListener {
                val user = it.toObject<UserData>()
                userData.value = user
                inProgress.value = false
                if (onLogin) {
                    navigateToCorrectScreen(user!!)
                }
            }
            .addOnFailureListener { exc ->
                handleException(exc, "사용자 정보를 불러올 수 없습니다.")
                inProgress.value = false
            }
    }

    fun handleException(exception: Exception? = null, customMessage: String = "") {
        exception?.printStackTrace()
        val errorMsg = exception?.localizedMessage ?: ""
        val message = if (customMessage.isEmpty()) errorMsg else "$customMessage: $errorMsg"
        popupNotification.value = Event(message)
    }

    fun onLogout() {
        auth.signOut()
        signedIn.value = false
        userData.value = null
        popupNotification.value = Event("로그아웃")
    }

    private fun navigateToPassengerScreen() {
        navController?.let {
            navigateTo(it, DestinationScreen.Passenger)
        }
    }

    private fun navigateToSelectBusStopScreen() {
        navController?.let {
            navigateTo(it, DestinationScreen.SelectBusStop)
        }
    }

    private fun navigateToDriverScreen() {
        navController?.let {
            navigateTo(it, DestinationScreen.Driver)
        }
    }


    fun resetDiscomfortInfoForNearestStop(currentLocation: Location?, busStops: List<BusStop>) {
        viewModelScope.launch {

            val nearestStop = getNearestStop(currentLocation, busStops)
            if (nearestStop != null) {
                val rideRef = db.collection("rides").document(nearestStop)
                rideRef.update(
                    mapOf(
                        "status" to null,
                        "blind" to false,
                        "deaf" to false,
                        "elderly" to false,
                        "wheelchair" to false
                    )
                ).addOnSuccessListener {
                    popupNotification.value = Event("불편 정보가 초기화되었습니다.")
                }.addOnFailureListener { e ->
                    popupNotification.value = Event("불편 정보 초기화에 실패했습니다: ${e.localizedMessage}")
                }
            }
        }
    }


    fun getNearestStop(currentLocation: Location?, busStops: List<BusStop>): String? {
        if (currentLocation == null) return null

        return busStops.minByOrNull { busStop ->
            haversine(currentLocation.latitude, currentLocation.longitude, busStop.latitude, busStop.longitude)
        }?.name
    }

    private fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6372.8 // 지구의 반지름 (단위: km)
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2.0) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2.0)
        val c = 2 * asin(sqrt(a))
        return R * c
    }



}
