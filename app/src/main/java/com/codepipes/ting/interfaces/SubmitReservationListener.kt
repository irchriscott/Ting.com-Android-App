package com.codepipes.ting.interfaces

interface SubmitReservationListener {
    public fun onSubmitReservation(people: String, date: String, time: String, table: Int)
}