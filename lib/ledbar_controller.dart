import 'color_enum.dart';
import 'ledbar_controller_platform_interface.dart';
export 'color_enum.dart';

/// Which side of the LED bar to control.
enum LedSide { right, left, both }

/// Main controller class for the iiyama tablet LED bar.
///
/// Communicates with the native Android plugin via MethodChannel ('procc/ledbar').
/// Supports multiple backends: JNI (B1PNR), sysfs direct, and sysfs via su (B3PNR 10").
class LedbarController {
  Future<String?> getPlatformVersion() {
    return LedbarControllerPlatform.instance.getPlatformVersion();
  }

  /// Sets the LED bar color using a named color string.
  ///
  /// [color] - Color name (e.g. 'red', 'green', 'blue', 'yellow', 'cyan', 'magenta', 'white')
  /// [side] - Which LED side to control (default: right)
  /// [brightness] - Brightness level 0-100 (default: 50)
  /// [hardReset] - Whether to force-reset the LED controller before applying
  /// [rawScale] - If true, brightness maps to 0-15 hardware scale directly
  Future<void> setColor(
      String color, {
        LedSide side = LedSide.right,
        int brightness = 50,
        bool hardReset = false,
        bool rawScale = true,
      }) {
    return LedbarControllerPlatform.instance.setColor(
      color,
      side: side,
      brightness: brightness,
      hardReset: hardReset,
      rawScale: rawScale,
    );
  }

  /// Sets the LED bar color using RGB values or a [ColorEnum] preset.
  ///
  /// If [colorEnum] is provided, its predefined RGB values are used.
  /// Otherwise, [r], [g], [b] values (0-100 scale) are used directly.
  Future<void> setRgb({
    ColorEnum? colorEnum,
    int? r,
    int? g,
    int? b,
    LedSide side = LedSide.right,
    bool rawScale = false,
  }) {
    // Use ColorEnum's predefined RGB values if provided
    Map<String, int> rgb;
    if (colorEnum != null) {
      rgb = colorEnum.toRgb();
    } else {
      rgb = {
        'r': r ?? 0,
        'g': g ?? 0,
        'b': b ?? 0,
      };
    }

    return LedbarControllerPlatform.instance.setRgb(
      r: rgb['r']!,
      g: rgb['g']!,
      b: rgb['b']!,
      side: side,
      rawScale: rawScale,
    );
  }

  /// Turns off the LED bar.
  Future<void> off({LedSide side = LedSide.right}) {
    return LedbarControllerPlatform.instance.off(side: side);
  }

  /// Sends a raw seek command to the LED controller (low-level JNI access).
  ///
  /// [flag] - Hardware flag (e.g. 0xA1=right red, 0xA2=right green, 0xA3=right blue)
  /// [brightness] - Brightness level 0-100 (default: 80)
  Future<void> rawSeek(int flag, {int brightness = 80}) {
    return LedbarControllerPlatform.instance.rawSeek(flag, brightness: brightness);
  }
}