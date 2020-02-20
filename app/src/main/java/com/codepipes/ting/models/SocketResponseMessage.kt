package com.codepipes.ting.models

class SocketResponseMessage (
    var uuid: String,
    var type: String,
    var sender: SocketUser?,
    var receiver: SocketUser?,
    var status: Int,
    var message: String?,
    var args: Map<String, String?>?,
    var data: Map<String, String?>?
) {}

class SocketUser (
    var id: Int?,
    var type: Int?, //1 => branch, 2 => waiter, 3 => user
    var name: String?,
    var email: String?,
    var image: String?,
    var channel: String?
){}