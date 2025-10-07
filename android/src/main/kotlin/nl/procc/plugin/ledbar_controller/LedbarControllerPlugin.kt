package nl.procc.plugin.ledbar_controller

import android.util.Log
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import com.example.elcapi.jnielc

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

    /* --- Yardımcılar --- */

    // Sağ için tek-kanal flag (A1/A2/A3). Sol şerit cihazında mono-red olduğu için,
    // sol flag'i direkt B1 (kırmızı) olarak ele alıyoruz (aşağıda ayrıca kontrol ediyoruz).
    private fun primaryRight(color: String): Int = when (color) {
        "red" -> 0xA1
        "green" -> 0xA2
        "blue" -> 0xA3
        else -> 0xA3 // default blue
    }

    // Kombineleri en yakın tek renge indir (cihaz presetleri güvenilmez)
    private fun normalizeToPrimary(color: String): String = when (color) {
        "yellow"  -> "red"    // R+G → R
        "cyan"    -> "green"  // G+B → G
        "magenta" -> "blue"   // R+B → B (istersen "red" de seçilebilir)
        "white"   -> "blue"   // R+G+B → B (görünürlük açısından)
        else      -> color
    }

    // Sol taraf (mono-red): yalnız kırmızı içeren renklerde yanmalı
    private fun leftWantsRed(color: String): Boolean = when (color) {
        "red", "yellow", "magenta", "white" -> true
        else -> false // green/blue/cyan için sol sönük
    }

    // Tek oturumda: önce global temizle (blink kontrollü) → sonra hedef flag(ler)i yaz
    private fun writeBothOnce(
        rightFlagOrNull: Int?,       // null ise sağ yazma
        leftRedOn: Boolean,          // sol şerit yanacak mı?
        value0_15: Int               // 0..15
    ) {
        // 1) temiz başlangıç (rawSeek akışıyla aynı)
        jnielc.seekstart()
        jnielc.ledoff()
        jnielc.seekstop()
        try { Thread.sleep(50) } catch (_: Throwable) {}

        // 2) tek oturumda hedef(ler)i yaz
        jnielc.seekstart()
        if (rightFlagOrNull != null) {
            jnielc.ledseek(rightFlagOrNull, value0_15)
        }
        if (leftRedOn) {
            jnielc.ledseek(0xB1, value0_15) // sol: yalnız kırmızı
        }
        jnielc.seekstop()
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        try {
            when (call.method) {

                /* ping: hızlı sağlık kontrolü */
                "ping" -> {
                    result.success("pong")
                }

                /* rawSeek: geliştirici testi (tek flag) */
                "rawSeek" -> {
                    val flag     = call.argument<Int>("flag") ?: 0xA3
                    val inBright = (call.argument<Int>("brightness") ?: 50).coerceIn(0,100)
                    val value0_15 = (inBright * 15 / 100).coerceIn(0,15)

                    jnielc.seekstart(); jnielc.ledoff(); jnielc.seekstop()
                    try { Thread.sleep(50) } catch (_: Throwable) {}
                    jnielc.seekstart(); jnielc.ledseek(flag, value0_15); jnielc.seekstop()
                    result.success(null)
                }

                /* off: tamamen kapat */
                "off" -> {
                    jnielc.seekstart(); jnielc.ledoff(); jnielc.seekstop()
                    result.success(null)
                }

                /* setColor: rawSeek akışı ile birebir; kombineleri normalize eder */
                "setColor" -> {
                    val colorIn  = (call.argument<String>("color") ?: "blue").lowercase()
                    val side     = (call.argument<String>("side")  ?: "right").lowercase() // right|left|both
                    val rawScale = call.argument<Boolean>("rawScale") ?: false
                    val inB      = call.argument<Int>("brightness") ?: 50

                    // parlaklık: rawScale=true ise 0..15 clamp; değilse 0..100 → 0..15 map
                    val value0_15 = if (rawScale)
                        inB.coerceIn(0, 15)
                    else
                        (inB.coerceIn(0,100) * 15 / 100).coerceIn(0,15)

                    // kombineleri en yakın tek renge indir
                    val color = normalizeToPrimary(colorIn)

                    // sağ/sol hedefleri hesapla
                    val rightFlag: Int? = when (side) {
                        "right" -> primaryRight(color)
                        "left"  -> null
                        "both"  -> primaryRight(color)
                        else    -> primaryRight(color)
                    }
                    val leftOn = when (side) {
                        "right" -> false
                        "left"  -> leftWantsRed(color)
                        "both"  -> leftWantsRed(color)
                        else    -> false
                    }

                    // tek-oturum yaz
                    writeBothOnce(rightFlag, leftOn, value0_15)
                    result.success(null)
                }

                "setRgb" -> {
                    val side     = (call.argument<String>("side") ?: "right").lowercase() // right|left|both
                    val rawScale = call.argument<Boolean>("rawScale") ?: false
                    val inR      = call.argument<Int>("r") ?: 0
                    val inG      = call.argument<Int>("g") ?: 0
                    val inB      = call.argument<Int>("b") ?: 0

                    // driver için 0..15 değere çevir
                    fun to015(x: Int) = if (rawScale) x.coerceIn(0,15) else (x.coerceIn(0,100) * 15 / 100).coerceIn(0,15)
                    val R = to015(inR); val G = to015(inG); val BL = to015(inB)

                    try {
                        // BLINK OLMASIN: doğrudan kanallara yaz
                        jnielc.seekstart()

                        // RIGHT (A1/A2/A3)
                        if (side == "right" || side == "both") {
                            jnielc.ledseek(0xA1, R)   // red
                            jnielc.ledseek(0xA2, G)   // green
                            jnielc.ledseek(0xA3, BL)  // blue
                        }

                        // LEFT: cihazında mono-red → sadece B1 anlamlı (kırmızı); B2/B3 yoksa 0 yazmaya gerek yok
                        if (side == "left"  || side == "both") {
                            // Eğer sol RGB destekliyorsa burayı B1/B2/B3 ile açarız; senin cihazda mono-red:
                            jnielc.ledseek(0xB1, R)   // kırmızı bileşen varsa yanar, yoksa 0
                        }

                        jnielc.seekstop()
                        result.success(null)
                    } catch (t: Throwable) {
                        Log.e("LED_PLUGIN", "setRgb error", t)
                        result.error("LED_ERROR", t.message, null)
                    }
                }

                else -> result.notImplemented()
            }
        } catch (t: Throwable) {
            Log.e("LED_PLUGIN", "onMethodCall error", t)
            result.error("LED_ERROR", t.message, null)
        }
    }
}