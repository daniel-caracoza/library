package com.example.library.database

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.android.parcel.Parcelize

@Parcelize
@Entity
data class Favorite(
    @PrimaryKey val bookid:String,
    @ColumnInfo(name="userid") val userid:Long,
    @ColumnInfo(name="author") val author:String,
    @ColumnInfo(name="title") val title:String,
    @ColumnInfo(name="desc") val desc:String,
    @ColumnInfo(name="image") val image:String
):Parcelable
