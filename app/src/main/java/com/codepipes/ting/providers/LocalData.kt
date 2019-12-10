package com.codepipes.ting.providers

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import com.codepipes.ting.models.Branch
import com.codepipes.ting.models.MenuPromotion
import com.codepipes.ting.models.RestaurantMenu
import com.codepipes.ting.models.User
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class LocalData (
    contxt: Context
) {

    private val context: Context = contxt
    private val sharedPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    private val sharedPreferencesEditor: SharedPreferences.Editor = sharedPreferences.edit()

    val gson = Gson()

    private val RESTAURANTS_SHARED_PREFERENCES_KEY = "restaurants"
    private val PROMOTIONS_SHARED_PREFERENCES_KEY = "promotions_"
    private val FOODS_SHARED_PREFERENCES_KEY = "foods_"
    private val DRINKS_SHARED_PREFERENCES_KEY = "drinks_"
    private val DISHES_SHARED_PREFERENCES_KEY = "dishes_"

    private val USERS_SHARED_PREFERENCES_KEY = "users"

    public fun saveRestaurants(data: String){
        this.sharedPreferencesEditor.putString(RESTAURANTS_SHARED_PREFERENCES_KEY, data)
        this.sharedPreferencesEditor.apply()
        this.sharedPreferencesEditor.commit()
    }

    private fun getRestaurants() : MutableList<Branch>{
        val restaurantsString = this.sharedPreferences.getString(RESTAURANTS_SHARED_PREFERENCES_KEY, "[]")
        return gson.fromJson<MutableList<Branch>>(restaurantsString, object : TypeToken<MutableList<Branch>>(){}.type)
    }

    public fun getRestaurant(id: Int) : Branch? = this.getRestaurants().findLast { it.id == id }

    public fun updateRestaurant(branch: Branch){
        val restaurantsString = this.sharedPreferences.getString(RESTAURANTS_SHARED_PREFERENCES_KEY, "[]")
        val restaurants = gson.fromJson<MutableList<Branch>>(restaurantsString, object : TypeToken<MutableList<Branch>>(){}.type)
        try{ restaurants.forEach { if(it.id == branch.id) restaurants.remove(it) } } catch (e: Exception){}
        restaurants.add(branch)
        this.saveRestaurants(gson.toJson(restaurants))
    }

    public fun savePromotions(id: Int, data: String){
        this.sharedPreferencesEditor.putString("${PROMOTIONS_SHARED_PREFERENCES_KEY}$id", data)
        this.sharedPreferencesEditor.apply()
        this.sharedPreferencesEditor.commit()
    }

    private fun getPromotions(branch: Int) : MutableList<MenuPromotion> {
        val promotionsString = this.sharedPreferences.getString("${PROMOTIONS_SHARED_PREFERENCES_KEY}$branch", "[]")
        return  gson.fromJson<MutableList<MenuPromotion>>(promotionsString, object : TypeToken<MutableList<MenuPromotion>>(){}.type)
    }

    private fun getAllPromotions() : MutableList<MenuPromotion> {
        val promotions = mutableListOf<MenuPromotion>()
        this.getRestaurants().forEach { it.promotions?.promotions?.let { p -> promotions.addAll(p) } }
        return promotions
    }

    public fun getPromotion(id: Int) : MenuPromotion? = this.getAllPromotions().find { it.id == id }

    public fun getPromotion(branch: Int, id: Int) : MenuPromotion? = this.getPromotions(branch).find { it.id == id }

    public fun saveFoods(id: Int, data: String){
        this.sharedPreferencesEditor.putString("${FOODS_SHARED_PREFERENCES_KEY}$id", data)
        this.sharedPreferencesEditor.apply()
        this.sharedPreferencesEditor.commit()
    }

    private fun getFoods(branch: Int) : MutableList<RestaurantMenu> {
        val foodsString = this.sharedPreferences.getString("${FOODS_SHARED_PREFERENCES_KEY}$branch", "[]")
        return  gson.fromJson<MutableList<RestaurantMenu>>(foodsString, object : TypeToken<MutableList<RestaurantMenu>>(){}.type)
    }

    public fun getFood(branch: Int, id: Int) : RestaurantMenu? = this.getFoods(branch).find { it.id == id }

    public fun saveDrinks(id: Int, data: String){
        this.sharedPreferencesEditor.putString("${DRINKS_SHARED_PREFERENCES_KEY}$id", data)
        this.sharedPreferencesEditor.apply()
        this.sharedPreferencesEditor.commit()
    }

    private fun getDrinks(branch: Int) : MutableList<RestaurantMenu> {
        val drinksString = this.sharedPreferences.getString("${DRINKS_SHARED_PREFERENCES_KEY}$branch", "[]")
        return  gson.fromJson<MutableList<RestaurantMenu>>(drinksString, object : TypeToken<MutableList<RestaurantMenu>>(){}.type)
    }

    public fun getDrink(branch: Int, id: Int) : RestaurantMenu? = this.getDrinks(branch).find { it.id == id }

    public fun saveDishes(id: Int, data: String){
        this.sharedPreferencesEditor.putString("${DISHES_SHARED_PREFERENCES_KEY}$id", data)
        this.sharedPreferencesEditor.apply()
        this.sharedPreferencesEditor.commit()
    }

    private fun getDishes(branch: Int) : MutableList<RestaurantMenu> {
        val dishesString = this.sharedPreferences.getString("${DISHES_SHARED_PREFERENCES_KEY}$branch", "[]")
        return  gson.fromJson<MutableList<RestaurantMenu>>(dishesString, object : TypeToken<MutableList<RestaurantMenu>>(){}.type)
    }

    public fun getDish(branch: Int, id: Int) : RestaurantMenu? = this.getDishes(branch).find { it.id == id }

    private fun getRestaurantMenus(branch: Int) : MutableList<RestaurantMenu> {
        val menus = mutableListOf<RestaurantMenu>()
        menus.addAll(this.getFoods(branch))
        menus.addAll(this.getDrinks(branch))
        menus.addAll(this.getDishes(branch))
        return menus
    }

    public fun getMenuRestaurant(branch: Int, id: Int) : RestaurantMenu? = this.getRestaurantMenus(branch).find { it.id == id }

    private fun getAllMenus() : MutableList<RestaurantMenu>{
        val menus = mutableListOf<RestaurantMenu>()
        this.getRestaurants().forEach { it.menus.menus?.let { m -> menus.addAll(m) } }
        return menus
    }

    public fun getMenu(id: Int) : RestaurantMenu? = this.getAllMenus().find { it.id == id }

    private fun saveUsers(data: String){
        this.sharedPreferencesEditor.putString(USERS_SHARED_PREFERENCES_KEY, data)
        this.sharedPreferencesEditor.apply()
        this.sharedPreferencesEditor.commit()
    }

    private fun getUsers() : MutableList<User>{
        val usersString = this.sharedPreferences.getString(USERS_SHARED_PREFERENCES_KEY, "[]")
        return gson.fromJson<MutableList<User>>(usersString, object : TypeToken<MutableList<User>>(){}.type)
    }

    public fun getUser(id: Int) : User? = this.getUsers().find { it.id == id }

    public fun updateUser(user: User){
        val usersString = this.sharedPreferences.getString(USERS_SHARED_PREFERENCES_KEY, "[]")
        val users = gson.fromJson<MutableList<User>>(usersString, object : TypeToken<MutableList<User>>(){}.type)
        users.forEach { if(it.id == user.id) users.remove(it) }
        users.add(user)
        this.saveUsers(gson.toJson(users))
    }
}