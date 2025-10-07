import 'package:plugin_platform_interface/plugin_platform_interface.dart';

import 'ledbar_controller.dart';
import 'ledbar_controller_method_channel.dart'; // <-- ÖNEMLİ: gerçek implementasyonu içeri al

abstract class LedbarControllerPlatform extends PlatformInterface {
  LedbarControllerPlatform() : super(token: _token);

  static final Object _token = Object();

  // <-- ÖNEMLİ: Varsayılan instance gerçek MethodChannel sınıfı olmalı
  static LedbarControllerPlatform _instance = MethodChannelLedbarController();

  static LedbarControllerPlatform get instance => _instance;

  static set instance(LedbarControllerPlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  Future<String?> getPlatformVersion() {
    throw UnimplementedError('getPlatformVersion() has not been implemented.');
  }

  Future<void> setColor(
      String color, {
        LedSide side = LedSide.right,
        int brightness = 50,
        bool hardReset = false,
        bool rawScale = true,
      }) {
    throw UnimplementedError('setColor() has not been implemented.');
  }

  Future<void> setRgb({
    required int r,   // 0..100 (yüzde) veya 0..15 (rawScale=true)
    required int g,
    required int b,
    LedSide side = LedSide.right,
    bool rawScale = false,
  });

  Future<void> rawSeek(
      int flag, {
        int brightness = 50,
      }) {
    throw UnimplementedError('rawSeek() has not been implemented.');
  }

  Future<void> off({LedSide side = LedSide.right}) {
    throw UnimplementedError('off() has not been implemented.');
  }
}

// DİKKAT: Burada **KESİNLİKLE** boş bir `class MethodChannelLedbarController …` TANIMLAMA!
// (Eğer daha önce eklediysen, onu SİL.)