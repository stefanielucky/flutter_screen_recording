import 'dart:async';

import 'package:flutter/services.dart';

class FlutterScreenRecording {
  static const MethodChannel _channel = const MethodChannel('flutter_screen_recording');

  static Future<bool> startRecordScreen(String name,[Map option]) async {
    final bool start = await _channel.invokeMethod('startRecordScreen', {"name": name, "audio": false,"width":option["Width"],"height":option["Height"],"bitRate":option["BitRate"],"frameRate":option["FrameRate"]});
    return start;
  }

  static Future<bool> startRecordScreenAndAudio(String name,[Map option]) async {
    final bool start = await _channel.invokeMethod('startRecordScreen', {"name": name, "audio": true,"width":option["Width"],"height":option["Height"],"bitRate":option["BitRate"],"frameRate":option["FrameRate"]});
    return start;
  }

  static Future<String> get stopRecordScreen async {
    final String path = await _channel.invokeMethod('stopRecordScreen');
    return path;
  }
}
