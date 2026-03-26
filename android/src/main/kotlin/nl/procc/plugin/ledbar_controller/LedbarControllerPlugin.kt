package nl.procc.plugin.ledbar_controller

import android.util.Log
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import com.example.elcapi.jnielc
import java.io.FileOutputStream

class LedbarControllerPlugin : FlutterPlugin, MethodChannel.MethodCallHandler {

    private lateinit var channel: MethodChannel

    /* -------------------- Backend Detection -------------------- */

    private enum class LedBackend { JNI, SYSFS_DIRECT, SYSFS_SU, UNKNOWN }

    /** İlk başarılı çağrıda tespit edilen backend; sonraki çağrılarda cache'den okunur. */
    private var detectedBackend: LedBackend = LedBackend.UNKNOWN

    /** Son gönderilen sysfs komutu — aynı komutu tekrar göndermemek için */
    private var lastSysfsCommand: String? = null

    /** Son LED yazma zamanı (ms) */
    private var lastWriteTime: Long = 0L

    /** LED komutları arası minimum bekleme süresi (ms) */
    private val LED_COOLDOWN_MS = 80L

    /** Eş zamanlı LED erişimini önlemek için kilit */
    private val ledLock = Object()

    companion object {
        private const val TAG = "LED_PLUGIN"
        private const val SYSFS_PATH = "/sys/devices/platform/led_con_h/zigbee_reset"
    }

    override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        Log.i(TAG, "onAttachedToEngine() called")
        channel = MethodChannel(binding.binaryMessenger, "procc/ledbar")
        channel.setMethodCallHandler(this)
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
    }

    /* -------------------- Helpers -------------------- */

    private fun to015(rawScale: Boolean, v: Int): Int =
        if (rawScale) v.coerceIn(0, 15) else (v.coerceIn(0, 100) * 15 / 100).coerceIn(0, 15)

    /** LED kontrolcünün hazır olmasını bekle */
    private fun waitForCooldown() {
        val elapsed = System.currentTimeMillis() - lastWriteTime
        if (elapsed < LED_COOLDOWN_MS) {
            Thread.sleep(LED_COOLDOWN_MS - elapsed)
        }
    }

    /* -------------------- Backend: JNI -------------------- */

    private fun jniSeekStartOrThrow() {
        val fp = jnielc.seekstart()
        if (fp < 0) throw RuntimeException("JNI seekstart failed: fp=$fp")
    }

    private fun tryJni(block: () -> Unit): Boolean {
        if (detectedBackend != LedBackend.UNKNOWN && detectedBackend != LedBackend.JNI) return false
        return try {
            block()
            if (detectedBackend == LedBackend.UNKNOWN) {
                detectedBackend = LedBackend.JNI
                Log.i(TAG, "Backend detected: JNI")
            }
            lastWriteTime = System.currentTimeMillis()
            true
        } catch (t: Throwable) {
            Log.w(TAG, "JNI failed: ${t.message}")
            false
        }
    }

    /* -------------------- Backend: Sysfs Direct (FileOutputStream) -------------------- */

    private fun trySysfsDirect(command: String): Boolean {
        if (detectedBackend != LedBackend.UNKNOWN && detectedBackend != LedBackend.SYSFS_DIRECT) return false
        return try {
            FileOutputStream(SYSFS_PATH).use { fos ->
                fos.write("$command\n".toByteArray(Charsets.US_ASCII))
                fos.flush()
            }
            if (detectedBackend == LedBackend.UNKNOWN) {
                detectedBackend = LedBackend.SYSFS_DIRECT
                Log.i(TAG, "Backend detected: SYSFS_DIRECT")
            }
            lastWriteTime = System.currentTimeMillis()
            Log.d(TAG, "sysfs direct OK -> $command")
            true
        } catch (t: Throwable) {
            Log.w(TAG, "sysfs direct failed: ${t.message}")
            false
        }
    }

    /* -------------------- Backend: Sysfs via su 0 (root shell) -------------------- */

    private fun trySysfsSu(command: String): Boolean {
        if (detectedBackend != LedBackend.UNKNOWN && detectedBackend != LedBackend.SYSFS_SU) return false
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "0", "sh", "-c",
                "echo $command > $SYSFS_PATH"))
            val exitCode = process.waitFor()
            if (exitCode == 0) {
                if (detectedBackend == LedBackend.UNKNOWN) {
                    detectedBackend = LedBackend.SYSFS_SU
                    Log.i(TAG, "Backend detected: SYSFS_SU")
                }
                lastWriteTime = System.currentTimeMillis()
                Log.d(TAG, "sysfs su OK -> $command")
                true
            } else {
                Log.w(TAG, "sysfs su exit=$exitCode for: $command")
                false
            }
        } catch (t: Throwable) {
            Log.e(TAG, "sysfs su failed: ${t.message}")
            false
        }
    }

    /* -------------------- Orchestrator -------------------- */

    /**
     * LED'e renk komutu gönderir. Senkronize + cooldown + deduplicate.
     */
    private fun writeLed(jniBlock: (() -> Unit)?, sysfsCommand: String) {
        synchronized(ledLock) {
            // Aynı komut 2 kez peş peşe gelmişse atla (debounce)
            if (sysfsCommand == lastSysfsCommand) {
                val elapsed = System.currentTimeMillis() - lastWriteTime
                if (elapsed < 200) {
                    Log.d(TAG, "Debounce: skipping duplicate '$sysfsCommand' (${elapsed}ms ago)")
                    return
                }
            }

            // Cooldown: LED kontrolcünün hazır olmasını bekle
            waitForCooldown()

            // 1) su 0 ile sysfs (B3PNR 10" için)
            if (trySysfsSu(sysfsCommand)) {
                lastSysfsCommand = sysfsCommand
                return
            }

            // 2) FileOutputStream ile doğrudan sysfs
            if (trySysfsDirect(sysfsCommand)) {
                lastSysfsCommand = sysfsCommand
                return
            }

            // 3) JNI (B1PNR / B3PNR 16" için)
            if (jniBlock != null && tryJni(jniBlock)) {
                lastSysfsCommand = sysfsCommand
                return
            }

            Log.e(TAG, "All backends failed for command: $sysfsCommand")
        }
    }

    /** RGB değerlerinden (0-100 ölçeği) sysfs komutu oluştur */
    private fun rgbToSysfsCommand(r100: Int, g100: Int, b100: Int): String {
        val r255 = (r100.coerceIn(0, 100) * 255) / 100
        val g255 = (g100.coerceIn(0, 100) * 255) / 100
        val b255 = (b100.coerceIn(0, 100) * 255) / 100
        return String.format("w 0x66%02x%02x%02x", r255, g255, b255)
    }

    /* -------------------- MethodChannel Handler -------------------- */

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        try {
            when (call.method) {

                "ping" -> {
                    result.success("pong")
                }

                "off" -> {
                    writeLed(
                        jniBlock = {
                            jniSeekStartOrThrow()
                            jnielc.ledoff()
                            jnielc.seekstop()
                        },
                        sysfsCommand = "w 0x02"
                    )
                    result.success(null)
                }

                "rawSeek" -> {
                    val flag = call.argument<Int>("flag") ?: 0xA3
                    val inBright = (call.argument<Int>("brightness") ?: 50).coerceIn(0, 100)
                    val v015 = to015(false, inBright)

                    val (r100, g100, b100) = when (flag) {
                        0xA1, 0xB1 -> Triple(inBright, 0, 0)       // red
                        0xA2, 0xB2 -> Triple(0, inBright, 0)       // green
                        else       -> Triple(0, 0, inBright)       // blue
                    }

                    writeLed(
                        jniBlock = {
                            jniSeekStartOrThrow()
                            jnielc.ledseek(flag, v015)
                            jnielc.seekstop()
                        },
                        sysfsCommand = rgbToSysfsCommand(r100, g100, b100)
                    )
                    result.success(null)
                }

                "setRgb" -> {
                    val side     = (call.argument<String>("side") ?: "right").lowercase()
                    val rawScale = call.argument<Boolean>("rawScale") ?: false
                    val inR      = call.argument<Int>("r") ?: 0
                    val inG      = call.argument<Int>("g") ?: 0
                    val inB      = call.argument<Int>("b") ?: 0

                    val R015 = to015(rawScale, inR)
                    val G015 = to015(rawScale, inG)
                    val B015 = to015(rawScale, inB)

                    writeLed(
                        jniBlock = {
                            jniSeekStartOrThrow()
                            if (side == "right" || side == "both") {
                                jnielc.ledseek(0xA1, R015)
                                jnielc.ledseek(0xA2, G015)
                                jnielc.ledseek(0xA3, B015)
                            }
                            if (side == "left" || side == "both") {
                                jnielc.ledseek(0xB1, R015)
                            }
                            jnielc.seekstop()
                        },
                        sysfsCommand = rgbToSysfsCommand(inR, inG, inB)
                    )
                    result.success(null)
                }

                "setColor" -> {
                    val color  = (call.argument<String>("color") ?: "blue").lowercase()
                    val side   = (call.argument<String>("side") ?: "right").lowercase()
                    val bright = (call.argument<Int>("brightness") ?: 50).coerceIn(0, 100)

                    val (r, g, b) = when (color) {
                        "red"     -> Triple(bright, 0, 0)
                        "green"   -> Triple(0, bright, 0)
                        "blue"    -> Triple(0, 0, bright)
                        "yellow"  -> Triple(bright, bright, 0)
                        "cyan"    -> Triple(0, bright, bright)
                        "magenta" -> Triple(bright, 0, bright)
                        "white"   -> Triple(bright, bright, bright)
                        else      -> Triple(0, 0, bright)
                    }

                    writeLed(
                        jniBlock = {
                            jniSeekStartOrThrow()
                            if (side == "right" || side == "both") {
                                jnielc.ledseek(0xA1, to015(false, r))
                                jnielc.ledseek(0xA2, to015(false, g))
                                jnielc.ledseek(0xA3, to015(false, b))
                            }
                            if (side == "left" || side == "both") {
                                jnielc.ledseek(0xB1, to015(false, r))
                            }
                            jnielc.seekstop()
                        },
                        sysfsCommand = rgbToSysfsCommand(r, g, b)
                    )
                    result.success(null)
                }

                else -> result.notImplemented()
            }
        } catch (t: Throwable) {
            Log.e(TAG, "onMethodCall error", t)
            result.error("LED_ERROR", t.message, null)
        }
    }
}