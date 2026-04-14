package com.example.unscramble.ui

@Entity
data class User(
    @PrimaryKey val uid: Int,
    @ColumnInfo(name = "first_name") val firstName: String?,
    @ColumnInfo(name = "last_name") val lastName: String?
)

class PrimaryKey {
 val uid: int,
    (name = "first_name") val firstName: String?,
}
