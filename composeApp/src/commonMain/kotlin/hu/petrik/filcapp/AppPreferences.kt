package hu.petrik.filcapp

expect object AppPreferences {
    fun getAccentHue(): Float
    fun setAccentHue(hue: Float)
    fun getThemeMode(): Int   // 0=system, 1=light, 2=dark
    fun setThemeMode(mode: Int)
}
