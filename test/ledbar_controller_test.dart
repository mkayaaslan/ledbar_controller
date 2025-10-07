import 'package:flutter_test/flutter_test.dart';
import 'package:ledbar_controller/ledbar_controller.dart';
import 'package:ledbar_controller/ledbar_controller_platform_interface.dart';
import 'package:ledbar_controller/ledbar_controller_method_channel.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

class MockLedbarControllerPlatform
    with MockPlatformInterfaceMixin
    implements LedbarControllerPlatform {

  @override
  Future<String?> getPlatformVersion() => Future.value('42');

  @override
  Future<void> setColor(
      String color, {
        LedSide side = LedSide.right,
        int brightness = 50,
        bool hardReset = false,
        bool rawScale = true,
      }) async {
    // test için no-op
    return;
  }

  @override
  Future<void> off({LedSide side = LedSide.right}) async {
    // test için no-op
    return;
  }

  @override
  Future<void> rawSeek(int flag, {int brightness = 50}) {
    // TODO: implement rawSeek
    throw UnimplementedError();
  }

  @override
  Future<void> setRgb({required int r, required int g, required int b, LedSide side = LedSide.right, bool
  rawScale = false}) {
    // TODO: implement setRgb
    throw UnimplementedError();
  }
}

void main() {
  final LedbarControllerPlatform initialPlatform =
      LedbarControllerPlatform.instance;

  test('$MethodChannelLedbarController is the default instance', () {
    expect(initialPlatform, isInstanceOf<MethodChannelLedbarController>());
  });

  test('getPlatformVersion', () async {
    final plugin = LedbarController();
    final fakePlatform = MockLedbarControllerPlatform();
    LedbarControllerPlatform.instance = fakePlatform;

    expect(await plugin.getPlatformVersion(), '42');
  });
}