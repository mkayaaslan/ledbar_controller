import 'package:plugin_platform_interface/plugin_platform_interface.dart';

import 'ledbar_controller.dart';
import 'ledbar_controller_method_channel.dart';

/// Platform interface for the ledbar_controller plugin.
///
/// This class serves as the abstraction layer between the Dart API
/// and the platform-specific implementation (Android MethodChannel).
abstract class LedbarControllerPlatform extends PlatformInterface {
  LedbarControllerPlatform() : super(token: _token);

  static final Object _token = Object();

  /// Default instance uses the real MethodChannel implementation.
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
    required int r,   // 0..100 (percentage) or 0..15 (if rawScale=true)
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