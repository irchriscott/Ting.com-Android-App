package com.codepipes.ting.utils

class Routes {

    private val END_POINT: String = "http://10.0.2.2:8000/api/v1/"

    //SIGN UP & AUTH ROUTES
    val checkEmailUsername: String = "${END_POINT}usr/check/email-username/"
    val submitEmailSignUp: String = "${END_POINT}usr/signup/email/"
    val submitGoogleSignUp: String = "${END_POINT}usr/signup/google/"
    val authLoginUser: String = "${END_POINT}usr/auth/login/"
    val authResetPassword = "${END_POINT}usr/auth/password/reset/"

}