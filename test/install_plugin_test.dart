import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:install_plugin/install_plugin.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  const channel = MethodChannel('install_plugin');

  tearDown(() {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(channel, null);
  });

  test('installApk 调用', () async {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(channel, (MethodCall call) async {
      expect(call.method, 'installApk');
      expect(call.arguments, isA<Map>());
      return 'success';
    });
    final result = await InstallPlugin.installApk('/path/to.apk', 'com.example');
    expect(result, 'success');
  });

  test('gotoAppStore 调用', () async {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(channel, (MethodCall call) async {
      expect(call.method, 'gotoAppStore');
      return 'ok';
    });
    final result = await InstallPlugin.gotoAppStore('https://apps.apple.com/xxx');
    expect(result, 'ok');
  });
}
