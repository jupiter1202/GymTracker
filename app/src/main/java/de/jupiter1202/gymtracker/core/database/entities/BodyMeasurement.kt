package de.jupiter1202.gymtracker.core.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "body_measurements")
data class BodyMeasurement(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: Long,
    @ColumnInfo(name = "weight_kg")
    val weightKg: Double? = null,
    @ColumnInfo(name = "chest_cm")
    val chestCm: Double? = null,
    @ColumnInfo(name = "waist_cm")
    val waistCm: Double? = null,
    @ColumnInfo(name = "hips_cm")
    val hipsCm: Double? = null,
    @ColumnInfo(name = "arms_cm")
    val armsCm: Double? = null,
    @ColumnInfo(name = "thighs_cm")
    val thighsCm: Double? = null
)
