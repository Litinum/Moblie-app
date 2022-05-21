package com.example.skucise

import android.os.Build
import androidx.annotation.RequiresApi
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class DateTimeHelper {
    companion object {
        fun getReadableDate(dt: LocalDateTime): String {
            val ct = LocalDateTime.now()
            val diff = Duration.between(dt, ct)
            var dateString = dt.format(DateTimeFormatter.ofPattern("EEEE,dd,MMM,yyyy"))
            return when(getDateAgeClass(dt)) {
                0 -> {
                    "Pre ${diff.seconds } sekundi"
                }
                1 -> {
                    "Pre ${diff.toMinutes()} minuta"
                }
                2 -> {
                    "Pre ${diff.toHours()} sati"
                }
                3 -> {
                    dateString.split(",")[0]
                }
                4 -> {
                    "${dateString.split(",")[2].substring(0, 3).capitalize()} ${dt.dayOfMonth}"
                }
                else -> {
                    dateString = dt.format(DateTimeFormatter.ofPattern("dd,MM,yy"))
                    dateString = dateString.replace(",", "/")
                    dateString
                }
            }
        }

        fun getReadableDateFull(dt: LocalDateTime): String {
            return dt.format(DateTimeFormatter.ofPattern("EEEE dd/MM/u HH:mm"))
        }

        fun getDateAgeClass(dt: LocalDateTime): Int {
            val ct = LocalDateTime.now()
            val diff = Duration.between(dt, ct)
            return when {
                diff < Duration.ofSeconds(0)-> {
                    -1
                }
                diff < Duration.ofMinutes(1) -> {
                    0 //Seconds
                }
                diff < Duration.ofHours(1) -> {
                    1 //Minutes
                }
                diff < Duration.ofDays(1) -> {
                    2 //Hours
                }
                diff < Duration.ofDays(7) -> {
                    3 //DayOfWeek
                }
                diff < Duration.ofDays(365) -> {
                    4 //DayOfMonth
                }
                else -> {
                    5 //DayOfYear
                }
            }
        }

        fun sameAgeClass(dt1: LocalDateTime, dt2: LocalDateTime): Boolean {
            return getDateAgeClass(dt1) == getDateAgeClass(dt2)
        }
    }
}