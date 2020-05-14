package com.example.library.database

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.android.parcel.Parcelize

@Parcelize
@Entity(tableName = "user_table", indices= arrayOf(Index(value = ["username"], unique = true)))
data class User(
    @PrimaryKey(autoGenerate = true)
    var uid:Long,
    @ColumnInfo(name= "fullname") val fullname:String?,
    @ColumnInfo(name = "username") val userName:String?,
    @ColumnInfo(name = "password") var password:String?
):Parcelable



