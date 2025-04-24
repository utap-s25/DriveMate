package edu.cs371m.fcgooglemaps.model

data class Vehicle(
    val make: String = "",
    val model: String = "",
    val year: Int = 0,
    val vin: String = ""
)

data class UserProfile(
    val uid: String = "",
    val username: String = "",
    val bio: String = "",
    val profilePicUrl: String = "",
    val vehicle: Vehicle = Vehicle()
)