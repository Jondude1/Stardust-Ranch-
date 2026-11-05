package android.graphics
class Bitmap private constructor(private val w: Int, private val h: Int, val px: IntArray) {
    enum class Config { ARGB_8888 }
    val width: Int get() = w
    val height: Int get() = h
    fun getPixels(out: IntArray, offset: Int, stride: Int, x: Int, y: Int, ww: Int, hh: Int) {
        for (j in 0 until hh) for (i in 0 until ww) out[offset + j * stride + i] = px[(y + j) * w + x + i]
    }
    fun getPixel(x: Int, y: Int) = px[y * w + x]
    companion object { @JvmStatic fun createBitmap(p: IntArray, w: Int, h: Int, c: Config) = Bitmap(w, h, p.copyOf()) }
}
