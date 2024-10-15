package com.example.finalproject.data

// mutableState의 경우에는 리컴포지션될때마다 값을 다시 띄워주는 자료형이기 때문에 에러 메시지를 단 한번만 띄워주기 위해서는
// 새로운 자료형을 정의해 줘야할 필요가 있고, 아래와 같이 이미 예외를 처리한 상태라면 리컴포지션이 되도 아무런 동작을 수행하지 않도록 하고, 그렇지 않은 상태에서만
// content를 전달해 줄 수 있도록 설계

open class Event<out T>(private val content: T) {

    var hasBeenHandled = false
        private set

    fun getContentOrNull(): T? {
        return if (hasBeenHandled) {
            null
        } else {
            hasBeenHandled = true
            content
        }
    }
}