import 'color_enum.dart';
import 'ledbar_controller_platform_interface.dart';
export 'color_enum.dart';

enum LedSide { right, left, both }




class LedbarController {
  Future<String?> getPlatformVersion() {
    return LedbarControllerPlatform.instance.getPlatformVersion();
  }

  Future<void> setColor(
      String color, {
        LedSide side = LedSide.right,
        int brightness = 50,
        bool hardReset = false,   // 👈 yeni
        bool rawScale = true,     // 👈 yeni
      }) {
    return LedbarControllerPlatform.instance.setColor(
      color,
      side: side,
      brightness: brightness,
      hardReset: hardReset,
      rawScale: rawScale,
    );
  }

  Future<void> setRgb({
    ColorEnum? colorEnum, // 👈 yeni parametre
    int? r,
    int? g,
    int? b,
    LedSide side = LedSide.right,
    bool rawScale = false,
  }) {
    // Eğer colorEnum verilmişse onun RGB değerlerini al
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

  Future<void> off({LedSide side = LedSide.right}) {
    return LedbarControllerPlatform.instance.off(side: side);
  }

  Future<void> rawSeek(int flag, {int brightness = 80}) {
    return LedbarControllerPlatform.instance.rawSeek(flag, brightness: brightness);
  }
}