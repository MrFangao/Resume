/// 应用配置类
class AppConfig {
  // API基础URL
  // 注意：
  // - Android模拟器使用: http://10.0.2.2:5000
  // - iOS模拟器使用: http://localhost:5000
  // - 真机使用: http://你的电脑IP:5000 (例如: http://192.168.1.100:5000)
  static const String apiBaseUrl = 'http://10.0.2.2:5000'; // Android模拟器默认
  
  // 如果需要在iOS模拟器或真机上使用，可以修改这里
  // static const String apiBaseUrl = 'http://localhost:5000'; // iOS模拟器
  // static const String apiBaseUrl = 'http://192.168.1.100:5000'; // 真机（替换为实际IP）
}

