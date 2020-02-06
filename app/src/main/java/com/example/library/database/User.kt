package com.example.library.database

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.android.parcel.Parcelize

@Parcelize
@Entity
data class User(
    @PrimaryKey(autoGenerate = true) val uid:Int,
    @ColumnInfo(name= "fullname") val fullname:String?,
    @ColumnInfo(name = "username") val userName:String?,
    @ColumnInfo(name = "password") val password:String?
):Parcelable

