/// Temel LED renkleri
enum ColorEnum {
  red,
  green,
  blue,
  orange,
  white;


  Map<String, int> toRgb() {
    switch (this) {
      case ColorEnum.red:
        return {'r': 100, 'g': 0, 'b': 0};
      case ColorEnum.green:
        return {'r': 0, 'g': 100, 'b': 0};
      case ColorEnum.blue:
        return {'r': 0, 'g': 0, 'b': 100};
      case ColorEnum.orange:
        return {'r': 100, 'g': 20, 'b': 0};
      case ColorEnum.white:
        return {'r': 100, 'g': 100, 'b': 100};
    }
  }
}

class LedColor {
  final ColorEnum? colorEnum;
  final int? r;
  final int? g;
  final int? b;

  const LedColor({this.colorEnum, this.r, this.g, this.b});

  Map<String, int> get rgb {
    if (colorEnum != null) return colorEnum!.toRgb();
    final hasManual = r != null || g != null || b != null;
    if (hasManual) {
      return {'r': r ?? 0, 'g': g ?? 0, 'b': b ?? 0};
    }
    return {'r': 0, 'g': 0, 'b': 100}; // default blue
  }

  @override
  String toString() =>
      'LedColor(r:${rgb['r']}, g:${rgb['g']}, b:${rgb['b']})';
}