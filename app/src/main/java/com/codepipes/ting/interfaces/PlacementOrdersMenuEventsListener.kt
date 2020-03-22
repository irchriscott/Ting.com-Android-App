package com.codepipes.ting.interfaces

interface PlacementOrdersMenuEventsListener {

    public fun onReorder(order: Int, quantity: Int, conditions: String)
    public fun onNotify(order: Int)
    public fun onCancel(order: Int, position: Int)

}