import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'ledbar_controller.dart';
import 'ledbar_controller_platform_interface.dart';

class MethodChannelLedbarController extends LedbarControllerPlatform {
  @visibleForTesting
  final methodChannel = const MethodChannel('procc/ledbar');

  Future<String?> ping() async {
    return await methodChannel.invokeMethod<String>('ping');
  }

  @override
  Future<String?> getPlatformVersion() async {
    // İstersen Android tarafına özel bir method ekleyip döndürebilirsin
    return null;
  }

  @override
  Future<void> setColor(
      String color, {
        LedSide side = LedSide.right,
        int brightness = 50,
        bool hardReset = false,
        bool rawScale = true,
      }) {
    return methodChannel.invokeMethod('setColor', {
      'color': color,
      'side': side.name,
      'brightness': brightness,
      'hardReset': hardReset,   // 👈 eklendi
      'rawScale': rawScale,     // 👈 eklendi
    });
  }


  @override
  Future<void> setRgb({
    required int r,
    required int g,
    required int b,
    LedSide side = LedSide.right,
    bool rawScale = false,
  }) {
    return methodChannel.invokeMethod('setRgb', {
      'r': r,
      'g': g,
      'b': b,
      'side': side.name,
      'rawScale': rawScale,
    });
  }

  @override
  Future<void> rawSeek(int flag, {int brightness = 80}) async {
    const ch = MethodChannel('procc/ledbar');
    await ch.invokeMethod('rawSeek', {
      'flag': flag,
      'brightness': brightness, // 0..100
    });
  }

  @override
  Future<void> off({LedSide side = LedSide.right}) {
    return methodChannel.invokeMethod('off', {'side': side.name});
  }
}