package hu.petrik.filcapp

expect object AppPreferences {
    fun getAccentHue(): Float
    fun setAccentHue(hue: Float)
}
