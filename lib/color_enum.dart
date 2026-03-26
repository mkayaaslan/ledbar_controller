/// Predefined LED color presets with RGB values on a 0-100 scale.
enum ColorEnum {
  red,
  green,
  blue,
  orange,
  white,
  dimWhite;

  /// Converts this color preset to an RGB map with values on a 0-100 scale.
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
      case ColorEnum.dimWhite:
        return {'r': 20, 'g': 20, 'b': 20};
    }
  }
}

/// Flexible LED color representation supporting both [ColorEnum] presets
/// and custom RGB values.
class LedColor {
  final ColorEnum? colorEnum;
  final int? r;
  final int? g;
  final int? b;

  const LedColor({this.colorEnum, this.r, this.g, this.b});

  /// Returns the resolved RGB values as a map.
  /// Priority: [colorEnum] > manual [r]/[g]/[b] > default blue.
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