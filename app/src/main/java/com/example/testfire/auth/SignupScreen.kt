package com.example.finalproject.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.finalproject.R
import com.example.testfire.CheckSignedIn
import com.example.testfire.CommonProgressSpinner
import com.example.testfire.DestinationScreen
import com.example.testfire.FinalViewModel
import com.example.testfire.navigateTo

@Composable
fun SignupScreen(navController: NavController, vm: FinalViewModel) {
    CheckSignedIn(vm = vm, navController = navController)

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val usernameState = remember { mutableStateOf(TextFieldValue()) }
            val emailState = remember { mutableStateOf(TextFieldValue()) }
            val passState = remember { mutableStateOf(TextFieldValue()) }
            val isPassenger = remember { mutableStateOf(true) }

            // 로고 이미지
            Image(
                painter = painterResource(id = R.drawable.cbnu_logo),
                contentDescription = null,
                modifier = Modifier
                    .width(300.dp)
                    .padding(top = 16.dp)
                    .padding(16.dp)
            )
            Text(
                text = "회원가입",
                modifier = Modifier.padding(16.dp),
                fontSize = 30.sp,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.ExtraBold,
            )
            OutlinedTextField(
                value = usernameState.value,
                onValueChange = { usernameState.value = it },
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxWidth(0.8f),
                label = { Text(text = "사용자 이름") },
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = Color(0xFF388E3C), // 초록색
                    unfocusedBorderColor = Color(0xFF388E3C)
                )
            )
            OutlinedTextField(
                value = emailState.value,
                onValueChange = { emailState.value = it },
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxWidth(0.8f),
                label = { Text(text = "이메일") },
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = Color(0xFF388E3C), // 초록색
                    unfocusedBorderColor = Color(0xFF388E3C)
                )
            )
            OutlinedTextField(
                value = passState.value,
                onValueChange = { passState.value = it },
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxWidth(0.8f),
                label = { Text(text = "비밀번호") },
                visualTransformation = PasswordVisualTransformation(),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = Color(0xFF388E3C), // 초록색
                    unfocusedBorderColor = Color(0xFF388E3C)
                )
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(8.dp)
            ) {
                RadioButton(
                    selected = isPassenger.value,
                    onClick = { isPassenger.value = true },
                    colors = RadioButtonDefaults.colors(
                        selectedColor = Color(0xFF388E3C), // 초록색
                        unselectedColor = Color(0xFF388E3C)
                    )
                )
                Text(text = "승객", modifier = Modifier.padding(end = 16.dp))
                RadioButton(
                    selected = !isPassenger.value,
                    onClick = { isPassenger.value = false },
                    colors = RadioButtonDefaults.colors(
                        selectedColor = Color(0xFF388E3C), // 초록색
                        unselectedColor = Color(0xFF388E3C)
                    )
                )
                Text(text = "기사")
            }

            Button(
                onClick = {
                    vm.onSignup(
                        usernameState.value.text,
                        emailState.value.text,
                        passState.value.text,
                        isPassenger = isPassenger.value
                    )
                },
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxWidth(0.8f),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color(0xFF388E3C), // 초록색
                    contentColor = Color.White
                )
            ) {
                Text(text = "회원가입")
            }
            Text(
                text = "이미 가입하셨나요? 로그인하러가기",
                color = Color(0xFF388E3C), // 초록색
                modifier = Modifier
                    .padding(16.dp)
                    .clickable {
                        navigateTo(navController, DestinationScreen.Login)
                    }
            )
        }

        // 버퍼링 구현
        val isLoading = vm.inProgress.value
        if (isLoading) {
            CommonProgressSpinner()
        }
    }
}
