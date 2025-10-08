package nl.procc.plugin.ledbar_controller

import android.util.Log
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import com.example.elcapi.jnielc
import java.io.FileOutputStream  // 👈 gerekli

class LedbarControllerPlugin : FlutterPlugin, MethodChannel.MethodCallHandler {

    private lateinit var channel: MethodChannel

    override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        Log.i("LED_PLUGIN", "onAttachedToEngine() called")
        channel = MethodChannel(binding.binaryMessenger, "procc/ledbar")
        channel.setMethodCallHandler(this)
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
    }

    /* -------------------- Helpers (minimal) -------------------- */

    private fun to015(rawScale: Boolean, v: Int): Int =
        if (rawScale) v.coerceIn(0, 15) else (v.coerceIn(0, 100) * 15 / 100).coerceIn(0, 15)

    /** Sysfs fallback: doğrudan dosyaya yaz (newline şart) */
    private fun writeSysfsColor(r255: Int, g255: Int, b255: Int) {
        val rr = r255.coerceIn(0, 255)
        val gg = g255.coerceIn(0, 255)
        val bb = b255.coerceIn(0, 255)
        val line = String.format("w 0x66%02x%02x%02x\n", rr, gg, bb) // \n önemli
        val path = "/sys/devices/platform/led_con_h/zigbee_reset"
        try {
            FileOutputStream(path).use { fos ->
                fos.write(line.toByteArray(Charsets.US_ASCII))
                fos.flush()
            }
            Log.i("LED_PLUGIN", "sysfs write OK -> $line")
        } catch (t: Throwable) {
            Log.e("LED_PLUGIN", "sysfs write FAIL -> $line at $path", t)
        }
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        try {
            when (call.method) {

                "ping" -> {
                    result.success("pong")
                }

                "off" -> {
                    try {
                        jnielc.seekstart()
                        jnielc.ledoff()
                        jnielc.seekstop()
                    } catch (t: Throwable) {
                        Log.w("LED_PLUGIN", "JNI off failed, fallback to sysfs", t)
                    }
                    writeSysfsColor(0, 0, 0)
                    result.success(null)
                }

                "rawSeek" -> {
                    val flag = call.argument<Int>("flag") ?: 0xA3
                    val inBright = (call.argument<Int>("brightness") ?: 50).coerceIn(0, 100)
                    val v015 = to015(false, inBright)

                    // 1) JNI dene
                    try {
                        jnielc.seekstart()
                        jnielc.ledseek(flag, v015)
                        jnielc.seekstop()
                    } catch (t: Throwable) {
                        Log.w("LED_PLUGIN", "JNI rawSeek failed, fallback to sysfs", t)
                    }
                    // 2) sysfs fallback (flag'a göre yaklaşık RGB)
                    val (r100,g100,b100) = when (flag) {
                        0xA1, 0xB1 -> Triple(inBright, 0, 0)     // red
                        0xA2, 0xB2 -> Triple(0, inBright, 0)     // green
                        else      -> Triple(0, 0, inBright)      // blue
                    }
                    writeSysfsColor((r100*255)/100, (g100*255)/100, (b100*255)/100)
                    result.success(null)
                }

                "setRgb" -> {
                    val side     = (call.argument<String>("side") ?: "right").lowercase() // right|left|both
                    val rawScale = call.argument<Boolean>("rawScale") ?: false
                    val inR      = call.argument<Int>("r") ?: 0
                    val inG      = call.argument<Int>("g") ?: 0
                    val inB      = call.argument<Int>("b") ?: 0

                    val R015 = to015(rawScale, inR)
                    val G015 = to015(rawScale, inG)
                    val B015 = to015(rawScale, inB)

                    // 1) JNI dene
                    try {
                        jnielc.seekstart()
                        if (side == "right" || side == "both") {
                            jnielc.ledseek(0xA1, R015)
                            jnielc.ledseek(0xA2, G015)
                            jnielc.ledseek(0xA3, B015)
                        }
                        if (side == "left" || side == "both") {
                            // sol: mono-red
                            jnielc.ledseek(0xB1, R015)
                        }
                        jnielc.seekstop()
                    } catch (t: Throwable) {
                        Log.w("LED_PLUGIN", "JNI setRgb failed, fallback to sysfs", t)
                    }

                    // 2) sysfs fallback (tek atışta RGB)
                    writeSysfsColor(
                        (inR.coerceIn(0, 100) * 255) / 100,
                        (inG.coerceIn(0, 100) * 255) / 100,
                        (inB.coerceIn(0, 100) * 255) / 100
                    )
                    result.success(null)
                }

                "setColor" -> {
                    // Geriye dönük uyumluluk: primary + kombineleri setRgb'e map’ler
                    val color = (call.argument<String>("color") ?: "blue").lowercase()
                    val side  = (call.argument<String>("side")  ?: "right").lowercase()
                    val bright = (call.argument<Int>("brightness") ?: 50).coerceIn(0,100)

                    val (r,g,b) = when (color) {
                        "red"     -> Triple(bright, 0, bright - bright)   // (bright,0,0)
                        "green"   -> Triple(0, bright, 0)
                        "blue"    -> Triple(0, 0, bright)
                        "yellow"  -> Triple(bright, bright, 0)
                        "cyan"    -> Triple(0, bright, bright)
                        "magenta" -> Triple(bright, 0, bright)
                        "white"   -> Triple(bright, bright, bright)
                        else      -> Triple(0, 0, bright)
                    }

                    // JNI dene
                    try {
                        jnielc.seekstart()
                        if (side == "right" || side == "both") {
                            jnielc.ledseek(0xA1, to015(false, r))
                            jnielc.ledseek(0xA2, to015(false, g))
                            jnielc.ledseek(0xA3, to015(false, b))
                        }
                        if (side == "left" || side == "both") {
                            jnielc.ledseek(0xB1, to015(false, r)) // sol: sadece kırmızı
                        }
                        jnielc.seekstop()
                    } catch (t: Throwable) {
                        Log.w("LED_PLUGIN", "JNI setColor failed, fallback to sysfs", t)
                    }

                    // sysfs fallback
                    writeSysfsColor((r*255)/100, (g*255)/100, (b*255)/100)
                    result.success(null)
                }

                else -> result.notImplemented()
            }
        } catch (t: Throwable) {
            Log.e("LED_PLUGIN", "onMethodCall error", t)
            result.error("LED_ERROR", t.message, null)
        }
    }
}