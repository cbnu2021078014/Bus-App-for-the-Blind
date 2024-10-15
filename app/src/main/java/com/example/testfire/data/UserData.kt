package com.example.finalproject.data

data class UserData (
    var userId: String? = null,
    var name: String? = null,
    var username: String? = null,
    var passenger: Boolean? = null,
    var driver: Boolean? = null
) {
    fun toMap() = mapOf(
        "userId" to userId,
        "name" to name,
        "username" to username,
        "passenger" to passenger,
        "driver" to driver
    )
}